package plutus

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import com.monovore.decline.*
import com.monovore.decline.time.*
import cue4s.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.implicits.*
import smithy4s.*
import smithy4s.http4s.*
import smithy4s.json.*
import smithy4s.time.Timestamp
import smithy4s.xml.*

import java.lang.Runtime
import java.nio.file.FileAlreadyExistsException
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

lazy val monzoOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "monzo",
  help = "Monzo commands."
):
  exportTransactionsOpts

lazy val monzoAuthUri: Uri = uri"https://auth.monzo.com"

lazy val callbackPort: Port = port"8080"

lazy val redirectUri: monzo.RedirectUri = monzo.RedirectUri:
  s"http://localhost:$callbackPort/oauth/callback"

lazy val monzoApiUri: Uri = uri"https://api.monzo.com"

lazy val exportTransactionsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "export-transactions",
  help = "Export Monzo transactions to OFX format."
):
  (
    verbosityOpts,
    sinceOpts,
    beforeOpts,
    outputOpts,
    dryRunOpts
  ).tupled.map: (verbosity, since, before, output, dryRun) =>
    exportTransactions(
      since,
      before,
      output,
      dryRun
    )(using verbosity)

lazy val sinceOpts: Opts[Option[Instant]] =
  Opts
    .option[Instant](
      "since",
      help =
        "Timestamp to export transactions from. If not specified defaults to the last recorded transaction ID for each account, unless there is no last recorded transaction for that account, in which case no transactions will be exported for it."
    )
    .orNone

lazy val beforeOpts: Opts[Option[Instant]] =
  Opts
    .option[Instant](
      "before",
      help =
        "Timestamp to export transactions to. If not specified defaults to now."
    )
    .orNone

lazy val outputOpts: Opts[fs2.io.file.Path] =
  Opts
    .option[java.nio.file.Path](
      "output",
      help =
        "Path to write OFX file to. If not specified defaults to monzo.ofx in the current directory."
    )
    .map:
      fs2.io.file.Path.fromNioPath
    .orElse:
      Opts:
        fs2.io.file.Path:
          "monzo.ofx"

lazy val dryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help = "Don't update state-file's last-transactions bookmarks."
    )
    .orFalse

def exportTransactions(
    since: Option[Instant],
    before: Option[Instant],
    output: fs2.io.file.Path,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  withMonzoApi(since): (monzoApi, state, now) =>
    exportTransactions(
      monzoApi,
      state,
      since,
      before = before.getOrElse(now),
      output,
      dryRun
    ).map: updatedState =>
      (state = updatedState, result = ())

// Shared Monzo session scaffolding for both export and import: load state,
// decide whether Strong Customer Authentication is required for the window,
// build the HTTP client (whose trace-level logger prints full request and
// response headers and bodies — including the bearer token, hence trace only),
// rotate/obtain an access token, and run `use` against the authenticated API.
// `use` returns the State to persist — export advances bookmarks, import leaves
// it untouched — alongside its own result, and the rotated refresh token is
// saved even if `use` fails, so it isn't lost.
def withMonzoApi[A](
    since: Option[Instant]
)(
    use: (monzo.Api[IO], State, Instant) => IO[(state: State, result: A)]
)(using verbosity: Verbosity): IO[A] = for
  maybeState <- loadState()
  now <- Clock[IO].realTime.map: finiteDuration =>
    Instant.ofEpochMilli:
      finiteDuration.toMillis
  // Remind (and, if confirmed, record) before the computed refresh-token expiry
  // passes, while there's still a working refresh token to extend in the app.
  checkedState <- maybeState.traverse:
    warnIfRefreshTokenNearExpiry(_, now)
  // From https://docs.monzo.com/?shell#list-transactions:
  //
  // Strong Customer Authentication
  //
  // After a user has authenticated, your client can fetch all of their
  // transactions, and after 5 minutes, it can only sync the last 90 days of
  // transactions. If you need the user’s entire transaction history, you
  // should consider fetching and storing it right after authentication.
  requireStrongCustomerAuthentication = checkedState match
    case None =>
      // n/a - there's no refresh token anyway, so we'll need to authenticate
      // for the first time, and that will happen regardless of whether we
      // explicitly request it with requireStrongCustomerAuthentication.
      true

    case Some(state) =>
      val leeway = Duration.ofSeconds:
        10
      since
        .getOrElse:
          state.lastTransactions.values
            .map:
              _.created.value.asInstant
            .min
        .isBefore:
          now
            .minus:
              Period.ofDays:
                90
            .plus:
              leeway
  result <- EmberClientBuilder
    .default[IO]
    .build
    .map:
      org.http4s.client.middleware.Logger.colored(
        logHeaders = true,
        logBody = true,
        logAction = Some:
          trace
      )
    .use: client =>
      for
        (state, accessToken) <- accessToken(
          client,
          checkedState,
          now,
          requireStrongCustomerAuthentication
        )
        used <- SimpleRestJsonBuilder:
          monzo.Api
        .client:
          client
        .uri:
          monzoApiUri
        .middleware:
          BearerAuthMiddleware:
            accessToken
        .resource
          .use: monzoApi =>
            use(monzoApi, state, now)
          .onError:
            // Ensure the refreshed token isn't lost if `use` fails after
            // accessToken() has already rotated it.
            case _ => saveState(state)
        _ <- saveState(used.state)
      yield used.result
yield result

def loadState()(using verbosity: Verbosity): IO[Option[State]] = for
  maybeBytes <- Keychain.load:
    stateKeychainAccount
  maybeState = maybeBytes.flatMap: bytes =>
    Json
      .read[State]:
        Blob:
          bytes
      .toOption
  _ <-
    if maybeState.isDefined then
      info:
        "Loaded state from Keychain."
    else
      warn:
        "Couldn't load state from Keychain."
yield maybeState

def warnIfRefreshTokenNearExpiry(
    state: State,
    now: Instant
)(using verbosity: Verbosity): IO[State] =
  val expiresAt = inferredRefreshTokenExpiry:
    state
  val expiryDate = expiresAt.atOffset(ZoneOffset.UTC).toLocalDate
  val warnFrom = expiresAt.minus:
    refreshTokenExpiryWarningWindow
  if now.isBefore(warnFrom) then
    verbose(
      s"Monzo access expires $expiryDate (90 days from when it was last granted)."
    ).as(state)
  else
    val daysRemaining = ChronoUnit.DAYS.between(now, expiresAt)
    for
      _ <- warn:
        if daysRemaining < 0 then
          s"Monzo access expired on $expiryDate (unless you've since extended it in-app). Extend it in the Monzo app under $monzoRefreshPermissionsPath, otherwise the next run may require full re-authentication."
        else
          s"Monzo access expires in $daysRemaining day(s), on $expiryDate. Extend it in the Monzo app under $monzoRefreshPermissionsPath."
      didRefresh <- IO.blocking:
        Prompts.sync.use:
          _.confirm(
            s"Did you just refresh permissions in the Monzo app ($monzoRefreshPermissionsPath)?"
          ).getOrRaise
      updatedState <-
        if didRefresh then
          // Anchor the extension on now, not the existing deadline: after a
          // refresh the Manage apps screen shows the session valid for 90 days
          // from that moment (it resets the lifetime, it doesn't stack onto the
          // remaining time), so an early refresh is exactly now + 90 days.
          // expiresAt + 90 would over-count by however long was left.
          val extendedExpiry = RefreshTokenExpiresAt:
            now
              .plus:
                refreshTokenTtl
              .asSmithyTimestamp
          val extended = state.copy(
            refreshTokenExpiresAt = Some(extendedExpiry)
          )
          saveState(extended).as(extended)
        else
          info("No refresh recorded; you'll be reminded again next run.")
            .as(state)
    yield updatedState

private val stateKeychainAccount = "plutus"

def saveState(state: State)(using verbosity: Verbosity): IO[Unit] = for
  _ <- Keychain.save(
    account = stateKeychainAccount,
    bytes = Json
      .writeBlob:
        state
      .toArray
  )
  _ <- info:
    "Saved state to Keychain."
yield ()

extension (timestamp: Timestamp)
  def asInstant: Instant =
    Instant.ofEpochSecond(timestamp.epochSecond, timestamp.nano)

extension (instant: Instant)
  def asSmithyTimestamp: Timestamp =
    Timestamp(instant.getEpochSecond, instant.getNano)

def accessToken(
    client: Client[IO],
    maybeState: Option[State],
    now: Instant,
    requireStrongCustomerAuthentication: Boolean
)(using verbosity: Verbosity): IO[(State, monzo.AccessToken)] =
  TokenExchangeBuilder:
    monzo.TokenApi
  .client:
    client
  .uri:
    monzoApiUri
  .resource
    .use: monzoTokenApi =>
      for
        (clientId, clientSecret) <- maybeState match
          case None =>
            IO.blocking:
              Prompts.sync.use: prompts =>
                val clientId =
                  prompts.text("Enter your Monzo client ID").getOrRaise
                val clientSecret =
                  prompts.password("Enter your Monzo client secret").getOrRaise
                (
                  monzo.ClientId(clientId),
                  monzo.ClientSecret(clientSecret.raw)
                )

          case Some(state) =>
            IO.pure(state.clientId, state.clientSecret)
        exchangeAuthCodeAndCreateOrUpdateState =
          for
            createAccessTokenOutput <- exchangeAuthCode(
              monzoTokenApi,
              clientId,
              clientSecret
            )
            authorizedAt = AuthorizedAt:
              now.asSmithyTimestamp
            refreshToken = createAccessTokenOutput.refreshToken
            inferredExpiry = RefreshTokenExpiresAt:
              now
                .plus:
                  refreshTokenTtl
                .asSmithyTimestamp
            state = maybeState match
              case None =>
                State(
                  clientId,
                  clientSecret,
                  authorizedAt,
                  refreshToken,
                  refreshTokenExpiresAt = Some(inferredExpiry),
                  lastTransactions = Map.empty
                )

              case Some(state) =>
                state.copy(
                  authorizedAt = authorizedAt,
                  refreshToken = refreshToken,
                  refreshTokenExpiresAt = Some(inferredExpiry)
                )
          yield (state, createAccessTokenOutput.accessToken)
        (state, accessToken) <- maybeState match
          case None =>
            warn:
              "No previous state, requesting authorization…"
            *>
              exchangeAuthCodeAndCreateOrUpdateState

          case Some(state)
              if requireStrongCustomerAuthentication && !lessThanFiveMinutesAgo(
                state.authorizedAt,
                now
              ) =>
            warn:
              "Strong authentication required, requesting authorization…"
            *>
              exchangeAuthCodeAndCreateOrUpdateState

          case Some(state) =>
            (for
              _ <- info:
                "Existing refresh token found, exchanging for tokens…"
              createAccessTokenOutput <- monzoTokenApi.createAccessToken(
                grantType = monzo.GrantType.REFRESH_TOKEN,
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = Some(state.refreshToken)
              )
              updatedState = state.copy(
                refreshToken = createAccessTokenOutput.refreshToken
              )
            yield (updatedState, createAccessTokenOutput.accessToken))
              // The refresh token may have expired, in which case we should
              // request authorization again.
              .recoverWith: throwable =>
                warn:
                  s"Couldn't refresh tokens, requesting re-authorization… (received error: ${throwable.getMessage})"
                *>
                  exchangeAuthCodeAndCreateOrUpdateState
      yield (state, accessToken)

def lessThanFiveMinutesAgo(
    authorizedAt: AuthorizedAt,
    now: Instant
): Boolean =
  val fiveMinutesAgo = now.minus:
    Duration.ofMinutes:
      5
  val leeway = Duration.ofSeconds:
    10
  authorizedAt.value.asInstant
    .plus:
      leeway
    .isAfter:
      fiveMinutesAgo

def inferredRefreshTokenExpiry(state: State): Instant =
  state.refreshTokenExpiresAt
    .map:
      _.value.asInstant
    .getOrElse:
      state.authorizedAt.value.asInstant
        .plus:
          refreshTokenTtl

// Monzo's token response carries no refresh-token expiry, so we compute one
// from when access was last granted (recorded in State at authorization) plus
// the 90-day lifetime Monzo states on its Manage apps screen, persisting it so
// it survives across runs. A user can extend access from the Monzo app, which
// we can't observe — hence the confirm-then-record handshake in
// warnIfRefreshTokenNearExpiry.
val refreshTokenTtl: Period = Period.ofDays:
  90

// Start reminding at the half-life, leaving ~45 days to act before access
// would lapse.
val refreshTokenExpiryWarningWindow: Period = Period.ofDays:
  45

// Where the extend-access feature lives in the Monzo app, so the reminder can
// point straight at it rather than leaving the user to hunt.
val monzoRefreshPermissionsPath: String =
  "Settings > Privacy & security > Manage apps > Refresh permissions"

def exchangeAuthCode(
    monzoTokenApi: monzo.TokenApi[IO],
    clientId: monzo.ClientId,
    clientSecret: monzo.ClientSecret
)(using verbosity: Verbosity): IO[monzo.CreateAccessTokenOutput] = for
  authorizationCodeAndStateDeferred <- Deferred[
    IO,
    (monzo.AuthorizationCode, monzo.State)
  ]
  createAccessTokenOutput <- EmberServerBuilder
    .default[IO]
    .withPort(callbackPort)
    .withHttpApp:
      org.http4s.server.middleware.Logger.httpApp[IO](
        logHeaders = true,
        logBody = true,
        logAction = Some:
          trace
      )(
        HttpRoutes
          .of[IO]:
            case GET -> Root / "oauth" / "callback" :?
                AuthorizationCodeQueryParamMatcher(code) +&
                StateQueryParamMatcher(state) =>
              authorizationCodeAndStateDeferred.complete(code -> state) *>
                (verbose:
                  "Received auth code.") *>
                // window.close() is best-effort: browsers only honour it for
                // windows opened by script, or windows that haven't been
                // navigated (history length 1). Monzo's redirect opens the
                // callback in a fresh tab, so its history length is 1 and the
                // close succeeds — but that's not guaranteed across browsers,
                // hence the visible fallback.
                Ok:
                  """<!DOCTYPE html>
                    |<html lang="en">
                    |<head>
                    |<meta charset="utf-8">
                    |<title>Plutus — authorization complete</title>
                    |</head>
                    |<body>
                    |<p>Authorization complete — you can close this tab and return to Plutus.</p>
                    |<script>window.close()</script>
                    |</body>
                    |</html>
                    |""".stripMargin
                .map:
                  _.withContentType:
                    `Content-Type`:
                      MediaType.text.html
          .orNotFound
      )
    .withShutdownTimeout:
      0.seconds
    .build
    .use: _ =>
      for
        _ <- verbose:
          "Requesting authorization…"
        generatedState <- requestAuthorization(clientId)
        (authorizationCode, receivedState) <-
          authorizationCodeAndStateDeferred.get
        _ <- IO.raiseUnless(generatedState == receivedState):
          Error:
            s"Authorization failed: OAuth state mismatch — expected $generatedState but received $receivedState."
        _ <- verbose:
          "Exchanging auth code for tokens…"
        createAccessTokenOutput <- monzoTokenApi.createAccessToken(
          grantType = monzo.GrantType.AUTHORIZATION_CODE,
          clientId = clientId,
          clientSecret = clientSecret,
          redirectUri = Some(redirectUri),
          code = Some(authorizationCode)
        )
        scaComplete <- IO.blocking:
          Prompts.sync.use:
            _.confirm(
              "Have you approved the request in your Monzo app?"
            ).getOrRaise
        _ <- IO.raiseUnless(scaComplete):
          Error:
            "Monzo app approval not completed."
      yield createAccessTokenOutput
yield createAccessTokenOutput

given authorizationCodeQueryParamDecoder
    : QueryParamDecoder[monzo.AuthorizationCode] =
  QueryParamDecoder[String].map:
    monzo.AuthorizationCode(_)

given stateQueryParamDecoder: QueryParamDecoder[monzo.State] =
  QueryParamDecoder[String].map:
    monzo.State(_)

object AuthorizationCodeQueryParamMatcher
    extends QueryParamDecoderMatcher[monzo.AuthorizationCode]("code")

object StateQueryParamMatcher
    extends QueryParamDecoderMatcher[monzo.State]("state")

def requestAuthorization(
    clientId: monzo.ClientId
): IO[monzo.State] = for
  state <- IO.randomUUID.map: uuid =>
    monzo.State:
      uuid.toString
  _ <- IO:
    Runtime
      .getRuntime()
      .exec:
        Array(
          "open",
          monzoAuthUri
            .withQueryParams:
              Map(
                "client_id" -> clientId.value,
                "redirect_uri" -> redirectUri.value,
                "response_type" -> "code",
                "state" -> state.value
              )
            .renderString
        )
yield state

object BearerAuthMiddleware:
  def apply(accessToken: monzo.AccessToken): ClientEndpointMiddleware[IO] =
    new ClientEndpointMiddleware.Simple[IO]:
      override def prepareWithHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): Client[IO] => Client[IO] =
        if serviceHints.has[smithy.api.HttpBearerAuth] && !endpointHints
            .get[smithy.api.Auth]
            .exists:
              _.value.isEmpty
        then
          client =>
            Client[IO]: request =>
              client.run:
                request.withHeaders:
                  Authorization:
                    Credentials.Token(AuthScheme.Bearer, accessToken.value)
        else identity

def exportTransactions(
    monzoApi: monzo.Api[IO],
    state: State,
    since: Option[Instant],
    before: Instant,
    output: fs2.io.file.Path,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[State] = for
  (accountsAndTransactions, potAccountsAndTransactions) <- listAllTransactions(
    monzoApi,
    state,
    since,
    before
  )
  exportedAccountsAndTransactions = accountsAndTransactions ++
    potAccountsAndTransactions
  materialAccountIdsAndTransactions = exportedAccountsAndTransactions.map:
    (account, transactions) => account.id -> materialTransactions(transactions)
  _ <- writeOfx(
    toOfx:
      materialAccountIdsAndTransactions
    ,
    output,
    overwrite = since.isDefined
  ).adaptError:
    case _: FileAlreadyExistsException =>
      Error:
        s"Cannot overwrite existing output in from-last-transactions mode. Delete $output or specify --since."
  updatedState =
    if dryRun then state
    else
      state.copy(
        lastTransactions = state.lastTransactions ++
          exportedAccountsAndTransactions
            .map: (account, transactions) =>
              account.id -> transactions.lastOption
            .collect:
              case (accountId, Some(lastTransaction)) =>
                accountId -> LastTransaction(
                  lastTransaction.id,
                  lastTransaction.created
                )
      )
yield updatedState

// Skip £0 active-card checks and declined authorisations: neither is real
// spend, so export leaves them out of the OFX and import leaves them out of the
// book. Shared so both paths filter identically.
def materialTransactions(
    transactions: List[monzo.Transaction]
): List[monzo.Transaction] =
  transactions.filterNot: transaction =>
    transaction.amount.value == 0 || transaction.declineReason.isDefined

// List every main account's transactions, then the pot accounts discovered
// from their metadata (plus any already bookmarked in state). Shared by export
// and import; returns the main and pot results separately because pots are
// discovered from the main accounts' metadata, so the two are produced in
// distinct phases. The verbose entity dump lives here so both commands emit it.
def listAllTransactions(
    monzoApi: monzo.Api[IO],
    state: State,
    since: Option[Instant],
    before: Instant
)(using
    verbosity: Verbosity
): IO[
  (
      List[(monzo.Account, List[monzo.Transaction])],
      List[(monzo.Account, List[monzo.Transaction])]
  )
] = for
  _ <- info:
    "Listing accounts…"
  accounts <- monzoApi
    .listAccounts()
    .map:
      _.accounts
  accountsAndSince = since match
    case Some(since) =>
      accounts.map: account =>
        (account, ListTransactionsSince.Timestamp(since))

    case None =>
      accounts
        .map: account =>
          (account, state.lastTransactions.get(account.id))
        .collect:
          case (account, Some(since)) =>
            (account, ListTransactionsSince.IdAndTimestamp(since))
  _ <- info:
    "Listing transactions for accounts…"
  accountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    accountsAndSince,
    before
  )
  potAccountsAndSince = discoverPotAccounts(
    state,
    accounts,
    accountsAndTransactions,
    since
  )
  _ <- (IO.whenA(potAccountsAndSince.nonEmpty)):
    info:
      "Listing transactions for pot accounts…"
  potAccountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    potAccountsAndSince,
    before
  )
  newPotAccountIds = discoveredPotAccountIds(accountsAndTransactions)
    .diff:
      accounts
        .map:
          _.id
        .toSet
        .union:
          potAccountsAndSince
            .map: (account, _) =>
              account.id
            .toSet
  _ <- (IO.whenA(newPotAccountIds.nonEmpty)):
    warn:
      s"Found pot accounts with no last recorded transaction; specify --since to export their transactions: ${newPotAccountIds.toList.map(_.value).sorted.mkString(", ")}"
  _ <- (IO.whenA:
    verbosity.ordinal >= Verbosity.VERBOSE.ordinal
  ):
    IO.println:
      Json.writeDocumentAsPrettyString:
        Document.array:
          (accountsAndTransactions ++ potAccountsAndTransactions).map:
            (account, transactions) =>
              Document.obj(
                "account" -> Document.encode:
                  account
                ,
                "transactions" -> Document.array:
                  transactions.map:
                    Document.encode(_)
              )
yield (accountsAndTransactions, potAccountsAndTransactions)

// Fetch every transaction (main accounts and discovered pots) for the window,
// grouped by account so import can post each account's transactions to the
// asset account its type maps to (see AssetAccounts). Used by import, which
// dedups on the Monzo ID rather than bookmarks, so it leaves the persisted
// state untouched. Returns `now` too, so import can stamp enter_date with the
// session's single clock read rather than taking a second one.
def fetchTransactionsByAccount(
    since: Option[Instant],
    before: Option[Instant]
)(using
    verbosity: Verbosity
): IO[
  (now: Instant, byAccount: List[(monzo.Account, List[monzo.Transaction])])
] =
  withMonzoApi(since): (monzoApi, state, now) =>
    listAllTransactions(
      monzoApi,
      state,
      since,
      before = before.getOrElse(now)
    ).map: (accountsAndTransactions, potAccountsAndTransactions) =>
      val byAccount = accountsAndTransactions ++ potAccountsAndTransactions
      (state = state, result = (now = now, byAccount = byAccount))

def listTransactionsForAccounts(
    monzoApi: monzo.Api[IO],
    accountsAndSince: List[(monzo.Account, ListTransactionsSince)],
    before: Instant
): IO[List[(monzo.Account, List[monzo.Transaction])]] =
  accountsAndSince
    .parTraverse: (account, since) =>
      listTransactions(
        monzoApi,
        accountId = account.id,
        since,
        before
      ).map: transactions =>
        account -> transactions
    .map:
      _.filter: (_, transactions) =>
        transactions.nonEmpty

// Pots are backed by account objects that /accounts doesn't list. Their IDs
// only surface as pot_account_id in the metadata of pot-transfer transactions,
// but passing one to /transactions returns the pot's own statement — including
// interest credits, which appear nowhere else. /pots is no alternative source:
// its live responses carry many undocumented fields, but none reference the
// pot's backing account — current_account_id is the owning account, and the
// pot id shares only its creation-timestamp prefix with the backing-account
// ID, so one can't be derived from the other. Once a pot has a bookmark in
// the state store it's recognisable there as a key /accounts doesn't return,
// so it keeps syncing even when no transfer falls in the export window. (That
// inference assumes /accounts never stops listing a main account — it keeps
// returning closed ones, so in practice only pots can be in state but not in
// /accounts.) Note that SCA verification doesn't extend to pot accounts: a
// window reaching back more than 90 days fails with
// forbidden.verification_required even right after authorisation, when main
// accounts would return their full history.
def discoverPotAccounts(
    state: State,
    accounts: List[monzo.Account],
    accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])],
    since: Option[Instant]
): List[(monzo.Account, ListTransactionsSince)] =
  val mainAccountIds = accounts
    .map:
      _.id
    .toSet
  val bookmarked = state.lastTransactions.keySet.diff:
    mainAccountIds
  since match
    case Some(since) =>
      val discovered = discoveredPotAccountIds(accountsAndTransactions).diff:
        mainAccountIds
      bookmarked
        .union:
          discovered
        .toList
        .sortBy:
          _.value
        .map: accountId =>
          monzo.Account(accountId) -> ListTransactionsSince.Timestamp(since)

    case None =>
      bookmarked.toList
        .sortBy:
          _.value
        .map: accountId =>
          monzo.Account(accountId) -> ListTransactionsSince.IdAndTimestamp(
            state.lastTransactions(accountId)
          )

def discoveredPotAccountIds(
    accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])]
): Set[monzo.AccountId] =
  accountsAndTransactions
    .flatMap: (_, transactions) =>
      transactions.flatMap:
        potAccountId
    .toSet

def potAccountId(transaction: monzo.Transaction): Option[monzo.AccountId] =
  transaction.metadata
    .flatMap:
      _.get("pot_account_id")
    .collect:
      case Document.DString(value) =>
        monzo.AccountId(value)

enum ListTransactionsSince:
  case Timestamp(instant: Instant)
  case IdAndTimestamp(lastTransaction: LastTransaction)

def listTransactions(
    monzoApi: monzo.Api[IO],
    accountId: monzo.AccountId,
    since: ListTransactionsSince,
    before: Instant
): IO[List[monzo.Transaction]] =
  val beforeForThisPage =
    val sinceInstant = since match
      case ListTransactionsSince.Timestamp(instant) =>
        instant

      case ListTransactionsSince.IdAndTimestamp(lastTransaction) =>
        lastTransaction.created.value.asInstant
    // See
    // https://community.monzo.com/t/changes-when-listing-with-our-api/158676.
    val maxPermittedBeforeForThisPage = sinceInstant.plus:
      Duration.ofHours:
        8760
    val mustPaginate = before.isAfter:
      maxPermittedBeforeForThisPage
    if mustPaginate then maxPermittedBeforeForThisPage else before
  for
    thisPage <- monzoApi
      .listTransactions(
        accountId,
        since = Some(monzo.Since(since match
          case ListTransactionsSince.Timestamp(instant) =>
            instant.asSmithyTimestamp.formatDateTime

          case ListTransactionsSince.IdAndTimestamp(lastTransaction) =>
            lastTransaction.id.value)),
        before = Some:
          monzo.Before:
            beforeForThisPage.asSmithyTimestamp
        ,
        limit = Some:
          monzo.Limit:
            100
      )
      .map(_.transactions)
    otherPages <- thisPage.lastOption match
      case None =>
        val haveRequestedAllPages = beforeForThisPage == before
        if haveRequestedAllPages
        then
          IO.pure:
            List.empty
        else
          listTransactions(
            monzoApi,
            accountId,
            since = ListTransactionsSince.Timestamp:
              beforeForThisPage
            ,
            before
          )

      case Some(transaction) =>
        listTransactions(
          monzoApi,
          accountId,
          since = ListTransactionsSince.IdAndTimestamp:
            LastTransaction(transaction.id, transaction.created)
          ,
          before
        )
  yield thisPage ++ otherPages

val ofxDateTimeFormatter: DateTimeFormatter =
  DateTimeFormatter.ofPattern:
    "yyyyMMddHHmmss.SSS"

def toOfx(
    accountsIdsAndTransactions: List[
      (monzo.AccountId, List[monzo.Transaction])
    ]
): ofx.Ofx =
  ofx.Ofx:
    ofx.BankMessageSetResponse:
      accountsIdsAndTransactions.map: (accountId, transactions) =>
        ofx.StatementTransactionsResponse:
          ofx.StatementResponse(
            ofx.BankAccountFrom:
              ofx.AccountId:
                accountId.value
            ,
            ofx.BankTransactionList:
              transactions.map: transaction =>
                ofx.StatementTransaction(
                  datePosted = ofx.Datetime:
                    transaction.created.value.asInstant
                      .atOffset:
                        ZoneOffset.UTC
                      .format:
                        ofxDateTimeFormatter
                  ,
                  transactionAmount = ofx.TransactionAmount:
                    BigDecimal:
                      transaction.amount.value
                    / 100
                  ,
                  financialInstitutionId = ofx.FinancialInstitutionId:
                    transaction.id.value
                  ,
                  name = ofx.Name(
                    transaction.merchant
                      .map:
                        _.name
                      .orElse:
                        transaction.counterparty.name
                      .map:
                        _.value
                      .getOrElse:
                        transaction.description.value
                  ),
                  memo = Some:
                    ofx.Memo:
                      transaction.notes.value
                )
          )

def writeOfx(
    content: ofx.Ofx,
    output: fs2.io.file.Path,
    overwrite: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  ((fs2.Stream:
    "ENCODING:UTF-8\n"
  )
  ++
    XmlDocument.documentEventifier
      .eventify:
        XmlDocument.Encoder
          .fromSchema:
            ofx.Ofx.schema
          .encode:
            content
      // GnuCash imports OFX through libofx, an SGML parser that doesn't
      // recognise the &apos; / &quot; entities smithy4s escapes apostrophes
      // and quotes to; both are valid literally in element text, so undo the
      // escapes.
      .map:
        case fs2.data.xml.XmlEvent.XmlString(s, isCDATA) =>
          fs2.data.xml.XmlEvent.XmlString(
            s.replace("&apos;", "'").replace("&quot;", "\""),
            isCDATA
          )
        case event => event
      .through:
        fs2.data.xml.render.prettyPrint(width = 60, indent = 4)
  )
    .through:
      fs2.io.file
        .Files[IO]
        .writeUtf8(
          output,
          if overwrite then fs2.io.file.Flags.Write
          else
            fs2.io.file.Flags(
              fs2.io.file.Flag.Write,
              fs2.io.file.Flag.CreateNew
            )
        )
    .compile
    .drain *>
    info:
      s"Wrote OFX to $output."
