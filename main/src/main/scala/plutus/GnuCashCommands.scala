package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import cue4s.*
import fs2.io.file.CopyFlag
import fs2.io.file.CopyFlags
import porcupine.*

import java.time.Instant
import java.util.Locale
import scala.collection.immutable.SortedMap

lazy val gnucashOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "gnucash",
  help = "GnuCash housekeeping."
):
  archiveAccountsOpts orElse restoreAccountOpts orElse importTransactionsOpts

lazy val archiveAccountsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "archive-accounts",
  help = "Archive hidden accounts."
):
  (verbosityOpts, inputOpts).tupled.map: (verbosity, input) =>
    archiveAccounts(input)(using verbosity)

def archiveAccounts(
    input: fs2.io.file.Path
)(using verbosity: Verbosity): IO[Unit] =
  Database
    .open[IO]:
      input.toString
    .use: db =>
      given Database[IO] = db
      for
        root <- Account.root
        // TODO: Change this to accept a single account to archive, like
        // restore-account does?
        _ <- info:
          "Finding hidden accounts…"
        // Whole run in one transaction, the Archive subroot's own creation
        // included: a failure partway through rolls every account archived so
        // far in this run — and, on a book's first archive, the subroot it
        // created to hold them — back to where it started, rather than
        // leaving some archived and others not.
        _ <- db.transact:
          for
            archiveSubroot <- Account.createOrRetrieveArchiveSubroot
            hiddenAccounts <- root.hiddenChildren:
              archiveSubroot
            _ <- (IO.traverse:
              hiddenAccounts
            ): hiddenAccount =>
              for
                hiddenAccountPath <- hiddenAccount.pathString
                livePathInit <- pathInitBelow(hiddenAccount, root)
                archiveParent <- mirrorParentFor(
                  livePathInit,
                  from = root,
                  to = archiveSubroot,
                  dryRun = false
                )
                _ <- cleanUpRedundantMirror(
                  original = hiddenAccount,
                  originalPath = hiddenAccountPath,
                  mirrorParent = archiveParent,
                  mirrorKind = "Archive"
                )
                archivedAccount <- hiddenAccount.update(
                  parent = archiveParent
                )
                archivedPath <- archivedAccount.pathString
                _ <- info:
                  s"Archived $hiddenAccountPath to $archivedPath."
              yield ()
          yield ()
        _ <- info:
          "Finished archiving hidden accounts."
      yield ()

lazy val restoreAccountOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "restore-account",
  help = "Restore archived account."
):
  (verbosityOpts, inputOpts).tupled.map: (verbosity, input) =>
    restoreAccount(input)(using verbosity)

def restoreAccount(
    input: fs2.io.file.Path
)(using verbosity: Verbosity): IO[Unit] =
  Database
    .open[IO]:
      input.toString
    .use: db =>
      given Database[IO] = db
      val nothingToRestore = Error("No archived accounts to restore.")
      for
        root <- Account.root
        // Retrieved, never created: a book that has never archived anything
        // has no Archive subroot and nothing to restore, and creating one
        // here would be a write outside the transaction below, on behalf of a
        // command that is about to do nothing.
        archiveSubroot <- Account.retrieveArchiveSubroot.flatMap:
          IO.fromOption(_):
            nothingToRestore
        archivedAccounts <- archiveSubroot.allChildren
        _ <- IO.raiseWhen(archivedAccounts.isEmpty):
          nothingToRestore
        archivedAccountsByPath <- (IO
          .traverse:
            archivedAccounts
          ): account =>
            account.pathString.map(_ -> account)
          .map:
            SortedMap.from
        _ <- IO.raiseUnless(
          archivedAccountsByPath.size == archivedAccounts.size
        ):
          Error:
            "Archived accounts have duplicate paths."
        archivedAccountPath <- IO.blocking:
          Prompts.sync.use:
            _.singleChoice(
              "Choose account to restore",
              archivedAccountsByPath.keys.toList
            ).getOrRaise
        archivedAccount = archivedAccountsByPath(archivedAccountPath)
        // Only the writes are transacted, not the prompt above: begin
        // immediate takes SQLite's write lock immediately, and holding that
        // open while waiting on stdin would block any other writer for as
        // long as the prompt sits unanswered.
        _ <- db.transact:
          for
            archivePathInit <- pathInitBelow(archivedAccount, archiveSubroot)
            nonArchiveParent <- mirrorParentFor(
              archivePathInit,
              from = archiveSubroot,
              to = root,
              dryRun = false
            )
            _ <- cleanUpRedundantMirror(
              original = archivedAccount,
              originalPath = archivedAccountPath,
              mirrorParent = nonArchiveParent,
              mirrorKind = "Non-archive"
            )
            restoredAccount <- archivedAccount.update(
              parent = nonArchiveParent
            )
            restoredPath <- restoredAccount.pathString
            _ <- info:
              s"Restored $archivedAccountPath to $restoredPath."
          yield ()
      yield ()

// Handles the case where a mirror already exists at `mirrorParent` with the
// same name as `original`. This happens when a child was already
// archived/restored, resulting in the creation of a mirror of the parent
// account we're now archiving/restoring.
//
// The correct handling is to move the children of the existing mirror to be
// children of the account we're now archiving/restoring (their original
// parent) and to delete the newly redundant mirror (because it will be
// replaced when the original is moved into its place).
def cleanUpRedundantMirror(
    original: Account,
    originalPath: String,
    mirrorParent: Account,
    mirrorKind: String
)(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
  for
    maybeExistingMirror <- mirrorParent.child(original.name)
    _ <- (IO.traverse:
      maybeExistingMirror
    ): existingMirror =>
      for
        _ <- warn:
          s"$mirrorKind mirror for $originalPath already exists."
        existingChildren <- existingMirror.directChildren
        _ <- (IO.traverse:
          existingChildren
        ): child =>
          for
            _ <- child.update(
              parent = original
            )
            childPath <- child.pathString
            _ <- warn:
              s"Moved $childPath to $originalPath."
          yield ()
        existingMirrorPath <- existingMirror.pathString
        _ <- existingMirror.delete
        _ <- warn:
          s"Deleted existing ${mirrorKind.toLowerCase} mirror $existingMirrorPath."
      yield ()
  yield ()

// Lives here rather than under `monzo` because it's conceptually a GnuCash
// import — a future variant could read the CSVs the Monzo app exports instead
// of the API. The Monzo session plumbing it borrows (fetchTransactionsByAccount)
// stays in MonzoCommands.
lazy val importTransactionsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "import-transactions",
  help = "Import Monzo transactions directly into the GnuCash book."
):
  (
    verbosityOpts,
    inputOpts,
    sinceOpts,
    beforeOpts,
    importDryRunOpts
  ).tupled.map: (verbosity, input, since, before, dryRun) =>
    importTransactions(input, since, before, dryRun)(using verbosity)

lazy val importDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print the plan (filed / already-present counts and the accounts that would be created) without writing to the book and without taking a backup."
    )
    .orFalse

def importTransactions(
    input: fs2.io.file.Path,
    since: Option[Instant],
    before: Option[Instant],
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] = for
  (now, byAccount, pots) <- fetchTransactionsByAccount(since, before)
  // Snapshot first: a bad run becomes a restore, not a rebuild. The snapshot
  // is of the run about to happen, so it replaces the one the last run left —
  // without ReplaceExisting the copy throws once a .bak exists, which is to
  // say on every run after the first.
  _ <- IO.unlessA(dryRun):
    val backup = fs2.io.file.Path(s"$input.bak")
    fs2.io.file
      .Files[IO]
      .copy(input, backup, CopyFlags(CopyFlag.ReplaceExisting)) *>
      info(s"Backed up to $backup.")
  _ <- Database
    .open[IO](input.toString)
    .use: db =>
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
          case (account, _) if isPotBacking(account) => account.id
        materialMonzoPotAccountIds = materialByAccount.collect:
          case (account, _) if isPotBacking(account) => account.id
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
          .map((account, _) => account.id) ++ allMonzoPotAccountIds
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
              retired = account.closed.exists(_.value),
              monzoAccountId = account.id,
              tagged = taggedByMonzoAccountId(account.id),
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
                  dryRun = dryRun
                ).map(account => monzoAccountId -> Some(account))
              case _ =>
                IO.pure(monzoAccountId -> taggedPots.get(monzoAccountId))
          .map:
            _.collect:
              case (monzoAccountId, Some(account)) => monzoAccountId -> account
            .toMap
        assets = typedAssets ++ potAssets
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
            liveParentFor(parentPath, dryRun).flatMap: parent =>
              paths.traverse: path =>
                createOrRetrieveChild(parent, path.last, dryRun)
                  .map(path -> _)
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
        results <- materialByAccount.flatTraverse: (account, material) =>
          // Total: unmapped types and unnamed pots failed the run up front,
          // and `assets` was built from this same list of accounts.
          val assetAccount = assets(account.id)
          material.traverse: transaction =>
            if importedIds.contains(transaction.id.value) then
              Imported.Skipped.pure[IO]
            else
              Posting
                .fromMonzo(
                  transaction,
                  assetAccount,
                  categories(categoryTarget(transaction)),
                  currency,
                  now
                )
                .flatMap: posting =>
                  IO.unlessA(dryRun)(posting.insert)
                    .as(Imported.Filed)
        _ <- info:
          val filed = results.count(_ == Imported.Filed)
          val skipped = results.count(_ == Imported.Skipped)
          s"$filed filed, $skipped already present."
      yield ()
      // Everything-or-nothing, unless we're only previewing.
      if dryRun then run else db.transact(run)
yield ()

enum Imported:
  case Filed, Skipped

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

// The canonical path of the asset account a Monzo account posts into: the
// code-defined path with that account's Monzo ID in the leaf name. Several
// Monzo accounts can share a code-defined path — the map is keyed by type, so
// a closed account and the one that replaced it land on the same one, and two
// pots may share a name — and each gets an account of its own regardless
// (resolveAssetAccount never adopts by location). The ID in the name is what
// tells those siblings apart in GnuCash's account tree, and it puts the
// identity the resolver actually matches on, the online_id tag, in plain
// sight beside them.
def assetAccountPath(
    livePath: List[String],
    monzoAccountId: monzo.AccountId
): List[String] =
  livePath.init :+ s"${livePath.last} (${monzoAccountIdLabel(monzoAccountId)})"

// The Monzo account ID as it reads in an account name: acc_ dropped, since
// every account named this way is a Monzo account and the prefix tells a
// reader nothing, and the rest upper-cased, so it sits among account names
// the way a sort code or an account number does rather than as a stretch of
// mixed-case noise. Purely cosmetic: the identity the resolver matches on is
// the raw ID in the online_id tag, so nothing needs the ID back out of a
// name. Upper-casing is lossy — Monzo's IDs are mixed-case — but only for the
// name: two IDs differing only in case still get an account each, because
// resolution matches on the tag, not on the name. Locale.ROOT because a
// Turkish-locale machine upper-cases i to İ, which would give one account two
// different canonical names on two machines.
def monzoAccountIdLabel(monzoAccountId: monzo.AccountId): String =
  monzoAccountId.value.stripPrefix("acc_").toUpperCase(Locale.ROOT)

// One resolver for every Monzo-backed asset account. The online_id tag is the
// only thing it matches on, and identity therefore survives moves and
// renames. An account a past GUI import of export-transactions' OFX
// associated already carries that slot (see Slot.OnlineId), so an untagged
// account is one no run and no import has ever touched: a fresh account is
// created at the canonical spot and tagged, which is what puts tags in the
// book at all and what makes every later run find it by tag.
//
// That creation is unconditional (createChild, not createOrRetrieveChild):
// an account already sitting at the canonical path is left alone, and the new
// one lands beside it under the same name — which GnuCash permits. Adopting
// it instead would be a guess, since nothing in the book says it belongs to
// this Monzo account, and a wrong guess is permanent: the tag would send
// every later run's rows to the same place and online_id dedup would skip
// them, so they could never be re-filed. Two same-named siblings are the
// visible, fixable outcome instead — the tagged one is what later runs post
// to, so move the other's transactions into it and delete the empty one.
// Placement is enforced either way — for a fresh child only the hidden flag
// can be out of line.
def resolveAssetAccount(
    livePath: List[String],
    retired: Boolean,
    monzoAccountId: monzo.AccountId,
    tagged: Option[Account],
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val canonicalPath = assetAccountPath(livePath, monzoAccountId)
  for
    account <- tagged match
      case Some(account) =>
        enforcePlacement(account, canonicalPath, retired, dryRun)
      case None =>
        for
          parent <- parentFor(canonicalPath.init, retired, dryRun)
          child <- createChild(parent, canonicalPath.last, dryRun)
          placed <- alignHidden(child, retired, canonicalPath, dryRun)
        yield placed
    _ <- IO.unlessA(dryRun || tagged.isDefined):
      tagOnlineId(account.guid, monzoAccountId)
  yield account

def tagOnlineId(guid: String, monzoAccountId: monzo.AccountId)(using
    db: Database[IO]
): IO[Unit] =
  Slot
    .stringSlot(
      objGuid = guid,
      name = Slot.OnlineId,
      value = monzoAccountId.value
    )
    .insert

// A canonical path's parent chain: the live one while the Monzo side is
// live, its Archive-nested twin once retired.
def parentFor(
    pathInit: List[String],
    retired: Boolean,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  if retired then archiveParentFor(pathInit, dryRun)
  else liveParentFor(pathInit, dryRun)

// Hidden tracks retirement, aligned in both directions.
def alignHidden(
    account: Account,
    hidden: Boolean,
    livePath: List[String],
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val label = canonicalPathString(livePath, retired = hidden)
  if account.hidden == hidden then IO.pure(account)
  else if dryRun then
    info(s"Would ${if hidden then "hide" else "unhide"} $label.").as(account)
  else
    account
      .updateHidden(hidden)
      .flatTap: _ =>
        info(s"${if hidden then "Hid" else "Unhid"} $label.")

// The canonical description of a Monzo-backed asset account is none at all:
// the name already says which Monzo account or pot posts into it, down to the
// ID that keeps two of a kind apart (see assetAccountPath), so anything here
// could only restate it. Aligned like hidden, so a description added by hand
// or carried in by a GUI OFX import doesn't outlive the next run. No
// description and an empty one both count as aligned, so whichever of the two
// the book holds is left alone.
def alignDescription(
    account: Account,
    livePath: List[String],
    retired: Boolean,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val label = canonicalPathString(livePath, retired)
  if account.description.forall(_.isEmpty) then IO.pure(account)
  else if dryRun then
    info(s"Would clear the description of $label.").as(account)
  else
    account.clearDescription
      .flatTap: _ =>
        info(s"Cleared the description of $label.")

// Textual, so a dry run can name targets whose parents don't exist yet.
def canonicalPathString(livePath: List[String], retired: Boolean): String =
  val canonical =
    if retired then Account.ArchiveName :: livePath else livePath
  ("Root Account" :: canonical).mkString("/")

// Monzo is authoritative for a Monzo-backed asset account's whole placement.
// Its canonical parent is the code-defined live path while the Monzo side is
// live, and the same path nested under the Archive subroot once retired (a
// closed account, a deleted pot); its name is the path's leaf — for pots, the
// pot's current Monzo name — with the Monzo account ID appended (see
// assetAccountPath); its description is empty (see alignDescription); and
// hidden tracks retirement. All of it is enforced in both directions on every
// run — un-archiving, un-hiding, renaming and un-describing included — so a
// hand-move lasts only until the next import. A *different* account already
// occupying the canonical spot fails the run: merging or deleting it could
// orphan its transactions, so the user resolves that collision by hand.
def enforcePlacement(
    account: Account,
    livePath: List[String],
    retired: Boolean,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  val name = livePath.last
  val targetPath = canonicalPathString(livePath, retired)
  for
    parent <- parentFor(livePath.init, retired, dryRun)
    inPlace = account.parentGuid.contains(parent.guid) && account.name == name
    placed <-
      if inPlace then IO.pure(account)
      else
        for
          collision <- parent.child(name)
          _ <- IO.raiseWhen(collision.exists(_.guid != account.guid)):
            Error(
              s"A different account already sits at $targetPath; resolve it by hand — merging or deleting it automatically could orphan its transactions."
            )
          accountPath <- account.pathString
          moved <-
            if dryRun then
              info(s"Would move $accountPath to $targetPath.").as(account)
            else
              account
                .update(parent = parent, name = name)
                .flatTap: _ =>
                  info(s"Moved $accountPath to $targetPath.")
        yield moved
    aligned <- alignHidden(placed, retired, livePath, dryRun)
    described <- alignDescription(aligned, livePath, retired, dryRun)
  yield described

// The canonical parent for a retired account: the live parent chain mirrored
// under the Archive subroot. In a dry run nothing is written; the subroot
// itself is fabricated when missing so the rest of the plan can proceed.
def archiveParentFor(
    livePathInit: List[String],
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    root <- Account.root
    archiveSubroot <- Account.retrieveArchiveSubroot.flatMap:
      case Some(archiveSubroot) => IO.pure(archiveSubroot)
      case None if dryRun       =>
        newGuid
          .map: guid =>
            root.copy(
              guid = guid,
              name = Account.ArchiveName,
              parentGuid = Some(root.guid),
              hidden = true,
              placeholder = true
            )
          .flatTap: _ =>
            info(s"Would create account Root Account/${Account.ArchiveName}.")
      case None => Account.createOrRetrieveArchiveSubroot
    archiveParent <- mirrorParentFor(
      livePathInit,
      from = root,
      to = archiveSubroot,
      dryRun
    )
  yield archiveParent

// One parent chain mirrored across the live/archive boundary, for every way
// an account crosses it: (from = root, to = the Archive subroot) going in,
// the reverse coming out, so import, archive-accounts and restore-account
// share one notion of what a mirror is rather than each carrying its own.
// Missing segments are created on demand, each a placeholder copy of its
// counterpart under `from` — or of the parent it's created under, when that
// counterpart no longer exists (an account whose live twin has since been
// deleted still needs somewhere to sit).
def mirrorParentFor(
    pathInit: List[String],
    from: Account,
    to: Account,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  pathInit
    .foldLeftM(
      (mirror = to, counterpart = Some(from): Option[Account])
    ): (cursors, segment) =>
      for
        nextCounterpart <- cursors.counterpart match
          case Some(counterpart) => counterpart.child(segment)
          case None              => IO.pure(None)
        nextMirror <- createOrRetrieveChild(
          cursors.mirror,
          segment,
          dryRun,
          placeholder = true,
          template = nextCounterpart
        )
      yield (mirror = nextMirror, counterpart = nextCounterpart)
    .map(_.mirror)

// The names between `ancestor` and `account`, exclusive: the path a mirror of
// `account` on the other side of the boundary has to reproduce. The import
// path knows its paths up front (they are code-defined); archive-accounts and
// restore-account start from an account instead and read theirs out of the
// book.
def pathInitBelow(account: Account, ancestor: Account)(using
    db: Database[IO]
): IO[List[String]] =
  account.path.flatMap: accounts =>
    accounts.indexWhere(_.guid == ancestor.guid) match
      case -1 =>
        IO.raiseError:
          Error(
            s"${ancestor.name} is not an ancestor of ${account.name}."
          )
      case index => IO.pure(accounts.map(_.name).drop(index + 1).init)

// The live parent chain for a code-defined path, created on demand below its
// top-level account — which must already exist, and does in any freshly
// created book (GnuCash makes Assets, Expenses, Income and Liabilities).
// Intermediate segments are placeholders: only leaves take postings.
def liveParentFor(
    pathInit: List[String],
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    top <- Account
      .atPath(List(pathInit.head))
      .flatMap:
        IO.fromOption(_):
          Error(s"No account at ${pathInit.head}")
    parent <- pathInit.tail.foldLeftM(top): (parent, segment) =>
      createOrRetrieveChild(parent, segment, dryRun, placeholder = true)
  yield parent

// Get-or-create one child, for the paths where sharing is the point: a
// category leaf several transactions file into, and the structural segments
// above it.
def createOrRetrieveChild(
    parent: Account,
    name: String,
    dryRun: Boolean,
    placeholder: Boolean = false,
    template: Option[Account] = None
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  parent
    .child(name)
    .flatMap:
      case Some(child) => IO.pure(child)
      case None => createChild(parent, name, dryRun, placeholder, template)

// Create one child, whether or not a sibling of that name already exists —
// GnuCash identifies an account by guid and permits the duplicate name, and
// resolveAssetAccount relies on that to avoid adopting an account it can't
// know is the right one.
//
// A created child inherits its account type and commodity from `template` —
// by default the parent, so an Expenses child is an EXPENSE account and a
// Liabilities child a LIABILITY (the literal accounts.account_type values);
// archiveParentFor passes the live twin so archived mirrors match what they
// mirror. Structural path segments are created as placeholders, leaves that
// take postings are not. A dry run inserts nothing but still yields the
// would-be account, so the rest of the plan can proceed against it.
def createChild(
    parent: Account,
    name: String,
    dryRun: Boolean,
    placeholder: Boolean = false,
    template: Option[Account] = None
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    guid <- newGuid
    parentPath <- parent.pathString
    child = template
      .getOrElse(parent)
      .copy(
        guid = guid,
        name = name,
        parentGuid = Some(parent.guid),
        code = None,
        description = None,
        hidden = false,
        placeholder = placeholder
      )
    _ <-
      if dryRun then info(s"Would create account $parentPath/$name.")
      else child.insert *> info(s"Created account $parentPath/$name.")
  yield child
