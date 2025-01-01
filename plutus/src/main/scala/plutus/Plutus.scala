package plutus

import cats.effect.*
import cats.effect.std.*
import cats.syntax.all.*
import org.http4s.*
import org.http4s.client.Client
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
import scala.jdk.DurationConverters.*
import scala.language.experimental.betterFors

object Plutus extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    EmberClientBuilder
      .default[IO]
      .build
      // .map(
      //   org.http4s.client.middleware.Logger.colored(
      //     logHeaders = true,
      //     logBody = true
      //   )
      // )
      .use(client =>
        program(
          client,
          args
        )
          .as(ExitCode.Success)
      )

  private def program(client: Client[IO], args: List[String]): IO[Unit] =
    for
      sinceString <- IO
        .fromOption(args.headOption)(
          Error("Since timestamp must be given.")
        )
      sinceTimestamp <- IO
        .fromOption(
          Timestamp.parse(
            string = sinceString,
            format = TimestampFormat.DATE_TIME
          )
        )(Error(s"${sinceString} is not a valid timestamp."))
      since = monzo.Since(sinceTimestamp)
      sinceInstant = Instant.ofEpochSecond(
        sinceTimestamp.epochSecond,
        sinceTimestamp.nano
      )
      now <- Clock[IO].realTime.map(finiteDuration =>
        Instant.ofEpochMilli(finiteDuration.toMillis)
      )
      accessToken <- accessToken(
        client,
        now,
        // From https://docs.monzo.com/?shell#list-transactions:
        //
        // Strong Customer Authentication
        //
        // After a user has authenticated, your client can fetch all of their
        // transactions, and after 5 minutes, it can only sync the last 90
        // days of transactions. If you need the user’s entire transaction
        // history, you should consider fetching and storing it right after
        // authentication.
        requireLessThanFiveMinutesOld =
          sinceInstant.isBefore(now.minus(Period.ofDays(90)))
      )
      _ <- SimpleRestJsonBuilder(monzo.Api)
        .client(client)
        .uri(monzoApiUri)
        .middleware(BearerAuthMiddleware(accessToken.value))
        .resource
        .use(exportTransactions(_, since, now))
    yield ()

  private lazy val monzoApiUri: Uri = uri"https://api.monzo.com"

  private def accessToken(
      client: Client[IO],
      now: Instant,
      requireLessThanFiveMinutesOld: Boolean
  ): IO[monzo.AccessToken] =
    TokenExchangeBuilder(monzo.TokenApi)
      .client(client)
      .uri(monzoApiUri)
      .resource
      .use(monzoTokenApi =>
        for
          clientId <- envParam("CLIENT_ID").map(monzo.ClientId(_))
          clientSecret <- envParam("CLIENT_SECRET").map(monzo.ClientSecret(_))
          createAndWriteTokens = (createTokens: IO[
            monzo.CreateAccessTokenOutput
          ]) =>
            for
              createAccessTokenOutput <- createTokens
              _ <- writeTokens(
                StoredTokens(
                  createAccessTokenOutput,
                  createdAt = CreatedAt(
                    Timestamp(
                      epochSecond = now.getEpochSecond(),
                      nano = now.getNano()
                    )
                  )
                )
              )
            yield createAccessTokenOutput.accessToken
          maybeStoredTokens <- readTokens
          accessToken <- maybeStoredTokens match
            case None =>
              createAndWriteTokens(
                exchangeAuthCode(
                  monzoTokenApi,
                  clientId,
                  clientSecret
                )
              )

            case Some(storedTokens)
                if requireLessThanFiveMinutesOld && !lessThanFiveMinutesOld(
                  storedTokens,
                  now
                ) =>
              createAndWriteTokens(
                exchangeAuthCode(
                  monzoTokenApi,
                  clientId,
                  clientSecret
                )
              )

            case Some(storedTokens) if requiresRefresh(storedTokens, now) =>
              createAndWriteTokens(
                monzoTokenApi.createAccessToken(
                  grantType = monzo.GrantType.REFRESH_TOKEN,
                  clientId = clientId,
                  clientSecret = clientSecret,
                  refreshToken =
                    Some(storedTokens.createAccessTokenOutput.refreshToken)
                )
              )

            case Some(storedTokens) =>
              IO.pure(storedTokens.createAccessTokenOutput.accessToken)
        yield accessToken
      )

  private def lessThanFiveMinutesOld(
      storedTokens: StoredTokens,
      now: Instant
  ): Boolean =
    val createdAt = Instant.ofEpochSecond(
      storedTokens.createdAt.value.epochSecond,
      storedTokens.createdAt.value.nano
    )
    val fiveMinutesAgo = now.minus(Duration.ofMinutes(5))
    val leeway = Duration.ofSeconds(10)
    createdAt.plus(leeway).isAfter(fiveMinutesAgo)

  private def requiresRefresh(
      storedTokens: StoredTokens,
      now: Instant
  ): Boolean =
    val createdAt = Instant.ofEpochSecond(
      storedTokens.createdAt.value.epochSecond,
      storedTokens.createdAt.value.nano
    )
    val expiresAt = createdAt.plus(
      Duration.ofSeconds(
        storedTokens.createAccessTokenOutput.expiresIn.value
      )
    )
    val leeway = Duration.ofSeconds(10)
    expiresAt.minus(leeway).isAfter(now)

  private def envParam(name: String): IO[String] = for
    maybeValue <- Env[IO].get(name)
    value <- IO.fromOption(maybeValue)(Error(s"$name must be set."))
  yield value

  private lazy val readTokens: IO[Option[StoredTokens]] =
    fs2.io.file
      .Files[IO]
      .readAll(tokensFilePath)
      .through(fs2.text.utf8.decode)
      .compile
      .string
      .flatMap(storedTokens =>
        IO.fromEither(
          Json.read[StoredTokens](Blob(storedTokens))
        )
      )
      .option

  private def writeTokens(storedTokens: StoredTokens): IO[Unit] =
    fs2
      .Stream(Json.writePrettyString(storedTokens))
      .through(fs2.text.utf8.encode)
      .through(
        fs2.io.file
          .Files[IO]
          .writeAll(tokensFilePath)
      )
      .compile
      .drain

  private lazy val tokensFilePath: fs2.io.file.Path =
    fs2.io.file.Path("tokens.json")

  private def toOfx(
      since: monzo.Since,
      now: Instant,
      accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])]
  ): ofx.Ofx =
    val (bankAccountsAndTransactions, creditCardAccountsAndTransactions) =
      accountsAndTransactions.partition { case (account, _) =>
        !account._type.contains("uk_monzo_flex")
      }
    ofx.Ofx(
      signonMessageSetResponse = ofx.SignonMessageSetResponse(
        signonResponse = ofx.SignonResponse(
          status = ofx.Status(
            code = ofx.Code(0),
            severity = ofx.Severity.INFO
          ),
          dateServer = toOfxDatetime(now),
          language = ofx.Language("ENG")
        )
      ),
      bankMessageSetResponse = ofx.BankMessageSetResponse(
        statementTransactionsResponses =
          for
            accountAndTransactions <- bankAccountsAndTransactions
            (account, transactions) = accountAndTransactions
            dateStartTimestamp = since.value
            dateStart = toOfxDatetime(
              Instant.ofEpochSecond(
                dateStartTimestamp.epochSecond,
                dateStartTimestamp.nano
              )
            )
            dateEndTimestamp = transactions.lastOption
              .map(_.created.value)
              .getOrElse(since.value)
            dateEnd = toOfxDatetime(
              Instant.ofEpochSecond(
                dateEndTimestamp.epochSecond,
                dateEndTimestamp.nano
              )
            )
          yield ofx.StatementTransactionsResponse(
            // TODO: Make this actually unique? GnuCash doesn't care though...
            transactionUniqueId = ofx.TransactionUniqueId(1),
            status = ofx.Status(
              code = ofx.Code(0),
              severity = ofx.Severity.INFO
            ),
            statementResponse = ofx.StatementResponse(
              currencyDefault = ofx.DefaultCurrency("GBP"),
              bankAccountFrom = toOfxBankAccountFrom(account),
              bankTransactionList = ofx.BankTransactionList(
                dateStart,
                dateEnd,
                statementTransactions = transactions
                  .map(toOfxStatementTransaction)
              )
            )
          )
      ),
      creditCardMessageSetResponse = ofx.CreditCardMessageSetResponse(
        creditCardStatementTransactionsResponses =
          for
            accountAndTransactions <- creditCardAccountsAndTransactions
            (account, transactions) = accountAndTransactions
            dateStartTimestamp = since.value
            dateStart = toOfxDatetime(
              Instant.ofEpochSecond(
                dateStartTimestamp.epochSecond,
                dateStartTimestamp.nano
              )
            )
            dateEndTimestamp = transactions.lastOption
              .map(_.created.value)
              .getOrElse(since.value)
            dateEnd = toOfxDatetime(
              Instant.ofEpochSecond(
                dateEndTimestamp.epochSecond,
                dateEndTimestamp.nano
              )
            )
          yield ofx.CreditCardStatementTransactionsResponse(
            transactionUniqueId = ofx.TransactionUniqueId(1),
            status = ofx.Status(
              code = ofx.Code(0),
              severity = ofx.Severity.INFO
            ),
            creditCardStatementResponse = ofx.CreditCardStatementResponse(
              currencyDefault = ofx.DefaultCurrency("GBP"),
              creditCardAccountFrom = toOfxCreditCardAccountFrom(account),
              bankTransactionList = ofx.BankTransactionList(
                dateStart,
                dateEnd,
                statementTransactions = transactions
                  .map(toOfxStatementTransaction)
              )
            )
          )
      )
    )

  private def toOfxBankAccountFrom(
      account: monzo.Account
  ): ofx.BankAccountFrom =
    ofx.BankAccountFrom(
      // TODO: What if they're empty?
      bankId = ofx.BankId(account.sortCode.getOrElse("")),
      accountId = ofx.AccountId(account.accountNumber.getOrElse("")),
      accountType = ofx.AccountType.CHECKING
    )

  private def toOfxCreditCardAccountFrom(
      account: monzo.Account
  ): ofx.CreditCardAccountFrom =
    ofx.CreditCardAccountFrom(
      accountId = ofx.AccountId(account.id.value)
    )

  private def toOfxStatementTransaction(
      transaction: monzo.Transaction
  ): ofx.StatementTransaction =
    ofx.StatementTransaction(
      transactionType =
        if transaction.amount.value.signum == 1 then ofx.TransactionType.CREDIT
        else ofx.TransactionType.DEBIT,
      datePosted = toOfxDatetime(
        Instant.ofEpochSecond(
          transaction.created.value.epochSecond,
          transaction.created.value.nano
        )
      ),
      transactionAmount =
        ofx.TransactionAmount(BigDecimal(transaction.amount.value) / 100),
      financialInstitutionId = ofx.FinancialInstitutionId(transaction.id.value),
      name = ofx.Name(name(transaction)),
      memo = Some(
        ofx.Memo(
          transaction.notes.value
        )
      )
    )

  private def name(transaction: monzo.Transaction): String =
    transaction.counterparty
      .flatMap(_.name)
      .orElse(transaction.merchant.flatMap(_.name))
      .getOrElse(transaction.description.value)

  // TODO: Add refinement to ofx.Datetime?
  private def toOfxDatetime(
      instant: Instant
  ): ofx.Datetime =
    ofx.Datetime(
      instant
        .atZone(ZoneId.of("GMT"))
        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS"))
    )

  private def writeOfx[A](a: A)(implicit schema: Schema[A]): IO[Unit] =
    XmlDocument.documentEventifier
      .eventify(XmlDocument.Encoder.fromSchema(schema).encode(a))
      .through(fs2.data.xml.render.prettyPrint(width = 60, indent = 4))
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
      monzoTokenApi: monzo.TokenApi[IO],
      clientId: monzo.ClientId,
      clientSecret: monzo.ClientSecret
  ): IO[monzo.CreateAccessTokenOutput] =
    for
      authorizationCodeAndStateDeferred <- Deferred[
        IO,
        (monzo.AuthorizationCode, monzo.State)
      ]
      authorizationCodeReceiverRoute <- IO.fromEither(
        SimpleRestJsonBuilder
          .routes(
            AuthorizationCodeReceiverImpl(authorizationCodeAndStateDeferred)
          )
          .make
      )
      createAccessTokenOutput <- EmberServerBuilder
        .default[IO]
        .withHttpApp(
          // org.http4s.server.middleware.Logger.httpApp(
          //   logHeaders = true,
          //   logBody = true
          // )(
          authorizationCodeReceiverRoute.orNotFound
            // )
        )
        .withShutdownTimeout(Duration.ZERO.toScala)
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
            _ <- Console[IO].print(
              "Complete SCA in app, then press enter to continue"
            )
            _ <- Console[IO].readLine
          yield createAccessTokenOutput
        )
    yield createAccessTokenOutput

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

  private class AuthorizationCodeReceiverImpl(
      deferred: Deferred[
        IO,
        (monzo.AuthorizationCode, monzo.State)
      ]
  ) extends monzo.AuthorizationCodeReceiver[IO]:
    override def receiveAuthorizationCode(
        code: monzo.AuthorizationCode,
        state: monzo.State
    ): IO[Unit] = deferred.complete(code -> state).void

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
      since: monzo.Since,
      now: Instant
  ): IO[Unit] = for
    listAccountsOutput <- monzoApi.listAccounts()
    accountsAndTransactions <-
      listAccountsOutput.accounts
        .filter(_.closed.contains(false))
        .traverse(account =>
          for
            _ <- Console[IO].println(
              Json.writePrettyString(account)
            )
            transactions <- listTransactions(
              monzoApi,
              since,
              now,
              account.id
            ).map(
              _.filterNot(_.notes.value == "Active card check")
                .filterNot(_.declineReason.isDefined)
            )
            _ <- transactions
              .traverse(transaction =>
                Console[IO].println(
                  Json.writePrettyString(transaction)
                )
              )
          yield (
            account,
            transactions
          )
        )
    _ <- writeOfx(
      toOfx(since, now, accountsAndTransactions)
    )
  yield ()

  private def listTransactions(
      monzoApi: monzo.Api[IO],
      since: monzo.Since,
      now: Instant,
      accountId: monzo.AccountId
  ): IO[List[monzo.Transaction]] = for
    // See
    // https://community.monzo.com/t/changes-when-listing-with-our-api/158676.
    maxPermittedBeforeInstant = Instant
      .ofEpochSecond(
        since.value.epochSecond,
        since.value.nano
      )
      .plus(Duration.ofHours(8760))
    transactions <-
      if now.isBefore(maxPermittedBeforeInstant) then
        monzoApi
          .listTransactions(
            accountId = accountId,
            since = Some(since),
            limit = Some(monzo.Limit(100))
          )
          .map(_.transactions)
      else
        val maxPermittedBefore = monzo.Before(
          Timestamp(
            maxPermittedBeforeInstant.getEpochSecond,
            maxPermittedBeforeInstant.getNano
          )
        )
        for
          firstPage <- monzoApi
            .listTransactions(
              accountId = accountId,
              since = Some(since),
              before = Some(maxPermittedBefore),
              limit = Some(monzo.Limit(100))
            )
            .map(_.transactions)
          otherPages <- listTransactions(
            monzoApi,
            monzo.Since(maxPermittedBefore.value),
            now,
            accountId
          )
        yield firstPage ++ otherPages
  yield transactions
