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
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*
import scala.language.experimental.betterFors
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object Plutus
    extends CommandIOApp(
      name = "plutus",
      header = "Monzo OFX exporter.",
      version = plutus.BuildInfo.version
    ):

  override def main: Opts[IO[ExitCode]] =
    (verbosityOpt, sinceOpt, beforeOpt, outputOpt, dryRunOpt)
      .mapN: (verbosity, since, before, output, dryRun) =>
        program(
          verbosity,
          since,
          before,
          output = output
            .map:
              fs2.io.file.Path.fromNioPath(_)
            .getOrElse(fs2.io.file.Path("monzo.ofx")),
          dryRun
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

  private lazy val dryRunOpt: Opts[Boolean] =
    Opts
      .flag(
        "dry-run",
        help = "Don't update state-file's last-transactions bookmarks."
      )
      .orFalse

  private def program(
      verbosity: Verbosity,
      since: Option[Instant],
      before: Option[Instant],
      output: fs2.io.file.Path,
      dryRun: Boolean
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
                output,
                dryRun
              )
        yield updatedState
    _ <- saveState(
      updatedState,
      mode =
        if maybeState.isEmpty then SaveStateMode.Create
        else SaveStateMode.Update,
      verbosity
    )
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
              IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
                Console[IO].println(
                  fansi.Color.Yellow(
                    "No previous state, requesting authorization..."
                  )
                )
              *>
                exchangeAuthCodeAndCreateOrUpdateState

            case Some(state)
                if requireStrongCustomerAuthentication && !lessThanFiveMinutesAgo(
                  state.authorizedAt,
                  now
                ) =>
              IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
                Console[IO].println(
                  fansi.Color.Yellow(
                    "Strong authentication required, requesting authorization..."
                  )
                )
              *>
                exchangeAuthCodeAndCreateOrUpdateState

            case Some(state) =>
              for
                _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
                  Console[IO].println(
                    fansi.Color.Green(
                      "Existing refresh token found, exchanging for tokens..."
                    )
                  )
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

  private def loadState(verbosity: Verbosity): IO[Option[State]] = for
    errorOrMaybeDataString <- IO:
      val resultPtr = stackalloc[macos.aliases.CFTypeRef]()
      (macos.functions
        .SecItemCopyMatching(
          query = toCfDictionary(
            macos.Forwarders.SecClass.value.unsafeToPtr -> macos.Forwarders.SecClassGenericPassword.value.unsafeToPtr,
            macos.Forwarders.SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr,
            macos.Forwarders.SecReturnData.value.unsafeToPtr -> macos.Forwarders.CFBooleanTrue.value.unsafeToPtr
          ),
          result = resultPtr
        )
        .value match
        case macos.constants.errSecItemNotFound =>
          Right(None)

        case macos.constants.errSecSuccess =>
          Right(
            Some(
              fromCString(
                macos.functions.CFStringGetCStringPtr(
                  theString = macos.functions
                    .CFStringCreateFromExternalRepresentation(
                      alloc = defaultAllocator,
                      data =
                        macos.aliases.CFDataRef((!resultPtr).value.unsafeToPtr),
                      encoding = utf8
                    ),
                  encoding = utf8
                )
              )
            )
          )

        case other =>
          Left(Error(s"Load state failed with status $other."))
      )
    maybeState <- errorOrMaybeDataString match
      case Right(None) =>
        IO.none

      case Right(Some(dataString)) =>
        IO.fromEither(Json.read[State](Blob(dataString))).option

      case Left(error) =>
        IO.raiseError(error)
    _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
      Console[IO].println(
        if maybeState.isDefined then
          fansi.Color.Green("Loaded state from Keychain.")
        else fansi.Color.Red("Couldn't load state from Keychain.")
      )
  yield maybeState

  private enum SaveStateMode:
    case Create
    case Update

  private def saveState(
      state: State,
      mode: SaveStateMode,
      verbosity: Verbosity
  ): IO[Unit] = for
    attributes <- IO:
      toCfDictionary(
        // TODO: The intent here is to always prompt for authentication, but
        // including them results in an errSecMissingEntitlement error
        // (https://developer.apple.com/documentation/security/errsecmissingentitlement?language=objc).
        // macos.Forwarders.SecUseDataProtectionKeychain.value.unsafeToPtr ->
        //   macos.Forwarders.CFBooleanTrue.value.unsafeToPtr,
        // macos.Forwarders.SecAttrSynchronizable.value.unsafeToPtr ->
        //   macos.Forwarders.CFBooleanTrue.value.unsafeToPtr,
        // macos.Forwarders.SecAttrAccessControl.value.unsafeToPtr -> macos.functions
        //   .SecAccessControlCreateWithFlags(
        //     allocator = defaultAllocator,
        //     protection =
        //       macos.Forwarders.SecAttrAccessibleWhenUnlocked.value.unsafeToPtr,
        //     flags = macos.aliases.CFOptionFlags(
        //       macos.constants.kSecAccessControlUserPresence
        //     ),
        //     error = null
        //   )
        //   .value
        //   .unsafeToPtr,
        macos.Forwarders.SecClass.value.unsafeToPtr -> macos.Forwarders.SecClassGenericPassword.value.unsafeToPtr,
        macos.Forwarders.SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr,
        macos.Forwarders.SecValueData.value.unsafeToPtr -> Zone(implicit z =>
          macos.functions
            .CFStringCreateExternalRepresentation(
              alloc = defaultAllocator,
              theString = macos.functions.CFStringCreateWithCString(
                alloc = defaultAllocator,
                cStr = toCString(
                  Json.writeBlob(state).toUTF8String,
                  java.nio.charset.StandardCharsets.UTF_8
                ),
                encoding = utf8
              ),
              encoding = utf8,
              lossByte = macos.aliases.UInt8(0.toUByte)
            )
            .value
            .unsafeToPtr
        )
      )
    osStatus <- mode match
      case SaveStateMode.Create =>
        IO:
          macos.functions.SecItemAdd(
            attributes = attributes,
            result = null
          )

      case SaveStateMode.Update =>
        IO:
          macos.functions.SecItemUpdate(
            query = toCfDictionary(
              macos.Forwarders.SecClass.value.unsafeToPtr -> macos.Forwarders.SecClassGenericPassword.value.unsafeToPtr,
              macos.Forwarders.SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr
            ),
            attributesToUpdate = attributes
          )
    _ <- IO.unlessA(osStatus.value == macos.constants.errSecSuccess)(
      IO.raiseError(
        Error(
          s"Save state failed with status $osStatus."
        )
      )
    )
    _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
      Console[IO].println(
        fansi.Color.Green("Saved state to Keychain.")
      )
  yield ()

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
                  IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
                    Console[IO].println(
                      fansi.Color.Green("Received auth code.")
                    )
                  *> Ok("Authorization code received. Return to Plutus.")
            .orNotFound
        )
      )
      .withShutdownTimeout(0.seconds)
      .build
      .use: server =>
        for
          _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
            Console[IO].println(
              fansi.Color.Green("Requesting authorization...")
            )
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
          _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
            Console[IO].println(
              fansi.Color.Green("Exchanging auth code for tokens...")
            )
          createAccessTokenOutput <- monzoTokenApi.createAccessToken(
            grantType = monzo.GrantType.AUTHORIZATION_CODE,
            clientId = clientId,
            clientSecret = clientSecret,
            redirectUri = Some(redirectUri),
            code = Some(authorizationCode)
          )
          _ <- Console[IO].print(
            fansi.Color.Green(
              "Complete SCA in app, then press enter to continue."
            )
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
      output: fs2.io.file.Path,
      dryRun: Boolean
  ): IO[State] = for
    _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
      Console[IO].println(
        fansi.Color.Green("Listing accounts...")
      )
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
    _ <- IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
      Console[IO].println(
        fansi.Color.Green("Listing transactions for accounts...")
      )
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
    _ <- IO.whenA(verbosity.ordinal >= Verbosity.VERBOSE.ordinal):
      Console[IO].println(
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
    materialAccountIdsAndTransactions = accountsAndTransactions.map:
      (account, transactions) =>
        account.id -> transactions.filterNot: transaction =>
          // Active card check.
          transaction.amount.value == 0 ||
            // What it says.
            transaction.declineReason.isDefined
    _ <- writeOfx(toOfx(materialAccountIdsAndTransactions), output, verbosity)
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

  private val defaultAllocator: macos.aliases.CFAllocatorRef =
    macos.aliases.CFAllocatorRef(null)

  private val utf8: macos.aliases.CFStringEncoding =
    macos.aliases.UInt32(macos.constants.kCFStringEncodingUTF8)

  private val secItemName: macos.aliases.CFStringRef =
    macos.functions
      .CFStringCreateWithCString(
        alloc = defaultAllocator,
        cStr = c"plutus",
        encoding = utf8
      )

  extension (ptr: Ptr[?])
    /** This is an unavoidable consequence of the way sn-bindgen generates code
      * and the types the macOS APIs define interact. macOS's CFStringRef for
      * example is generated as:
      *
      * opaque type CFStringRef = Ptr[__CFString]
      *
      * with:
      *
      * opaque type __CFString = CStruct0
      *
      * This is the behaviour documented at
      * https://sn-bindgen.indoorvivants.com/semantics/index.html#structs-are-converted-to-opaque-types.
      *
      * This would be OK if macOS didn't define APIs like CFDictionaryCreate to
      * take heterogenous map inputs like this:
      *
      * keys : Ptr[Ptr[Byte]], values : Ptr[Ptr[Byte]]
      *
      * (Read this as two pointers to arrays, where each array is of
      * heterogeneous type, i.e. some values may be __CFString while others may
      * be __CFBoolean, etc).
      *
      * @param A
      * @return
      */
    def unsafeToPtr[A]: Ptr[A] = ptr.asInstanceOf[Ptr[A]]

  private def toCfDictionary(
      entries: (Ptr[Byte], Ptr[Byte])*
  ): macos.aliases.CFDictionaryRef =
    val keys = stackalloc[Ptr[Byte]](entries.length.toUInt)
    val values = stackalloc[Ptr[Byte]](entries.length.toUInt)
    entries.zipWithIndex.foreach: (entry, index) =>
      val (key, value) = entry
      keys.update(index.toULong, key)
      values.update(index.toULong, value)
    macos.functions.CFDictionaryCreate(
      allocator = defaultAllocator,
      keys = keys,
      values = values,
      numValues = macos.aliases.CFIndex(entries.length),
      keyCallBacks = null,
      valueCallBacks = null
    )

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
      IO.whenA(verbosity.ordinal >= Verbosity.DEFAULT.ordinal):
        Console[IO].println(
          fansi.Color.Green(s"Wrote OFX to $output.")
        )
