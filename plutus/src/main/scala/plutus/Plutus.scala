package plutus

import fs2.{Chunk as _, *}
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
import smithy4s.schema.*
import smithy4s.xml.*
import zio.*
import zio.interop.catz.*
import zio.nio.channels.*
import zio.nio.file.*
import zio.stream.interop.fs2z.io.*

import java.lang.Runtime
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Plutus extends ZIOAppDefault:

  override def run: RIO[ZIOAppArgs, Unit] =
    program.provideSome[ZIOAppArgs](
      ZLayer.scoped(
        EmberClientBuilder
          .default[Task]
          .build
          .toScopedZIO
          // .map(
          //   org.http4s.client.middleware.Logger.colored(
          //     logHeaders = true,
          //     logBody = true
          //   )
          // )
      )
    )

  private lazy val program: RIO[ZIOAppArgs & Client[Task], Unit] =
    ZIO.scoped(
      for
        zioAppArgs <- ZIOAppArgs.getArgs
        sinceString <- ZIO
          .fromOption(zioAppArgs.headOption)
          .mapError(_ => Error("Since timestamp must be given."))
        sinceTimestamp <- ZIO
          .fromOption(
            Timestamp.parse(
              string = sinceString,
              format = TimestampFormat.DATE_TIME
            )
          )
          .mapError(_ => Error(s"${sinceString} is not a valid timestamp."))
        since = monzo.Since(sinceTimestamp)
        sinceInstant = Instant.ofEpochSecond(
          sinceTimestamp.epochSecond,
          sinceTimestamp.nano
        )
        now <- Clock.instant
        accessToken <- accessToken(
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
            sinceInstant.isBefore(now.minus(90.days.asJava))
        )
        client <- ZIO.service[Client[Task]]
        monzoApi <- SimpleRestJsonBuilder(monzo.Api)
          .client(client)
          .uri(monzoApiUri)
          .middleware(BearerAuthMiddleware(accessToken.value))
          .resource
          .toScopedZIO
        whoAmIOutput <- monzoApi.whoAmI()
        _ <- Console.printLine(show(whoAmIOutput))
        _ <- exportTransactions(monzoApi, since)
      yield ()
    )

  private lazy val monzoApiUri: Uri = uri"https://api.monzo.com"

  private def accessToken(
      requireLessThanFiveMinutesOld: Boolean
  ): RIO[Client[Task], monzo.AccessToken] = ZIO.scoped(
    for
      client <- ZIO.service[Client[Task]]
      monzoTokenApi <- TokenExchangeBuilder(monzo.TokenApi)
        .client(client)
        .uri(monzoApiUri)
        .resource
        .toScopedZIO
      clientId <- envParam("CLIENT_ID").map(monzo.ClientId(_))
      clientSecret <- envParam("CLIENT_SECRET").map(monzo.ClientSecret(_))
      now <- Clock.instant
      createAndWriteTokens = (createTokens: Task[
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
          ZIO.succeed(storedTokens.createAccessTokenOutput.accessToken)
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
    val fiveMinutesAgo = now.minus(5.minutes)
    val leeway = 10.seconds.asJava
    createdAt.plus(leeway).isAfter(fiveMinutesAgo)

  private def requiresRefresh(
      storedTokens: StoredTokens,
      now: Instant
  ): Boolean =
    val createdAt = Instant.ofEpochSecond(
      storedTokens.createdAt.value.epochSecond,
      storedTokens.createdAt.value.nano
    )
    val expiresAt = createdAt.plusSeconds(
      storedTokens.createAccessTokenOutput.expiresIn.value
    )
    val leeway = 10.seconds.asJava
    expiresAt.minus(leeway).isAfter(now)

  private def envParam(name: String): Task[String] = System
    .env(name)
    .someOrFail(Error(s"$name must be set."))

  private lazy val readTokens: Task[Option[StoredTokens]] = ZIO.scoped(
    for
      maybeFileChannel <- AsynchronousFileChannel
        .open(
          tokensFilePath,
          StandardOpenOption.CREATE,
          StandardOpenOption.READ
        )
        .option
      maybeStoredTokens <- maybeFileChannel match
        case None =>
          ZIO.none

        case Some(fileChannel) =>
          for
            size <- fileChannel.size.map(_.toInt)
            chunk <- fileChannel.readChunk(
              capacity = size,
              position = 0L
            )
            storedTokens <- ZIO.fromEither(
              Json.read[StoredTokens](Blob(chunk.toArray))
            )
          yield Some(storedTokens)
    yield maybeStoredTokens
  )

  private def writeTokens(storedTokens: StoredTokens): Task[Unit] =
    ZIO.scoped(
      for
        fileChannel <- AsynchronousFileChannel.open(
          tokensFilePath,
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
        _ <- fileChannel.writeChunk(
          Chunk.fromArray(
            Json.writeBlob(storedTokens).toArray
          ),
          position = 0L
        )
      yield ()
    )

  private lazy val tokensFilePath: Path = Path("tokens.json")

  private trait Show[-A]:
    def apply(a: A, indent: Int = 0): String

  // TODO: Compare to
  // https://github.com/disneystreaming/smithy4s/blob/e369dd40850690cb38efd385c1cb16ed3d09a192/modules/cats/src/smithy4s/interopcats/SchemaVisitorShow.scala#L38.
  // TODO: Special case for UnknownProperties?
  private object SchemaVisitorShowCodec
      extends SchemaVisitor.Cached[Show]
      with SchemaVisitor.Default[Show]:

    override protected val cache: CompilationCache[Show] =
      CompilationCache.make[Show]

    override def default[A]: Show[A] = (a, _) => a.toString()

    override def struct[S](
        shapeId: ShapeId,
        hints: Hints,
        fields: Vector[Field[S, ?]],
        make: IndexedSeq[Any] => S
    ): Show[S] = (struct: S, indent: Int) =>
      val longestLabel = fields.map(_.label.length()).max
      val sep = "\n" + (" " * indent)
      val start = sep
      fields
        .map(field =>
          showField(
            field,
            paddedLabel = field.label.padTo(longestLabel, ' '),
            struct,
            indent
          )
        )
        .mkString(start, sep, "")

    override def option[A](schema: Schema[A]): Show[Option[A]] =
      (option: Option[A], indent: Int) =>
        val show = apply(schema)
        option match
          case None        => "n/a"
          case Some(value) => s"${show(value, indent + 2)}"

    private def showField[S, A](
        field: Field[S, A],
        paddedLabel: String,
        struct: S,
        indent: Int
    ): String =
      val show = apply(field.schema)
      s"$paddedLabel : ${show(field.get(struct), indent + 2)}"

  private def show[A](a: A)(implicit schema: Schema[A]): String =
    SchemaVisitorShowCodec(schema)(a)

  private def toOfx(
      since: monzo.Since,
      now: Instant,
      accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])]
  ): ofx.Ofx =
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
            accountAndTransactions <- accountsAndTransactions
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

  private def writeOfx[A](a: A)(implicit schema: Schema[A]): Task[Unit] =
    ZIO.scoped(
      for
        fileChannel <- AsynchronousFileChannel.open(
          Path("monzo-export.ofx"),
          StandardOpenOption.CREATE,
          StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
        _ <- fileChannel.writeChunk(
          toXmlBytes(a),
          position = 0L
        )
      yield ()
    )

  private def toXmlBytes[A](
      a: A
  )(implicit schema: Schema[A]): Chunk[Byte] =
    XmlDocument.documentEventifier
      .eventify(XmlDocument.Encoder.fromSchema(schema).encode(a))
      // TODO: Fix special char escaping.
      .through(fs2.data.xml.render.prettyPrint(width = 60, indent = 4))
      .through(fs2.text.utf8.encode)
      .compile
      .to(Chunk)

  private def exchangeAuthCode(
      monzoTokenApi: monzo.TokenApi[Task],
      clientId: monzo.ClientId,
      clientSecret: monzo.ClientSecret
  ): Task[monzo.CreateAccessTokenOutput] = ZIO.scoped(
    for
      authorizationCodeAndStatePromise <- Promise
        .make[
          Nothing,
          (monzo.AuthorizationCode, monzo.State)
        ]
      authorizationCodeReceiverRoute <- ZIO.fromEither(
        SimpleRestJsonBuilder
          .routes(
            AuthorizationCodeReceiverImpl(authorizationCodeAndStatePromise)
          )
          .make
      )
      server <- EmberServerBuilder
        .default[Task]
        .withHttpApp(
          // org.http4s.server.middleware.Logger.httpApp(
          //   logHeaders = true,
          //   logBody = true
          // )(
          authorizationCodeReceiverRoute.orNotFound
            // )
        )
        .withShutdownTimeout(0.seconds.asScala)
        .build
        .toScopedZIO
      redirectUri = monzo.RedirectUri("http://localhost:8080/oauth/callback")
      generatedState <- redirectUserToMonzo(clientId, redirectUri)
      authorizationCodeAndReceivedState <-
        authorizationCodeAndStatePromise.await
      (authorizationCode, receivedState) = authorizationCodeAndReceivedState
      _ <- ZIO.unless(generatedState == receivedState)(
        ZIO.fail(
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
      _ <- Console.readLine("Complete SCA in app, then press enter to continue")
    yield createAccessTokenOutput
  )

  private def redirectUserToMonzo(
      clientId: monzo.ClientId,
      redirectUri: monzo.RedirectUri
  ): Task[monzo.State] = for
    state <- Random.nextUUID.map(uuid => monzo.State(uuid.toString()))
    monzoAuthEndpoint = uri"https://auth.monzo.com".withQueryParams(
      Map(
        "client_id" -> clientId.value,
        "redirect_uri" -> redirectUri.value.toString,
        "response_type" -> "code",
        "state" -> state.value
      )
    )
    _ <- ZIO.attempt(
      Runtime.getRuntime().exec(Array("open", monzoAuthEndpoint.toString))
    )
  yield state

  private class AuthorizationCodeReceiverImpl(
      promise: Promise[
        Nothing,
        (monzo.AuthorizationCode, monzo.State)
      ]
  ) extends monzo.AuthorizationCodeReceiver[Task]:
    override def receiveAuthorizationCode(
        code: monzo.AuthorizationCode,
        state: monzo.State
    ): Task[Unit] = promise.succeed(code -> state).unit

  private object BearerAuthMiddleware:
    def apply(bearerToken: String): ClientEndpointMiddleware[Task] =
      new ClientEndpointMiddleware.Simple[Task]:
        val middleware = (client: Client[Task]) =>
          Client[Task](request =>
            client.run(
              request.withHeaders(
                Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
              )
            )
          )
        override def prepareWithHints(
            serviceHints: Hints,
            endpointHints: Hints
        ): Client[Task] => Client[Task] =
          serviceHints.get[smithy.api.HttpBearerAuth] match
            case None => identity
            case Some(_) =>
              endpointHints.get[smithy.api.Auth] match
                case Some(auths) if auths.value.isEmpty => identity
                case _                                  => middleware

  private def exportTransactions(
      monzoApi: monzo.Api[Task],
      since: monzo.Since
  ): Task[Unit] = for
    listAccountsOutput <- monzoApi.listAccounts()
    accountsAndTransactions <- ZIO.foreach(
      listAccountsOutput.accounts.filter(_.closed.contains(false))
    )(account =>
      for
        _ <- Console.printLine(show(account))
        transactions <- listTransactions(
          monzoApi,
          since,
          account.id
        )
        formatter = java.text.NumberFormat.getCurrencyInstance
        _ = formatter.setCurrency(
          java.util.Currency.getInstance(java.util.Locale.UK)
        )
        _ <- ZIO.foldLeft(
          transactions
            .filterNot(_.notes.value == "Active card check")
            .filterNot(_.declineReason.isDefined)
            .sortBy(_.created.value.epochSecond)
        )(
          BigDecimal(0)
        ) { case (balance, transaction) =>
          for _ <- Console.printLine(
              transaction.created.value.toString.padTo(30, ' ') + " " +
                s"${name(transaction)} (${transaction.notes.value})"
                  .padTo(45, ' ') + " " +
                formatter
                  .format(BigDecimal(transaction.amount.value) / 100)
                  .padTo(10, ' ') + " " +
                formatter.format(
                  (balance + BigDecimal(transaction.amount.value)) / 100
                )
            )
          yield balance + BigDecimal(transaction.amount.value)
        }
        balance <- monzoApi.getBalance(account.id)
        _ <- Console.printLine(show(balance))
      yield (
        account,
        transactions
          .filterNot(_.notes.value == "Active card check")
          .filterNot(_.declineReason.isDefined)
      )
    )
    now <- Clock.instant
    _ <- writeOfx(
      toOfx(since, now, accountsAndTransactions)
    )
  yield ()

  private def listTransactions(
      monzoApi: monzo.Api[Task],
      since: monzo.Since,
      accountId: monzo.AccountId
  ): Task[List[monzo.Transaction]] = for
    now <- Clock.instant
    // See
    // https://community.monzo.com/t/changes-when-listing-with-our-api/158676.
    maxPermittedBeforeInstant = Instant
      .ofEpochSecond(
        since.value.epochSecond,
        since.value.nano
      )
      .plus(8760.hours)
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
            accountId
          )
        yield firstPage ++ otherPages
  yield transactions
