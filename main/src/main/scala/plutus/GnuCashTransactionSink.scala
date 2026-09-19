package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

def gnuCashTransactionSink(
    source: TransactionSource,
    input: fs2.io.file.Path,
    dryRun: Boolean,
    ignoreLock: Boolean
)(using verbosity: Verbosity): IO[Unit] = for
  // Checked here as well as in withBook below, so a mistyped --input costs a
  // message rather than the whole OAuth-and-fetch round trip that would
  // otherwise run before the book is ever opened.
  _ <- requireExistingBook(input)
  // The run's instant, taken at invocation and spent on both the source's
  // window and this run's own writes — the backup's name and every row's
  // enter_date — so one run stamps one time. Same as archiveAccounts and
  // restoreAccount, which read their own here too.
  now <- IO.realTimeInstant
  // The zone the transactions' calendar dates are taken in (see
  // neutralPostDate), read once so every row of a run agrees.
  zone <- IO.delay(ZoneId.systemDefault)
  // IO.pure, not the book work: the book is opened after the source has let
  // go, so nothing it holds open outlives the fetch, and the pot links it
  // records survive an import that then fails.
  fetched <- source.use(now)(IO.pure)
  byAccount = fetched.byAccount
  pots = fetched.pots
  _ <- withBook(input, now, dryRun, ignoreLock): db =>
    given Database[IO] = db
    val assetAccounts = AssetAccounts.default
    // Only material transactions get posted; an account with none needs no
    // asset account (byAccount lists every account, active or not) and would
    // only add noise below, so drop it here.
    val materialByAccount = byAccount
      .map: (account, transactions) =>
        (account, materialTransactions(transactions))
      .filter: (_, transactions) =>
        transactions.nonEmpty
    val run = for
      // Fail fast, before anything is resolved or written: an account type
      // missing from the map needs a byAccountType entry, not a guess.
      unmappedTypes = materialByAccount
        .flatMap: (account, _) =>
          account.accountType.filterNot: accountType =>
            assetAccounts.byAccountType.contains(accountType.value)
        .map(_.value)
        .distinct
      _ <- IO.raiseUnless(unmappedTypes.isEmpty):
        Error(
          s"No asset account mapped for Monzo account type(s) ${unmappedTypes.mkString(", ")}; add them to AssetAccounts.byAccountType."
        )
      // Fail fast on the assumption the dedup set below rests on. That
      // set is read once, before the write loop, and never updated as
      // postings are inserted, so one transaction ID fetched twice in the
      // same run would be judged unseen twice and posted twice — and there
      // is no unique constraint on the online_id slot to catch it. Monzo's
      // model says a pot transfer is one transaction in the main account's
      // statement and a separate one in the pot's own (see
      // monzoTransactionSource), so neither check below should ever
      // fire; they are here because the alternative to them firing is a
      // silent double-post. They are separate checks because the two say
      // different things about Monzo: the same ID under two accounts means
      // one transaction appears in two statements, while the same ID twice
      // under one account means a page repeated it.
      occurrences = materialByAccount
        .flatMap: (account, material) =>
          material.map: transaction =>
            transaction.id.value -> account.id.value
        .groupMap((transactionId, _) => transactionId): (_, monzoAccountId) =>
          monzoAccountId
      acrossAccounts = occurrences.filter: (_, monzoAccountIds) =>
        monzoAccountIds.distinct.sizeIs > 1
      _ <- IO.raiseUnless(acrossAccounts.isEmpty):
        val listed = acrossAccounts.toList
          .sortBy((transactionId, _) => transactionId)
          .map: (transactionId, monzoAccountIds) =>
            s"$transactionId (in ${monzoAccountIds.distinct.sorted.mkString(", ")})"
          .mkString("; ")
        Error(
          s"Monzo returned the same transaction under more than one account in a single run: $listed. Filing every occurrence would double-count it, so nothing has been written. Please report this."
        )
      withinAccount = occurrences.filter: (_, monzoAccountIds) =>
        monzoAccountIds.distinct.sizeIs == 1 && monzoAccountIds.sizeIs > 1
      _ <- IO.raiseUnless(withinAccount.isEmpty):
        val listed = withinAccount.toList
          .sortBy((transactionId, _) => transactionId)
          .map: (transactionId, monzoAccountIds) =>
            s"$transactionId (${monzoAccountIds.size} times in ${monzoAccountIds.head})"
          .mkString("; ")
        Error(
          s"Monzo returned the same transaction more than once for one account in a single run: $listed. Filing every occurrence would double-count it, so nothing has been written. Please report this."
        )
      // Every typed account paired with the code-defined path its type
      // maps to. The mapping is type-keyed, so several Monzo accounts — a
      // closed account and the one that replaced it — can share a path;
      // the Monzo account ID in the leaf name is what keeps them apart
      // (see assetAccountPath), so each gets its own asset account and is
      // retired on its own closure rather than the whole type's.
      typedAccountsAndPaths = byAccount.flatMap: (account, _) =>
        account.accountType
          .flatMap: accountType =>
            assetAccounts.byAccountType.get(accountType.value)
          .map: assetPath =>
            (account, assetPath)
      allMonzoPotAccountIds = byAccount.collect:
        case (account, _) if account.potBacking => account.id
      materialMonzoPotAccountIds = materialByAccount.collect:
        case (account, _) if account.potBacking => account.id
      // An account a source names without saying what kind it is — a CSV
      // statement gives an ID and nothing else (see csvTransactionSource).
      // There's no type to map to a code-defined path, so the book's own tag
      // is the whole of the resolution below and the account is posted to
      // exactly where it already sits: a placement enforced from a path
      // guessed at here would move a joint account under the personal
      // account's, and would do it on every run.
      untypedMonzoAccountIds = byAccount.collect:
        case (account, _)
            if account.accountType.isEmpty && !account.potBacking =>
          account.id
      // Every online_id in the book, in one scan: the tags below and the
      // dedup check further down are the run's only two readers of them,
      // and both would otherwise scan an unindexed table that grows with
      // the book's history. See Slot.onlineIds.
      onlineIds <- Slot.onlineIds
      // The book is the durable home of the account associations: each
      // asset account is tagged with the Monzo account ID that posts into
      // it in an account-level online_id slot, so a book that outlives the
      // state store (say, moved to a new machine) still resolves by tag.
      // Resolved from the prefetch by primary key, and only for the IDs
      // this run asks about — most online_id slots name a split, not an
      // account. Two accounts tagged with one Monzo account ID fail the
      // run: nothing in the book says which is meant, and posting into the
      // wrong one would be permanent.
      monzoAccountIds = typedAccountsAndPaths
        .map((account, _) => account.id) ++ allMonzoPotAccountIds ++
        untypedMonzoAccountIds
      taggedGuids = onlineIds
        .filter: (value, _) =>
          monzoAccountIds.exists(_.value == value)
        .groupMap((value, _) => value)((_, objGuid) => objGuid)
      taggedByMonzoAccountId <- monzoAccountIds
        .traverse: monzoAccountId =>
          taggedGuids
            .getOrElse(monzoAccountId.value, Nil)
            .distinct
            .traverse(Account.byGuid)
            .map(_.flatten)
            .flatMap:
              case Nil            => IO.none
              case account :: Nil => IO.pure(Some(account))
              case accounts       =>
                IO.raiseError:
                  Error(
                    s"Several accounts are tagged with the Monzo account ID ${monzoAccountId.value}: ${accounts.map(_.name).sorted.mkString(", ")}; resolve by hand — only one account can be the one it posts into."
                  )
            .map(monzoAccountId -> _)
        .map(_.toMap)
      taggedPots = allMonzoPotAccountIds
        .flatMap: monzoAccountId =>
          taggedByMonzoAccountId(monzoAccountId).map(monzoAccountId -> _)
        .toMap
      // Fail fast on a pot the book doesn't know and no recorded link
      // names: it can't be filed into its own account, and a mis-filed row
      // would be permanent — online_id dedup skips it on every later run.
      // One run whose window spans a transfer for the pot records the link.
      unnamedPots = materialMonzoPotAccountIds.filterNot: monzoAccountId =>
        taggedPots.contains(monzoAccountId) || pots.contains(monzoAccountId)
      _ <- IO.raiseUnless(unnamedPots.isEmpty):
        Error(
          s"Nothing identifies the pot(s) behind ${unnamedPots.map(_.value).mkString(", ")} — no tagged account in the book and no recorded pot link; re-run with --since spanning a transfer for each to record the link(s)."
        )
      // The same fail-fast for an untyped account, and for the same reason:
      // with no type there is no path to create one at, so an untagged one
      // can only be guessed at, and a guess is permanent — online_id dedup
      // skips a mis-filed row on every later run. The remedy differs because
      // the API knows the type: one run from --from-monzo creates the account
      // and tags it, and every CSV run afterwards finds it.
      untaggedAccounts = materialByAccount.collect:
        case (account, _)
            if untypedMonzoAccountIds.contains(account.id) &&
              taggedByMonzoAccountId(account.id).isEmpty =>
          account.id.value
      _ <- IO.raiseUnless(untaggedAccounts.isEmpty):
        Error(
          s"No account in the book is tagged with the Monzo account ID(s) ${untaggedAccounts.sorted.mkString(", ")}, and a CSV statement doesn't say what kind of account they are; run once with --from-monzo to create and tag them, or tag an existing account by hand."
        )
      currency <- Commodity.gbp
      // Fail fast on a pot denominated in anything but the book's currency:
      // its minor units would otherwise be posted as if they were pence.
      foreignPots = materialMonzoPotAccountIds.flatMap: monzoAccountId =>
        pots
          .get(monzoAccountId)
          .filterNot(_.currency.value == currency.mnemonic)
          .map: pot =>
            s"${pot.name.value} (${pot.currency.value})"
      _ <- IO.raiseUnless(foreignPots.isEmpty):
        Error(
          s"Pot(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignPots.mkString(", ")}."
        )
      // The same check for an account whose source stated a currency — a CSV
      // statement's Currency column — rather than only for the pots /pots
      // named. The Monzo source states none, so this is silent on that path;
      // see FetchedAccount. Distinct from the book-side check further down,
      // which asks what the accounts being posted *to* are denominated in.
      foreignMonzoAccounts = materialByAccount.flatMap: (account, _) =>
        account.currency
          .filterNot(_.value == currency.mnemonic)
          .map: accountCurrency =>
            s"${account.id.value} (${accountCurrency.value})"
      _ <- IO.raiseUnless(foreignMonzoAccounts.isEmpty):
        Error(
          s"Monzo account(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignMonzoAccounts.sorted.mkString(", ")}."
        )
      // The root and the Archive subroot every placement below hangs off,
      // resolved once for the run. The subroot stays lazy inside: a run with
      // nothing retired must not create one.
      roots <- BookRoots.creating(dryRun)
      // Typed asset accounts and pot accounts resolve through the same
      // path: resolveAssetAccount finds by online_id tag, creates the
      // account otherwise, enforces placement, and tags one it created.
      typedAssets <- typedAccountsAndPaths
        .traverse: (account, path) =>
          resolveAssetAccount(
            livePath = path,
            // Each Monzo account has its own asset account, so retirement
            // is its own closure: a closed account is archived while the
            // account that replaced it goes on being posted to.
            retired = account.closed,
            monzoAccountId = account.id,
            tagged = taggedByMonzoAccountId(account.id),
            roots = roots,
            dryRun = dryRun
          ).map(account.id -> _)
        .map(_.toMap)
      // A pot's canonical leaf name is its current Monzo name plus its
      // backing-account ID, so renames propagate and two pots that share a
      // name still get an account each. Only pots that are material this
      // run or already known to the book get resolved; the rest are posted
      // to as-is. `pots` only holds a backing account whose pot some
      // transfer has named (see State.potIds), and a tagged account can
      // outlive the state store that named it — so until a window spanning
      // one of its transfers is fetched there is no name and no deleted
      // flag to enforce against.
      // The run that does fetch one records the link and enforces then.
      potAssets <- allMonzoPotAccountIds
        .traverse: monzoAccountId =>
          pots.get(monzoAccountId) match
            case Some(pot)
                if materialMonzoPotAccountIds.contains(monzoAccountId) ||
                  taggedPots.contains(monzoAccountId) =>
              resolveAssetAccount(
                livePath = assetAccounts.pots :+ pot.name.value,
                retired = pot.deleted.value,
                monzoAccountId = monzoAccountId,
                tagged = taggedByMonzoAccountId(monzoAccountId),
                roots = roots,
                dryRun = dryRun
              ).map(account => monzoAccountId -> Some(account))
            case _ =>
              IO.pure(monzoAccountId -> taggedPots.get(monzoAccountId))
        .map:
          _.collect:
            case (monzoAccountId, Some(account)) => monzoAccountId -> account
          .toMap
      // Resolved by tag alone: untaggedAccounts above has already failed the
      // run for any that's material and untagged, and an untyped account
      // with no material transactions needs no asset account at all.
      untypedAssets = untypedMonzoAccountIds
        .flatMap: monzoAccountId =>
          taggedByMonzoAccountId(monzoAccountId).map(monzoAccountId -> _)
        .toMap
      assets = typedAssets ++ potAssets ++ untypedAssets
      // One book account per Monzo account, checked rather than assumed.
      // Resolution never adopts an account it found by location, so two
      // Monzo accounts can only land on one book account if the book itself
      // says they do: an online_id tag added by hand to an account another
      // one already answers to. Sharing an account would commingle two
      // Monzo accounts' rows permanently — online_id dedup skips them on
      // every later run — so the run fails instead.
      // Resolution creates, moves and renames accounts but files nothing,
      // and the whole run is one transaction (a dry run writes nothing at
      // all), so failing here leaves the book unimported.
      overloaded = assets.toList
        .groupBy((_, account) => account.guid)
        .values
        .filter(_.sizeIs > 1)
        .map: shared =>
          val (_, account) = shared.head
          val monzoAccountIds =
            shared.map((monzoAccountId, _) => monzoAccountId.value)
          s"${account.name} (${monzoAccountIds.sorted.mkString(", ")})"
        .toList
      _ <- IO.raiseUnless(overloaded.isEmpty):
        Error(
          s"Book account(s) shared by several Monzo accounts: ${overloaded.mkString("; ")}; resolve by hand — filing two Monzo accounts into one book account would commingle their transactions."
        )
      // Monzo's categories are authoritative: each files into the account
      // categoryTarget names, created on first sight — no mapping to
      // maintain. Grouped by parent so each distinct parent chain is
      // resolved once, not once per category.
      categories <- materialByAccount
        .flatMap: (_, transactions) =>
          transactions
        .map(categoryTarget)
        .distinct
        .groupBy(_.init)
        .toList
        .flatTraverse: (parentPath, paths) =>
          liveParentFor(parentPath, roots, dryRun).flatMap: parent =>
            paths.traverse: path =>
              createOrRetrieveChild(
                parent,
                canonicalPathString(parentPath, retired = false),
                path.last,
                dryRun,
                // A category leaf takes postings, is never retired on its
                // own, and is born from its parent.
                placeholder = false,
                hidden = false,
                template = None
              ).map(path -> _)
        .map(_.toMap)
      // Every account a posting touches is denominated in the book's
      // currency, so a split's value and its quantity are the same rational
      // number and neither needs an exchange rate — which is what lets
      // Posting.fromMonzo write one pair of numerators and denominators for
      // both. An account in another commodity would need a rate, and
      // posting Monzo's minor units into it as though they were the book's
      // would be wrong by it, so fail rather than file: online_id dedup
      // skips a mis-filed row on every later run, so it could never be
      // re-filed.
      foreignAccounts = (assets.values ++ categories.values).toList
        .filterNot(_.commodityGuid.contains(currency.guid))
        .map(_.name)
        .distinct
      _ <- IO.raiseUnless(foreignAccounts.isEmpty):
        Error(
          s"Account(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignAccounts.sorted.mkString(", ")}."
        )
      // The dedup keys out of the same prefetch: the whole run sits in a
      // single SQLite transaction and fetched transaction IDs are unique,
      // so the set can't go stale mid-run. The account tags resolution has
      // just written aren't in it, and needn't be — those are Monzo account
      // IDs, and what this answers is whether a Monzo *transaction* ID is
      // already filed.
      importedIds = onlineIds.map((value, _) => value).toSet
      // Every row the run will file, settled before any of them is
      // written: the lines below pad each column to the widest value in
      // it, which isn't known until every row is.
      rows <- materialByAccount.flatTraverse: (account, material) =>
        // Total: unmapped types and unnamed pots failed the run up front,
        // and `assets` was built from this same list of accounts.
        val assetAccount = assets(account.id)
        material
          .filterNot: transaction =>
            importedIds.contains(transaction.id.value)
          .traverse: transaction =>
            val categoryPath = categoryTarget(transaction)
            Posting
              .fromMonzo(
                transaction,
                assetAccount,
                categories(categoryPath),
                currency,
                now,
                zone
              )
              .map: posting =>
                (
                  transaction = transaction,
                  assetAccount = assetAccount,
                  category = categoryPath.mkString(":"),
                  amount = formatAmount(posting.assetSplit.valueNum, currency),
                  posting = posting
                )
      skipped = materialByAccount
        .flatMap((_, material) => material)
        .count: transaction =>
          importedIds.contains(transaction.id.value)
      // One line per transaction filed, so the plan can be read row by row
      // rather than trusted as a count: the post date and payee GnuCash
      // will show, the signed amount as it lands on the asset leg (the
      // category leg is its negation), and the two accounts the money moves
      // between. The asset account is named by its leaf, which carries the
      // Monzo account ID, so two accounts of a kind are told apart without
      // repeating the path on every line. A dry run says what it would do,
      // as the account creations above do.
      verb = if dryRun then "Would file" else "Filed"
      // Each column padded to the widest value in it, so a run's lines read
      // as a table rather than as prose of varying length. The amounts are
      // right-aligned — every one carries the currency's full fraction, so
      // that lines them up on the decimal point — and the rest left.
      amountWidth = rows.map(_.amount.length).maxOption.getOrElse(0)
      assetWidth = rows.map(_.assetAccount.name.length).maxOption.getOrElse(0)
      categoryWidth = rows.map(_.category.length).maxOption.getOrElse(0)
      _ <- rows.traverse: row =>
        val line = List(
          verb,
          formatConsoleTimestamp(row.transaction.created.value.asInstant),
          " " * (amountWidth - row.amount.length) + row.amount,
          row.assetAccount.name.padTo(assetWidth, ' '),
          "/",
          // The colon punctuates the category, so it goes before the
          // padding rather than after it, where it would sit a column away
          // from the word it belongs to.
          (row.category + ":").padTo(categoryWidth + 1, ' '),
          s"${payee(row.transaction)}."
        ).mkString(" ")
        IO.unlessA(dryRun)(row.posting.insert) *> info(line)
      _ <- info:
        // A dry run files nothing, so it says what it would have done,
        // as the account creations above do.
        val verb = if dryRun then "would file" else "filed"
        s"${rows.size} $verb, $skipped already present."
    yield ()
    // Everything-or-nothing either way: a real run commits, a dry run is
    // rolled back rather than merely left unwritten, so that a write that
    // slipped past a dryRun guard doesn't survive a run that took no
    // backup.
    db.transactOrRollBack(dryRun)(run)
yield ()

// The account path a transaction's category leg posts to. Monzo's categories
// are authoritative, but not all of them are spending: income files under
// Income — as "General", mirroring the expense side's catch-all, since
// title-casing the category itself would produce Income:Income — and the two
// transfer-ish categories share one wash account under Assets: the two legs
// of a pot transfer cancel there, and what remains is money moved to
// institutions the book imports nothing from (still an asset, not an
// expense), awaiting manual re-filing. Everything else is an expense, named
// by title-casing the category (eating_out -> "Eating Out"; no category ->
// "General", Monzo's default). A refund arrives sign-flipped in its spending
// category and negates the expense, which is why the amount's sign plays no
// part here.
def categoryTarget(transaction: monzo.Transaction): List[String] =
  transaction.category.fold("general")(_.value) match
    case "income"                => List("Income", "General")
    case "savings" | "transfers" => List("Assets", "Transfers")
    case category                => List("Expenses", titleCased(category))

def titleCased(category: String): String =
  category
    .split('_')
    .filter(_.nonEmpty)
    .map(_.capitalize)
    .mkString(" ")

// Minor units as the book's currency reads them: 1234 at a fraction of 100 is
// "£12.34". Scaled rather than divided, so the string is exact and keeps its
// trailing zeroes; the scale is the fraction's digit count, which is what a
// power-of-ten fraction means, and GnuCash's currency commodities have no
// other kind. The sign goes outside the symbol ("-£3.60"), where a reader
// expects it.
def formatAmount(minorUnits: Long, currency: Commodity): String =
  val scale = currency.fraction.toString.length - 1
  val magnitude = BigDecimal(minorUnits.abs, scale).toString
  val sign = if minorUnits < 0 then "-" else ""
  s"$sign${currencySymbol(currency.mnemonic)}$magnitude"

// GnuCash's commodities table carries a mnemonic but no symbol, and
// java.util.Currency would answer locale-dependently — the same book would
// read "£12.34" on one machine and "GBP12.34" on another. So the symbol is
// named here, and a currency not named falls back to its own mnemonic, which
// is unambiguous if less compact. Only GBP is reachable today: Commodity.gbp
// is where the book's currency comes from.
def currencySymbol(mnemonic: String): String = mnemonic match
  case "GBP"    => "£"
  case mnemonic => mnemonic

// The console's own timestamp, deliberately not the one the transactions table
// is written with: the two happen to agree on a shape, and sharing a formatter
// would mean a change to how GnuCash stores a date silently reshaped what a
// run prints. UTC, matching the raw instant Monzo reports, rather than the
// local calendar day post_date is normalised onto — the line is a record of
// what was fetched.
val consoleTimestamp: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

def formatConsoleTimestamp(instant: Instant): String =
  instant.atOffset(ZoneOffset.UTC).format(consoleTimestamp)
