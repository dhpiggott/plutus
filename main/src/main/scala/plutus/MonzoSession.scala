package plutus

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
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

import java.lang.Runtime
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

lazy val monzoApiUri: Uri = uri"https://api.monzo.com"

// Shared Monzo session scaffolding for every command that talks to the API:
// load state, decide whether Strong Customer Authentication is required for
// the window, build the HTTP client (whose trace-level logger redacts the
// Authorization header — http4s's default — but prints full bodies, and the
// token-exchange response body carries the tokens, hence trace only),
// rotate/obtain an access token, and run `use` against the authenticated API.
// `use` returns the State to persist alongside its own result, and the rotated
// refresh token is saved even if `use` fails, so it isn't lost. `now` is the
// run's instant rather than one read here, so the token arithmetic below and
// whatever the command goes on to stamp agree on when the run happened.
def withMonzoApi[A](
    since: Option[Instant],
    now: Instant
)(
    use: (monzo.Api[IO], State) => IO[(state: State, result: A)]
)(using verbosity: Verbosity): IO[A] = for
  maybeState <- loadState()
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
      // minOption, not min: a state can hold tokens and no bookmarks at all —
      // accessToken writes one on first authorisation, and withMonzoApi's
      // onError persists it if the fetch then fails — and `min` on an empty
      // collection throws with no message. With no bookmark and no --since
      // there is no window, so nothing is fetched and no authentication is
      // needed for one; the warning below says as much.
      val windowStart = since.orElse:
        state.lastTransactions.values
          .map:
            _.created.value.asInstant
          .minOption
      windowStart
        .exists:
          _.isBefore:
            now
              .minus:
                Period.ofDays:
                  90
              .plus:
                leeway
  // The state that reaches here with tokens but no bookmarks fetches nothing
  // at all (listAllTransactions keeps only the accounts that have one), which
  // would otherwise look like a successful run against a quiet account.
  _ <- IO.whenA(
    since.isEmpty && checkedState.exists(_.lastTransactions.isEmpty)
  ):
    warn:
      "No --since given and no last transaction recorded for any account, so there is no window to fetch; specify --since."
  result <- EmberClientBuilder
    .default[IO]
    .build
    .map:
      org.http4s.client.middleware.Logger.colored(
        logHeaders = true,
        logBody = true,
        logAction = Some:
          trace(_)
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
            use(monzoApi, state)
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
  "Settings > Security > Manage apps > Refresh permissions"

lazy val callbackPort: Port = port"8080"

lazy val redirectUri: monzo.RedirectUri = monzo.RedirectUri:
  s"http://localhost:$callbackPort/oauth/callback"

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
          trace(_)
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

lazy val monzoAuthUri: Uri = uri"https://auth.monzo.com"

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
