package plutus

import cats.effect.*
import cats.syntax.all.*
import smithy4s.*
import smithy4s.json.*

import java.time.Duration
import java.time.Instant

// Every transaction (main accounts and pots) from where the book left off with
// each account (see startsFromBook) to now, grouped by account so a sink can
// post each account's transactions to the asset account its type maps to (see
// AssetAccounts), plus each pot backing account's pot, so pots get individual
// asset accounts, currency-checked and archived when their pot goes. The
// accounts and pots are listed before the window is settled, because an
// account's or pot's opening date is part of it (see accountStarts).
//
// The book is only ever asked about an ID this run already has from Monzo: an
// account /accounts lists, a pot /pots lists, or a pot a transfer in the
// window names. It is never asked which of its own tags are Monzo's, which it
// couldn't answer, since GnuCash's own OFX importer tags other banks' accounts
// with the same slot.
def monzoTransactionSource(book: BookStarts): TransactionSource =
  new TransactionSource:
    def fetch(now: Instant)(using verbosity: Verbosity): IO[Fetched] =
      def listAccountsAndPots(monzoApi: monzo.Api[IO]): IO[
        (
            earliestWindowStart: Instant,
            plan: (
                accounts: List[monzo.Account],
                pots: List[monzo.Pot],
                starts: AccountStarts,
                earliestWindowStart: Instant
            )
        )
      ] = for
        _ <- info:
          "Listing accounts…"
        accounts <- monzoApi
          .listAccounts()
          .map:
            _.accounts
        pots <- listPots(monzoApi, accounts)
        starts = accountStarts(book, accounts)
        // A run that reads nothing asks for nothing beyond what any token can
        // do.
        earliestWindowStart = (accounts.flatMap(starts.forAccount) ++
          pots.flatMap(starts.forListedPot)).minOption
          .getOrElse(now)
      yield (
        earliestWindowStart = earliestWindowStart,
        plan = (
          accounts = accounts,
          pots = pots,
          starts = starts,
          earliestWindowStart = earliestWindowStart
        )
      )
      withMonzoApi(now = now)(plan = listAccountsAndPots)(
        use = (monzoApi, planned) =>
          val bookPots = bookPotAccounts(book, planned.pots)
          for
            byAccount <- listAllTransactions(
              monzoApi,
              planned.starts,
              planned.accounts,
              planned.pots,
              bookPots,
              planned.earliestWindowStart,
              before = now
            )
            potsByAccount = potsByAccountId(byAccount, planned.pots, bookPots)
            _ <- warnUnlinkedPots(planned.pots, potsByAccount)
            // Every sink downstream takes potBacking's word for which accounts
            // back pots rather than re-deriving it from a shape only /accounts
            // produces. /accounts and /transactions never name a currency; a
            // pot's own details do, and the book sink checks those
            // separately. See FetchedAccount.
            fetchedAccounts = byAccount.map: (account, transactions) =>
              val fetchedAccount = account match
                case ReadAccount.Main(mainAccount) =>
                  FetchedAccount(
                    id = mainAccount.id,
                    accountType = mainAccount.accountType,
                    closed = mainAccount.closed.exists(_.value),
                    potBacking = false,
                    currency = None,
                    description = mainAccount.description
                  )

                case ReadAccount.PotBacking(accountId) =>
                  FetchedAccount(
                    id = accountId,
                    accountType = None,
                    closed = false,
                    potBacking = true,
                    currency = None,
                    description = None
                  )
              fetchedAccount -> transactions
          yield (
            byAccount = fetchedAccounts,
            pots = potsByAccount
          )
      )

// Where each account's window starts, or None for one that isn't read. Main
// accounts and pots with nothing filed in the book are read from their
// opening, because Monzo serves a main account's whole history for five
// minutes after authorising, and withMonzoApi asks for that whenever a window
// reaches back further than 90 days. That includes one the book has tagged
// while it was empty: it may have taken transactions since, and nothing
// records when it was last read, so a later start could miss them.
//
// A closed account or a deleted pot is only read on a first run into a fresh
// book, one with nothing filed for any account /accounts lists. Otherwise it
// is still listed, with nothing in it, so the book sink archives it, but it is
// not read: it takes no new transactions, and reading one whose last filed
// transaction is more than 90 days old would ask for authorisation on every
// run. Whatever it took between the last run and its closure comes from a CSV
// statement.
def accountStarts(
    book: BookStarts,
    accounts: List[monzo.Account]
): AccountStarts =
  AccountStarts(
    book = book,
    freshBook = accounts.forall: account =>
      book.start(account.id).isEmpty
  )

final case class AccountStarts(
    book: BookStarts,
    freshBook: Boolean
):
  def forAccount(account: monzo.Account): Option[Instant] =
    Option.when(freshBook || !account.closed.exists(_.value)):
      book
        .start(account.id)
        .getOrElse:
          account.created.value.asInstant

  def readsPot(pot: monzo.Pot): Boolean =
    freshBook || !pot.deleted.value

  // A pot's start as far as the plan can know it, which is only for a pot the
  // book links to its backing account, and on a first run, when every pot the
  // window names is read from its opening. Any other pot is one a transfer in
  // the window may yet name, and discoverPotAccounts keeps it inside what the
  // plan authorised.
  def forListedPot(pot: monzo.Pot): Option[Instant] =
    if !readsPot(pot) then None
    else
      book.potAccount(pot.id) match
        case Some(accountId) =>
          Some(forPot(accountId, pot))

        case None =>
          Option.when(freshBook)(pot.created.value.asInstant)

  def forPot(accountId: monzo.AccountId, pot: monzo.Pot): Instant =
    book
      .start(accountId)
      .getOrElse:
        pot.created.value.asInstant

// Every pot of every main account, deleted ones included: /pots is listed per
// owning account, and /accounts lists every main account, closed ones too.
def listPots(
    monzoApi: monzo.Api[IO],
    accounts: List[monzo.Account]
)(using verbosity: Verbosity): IO[List[monzo.Pot]] = for
  _ <- info:
    "Listing pots…"
  pots <- accounts.parTraverse: account =>
    monzoApi
      .listPots(account.id)
      .map(_.pots)
yield pots.flatten

// The pots the book already has an account for, keyed by that account's
// backing-account ID. This is how a pot with no transfer in the window, such
// as a dormant one earning only interest, is still read: nothing in /pots
// names a pot's backing account (see discoverPotAccounts), but the book
// records the link once a run has seen one (see Account.tagPotId).
def bookPotAccounts(
    book: BookStarts,
    pots: List[monzo.Pot]
): Map[monzo.AccountId, monzo.Pot] =
  pots
    .flatMap: pot =>
      book.potAccount(pot.id).map(_ -> pot)
    .toMap

// List every main account's transactions, then the pot accounts discovered
// from their metadata and the ones the book links to a pot, combined into a
// single by-account list — no caller consumes the two phases separately. Every
// /accounts account, and every pot the book links, appears as a key, keeping
// an empty transaction list when it had no activity or wasn't read (see
// accountStarts), and consumers treat an empty value as nothing to file. The
// verbose entity dump lives here, so every sink emits it.
def listAllTransactions(
    monzoApi: monzo.Api[IO],
    starts: AccountStarts,
    accounts: List[monzo.Account],
    pots: List[monzo.Pot],
    bookPots: Map[monzo.AccountId, monzo.Pot],
    earliestWindowStart: Instant,
    before: Instant
)(using
    verbosity: Verbosity
): IO[List[(ReadAccount, List[monzo.Transaction])]] = for
  accountsAndSince = accounts.flatMap: account =>
    starts
      .forAccount(account)
      .map: start =>
        (ReadAccount.Main(account), ListTransactionsSince.Timestamp(start))
  _ <- info:
    "Listing transactions for accounts…"
  accountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    accountsAndSince,
    before
  )
  potAccounts = discoverPotAccounts(
    starts,
    pots,
    bookPots,
    earliestWindowStart,
    accounts,
    accountsAndTransactions
  )
  // A transfer can name a backing account whose pot /pots doesn't list, which
  // shouldn't happen, since /pots lists every pot, deleted ones included. Such
  // a pot has no opening to read from, and reading it from as far back as
  // needs no fresh authorisation could miss something, so it isn't read.
  _ <- potAccounts.unidentified.traverse_ : accountId =>
    warn:
      s"The pot behind ${accountId.value} wasn't read: a transfer names it, but no pot Monzo lists matches. Bring its transactions in with --from-csv-pot."
  // Only a pot the plan couldn't know about, and so didn't authorise for, is
  // clamped (see discoverPotAccounts); say so rather than miss its earlier
  // rows silently.
  _ <- potAccounts.clamped.traverse_ : clamped =>
    warn:
      s"The pot behind ${clamped.accountId.value} was only read from ${earliestWindowStart}, not from ${clamped.start}, because this run didn't authorise for further back. Bring anything older in with --from-csv-pot."
  _ <- (IO.whenA(potAccounts.read.nonEmpty)):
    info:
      "Listing transactions for pot accounts…"
  potAccountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    potAccounts.read,
    before
  )
  readIds = (accountsAndTransactions ++ potAccountsAndTransactions)
    .map((account, _) => account.id)
    .toSet
  bookPotBackings = bookPots.keys.toList
    .sortBy(_.value)
    .map(ReadAccount.PotBacking(_))
  unread = (accounts.map(ReadAccount.Main(_)) ++ bookPotBackings)
    .filterNot(account => readIds.contains(account.id))
    .map(_ -> List.empty[monzo.Transaction])
  _ <- log(Verbosity.VERBOSE):
    Json.writeDocumentAsPrettyString:
      Document.array:
        (accountsAndTransactions ++ potAccountsAndTransactions).map:
          (account, transactions) =>
            Document.obj(
              "account" -> (account match
                case ReadAccount.Main(mainAccount) =>
                  Document.encode(mainAccount)

                case ReadAccount.PotBacking(accountId) =>
                  Document.obj("id" -> Document.DString(accountId.value))),
              "transactions" -> Document.array:
                transactions.map:
                  Document.encode(_)
            )
yield accountsAndTransactions ++ potAccountsAndTransactions ++ unread

// Pot backing accounts carry no pot details of their own, but /pots lists
// every pot keyed by pot ID, and two things link a backing account to one: a
// transfer in the window (potLinks) and the book. Where both do, they agree,
// and the window's is taken. A backing account neither links stays unnamed;
// unless the book already carries a tagged account for it, import refuses to
// run rather than mis-file.
def potsByAccountId(
    byAccount: List[(ReadAccount, List[monzo.Transaction])],
    pots: List[monzo.Pot],
    bookPots: Map[monzo.AccountId, monzo.Pot]
): Map[monzo.AccountId, monzo.Pot] =
  val potsById = pots
    .map: pot =>
      pot.id -> pot
    .toMap
  val windowPots = potLinks(byAccount).flatMap: (accountId, potId) =>
    potsById.get(potId).map(accountId -> _)
  val potAccountIds = byAccount
    .collect:
      case (ReadAccount.PotBacking(accountId), _) => accountId
    .toSet
  (bookPots ++ windowPots).filter: (accountId, _) =>
    potAccountIds.contains(accountId)

// Backing-account ID -> pot ID, from pot-transfer legs' metadata: the main
// account's side carries pot_account_id alongside pot_id, and on the pot's own
// side the account the leg was fetched from *is* the backing account. Only the
// window's own transfers are read; a pot with none in it is linked by the book
// if it is linked at all.
def potLinks(
    byAccount: List[(ReadAccount, List[monzo.Transaction])]
): Map[monzo.AccountId, monzo.PotId] =
  byAccount
    .flatMap: (account, transactions) =>
      transactions.flatMap: transaction =>
        potId(transaction).flatMap: potId =>
          account match
            case ReadAccount.PotBacking(accountId) =>
              Some(accountId -> potId)

            case ReadAccount.Main(_) =>
              potAccountId(transaction).map(_ -> potId)
    .toMap

// A live pot holding money that nothing links to a backing account was not
// read at all, and would otherwise go unnoticed: no error, just a pot whose
// interest stops arriving. It happens to a pot whose book account predates
// the book recording pot links, until its next transfer falls in a run's
// window. An empty pot is left out, since there is nothing in it to miss.
def warnUnlinkedPots(
    pots: List[monzo.Pot],
    linked: Map[monzo.AccountId, monzo.Pot]
)(using verbosity: Verbosity): IO[Unit] =
  val linkedIds = linked.values.map(_.id).toSet
  pots
    .filter: pot =>
      !pot.deleted.value && pot.balance.exists(_.value.signum != 0) &&
        !linkedIds.contains(pot.id)
    .traverse_ : pot =>
      warn:
        s"Pot ${pot.name.value} wasn't read: nothing in the book or in this run's window links it to its backing account. The next run whose window holds a transfer to or from it will link it."

// An account a run reads: a main account, as /accounts lists it, or a pot's
// backing account, which /accounts never lists, and which a transfer's
// metadata and the book name only by ID.
enum ReadAccount:
  case Main(account: monzo.Account)
  case PotBacking(accountId: monzo.AccountId)

  def id: monzo.AccountId = this match
    case Main(account) =>
      account.id

    case PotBacking(accountId) =>
      accountId

def listTransactionsForAccounts(
    monzoApi: monzo.Api[IO],
    accountsAndSince: List[(ReadAccount, ListTransactionsSince)],
    before: Instant
): IO[List[(ReadAccount, List[monzo.Transaction])]] =
  accountsAndSince
    .parTraverse: (account, since) =>
      listTransactions(
        monzoApi,
        accountId = account.id,
        since,
        before
      ).map: transactions =>
        account -> transactions

// Pots are backed by account objects that /accounts doesn't list. Their IDs
// surface as pot_account_id in the metadata of pot-transfer transactions, and
// passing one to /transactions returns the pot's own statement — including
// interest credits, which appear nowhere else. /pots can't say which backing
// account a pot has: nothing in its responses references it —
// current_account_id is the owning account, and the pot ID shares only its
// creation-timestamp prefix with the backing-account ID, so one can't be
// derived from the other. So a transfer is the only way to learn the link, and
// the book keeps it from then on (see bookPotAccounts), which is what keeps a
// pot syncing when no transfer falls in the window. SCA verification was once
// seen not to extend to pot accounts, a window reaching back more than 90 days
// failing with forbidden.verification_required even right after
// authorisation, when main accounts returned their full history. A pot is read
// from its opening regardless, on the chance that has changed; if it hasn't,
// the run fails on the first pot older than 90 days with nothing filed.
//
// A pot is read from where the book left off with it, else from its opening
// (see AccountStarts.forPot), but never from before the earliest start the
// plan authorised for: a pot the book doesn't link wasn't in the plan, and a
// window reaching back past what the run authorised fails outright. Such a
// pot's history from before its first transfer is usually empty anyway, but
// one the book holds under an account from before pot links were recorded can
// have rows the clamp skips, so every clamped pot is reported. A pot /pots
// doesn't list has no opening to read from at all, so it is reported and left
// unread rather than read from a guess.
def discoverPotAccounts(
    starts: AccountStarts,
    pots: List[monzo.Pot],
    bookPots: Map[monzo.AccountId, monzo.Pot],
    earliestWindowStart: Instant,
    accounts: List[monzo.Account],
    accountsAndTransactions: List[(ReadAccount, List[monzo.Transaction])]
): (
    read: List[(ReadAccount, ListTransactionsSince)],
    unidentified: List[monzo.AccountId],
    clamped: List[(accountId: monzo.AccountId, start: Instant)]
) =
  val potsById = pots
    .map: pot =>
      pot.id -> pot
    .toMap
  val windowPots = potLinks(accountsAndTransactions).flatMap:
    (accountId, potId) => potsById.get(potId).map(accountId -> _)
  val potAccountIds = bookPots
    .collect:
      case (accountId, pot) if starts.readsPot(pot) => accountId
    .toSet
    .union:
      discoveredPotAccountIds(accountsAndTransactions)
    .diff:
      accounts
        .map:
          _.id
        .toSet
    .toList
    .sortBy:
      _.value
  val (unidentified, read) = potAccountIds.partitionMap: accountId =>
    windowPots
      .get(accountId)
      .orElse(bookPots.get(accountId))
      .toRight(accountId)
      .map: pot =>
        (accountId, starts.forPot(accountId, pot))
  val clamped = read.collect:
    case (accountId, start) if start.isBefore(earliestWindowStart) =>
      (accountId = accountId, start = start)
  val readFrom = read.map: (accountId, start) =>
    (
      ReadAccount.PotBacking(accountId),
      ListTransactionsSince.Timestamp:
        if start.isBefore(earliestWindowStart) then earliestWindowStart
        else start
    )
  (read = readFrom, unidentified = unidentified, clamped = clamped)

def discoveredPotAccountIds(
    accountsAndTransactions: List[(ReadAccount, List[monzo.Transaction])]
): Set[monzo.AccountId] =
  accountsAndTransactions
    .flatMap: (_, transactions) =>
      transactions.flatMap:
        potAccountId
    .toSet

def potAccountId(transaction: monzo.Transaction): Option[monzo.AccountId] =
  transaction.metadata.flatMap(_.potAccountId)

def potId(transaction: monzo.Transaction): Option[monzo.PotId] =
  transaction.metadata.flatMap(_.potId)

enum ListTransactionsSince:
  case Timestamp(instant: Instant)
  case IdAndTimestamp(id: monzo.TransactionId, created: monzo.Created)

  def start: Instant = this match
    case Timestamp(instant) =>
      instant

    case IdAndTimestamp(_, created) =>
      created.value.asInstant

def listTransactions(
    monzoApi: monzo.Api[IO],
    accountId: monzo.AccountId,
    since: ListTransactionsSince,
    before: Instant
): IO[List[monzo.Transaction]] =
  val beforeForThisPage =
    // See
    // https://community.monzo.com/t/changes-when-listing-with-our-api/158676.
    val maxPermittedBeforeForThisPage = since.start.plus:
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

          case ListTransactionsSince.IdAndTimestamp(id, _) =>
            id.value)),
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
          since = ListTransactionsSince.IdAndTimestamp(
            transaction.id,
            transaction.created
          ),
          before
        )
  yield thisPage ++ otherPages
