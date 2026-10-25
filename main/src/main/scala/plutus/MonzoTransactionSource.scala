package plutus

import cats.effect.*
import cats.syntax.all.*
import smithy4s.*
import smithy4s.json.*

import java.time.Duration
import java.time.Instant

// Every transaction (main accounts and discovered pots) for the window,
// grouped by account so a sink can post each account's transactions to the
// asset account its type maps to (see AssetAccounts), plus each pot backing
// account's pot, so pots get individual asset accounts, currency-checked and
// archived when their pot goes.
def monzoTransactionSource(window: MonzoWindow): TransactionSource =
  new TransactionSource:
    def fetch(now: Instant)(using verbosity: Verbosity): IO[Fetched] =
      val starts = accountStarts(window, now)
      withMonzoApi(starts.all, now): monzoApi =>
        for
          byAccount <- listAllTransactions(
            monzoApi,
            starts,
            before = window match
              case MonzoWindow.Between(_, before) => before.getOrElse(now)
              case MonzoWindow.FromBook(_)        => now
          )
          pots <- potsByAccountId(monzoApi, byAccount, potLinks(byAccount))
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
              currency = None
            ) -> transactions
        yield (
          byAccount = fetchedAccounts,
          pots = pots
        )

// The window a run reads: one the command line names, or one per account
// worked out from a book (see startsFromBook). Nothing about where a run left
// off is kept anywhere else, so a run that names neither has no window.
enum MonzoWindow:
  case Between(since: Instant, before: Option[Instant])

  // One start per account the book knows, or None for one it knows but has
  // nothing filed for. An account missing from the map is new to the book.
  // How far back either kind is read is the source's call (see
  // accountStarts), since only the source knows how far back it can reach.
  case FromBook(starts: BookStarts)

// Where each account's window starts: everywhere at --since when the command
// line names one.
//
// When a book names the starting points, an account it knows without a
// start, or a pot it has never seen, is read from 89 days back: a day inside
// the 90 Strong Customer Authentication allows without authorising again (see
// withMonzoApi), and the most a pot's statement will return at all (see
// discoverPotAccounts). A main account the book has never seen is read from
// its opening if the book knows no account at all — a first run into a fresh
// book — because Monzo serves a main account's whole history for five minutes
// after authorising, which such a run is made to do. A main account opened
// later gets the 89 days, and anything older comes from a CSV statement.
def accountStarts(window: MonzoWindow, now: Instant): AccountStarts =
  window match
    case MonzoWindow.Between(since, _) =>
      AccountStarts(
        known = Map.empty,
        otherwise = ListTransactionsSince.Timestamp(since),
        newAccountsFromOpening = false
      )

    case MonzoWindow.FromBook(starts) =>
      val reachable = ListTransactionsSince.Timestamp:
        now.minus(Duration.ofDays(89))
      AccountStarts(
        known = starts.view
          .mapValues:
            _.fold(reachable)(ListTransactionsSince.Timestamp(_))
          .toMap,
        otherwise = reachable,
        newAccountsFromOpening = starts.isEmpty
      )

// `known` holds the accounts with somewhere to start of their own, which is
// also how a pot /accounts doesn't list is found when no transfer for it falls
// in the window; `otherwise` is where every other account starts, except a
// main account read from its opening.
final case class AccountStarts(
    known: Map[monzo.AccountId, ListTransactionsSince],
    otherwise: ListTransactionsSince,
    newAccountsFromOpening: Boolean
):
  def forAccount(account: monzo.Account): ListTransactionsSince =
    known
      .get(account.id)
      .orElse:
        account.created
          .filter(_ => newAccountsFromOpening)
          .map: created =>
            ListTransactionsSince.Timestamp(created.value.asInstant)
      .getOrElse(otherwise)

  def forPot(accountId: monzo.AccountId): ListTransactionsSince =
    known.getOrElse(accountId, otherwise)

  // Every start the run could use, for withMonzoApi to judge the window by.
  // An account's opening isn't known until /accounts has answered, which needs
  // the token this decides how to get, but it is always further back than
  // Strong Customer Authentication reaches, so the epoch stands in for it.
  def all: List[Instant] =
    (otherwise :: known.values.toList).map(_.start) ++
      Option.when(newAccountsFromOpening)(Instant.EPOCH)

// List every main account's transactions, then the pot accounts discovered
// from their metadata (plus any with a start of their own), combined into a
// single by-account list — no caller consumes the two phases separately. Every
// /accounts account appears as a key, keeping an empty transaction list when
// it had no activity: pot naming needs every owner present, and consumers
// treat an empty value as nothing to do. The verbose entity dump lives here, so every sink emits it.
def listAllTransactions(
    monzoApi: monzo.Api[IO],
    starts: AccountStarts,
    before: Instant
)(using
    verbosity: Verbosity
): IO[List[(monzo.Account, List[monzo.Transaction])]] = for
  _ <- info:
    "Listing accounts…"
  accounts <- monzoApi
    .listAccounts()
    .map:
      _.accounts
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

// Backing-account ID -> pot ID, from pot-transfer legs' metadata: the main
// account's side carries pot_account_id alongside pot_id, and on the pot's own
// side the account the leg was fetched from *is* the backing account. Only the
// window's own transfers are read, so a pot with none in it (a dormant pot
// earning only interest) goes unnamed; the book sink files it by its tag.
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

// Pot backing accounts carry no pot details of their own, but /pots lists
// every pot keyed by pot ID, and potLinks links backing accounts to pot IDs.
// A backing account with no transfer leg in the window stays unresolved;
// unless the book already carries a tagged account for it, import refuses to
// run rather than mis-file. Pots are listed per owning account; byAccount
// carries every main account, even those with no transactions in the window,
// so the owners are complete.
def potsByAccountId(
    monzoApi: monzo.Api[IO],
    byAccount: List[(monzo.Account, List[monzo.Transaction])],
    potIds: Map[monzo.AccountId, monzo.PotId]
)(using verbosity: Verbosity): IO[Map[monzo.AccountId, monzo.Pot]] =
  val potAccountIds = byAccount
    .collect:
      case (account, _) if isPotBacking(account) => account.id
  if potAccountIds.isEmpty then IO.pure(Map.empty)
  else
    for
      _ <- info:
        "Listing pots…"
      pots <- byAccount
        .collect:
          case (account, _) if !isPotBacking(account) => account.id
        .parTraverse: accountId =>
          monzoApi
            .listPots(accountId)
            .map(_.pots)
      potsById = pots.flatten
        .map: pot =>
          pot.id -> pot
        .toMap
    yield potAccountIds
      .flatMap: accountId =>
        potIds
          .get(accountId)
          .flatMap(potsById.get)
          .map(accountId -> _)
      .toMap

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
// only surface as pot_account_id in the metadata of pot-transfer transactions,
// but passing one to /transactions returns the pot's own statement — including
// interest credits, which appear nowhere else. /pots is no alternative source
// for this discovery: nothing in its responses references the pot's backing
// account — current_account_id is the owning account, and the pot ID shares
// only its creation-timestamp prefix with the backing-account ID, so one can't
// be derived from the other. (Import does still use /pots, but only to *name*
// pots discovered here — see potsByAccountId.) Once a pot has a tagged
// account in the book, it's recognisable there as a key /accounts doesn't
// return, so it keeps
// syncing even when no transfer falls in the window. (That inference assumes
// /accounts never stops listing a main account — it keeps returning closed
// ones, so in practice only pots can be resumable but not in /accounts.) Note
// that SCA verification doesn't extend to pot accounts: a window reaching back
// more than 90 days fails with forbidden.verification_required even right
// after authorisation, when main accounts would return their full history.
def discoverPotAccounts(
    starts: AccountStarts,
    accounts: List[monzo.Account],
    accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])]
): List[(monzo.Account, ListTransactionsSince)] =
  starts.known.keySet
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
