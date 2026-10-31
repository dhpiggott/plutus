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
// accounts are listed before the window is settled, because an account's
// opening date is part of it (see accountStarts).
//
// The book is only ever asked about an ID this run already has from Monzo: an
// account /accounts lists, a pot /pots lists, or a pot a transfer in the
// window names. It is never asked which of its own tags are Monzo's, which it
// couldn't answer, since GnuCash's own OFX importer tags other banks' accounts
// with the same slot.
def monzoTransactionSource(book: BookStarts): TransactionSource =
  new TransactionSource:
    def fetch(now: Instant)(using verbosity: Verbosity): IO[Fetched] =
      def listAccounts(monzoApi: monzo.Api[IO]) = for
        _ <- info:
          "Listing accounts…"
        accounts <- monzoApi
          .listAccounts()
          .map:
            _.accounts
        starts = accountStarts(book, accounts, now)
      yield (
        windowStarts = accounts.map(starts.forAccount(_).start),
        plan = (accounts = accounts, starts = starts)
      )
      withMonzoApi(now)(listAccounts): (monzoApi, planned) =>
        for
          pots <- listPots(monzoApi, planned.accounts)
          bookPots = bookPotAccounts(book, pots)
          byAccount <- listAllTransactions(
            monzoApi,
            planned.starts,
            planned.accounts,
            bookPots.keySet,
            before = now
          )
          potsByAccount = potsByAccountId(byAccount, pots, bookPots)
          _ <- warnUnlinkedPots(pots, potsByAccount)
          // The one reading of the missing-type rule: every sink downstream
          // takes potBacking's word for it rather than re-deriving it from a
          // shape only /accounts produces. See FetchedAccount.
          fetchedAccounts = byAccount.map: (account, transactions) =>
            FetchedAccount(
              id = account.id,
              accountType = account.accountType,
              closed = account.closed.exists(_.value),
              potBacking = isPotBacking(account),
              // /accounts and /transactions never name a currency; a pot's
              // own details do, and the book sink checks those separately.
              // See FetchedAccount.
              currency = None,
              description = account.description
            ) -> transactions
        yield (
          byAccount = fetchedAccounts,
          pots = potsByAccount
        )

// Where each account's window starts. A main account the book has never seen
// is read from its opening, because Monzo serves a main account's whole
// history for five minutes after authorising, and withMonzoApi asks for that
// whenever a window reaches back further than 90 days. A closed one is only
// read that way on a first run into a fresh book, one that knows none of the
// accounts /accounts lists: one with nothing ever filed in it would otherwise
// be unknown to the book, and ask for authorisation again, on every run. An
// account the book knows without a start, a closed account it has never seen,
// or a pot it has never seen, is read from 89 days back: a day inside the 90
// Strong Customer Authentication allows without authorising again, and the
// most a pot's statement will return at all (see discoverPotAccounts).
// Anything older for those comes from a CSV statement.
def accountStarts(
    book: BookStarts,
    accounts: List[monzo.Account],
    now: Instant
): AccountStarts =
  AccountStarts(
    book = book,
    reachable = now.minus(Duration.ofDays(89)),
    freshBook = accounts.forall: account =>
      book.start(account.id).isEmpty
  )

final case class AccountStarts(
    book: BookStarts,
    reachable: Instant,
    freshBook: Boolean
):
  def forAccount(account: monzo.Account): ListTransactionsSince =
    ListTransactionsSince.Timestamp:
      book.start(account.id) match
        case Some(start) => start.getOrElse(reachable)
        case None        =>
          account.created
            .filter(_ => freshBook || !account.closed.exists(_.value))
            .fold(reachable)(_.value.asInstant)

  def forPot(accountId: monzo.AccountId): ListTransactionsSince =
    ListTransactionsSince.Timestamp:
      book.start(accountId).flatten.getOrElse(reachable)

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
// /accounts account appears as a key, keeping an empty transaction list when
// it had no activity, and consumers treat an empty value as nothing to do. The
// verbose entity dump lives here, so every sink emits it.
def listAllTransactions(
    monzoApi: monzo.Api[IO],
    starts: AccountStarts,
    accounts: List[monzo.Account],
    bookPotAccountIds: Set[monzo.AccountId],
    before: Instant
)(using
    verbosity: Verbosity
): IO[List[(monzo.Account, List[monzo.Transaction])]] = for
  accountsAndSince = accounts.map: account =>
    (account, starts.forAccount(account))
  _ <- info:
    "Listing transactions for accounts…"
  accountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    accountsAndSince,
    before
  )
  potAccountsAndSince = discoverPotAccounts(
    starts,
    bookPotAccountIds,
    accounts,
    accountsAndTransactions
  )
  _ <- (IO.whenA(potAccountsAndSince.nonEmpty)):
    info:
      "Listing transactions for pot accounts…"
  potAccountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    potAccountsAndSince,
    before
  )
  _ <- log(Verbosity.VERBOSE):
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
yield accountsAndTransactions ++ potAccountsAndTransactions

// Pot backing accounts carry no pot details of their own, but /pots lists
// every pot keyed by pot ID, and two things link a backing account to one: a
// transfer in the window (potLinks) and the book. Where both do, they agree,
// and the window's is taken. A backing account neither links stays unnamed;
// unless the book already carries a tagged account for it, import refuses to
// run rather than mis-file.
def potsByAccountId(
    byAccount: List[(monzo.Account, List[monzo.Transaction])],
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
      case (account, _) if isPotBacking(account) => account.id
    .toSet
  (bookPots ++ windowPots).filter: (accountId, _) =>
    potAccountIds.contains(accountId)

// Backing-account ID -> pot ID, from pot-transfer legs' metadata: the main
// account's side carries pot_account_id alongside pot_id, and on the pot's own
// side the account the leg was fetched from *is* the backing account. Only the
// window's own transfers are read; a pot with none in it is linked by the book
// if it is linked at all.
def potLinks(
    byAccount: List[(monzo.Account, List[monzo.Transaction])]
): Map[monzo.AccountId, monzo.PotId] =
  byAccount
    .flatMap: (account, transactions) =>
      transactions.flatMap: transaction =>
        potId(transaction).flatMap: potId =>
          if isPotBacking(account) then Some(account.id -> potId)
          else potAccountId(transaction).map(_ -> potId)
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

// A pot backing account is constructed from transfer metadata rather than
// decoded from /accounts, so it never carries a type — this absence *is* the
// definition of "pot backing account" (see AccountType in the smithy spec).
def isPotBacking(account: monzo.Account): Boolean =
  account.accountType.isEmpty

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

// Pots are backed by account objects that /accounts doesn't list. Their IDs
// surface as pot_account_id in the metadata of pot-transfer transactions, and
// passing one to /transactions returns the pot's own statement — including
// interest credits, which appear nowhere else. /pots can't say which backing
// account a pot has: nothing in its responses references it —
// current_account_id is the owning account, and the pot ID shares only its
// creation-timestamp prefix with the backing-account ID, so one can't be
// derived from the other. So a transfer is the only way to learn the link, and
// the book keeps it from then on (see bookPotAccounts), which is what keeps a
// pot syncing when no transfer falls in the window. Note that SCA verification
// doesn't extend to pot accounts: a window reaching back more than 90 days
// fails with forbidden.verification_required even right after authorisation,
// when main accounts would return their full history.
def discoverPotAccounts(
    starts: AccountStarts,
    bookPotAccountIds: Set[monzo.AccountId],
    accounts: List[monzo.Account],
    accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])]
): List[(monzo.Account, ListTransactionsSince)] =
  bookPotAccountIds
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
    .map: accountId =>
      (monzo.Account(accountId), starts.forPot(accountId))

def discoveredPotAccountIds(
    accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])]
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
