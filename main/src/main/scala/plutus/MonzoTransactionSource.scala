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
// archived when their pot goes. The Monzo session's whole state lifecycle
// lives here: pot links are recorded whenever they're seen (see potLinks) and
// the bookmarks are advanced only if advanceBookmarks and `consume` both say
// so, which is what makes a source the one thing a sink has to be given.
//
// Import passes advanceBookmarks = false rather than !dryRun: it dedups on the
// online_id slot rather than on bookmarks, so advancing them would make the
// next export skip the very window that was just imported.
def monzoTransactionSource(
    since: Option[Instant],
    before: Option[Instant],
    advanceBookmarks: Boolean
): TransactionSource = new TransactionSource:
  def use[A](now: Instant)(consume: Fetched => IO[A])(using
      verbosity: Verbosity
  ): IO[A] =
    withMonzoApi(since, now): (monzoApi, state) =>
      for
        byAccount <- listAllTransactions(
          monzoApi,
          state,
          since,
          before = before.getOrElse(now)
        )
        // Merge before resolving, so this run's own discoveries serve this
        // run's pots too.
        potIds = state.potIds ++ potLinks(byAccount)
        pots <- potsByAccountId(monzoApi, byAccount, potIds)
        // The one reading of the missing-type rule: every sink downstream
        // takes potBacking's word for it rather than re-deriving it from a
        // shape only /accounts produces. See FetchedAccount.
        fetchedAccounts = byAccount.map: (account, transactions) =>
          FetchedAccount(
            id = account.id,
            accountType = account.accountType,
            closed = account.closed.exists(_.value),
            potBacking = isPotBacking(account),
            // /accounts and /transactions never name a currency; a pot's own
            // details do, and the book sink checks those separately. See
            // FetchedAccount.
            currency = None
          ) -> transactions
        result <- consume(
          (
            byAccount = fetchedAccounts,
            pots = pots,
            // Bookmark mode carries on from each account's last exported
            // transaction; a --since run names its own window.
            incremental = since.isEmpty
          )
        )
        // Pot links are facts about Monzo's account topology, not export
        // progress, so they're recorded even on a dry run; only the bookmarks
        // respect --dry-run. See potLinks.
        linkedState = state.copy(potIds = potIds)
      yield (
        state =
          if !advanceBookmarks then linkedState
          else
            linkedState.copy(
              lastTransactions = linkedState.lastTransactions ++
                byAccount
                  .map: (account, transactions) =>
                    account.id -> transactions.lastOption
                  .collect:
                    case (accountId, Some(lastTransaction)) =>
                      accountId -> LastTransaction(
                        lastTransaction.id,
                        lastTransaction.created
                      )
            )
        ,
        result = result
      )

// List every main account's transactions, then the pot accounts discovered
// from their metadata (plus any already bookmarked in state), combined into a
// single by-account list — no caller consumes the two phases separately. Every
// /accounts account appears as a key, keeping an empty transaction list when
// nothing was fetched for it (no bookmark in bookmark mode, or no activity):
// pot naming needs every owner present, and consumers treat an empty value as
// nothing to do. The verbose entity dump lives here, so every sink emits it.
def listAllTransactions(
    monzoApi: monzo.Api[IO],
    state: State,
    since: Option[Instant],
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
  accountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    accountsAndSince,
    before
  )
  potAccountsAndSince = discoverPotAccounts(
    state,
    accounts,
    accountsAndTransactions,
    since
  )
  _ <- (IO.whenA(potAccountsAndSince.nonEmpty)):
    info:
      "Listing transactions for pot accounts…"
  potAccountsAndTransactions <- listTransactionsForAccounts(
    monzoApi,
    potAccountsAndSince,
    before
  )
  newPotAccountIds = discoveredPotAccountIds(accountsAndTransactions)
    .diff:
      accounts
        .map:
          _.id
        .toSet
        .union:
          potAccountsAndSince
            .map: (account, _) =>
              account.id
            .toSet
  _ <- (IO.whenA(newPotAccountIds.nonEmpty)):
    warn:
      s"Found pot accounts with no last recorded transaction; specify --since to fetch their transactions: ${newPotAccountIds.toList.map(_.value).sorted.mkString(", ")}"
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
  fetchedMainIds = accountsAndTransactions
    .map: (account, _) =>
      account.id
    .toSet
  unfetchedMain = accounts
    .filterNot: account =>
      fetchedMainIds(account.id)
    .map(_ -> List.empty[monzo.Transaction])
yield accountsAndTransactions ++ unfetchedMain ++ potAccountsAndTransactions

// Backing-account ID -> pot ID, from pot-transfer legs' metadata: the main
// account's side carries pot_account_id alongside pot_id, and on the pot's own
// side the account the leg was fetched from *is* the backing account. The
// Monzo source merges these into State.potIds on every fetch, whichever sink
// asked for it, so a link seen once names the pot forever — even in a later
// window holding no transfer for it (a dormant pot earning only interest).
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
// every pot keyed by pot ID, and State.potIds links backing accounts to pot
// IDs. A backing account with no recorded link (bookmarked before links were
// recorded, and no transfer leg seen since) stays unresolved; unless the book
// already carries a tagged account for it, import refuses to run rather than
// mis-file — one run whose window spans a transfer for the pot records the
// link. Pots are listed per owning account; byAccount carries every main
// account, even those with no transactions in the window, so the owners are
// complete.
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
// pots discovered here — see potsByAccountId.) Once a pot has a bookmark in
// the state store it's recognisable there as a key /accounts doesn't return,
// so it keeps syncing even when no transfer falls in the export window. (That
// inference assumes /accounts never stops listing a main account — it keeps
// returning closed ones, so in practice only pots can be in state but not in
// /accounts.) Note that SCA verification doesn't extend to pot accounts: a
// window reaching back more than 90 days fails with
// forbidden.verification_required even right after authorisation, when main
// accounts would return their full history.
def discoverPotAccounts(
    state: State,
    accounts: List[monzo.Account],
    accountsAndTransactions: List[(monzo.Account, List[monzo.Transaction])],
    since: Option[Instant]
): List[(monzo.Account, ListTransactionsSince)] =
  val mainAccountIds = accounts
    .map:
      _.id
    .toSet
  val bookmarked = state.lastTransactions.keySet.diff:
    mainAccountIds
  since match
    case Some(since) =>
      val discovered = discoveredPotAccountIds(accountsAndTransactions).diff:
        mainAccountIds
      bookmarked
        .union:
          discovered
        .toList
        .sortBy:
          _.value
        .map: accountId =>
          monzo.Account(accountId) -> ListTransactionsSince.Timestamp(since)

    case None =>
      bookmarked.toList
        .sortBy:
          _.value
        .map: accountId =>
          monzo.Account(accountId) -> ListTransactionsSince.IdAndTimestamp(
            state.lastTransactions(accountId)
          )

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
