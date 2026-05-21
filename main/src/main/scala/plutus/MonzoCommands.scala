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
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
  (verbosityOpts, sinceOpts, beforeOpts, outputOpts, dryRunOpts).tupled.map:
    (verbosity, since, before, output, dryRun) =>
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
)(using verbosity: Verbosity): IO[Unit] = for
  maybeState <- loadState()
  now <- Clock[IO].realTime.map: finiteDuration =>
    Instant.ofEpochMilli:
      finiteDuration.toMillis
  // From https://docs.monzo.com/?shell#list-transactions:
  //
  // Strong Customer Authentication
  //
  // After a user has authenticated, your client can fetch all of their
  // transactions, and after 5 minutes, it can only sync the last 90 days of
  // transactions. If you need the user’s entire transaction history, you
  // should consider fetching and storing it right after authentication.
  requireStrongCustomerAuthentication = maybeState match
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
  updatedState <- EmberClientBuilder
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
          maybeState,
          now,
          requireStrongCustomerAuthentication
        )
        updatedState <- SimpleRestJsonBuilder:
          monzo.Api
        .client:
          client
        .uri:
          monzoApiUri
        .middleware:
          BearerAuthMiddleware:
            accessToken
        .resource
          .use:
            exportTransactions(
              _,
              state,
              since,
              before = before.getOrElse(now),
              output,
              dryRun
            )
          .onError:
            // Ensure the refreshed token isn't lost if export fails after
            // accessToken() has already rotated it.
            case _ => saveState(state)
      yield updatedState
  _ <- saveState(updatedState)
yield ()

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
            state = maybeState match
              case None =>
                State(
                  clientId,
                  clientSecret,
                  authorizedAt,
                  refreshToken,
                  lastTransactions = Map.empty
                )

              case Some(state) =>
                state.copy(
                  authorizedAt = authorizedAt,
                  refreshToken = refreshToken
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
  accountsAndTransactions <- accountsAndSince
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
  _ <- (IO.whenA:
    verbosity.ordinal >= Verbosity.VERBOSE.ordinal
  ):
    IO.println:
      Json.writeDocumentAsPrettyString:
        Document.array:
          accountsAndTransactions.map: (account, transactions) =>
            Document.obj(
              "account" -> Document.encode:
                account
              ,
              "transactions" -> Document.array:
                transactions.map:
                  Document.encode(_)
            )
  materialAccountIdsAndTransactions = accountsAndTransactions.map:
    (account, transactions) =>
      account.id -> transactions.filterNot: transaction =>
        // Active card check.
        transaction.amount.value == 0 ||
          // What it says.
          transaction.declineReason.isDefined
  _ <- writeOfx(
    toOfx:
      materialAccountIdsAndTransactions
    ,
    output,
    overwrite = since.isDefined
  ).adaptError:
    case _: java.nio.file.FileAlreadyExistsException =>
      Error:
        s"Cannot overwrite existing output in from-last-transactions mode. Delete $output or specify --since."
  updatedState =
    if dryRun then state
    else
      state.copy(
        lastTransactions = state.lastTransactions ++
          accountsAndTransactions
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
