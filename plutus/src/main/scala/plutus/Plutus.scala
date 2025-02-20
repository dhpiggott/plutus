package plutus

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import com.monovore.decline.time.*
import epollcat.EpollApp
import org.http4s.*
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.*
import org.http4s.implicits.*
import smithy.api.TimestampFormat
import smithy4s.*
import smithy4s.http4s.*
import smithy4s.json.*
import smithy4s.xml.*

import java.lang.Runtime
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*
import scala.language.experimental.betterFors

object Plutus
    extends CommandIOApp(
      name = "plutus",
      header = "Monzo OFX exporter."
    )
    with EpollApp:

  override def main: Opts[IO[ExitCode]] =
    (verbosityOpt, sinceOpt, beforeOpt, outputOpt)
      .mapN: (verbosity, since, before, output) =>
        program(
          verbosity,
          since,
          before,
          output = output
            .map:
              fs2.io.file.Path.fromNioPath(_)
            .getOrElse(fs2.io.file.Path("monzo.ofx"))
        )
          .as(ExitCode.Success)

  private lazy val verbosityOpt: Opts[Verbosity] =
    silentOpt orElse verboseOpt orElse debugOpt withDefault Verbosity.DEFAULT

  private lazy val silentOpt: Opts[Verbosity] =
    Opts
      .flag("silent", help = "Don't log anything.")
      .as(Verbosity.SILENT)

  private lazy val verboseOpt: Opts[Verbosity] =
    Opts
      .flag(
        "verbose",
        help =
          "Log decoded account and transaction entities. This includes default logging."
      )
      .as(Verbosity.VERBOSE)

  private lazy val debugOpt: Opts[Verbosity] =
    Opts
      .flag(
        "debug",
        help =
          "Log raw HTTP requests and responses. This includes --verbose logging."
      )
      .as(Verbosity.DEBUG)

  private enum Verbosity:
    case SILENT
    case DEFAULT
    case VERBOSE
    case DEBUG

  private lazy val sinceOpt: Opts[Option[Instant]] =
    Opts
      .option[Instant](
        "since",
        help =
          "Timestamp to export transactions from. If not specified defaults to the last recorded transaction ID for each account, unless there is no last recorded transaction for that account, in which case no transactions will be exported for it."
      )
      .orNone

  private lazy val beforeOpt: Opts[Option[Instant]] =
    Opts
      .option[Instant](
        "before",
        help =
          "Timestamp to export transactions to. If not specified defaults to now."
      )
      .orNone

  private lazy val outputOpt: Opts[Option[Path]] =
    Opts
      .option[Path](
        "output",
        help =
          "Path to write OFX file to. If not specified defaults to monzo.ofx in the current directory."
      )
      .orNone

  private def program(
      verbosity: Verbosity,
      since: Option[Instant],
      before: Option[Instant],
      output: fs2.io.file.Path
  ): IO[Unit] = for
    _ <- fs2.io.file
      .Files[IO]
      .exists(output)
      .map:
        _ && since.isEmpty
      .flatMap:
        IO.raiseWhen(_)(
          Error(
            s"Cannot overwrite existing output in from-last-transactions mode. Delete $output or specify --since."
          )
        )
    maybeState <- loadState(verbosity)
    now <- Clock[IO].realTime.map: finiteDuration =>
      Instant.ofEpochMilli(finiteDuration.toMillis)
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
        val leeway = Duration.ofSeconds(10)
        since
          .getOrElse:
            state.lastTransactions.values
              .map: lastTransaction =>
                Instant.ofEpochSecond(
                  lastTransaction.created.value.epochSecond,
                  lastTransaction.created.value.nano
                )
              .min
          .isBefore(now.minus(Period.ofDays(90)).plus(leeway))
    updatedState <- EmberClientBuilder
      .default[IO]
      .build
      .map:
        if verbosity.ordinal >= Verbosity.DEBUG.ordinal
        then
          org.http4s.client.middleware.Logger.colored(
            logHeaders = true,
            logBody = true
          )
        else identity
      .use: client =>
        for
          (state, accessToken) <- accessToken(
            client,
            maybeState,
            verbosity,
            now,
            requireStrongCustomerAuthentication
          )
          updatedState <- SimpleRestJsonBuilder(monzo.Api)
            .client(client)
            .uri(monzoApiUri)
            .middleware(BearerAuthMiddleware(accessToken.value))
            .resource
            .use:
              exportTransactions(
                _,
                state,
                verbosity,
                since match
                  case None =>
                    ExportTransactionsSince
                      .LastTransactions(state.lastTransactions)

                  case Some(since) =>
                    ExportTransactionsSince.Timestamp(since),
                before = before.getOrElse(now),
                output
              )
        yield updatedState
    _ <- saveState(updatedState, verbosity)
  yield ()

  private lazy val monzoApiUri: Uri = uri"https://api.monzo.com"

  private def accessToken(
      client: Client[IO],
      maybeState: Option[State],
      verbosity: Verbosity,
      now: Instant,
      requireStrongCustomerAuthentication: Boolean
  ): IO[(State, monzo.AccessToken)] =
    TokenExchangeBuilder(monzo.TokenApi)
      .client(client)
      .uri(monzoApiUri)
      .resource
      .use: monzoTokenApi =>
        for
          (clientId, clientSecret) <- maybeState match
            case None =>
              for
                _ <- Console[IO].print("Enter client ID: ")
                clientId <- Console[IO].readLine.map:
                  monzo.ClientId(_)
                _ <- Console[IO].print("Enter client secret: ")
                // TODO: Obfuscate input using
                // https://github.com/hnaderi/scala-readpass?
                // TODO: Encrypt state?
                clientSecret <- Console[IO].readLine.map:
                  monzo.ClientSecret(_)
              yield (clientId, clientSecret)

            case Some(state) =>
              IO.pure(state.clientId, state.clientSecret)
          exchangeAuthCodeAndCreateOrUpdateState = for
            createAccessTokenOutput <- exchangeAuthCode(
              monzoTokenApi,
              verbosity,
              clientId,
              clientSecret
            )
            authorizedAt = AuthorizedAt(
              Timestamp(now.getEpochSecond, now.getNano)
            )
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
              Console[IO]
                .println("No previous state, requesting authorization...")
                .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal) *>
                exchangeAuthCodeAndCreateOrUpdateState

            case Some(state)
                if requireStrongCustomerAuthentication && !lessThanFiveMinutesAgo(
                  state.authorizedAt,
                  now
                ) =>
              Console[IO]
                .println(
                  "Strong authentication required, requesting authorization..."
                )
                .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal) *>
                exchangeAuthCodeAndCreateOrUpdateState

            case Some(state) =>
              for
                _ <- Console[IO]
                  .println(
                    "Existing refresh token found, exchanging for tokens..."
                  )
                  .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)
                createAccessTokenOutput <- monzoTokenApi.createAccessToken(
                  grantType = monzo.GrantType.REFRESH_TOKEN,
                  clientId = clientId,
                  clientSecret = clientSecret,
                  refreshToken = Some(state.refreshToken)
                )
                updatedState = state.copy(
                  refreshToken = createAccessTokenOutput.refreshToken
                )
              yield (updatedState, createAccessTokenOutput.accessToken)
        yield (state, accessToken)

  private def lessThanFiveMinutesAgo(
      authorizedAt: AuthorizedAt,
      now: Instant
  ): Boolean =
    val authorizedAtInstant = Instant.ofEpochSecond(
      authorizedAt.value.epochSecond,
      authorizedAt.value.nano
    )
    val fiveMinutesAgo = now.minus(Duration.ofMinutes(5))
    val leeway = Duration.ofSeconds(10)
    authorizedAtInstant.plus(leeway).isAfter(fiveMinutesAgo)

  private def loadState(verbosity: Verbosity): IO[Option[State]] =
    fs2.io.file
      .Files[IO]
      .readUtf8(stateFilePath)
      .compile
      .string
      .flatMap: state =>
        IO.fromEither(Json.read[State](Blob(state)))
      .option
      .flatTap: maybeState =>
        Console[IO]
          .println(
            if maybeState.isDefined then
              s"Loaded state file from $stateFilePath."
            else s"Couldn't load state file from $stateFilePath."
          )
          .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)

  private def saveState(state: State, verbosity: Verbosity): IO[Unit] =
    fs2
      .Stream(Json.writePrettyString(state))
      .through(
        fs2.io.file
          .Files[IO]
          .writeUtf8(stateFilePath)
      )
      .compile
      .drain *>
      Console[IO]
        .println(s"Saved state file to $stateFilePath.")
        .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)

  private lazy val stateFilePath: fs2.io.file.Path =
    fs2.io.file.Path(System.getProperty("user.home")) /
      "Library" / "Application Support" / "plutus" / "state.json"

  private def exchangeAuthCode(
      monzoTokenApi: monzo.TokenApi[IO],
      verbosity: Verbosity,
      clientId: monzo.ClientId,
      clientSecret: monzo.ClientSecret
  ): IO[monzo.CreateAccessTokenOutput] = for
    authorizationCodeAndStateDeferred <- Deferred[
      IO,
      (monzo.AuthorizationCode, monzo.State)
    ]
    createAccessTokenOutput <- EmberServerBuilder
      .default[IO]
      .withHttpApp(
        (if verbosity.ordinal >= Verbosity.DEBUG.ordinal
         then
           org.http4s.server.middleware.Logger.httpApp[IO](
             logHeaders = true,
             logBody = true
           )
         else identity[HttpApp[IO]]) (
          HttpRoutes
            .of[IO]:
              case GET -> Root / "oauth" / "callback" :?
                  AuthorizationCodeQueryParamMatcher(code) +&
                  StateQueryParamMatcher(state) =>
                authorizationCodeAndStateDeferred.complete(code -> state) *>
                  Console[IO]
                    .println("Received auth code.")
                    .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal) *>
                  Ok("Authorization code received. Return to Plutus.")
            .orNotFound
        )
      )
      .withShutdownTimeout(0.seconds)
      .build
      .use: server =>
        for
          _ <- Console[IO]
            .println("Requesting authorization...")
            .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)
          redirectUri = monzo
            .RedirectUri("http://localhost:8080/oauth/callback")
          generatedState <- requestAuthorization(clientId, redirectUri)
          authorizationCodeAndReceivedState <-
            authorizationCodeAndStateDeferred.get
          (authorizationCode, receivedState) =
            authorizationCodeAndReceivedState
          _ <- IO.raiseUnless(generatedState == receivedState)(
            Error(
              s"generatedState != receivedState ($generatedState != $receivedState)"
            )
          )
          _ <- Console[IO]
            .println("Exchanging auth code for tokens...")
            .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)
          createAccessTokenOutput <- monzoTokenApi.createAccessToken(
            grantType = monzo.GrantType.AUTHORIZATION_CODE,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = Some(redirectUri),
            code = Some(authorizationCode)
          )
          _ <- Console[IO].print(
            "Complete SCA in app, then press enter to continue."
          )
          _ <- Console[IO].readLine
        yield createAccessTokenOutput
  yield createAccessTokenOutput

  private implicit val authorizationCodeQueryParamDecoder
      : QueryParamDecoder[monzo.AuthorizationCode] =
    QueryParamDecoder[String].map:
      monzo.AuthorizationCode(_)

  private implicit val stateQueryParamDecoder: QueryParamDecoder[monzo.State] =
    QueryParamDecoder[String].map:
      monzo.State(_)

  private object AuthorizationCodeQueryParamMatcher
      extends QueryParamDecoderMatcher[monzo.AuthorizationCode]("code")

  private object StateQueryParamMatcher
      extends QueryParamDecoderMatcher[monzo.State]("state")

  private def requestAuthorization(
      clientId: monzo.ClientId,
      redirectUri: monzo.RedirectUri
  ): IO[monzo.State] = for
    state <- IO.randomUUID.map: uuid =>
      monzo.State(uuid.toString)
    _ <- IO(
      Runtime
        .getRuntime()
        .exec(
          Array(
            "open",
            uri"https://auth.monzo.com"
              .withQueryParams(
                Map(
                  "client_id" -> clientId.value,
                  "redirect_uri" -> redirectUri.value,
                  "response_type" -> "code",
                  "state" -> state.value
                )
              )
              .renderString
          )
        )
    )
  yield state

  private object BearerAuthMiddleware:
    def apply(bearerToken: String): ClientEndpointMiddleware[IO] =
      new ClientEndpointMiddleware.Simple[IO]:
        override def prepareWithHints(
            serviceHints: Hints,
            endpointHints: Hints
        ): Client[IO] => Client[IO] =
          if serviceHints.has[smithy.api.HttpBearerAuth] && !endpointHints
              .get[smithy.api.Auth]
              .exists(_.value.isEmpty)
          then
            client =>
              Client[IO]: request =>
                client.run(
                  request.withHeaders(
                    Authorization(
                      Credentials.Token(AuthScheme.Bearer, bearerToken)
                    )
                  )
                )
          else identity

  private enum ExportTransactionsSince:
    case Timestamp(instant: Instant)
    case LastTransactions(
        lastTransactions: Map[monzo.AccountId, LastTransaction]
    )

  private def exportTransactions(
      monzoApi: monzo.Api[IO],
      state: State,
      verbosity: Verbosity,
      since: ExportTransactionsSince,
      before: Instant,
      output: fs2.io.file.Path
  ): IO[State] = for
    _ <- Console[IO]
      .println("Listing accounts...")
      .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)
    accounts <- monzoApi
      .listAccounts()
      .map:
        _.accounts
    accountsAndSince = since match
      case ExportTransactionsSince.Timestamp(since) =>
        accounts.map: account =>
          (account, ListTransactionsSince.Timestamp(since))

      case ExportTransactionsSince.LastTransactions(lastTransactions) =>
        accounts
          .map: account =>
            (account, lastTransactions.get(account.id))
          .collect:
            case (account, Some(since)) =>
              (account, ListTransactionsSince.IdAndTimestamp(since))
    _ <- Console[IO]
      .println("Listing transactions for accounts...")
      .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)
    accountsAndTransactions <- accountsAndSince
      .traverse: (account, since) =>
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
    _ <- Console[IO]
      .println(
        Json.writeDocumentAsPrettyString(
          Document.array(
            accountsAndTransactions.map: (account, transactions) =>
              Document.obj(
                "account" -> Document.encode(account),
                "transactions" -> Document.array(
                  transactions.map:
                    Document.encode(_)
                )
              )
          )
        )
      )
      .whenA(verbosity.ordinal >= Verbosity.VERBOSE.ordinal)
    materialAccountIdsAndTransactions = accountsAndTransactions.map:
      (account, transactions) =>
        account.id -> transactions.filterNot: transaction =>
          // Active card check.
          transaction.amount.value == 0 ||
            // What it says.
            transaction.declineReason.isDefined
    _ <- writeOfx(toOfx(materialAccountIdsAndTransactions), output, verbosity)
    updatedState = state.copy(
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

  private enum ListTransactionsSince:
    case Timestamp(instant: Instant)
    case IdAndTimestamp(lastTransaction: LastTransaction)

  private def listTransactions(
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
          Instant.ofEpochSecond(
            lastTransaction.created.value.epochSecond,
            lastTransaction.created.value.nano
          )
      // See
      // https://community.monzo.com/t/changes-when-listing-with-our-api/158676.
      val maxPermittedBeforeForThisPage =
        sinceInstant.plus(Duration.ofHours(8760))
      val mustPaginate = before.isAfter(maxPermittedBeforeForThisPage)
      if mustPaginate then maxPermittedBeforeForThisPage else before
    def toTimestamp(instant: Instant): Timestamp =
      Timestamp(instant.getEpochSecond, instant.getNano)
    for
      thisPage <- monzoApi
        .listTransactions(
          accountId,
          since = Some(monzo.Since(since match
            case ListTransactionsSince.Timestamp(instant) =>
              toTimestamp(instant).format(TimestampFormat.DATE_TIME)

            case ListTransactionsSince.IdAndTimestamp(lastTransaction) =>
              lastTransaction.id.value
          )),
          before = Some(monzo.Before(toTimestamp(beforeForThisPage))),
          limit = Some(monzo.Limit(100))
        )
        .map(_.transactions)
      otherPages <- thisPage.lastOption match
        case None =>
          val haveRequestedAllPages = beforeForThisPage == before
          if haveRequestedAllPages
          then IO.pure(List.empty)
          else
            listTransactions(
              monzoApi,
              accountId,
              since = ListTransactionsSince.Timestamp(beforeForThisPage),
              before
            )

        case Some(transaction) =>
          listTransactions(
            monzoApi,
            accountId,
            since = ListTransactionsSince.IdAndTimestamp(
              LastTransaction(transaction.id, transaction.created)
            ),
            before
          )
    yield thisPage ++ otherPages

  private def toOfx(
      accountsIdsAndTransactions: List[
        (monzo.AccountId, List[monzo.Transaction])
      ]
  ): ofx.Ofx =
    ofx.Ofx(
      ofx.BankMessageSetResponse(
        accountsIdsAndTransactions.map: (accountId, transactions) =>
          ofx.StatementTransactionsResponse(
            ofx.StatementResponse(
              ofx.BankAccountFrom(
                ofx.AccountId(accountId.value)
              ),
              ofx.BankTransactionList(
                transactions.map: transaction =>
                  ofx.StatementTransaction(
                    datePosted = ofx.Datetime(
                      Instant
                        .ofEpochSecond(
                          transaction.created.value.epochSecond,
                          transaction.created.value.nano
                        )
                        .atZone(ZoneId.of("GMT"))
                        .format(
                          DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS")
                        )
                    ),
                    transactionAmount = ofx.TransactionAmount(
                      BigDecimal(transaction.amount.value) / 100
                    ),
                    financialInstitutionId =
                      ofx.FinancialInstitutionId(transaction.id.value),
                    name = ofx.Name(
                      transaction.merchant
                        .map(_.name)
                        .orElse(transaction.counterparty.name)
                        .map(_.value)
                        .getOrElse(transaction.description.value)
                    ),
                    memo = Some(ofx.Memo(transaction.notes.value))
                  )
              )
            )
          )
      )
    )

  private def writeOfx[A](a: A, output: fs2.io.file.Path, verbosity: Verbosity)(
      implicit schema: Schema[A]
  ): IO[Unit] =
    (fs2.Stream("ENCODING:UTF-8\n") ++
      XmlDocument.documentEventifier
        .eventify(XmlDocument.Encoder.fromSchema(schema).encode(a))
        .through(fs2.data.xml.render.prettyPrint(width = 60, indent = 4)))
      .through(fs2.io.file.Files[IO].writeUtf8(output))
      .compile
      .drain *>
      Console[IO]
        .println(s"Wrote OFX to $output.")
        .whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal)
