package plutus

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import com.monovore.decline.time.*
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
      header = "Monzo to OFX export tool."
    ):

  private val verboseOpts: Opts[Boolean] =
    Opts
      .flag(
        "verbose",
        help = "Log HTTP requests and responses."
      )
      .orFalse

  private val sinceOpts: Opts[Instant] =
    Opts.option[Instant](
      "since",
      help = "Timestamp to export transactions from."
    )

  override def main: Opts[IO[ExitCode]] =
    (verboseOpts, sinceOpts).mapN { (verbose, since) =>
      EmberClientBuilder
        .default[IO]
        .build
        .map(
          if verbose then
            org.http4s.client.middleware.Logger.colored(
              logHeaders = true,
              logBody = true
            )
          else identity
        )
        .use(program(verbose, since, _).as(ExitCode.Success))
    }

  private def program(
      verbose: Boolean,
      since: Instant,
      client: Client[IO]
  ): IO[Unit] =
    for
      now <- Clock[IO].realTime.map(finiteDuration =>
        Instant.ofEpochMilli(finiteDuration.toMillis)
      )
      leeway = Duration.ofSeconds(10)
      accessToken <- accessToken(
        verbose,
        client,
        now,
        // From https://docs.monzo.com/?shell#list-transactions:
        //
        // Strong Customer Authentication
        //
        // After a user has authenticated, your client can fetch all of their
        // transactions, and after 5 minutes, it can only sync the last 90 days
        // of transactions. If you need the user’s entire transaction history,
        // you should consider fetching and storing it right after
        // authentication.
        requireStrongCustomerAuthentication =
          since.isBefore(now.minus(Period.ofDays(90)).plus(leeway))
      )
      _ <- SimpleRestJsonBuilder(monzo.Api)
        .client(client)
        .uri(monzoApiUri)
        .middleware(BearerAuthMiddleware(accessToken.value))
        .resource
        .use(exportTransactions(_, since, before = now))
    yield ()

  private lazy val monzoApiUri: Uri = uri"https://api.monzo.com"

  private def accessToken(
      verbose: Boolean,
      client: Client[IO],
      now: Instant,
      requireStrongCustomerAuthentication: Boolean
  ): IO[monzo.AccessToken] =
    TokenExchangeBuilder(monzo.TokenApi)
      .client(client)
      .uri(monzoApiUri)
      .resource
      .use(monzoTokenApi =>
        for
          clientId <- envParam("CLIENT_ID").map(monzo.ClientId(_))
          clientSecret <- envParam("CLIENT_SECRET").map(monzo.ClientSecret(_))
          maybeState <- loadState
          exchangeAuthCodeAndStoreTokens = for
            createAccessTokenOutput <- exchangeAuthCode(
              verbose,
              monzoTokenApi,
              clientId,
              clientSecret
            )
            _ <- saveState(
              State(
                authorizedAt = AuthorizedAt(
                  Timestamp(
                    epochSecond = now.getEpochSecond(),
                    nano = now.getNano()
                  )
                ),
                refreshToken = createAccessTokenOutput.refreshToken
              )
            )
          yield createAccessTokenOutput.accessToken
          accessToken <- maybeState match
            case None =>
              exchangeAuthCodeAndStoreTokens

            case Some(state)
                if requireStrongCustomerAuthentication && !lessThanFiveMinutesAgo(
                  state.authorizedAt,
                  now
                ) =>
              exchangeAuthCodeAndStoreTokens

            case Some(state) =>
              for
                createAccessTokenOutput <- monzoTokenApi.createAccessToken(
                  grantType = monzo.GrantType.REFRESH_TOKEN,
                  clientId = clientId,
                  clientSecret = clientSecret,
                  refreshToken = Some(state.refreshToken)
                )
                _ <- saveState(
                  state.copy(
                    refreshToken = createAccessTokenOutput.refreshToken
                  )
                )
              yield createAccessTokenOutput.accessToken
        yield accessToken
      )

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

  private def envParam(name: String): IO[String] = for
    maybeValue <- Env[IO].get(name)
    value <- IO.fromOption(maybeValue)(Error(s"$name must be set."))
  yield value

  private lazy val loadState: IO[Option[State]] =
    fs2.io.file
      .Files[IO]
      .readAll(stateFilePath)
      .through(fs2.text.utf8.decode)
      .compile
      .string
      .flatMap(state =>
        IO.fromEither(
          Json.read[State](Blob(state))
        )
      )
      .option

  private def saveState(state: State): IO[Unit] =
    fs2
      .Stream(Json.writePrettyString(state))
      .through(fs2.text.utf8.encode)
      .through(
        fs2.io.file
          .Files[IO]
          .writeAll(stateFilePath)
      )
      .compile
      .drain

  private lazy val stateFilePath: fs2.io.file.Path =
    fs2.io.file.Path(System.getProperty("user.home")) /
      "Library" / "Application Support" / "plutus" / "state.json"

  private def toOfx(
      accountsIdsAndTransactions: List[
        (monzo.AccountId, List[monzo.Transaction])
      ]
  ): ofx.Ofx = ofx.Ofx(
    bankMessageSetResponse = ofx.BankMessageSetResponse(
      statementTransactionsResponses =
        accountsIdsAndTransactions.map { (accountId, transactions) =>
          ofx.StatementTransactionsResponse(
            statementResponse = ofx.StatementResponse(
              bankAccountFrom = ofx.BankAccountFrom(
                accountId = ofx.AccountId(accountId.value)
              ),
              bankTransactionList = ofx.BankTransactionList(
                statementTransactions = transactions.map(transaction =>
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
                    // TODO: Can we make this easier to reason about?
                    name = ofx.Name(
                      transaction.counterparty.name
                        .orElse(transaction.merchant.map(_.name))
                        .map(_.value)
                        .getOrElse(transaction.description.value)
                    ),
                    // TODO: Can we make this easier to reason about?
                    memo = Some(
                      ofx.Memo(
                        transaction.notes.value
                      )
                    )
                  )
                )
              )
            )
          )
        }
    )
  )

  private def writeOfx[A](a: A)(implicit schema: Schema[A]): IO[Unit] =
    (fs2.Stream("ENCODING:UTF-8\n") ++
      XmlDocument.documentEventifier
        .eventify(XmlDocument.Encoder.fromSchema(schema).encode(a))
        .through(fs2.data.xml.render.prettyPrint(width = 60, indent = 4)))
      .through(fs2.text.utf8.encode)
      .through(
        fs2.io.file
          .Files[IO]
          .writeAll(
            fs2.io.file.Path("monzo-export.ofx")
          )
      )
      .compile
      .drain

  private def exchangeAuthCode(
      verbose: Boolean,
      monzoTokenApi: monzo.TokenApi[IO],
      clientId: monzo.ClientId,
      clientSecret: monzo.ClientSecret
  ): IO[monzo.CreateAccessTokenOutput] =
    for
      authorizationCodeAndStateDeferred <- Deferred[
        IO,
        (monzo.AuthorizationCode, monzo.State)
      ]
      createAccessTokenOutput <- EmberServerBuilder
        .default[IO]
        .withHttpApp(
          (if verbose then
             org.http4s.server.middleware.Logger.httpApp[IO](
               logHeaders = true,
               logBody = true
             )
           else identity[HttpApp[IO]]) (
            HttpRoutes
              .of[IO] {
                case GET -> Root / "oauth" / "callback" :?
                    AuthorizationCodeQueryParamMatcher(code) +&
                    StateQueryParamMatcher(state) =>
                  authorizationCodeAndStateDeferred.complete(code -> state) >>
                    Ok("Authorization code received. Return to Plutus.")
              }
              .orNotFound
          )
        )
        .withShutdownTimeout(0.seconds)
        .build
        .use(server =>
          for
            redirectUri = monzo
              .RedirectUri("http://localhost:8080/oauth/callback")
            generatedState <- redirectUserToMonzo(clientId, redirectUri)
            authorizationCodeAndReceivedState <-
              authorizationCodeAndStateDeferred.get
            (authorizationCode, receivedState) =
              authorizationCodeAndReceivedState
            _ <- IO.unlessA(generatedState == receivedState)(
              IO.raiseError(
                Error(
                  s"generatedState != receivedState ($generatedState != $receivedState)"
                )
              )
            )
            createAccessTokenOutput <- monzoTokenApi.createAccessToken(
              grantType = monzo.GrantType.AUTHORIZATION_CODE,
              clientId = clientId,
              clientSecret = clientSecret,
              redirectUri = Some(redirectUri),
              code = Some(authorizationCode)
            )
            _ <- Console[IO].println(
              "Complete SCA in app, then press enter to continue."
            )
            _ <- Console[IO].readLine
          yield createAccessTokenOutput
        )
    yield createAccessTokenOutput

  private implicit val authorizationCodeQueryParamDecoder
      : QueryParamDecoder[monzo.AuthorizationCode] =
    QueryParamDecoder[String].map(monzo.AuthorizationCode(_))

  private implicit val stateQueryParamDecoder: QueryParamDecoder[monzo.State] =
    QueryParamDecoder[String].map(monzo.State(_))

  private object AuthorizationCodeQueryParamMatcher
      extends QueryParamDecoderMatcher[monzo.AuthorizationCode]("code")

  private object StateQueryParamMatcher
      extends QueryParamDecoderMatcher[monzo.State]("state")

  private def redirectUserToMonzo(
      clientId: monzo.ClientId,
      redirectUri: monzo.RedirectUri
  ): IO[monzo.State] = for
    state <- UUIDGen[IO].randomUUID.map(uuid => monzo.State(uuid.toString()))
    monzoAuthEndpoint = uri"https://auth.monzo.com".withQueryParams(
      Map(
        "client_id" -> clientId.value,
        "redirect_uri" -> redirectUri.value.toString,
        "response_type" -> "code",
        "state" -> state.value
      )
    )
    _ <- IO(
      Runtime.getRuntime().exec(Array("open", monzoAuthEndpoint.toString))
    )
  yield state

  private object BearerAuthMiddleware:
    def apply(bearerToken: String): ClientEndpointMiddleware[IO] =
      new ClientEndpointMiddleware.Simple[IO]:
        val middleware = (client: Client[IO]) =>
          Client[IO](request =>
            client.run(
              request.withHeaders(
                Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
              )
            )
          )
        override def prepareWithHints(
            serviceHints: Hints,
            endpointHints: Hints
        ): Client[IO] => Client[IO] =
          serviceHints.get[smithy.api.HttpBearerAuth] match
            case None => identity
            case Some(_) =>
              endpointHints.get[smithy.api.Auth] match
                case Some(auths) if auths.value.isEmpty => identity
                case _                                  => middleware

  private def exportTransactions(
      monzoApi: monzo.Api[IO],
      since: Instant,
      before: Instant
  ): IO[Unit] =
    def collect(
        listAccountsOutput: monzo.ListAccountsOutput,
        pf: PartialFunction[monzo.Account, (monzo.Account, monzo.AccountId)]
    ): IO[List[(monzo.AccountId, List[monzo.Transaction])]] =
      listAccountsOutput.accounts
        .collect(pf)
        .traverse { (account, accountId) =>
          for
            _ <- Console[IO].println(
              Json.writePrettyString(account)
            )
            transactions <- listTransactions(
              monzoApi,
              since = Since.Timestamp(since),
              before,
              accountId
            ).map(
              _.filterNot(_.notes.value == "Active card check")
                .filterNot(_.declineReason.isDefined)
            )
            _ <- transactions.traverse(transaction =>
              Console[IO].println(
                Json.writePrettyString(transaction)
              )
            )
          yield accountId -> transactions
        }
    for
      listAccountsOutput <- monzoApi.listAccounts()
      bankAccountIdsAndTransactions <- collect(
        listAccountsOutput,
        { case account: monzo.Account.UkRetailAccount =>
          account -> account.id
        }
      )
      creditCardAccountIdsAndTransactions <- collect(
        listAccountsOutput,
        { case account: monzo.Account.UkMonzoFlexAccount =>
          account -> account.id
        }
      )
      _ <- writeOfx(
        toOfx(
          bankAccountIdsAndTransactions ++ creditCardAccountIdsAndTransactions
        )
      )
    yield ()

  private enum Since:
    case Timestamp(instant: Instant)
    case Transaction(transaction: monzo.Transaction)

  private def listTransactions(
      // TODO: Refine grouping and order, here and elsewhere.
      monzoApi: monzo.Api[IO],
      since: Since,
      before: Instant,
      accountId: monzo.AccountId
  ): IO[List[monzo.Transaction]] =
    val sinceInstant = since match
      case Since.Timestamp(instant) =>
        instant

      case Since.Transaction(transaction) =>
        Instant.ofEpochSecond(
          transaction.created.value.epochSecond,
          transaction.created.value.nano
        )
    // See
    // https://community.monzo.com/t/changes-when-listing-with-our-api/158676.
    val maxPermittedThisPageBefore = sinceInstant.plus(Duration.ofHours(8760))
    val mustPaginate = before.isAfter(maxPermittedThisPageBefore)
    val thisPageBefore =
      if mustPaginate then maxPermittedThisPageBefore else before
    def toTimestamp(instant: Instant): Timestamp =
      Timestamp(
        instant.getEpochSecond,
        instant.getNano
      )
    for
      thisPage <- monzoApi
        .listTransactions(
          accountId,
          since = Some(monzo.Since(since match
            case Since.Timestamp(instant) =>
              toTimestamp(instant).format(TimestampFormat.DATE_TIME)

            case Since.Transaction(transaction) =>
              transaction.id.value
          )),
          before = Some(monzo.Before(toTimestamp(thisPageBefore))),
          limit = Some(monzo.Limit(100))
        )
        .map(_.transactions)
      otherPages <- thisPage.lastOption match
        case None =>
          val haveRequestedAllPages = thisPageBefore == before
          if haveRequestedAllPages then IO.pure(List.empty)
          else
            listTransactions(
              monzoApi,
              since = Since.Timestamp(thisPageBefore),
              before,
              accountId
            )

        case Some(transaction) =>
          listTransactions(
            monzoApi,
            since = Since.Transaction(transaction),
            before,
            accountId
          )
    yield thisPage ++ otherPages
