package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import cue4s.*
import porcupine.*

import java.time.Instant
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
        archiveSubroot <- Account.createOrRetrieveArchiveSubroot
        // TODO: Change this to accept a single account to archive, like
        // restore-account does?
        _ <- info:
          "Finding hidden accounts…"
        hiddenAccounts <- root.hiddenChildren:
          archiveSubroot
        _ <- (IO.traverse:
          hiddenAccounts
        ): hiddenAccount =>
          archiveAccount(hiddenAccount, root, archiveSubroot)
        _ <- info:
          "Finished archiving hidden accounts."
      yield ()

// Move an account under the Archive subroot, mirroring its parent chain.
// Only archive-accounts uses this; import places retired accounts through
// enforcePlacement instead, which fails on collisions rather than merging
// mirrors.
def archiveAccount(
    account: Account,
    root: Account,
    archiveSubroot: Account
)(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
  for
    accountPath <- account.pathString
    archiveParent <- account.createOrRetrieveMirrorParent(
      from = root,
      to = archiveSubroot
    )
    _ <- cleanUpRedundantMirror(
      original = account,
      originalPath = accountPath,
      mirrorParent = archiveParent,
      mirrorKind = MirrorKind.Archive
    )
    archivedAccount <- account.update(
      parent = archiveParent
    )
    archivedPath <- archivedAccount.pathString
    _ <- info:
      s"Archived $accountPath to $archivedPath."
  yield ()

// Which side of the archive boundary a mirror sits on. Only ever rendered
// into cleanUpRedundantMirror's warnings — behaviour never branches on it —
// but an enum keeps the two valid values from being spelled ad hoc.
enum MirrorKind(val label: String):
  case Archive extends MirrorKind("Archive")
  case NonArchive extends MirrorKind("Non-archive")

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
      for
        root <- Account.root
        archiveSubroot <- Account.createOrRetrieveArchiveSubroot
        archivedAccounts <- archiveSubroot.allChildren
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
        nonArchiveParent <- archivedAccount.createOrRetrieveMirrorParent(
          from = archiveSubroot,
          to = root
        )
        _ <- cleanUpRedundantMirror(
          original = archivedAccount,
          originalPath = archivedAccountPath,
          mirrorParent = nonArchiveParent,
          mirrorKind = MirrorKind.NonArchive
        )
        restoredAccount <- archivedAccount.update(
          parent = nonArchiveParent
        )
        restoredPath <- restoredAccount.pathString
        _ <- info:
          s"Restored $archivedAccountPath to $restoredPath."
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
    mirrorKind: MirrorKind
)(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
  for
    maybeExistingMirror <- mirrorParent.child(original.name)
    _ <- (IO.traverse:
      maybeExistingMirror
    ): existingMirror =>
      for
        _ <- warn:
          s"${mirrorKind.label} mirror for $originalPath already exists."
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
          s"Deleted existing ${mirrorKind.label.toLowerCase} mirror $existingMirrorPath."
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
  // Snapshot first: a bad run becomes a restore, not a rebuild.
  _ <- IO.unlessA(dryRun):
    val backup = fs2.io.file.Path(s"$input.bak")
    fs2.io.file.Files[IO].copy(input, backup) *> info(s"Backed up to $backup.")
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
        // Group every typed account by the asset path its type maps to (the
        // mapping is type-keyed, so several Monzo accounts can share one
        // asset account), marking the path retired only once every account
        // posting to it is closed — a closed account replaced by an open one
        // of the same type must keep their shared asset account live.
        typedAccountsByAssetPath = byAccount
          .flatMap: (account, _) =>
            account.accountType
              .flatMap: accountType =>
                assetAccounts.byAccountType.get(accountType.value)
              .map: assetPath =>
                (assetPath, account)
          .groupMap((assetPath, _) => assetPath)((_, account) => account)
        allPotAccountIds = byAccount.collect:
          case (account, _) if isPotBacking(account) => account.id
        materialPotAccountIds = materialByAccount
          .collect:
            case (account, _) if isPotBacking(account) => account.id
          .toSet
        // The book is the durable home of the pot association: each pot child
        // is tagged at creation with its backing-account ID in an
        // account-level online_id slot, so a book that outlives the state
        // store (say, moved to a new machine) still resolves by tag.
        taggedPots <- allPotAccountIds
          .traverse: accountId =>
            Account
              .bySlot(Slot.OnlineId, accountId.value)
              .map(accountId -> _)
          .map:
            _.collect:
              case (accountId, Some(account)) => accountId -> account
            .toMap
        // Fail fast on a pot the book doesn't know and no recorded link
        // names: it can't be filed into its own account, and a mis-filed row
        // would be permanent — online_id dedup skips it on every later run.
        // One run whose window spans a transfer for the pot records the link.
        unnamedPots = materialPotAccountIds.toList.filterNot: accountId =>
          taggedPots.contains(accountId) || pots.contains(accountId)
        _ <- IO.raiseUnless(unnamedPots.isEmpty):
          Error(
            s"Nothing identifies the pot(s) behind ${unnamedPots.map(_.value).mkString(", ")} — no tagged account in the book and no recorded pot link; re-run with --since spanning a transfer for each to record the link(s)."
          )
        currency <- Commodity.gbp
        // Fail fast on a pot denominated in anything but the book's currency:
        // its minor units would otherwise be posted as if they were pence.
        foreignPots = materialPotAccountIds.toList.flatMap: accountId =>
          pots
            .get(accountId)
            .filterNot(_.currency.value == currency.mnemonic)
            .map: pot =>
              s"${pot.name.value} (${pot.currency.value})"
        _ <- IO.raiseUnless(foreignPots.isEmpty):
          Error(
            s"Pot(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignPots.mkString(", ")}."
          )
        // Typed asset accounts and pot accounts resolve through the same
        // path: resolveAssetAccount finds by online_id tag, then by canonical
        // location, creates otherwise, enforces placement, and adopts
        // untagged IDs.
        typedAssets <- typedAccountsByAssetPath.toList
          .traverse: (path, accounts) =>
            resolveAssetAccount(
              livePath = path,
              retired = accounts.forall(_.closed.exists(_.value)),
              tagIds = accounts.map(_.id),
              dryRun = dryRun
            ).map(path -> _)
          .map(_.toMap)
        // A pot's canonical leaf name is its current Monzo name, so renames
        // propagate. Only pots that are material this run or already known to
        // the book get resolved; a tagged account with no pot record (say, a
        // fresh state store) is posted to as-is — the next linking run
        // enforces it.
        potAssets <- allPotAccountIds
          .traverse: accountId =>
            pots.get(accountId) match
              case Some(pot)
                  if materialPotAccountIds(accountId) ||
                    taggedPots.contains(accountId) =>
                resolveAssetAccount(
                  livePath = assetAccounts.pots :+ pot.name.value,
                  retired = pot.deleted.value,
                  tagIds = List(accountId),
                  dryRun = dryRun
                ).map(account => accountId -> Some(account))
              case _ =>
                IO.pure(accountId -> taggedPots.get(accountId))
          .map:
            _.collect:
              case (accountId, Some(account)) => accountId -> account
            .toMap
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
        // One upfront read of every online_id in the book; the whole run sits
        // in a single SQLite transaction and fetched transaction IDs are
        // unique, so the set can't go stale mid-run. See Slot.onlineIdValues.
        importedIds <- Slot.onlineIdValues
        results <- materialByAccount.flatTraverse: (account, material) =>
          // Both lookups are total: unmapped types and unnamed pots failed the
          // run up front, and the maps were built from this same list.
          val assetAccount = account.accountType match
            case Some(accountType) =>
              typedAssets(assetAccounts.byAccountType(accountType.value))
            case None =>
              potAssets(account.id)
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

// One resolver for every Monzo-backed asset account. Finds the account by
// its online_id tag first (identity survives moves and renames; distinct
// hits across the given IDs contradict a shared mapping and fail the run),
// then at its canonical location — live path or its archived twin — and
// creates it at the canonical spot otherwise. Placement is then enforced
// (for a fresh child only the hidden flag can be out of line), and every
// untagged ID is adopted, so the next run finds the account by identity.
// Tagging on retrieval also adopts accounts that predate tagging, were
// created by hand, or were associated by a past GUI import of
// export-transactions' OFX (which stores the same slot — see Slot.OnlineId).
def resolveAssetAccount(
    livePath: List[String],
    retired: Boolean,
    tagIds: List[monzo.AccountId],
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    taggedById <- tagIds.traverse: accountId =>
      Account
        .bySlot(Slot.OnlineId, accountId.value)
        .map(accountId -> _)
    tagged = taggedById
      .flatMap((_, account) => account)
      .distinctBy(_.guid)
    _ <- IO.raiseWhen(tagged.sizeIs > 1):
      Error(
        s"The Monzo accounts mapping to ${livePath.mkString(":")} are tagged onto ${tagged.size} different book accounts; resolve by hand — merging them automatically could orphan transactions."
      )
    located <- tagged.headOption match
      case Some(account) => IO.pure(Some(account))
      case None          =>
        Account
          .atPath(livePath)
          .flatMap:
            case Some(account) => IO.pure(Some(account))
            case None => Account.atPath(Account.ArchiveName :: livePath)
    account <- located match
      case Some(account) =>
        enforcePlacement(account, livePath, retired, dryRun)
      case None =>
        for
          parent <- parentFor(livePath.init, retired, dryRun)
          child <- createOrRetrieveChild(parent, livePath.last, dryRun)
          placed <- alignHidden(child, retired, livePath, dryRun)
        yield placed
    _ <- IO.unlessA(dryRun):
      taggedById
        .collect:
          case (accountId, None) => accountId
        .traverse(tagOnlineId(account.guid, _))
        .void
  yield account

def tagOnlineId(guid: String, accountId: monzo.AccountId)(using
    db: Database[IO]
): IO[Unit] =
  Slot
    .stringSlot(
      objGuid = guid,
      name = Slot.OnlineId,
      value = accountId.value
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

// Textual, so a dry run can name targets whose parents don't exist yet.
def canonicalPathString(livePath: List[String], retired: Boolean): String =
  val canonical =
    if retired then Account.ArchiveName :: livePath else livePath
  ("Root Account" :: canonical).mkString("/")

// Monzo is authoritative for a Monzo-backed asset account's whole placement.
// Its canonical parent is the code-defined live path while the Monzo side is
// live, and the same path nested under the Archive subroot once retired (a
// closed account, a deleted pot); its name is the path's leaf — for pots, the
// pot's current Monzo name; and hidden tracks retirement. All of it is
// enforced in both directions on every run — un-archiving, un-hiding and
// renaming included — so a hand-move lasts only until the next import. A
// *different* account already occupying the canonical spot fails the run:
// merging or deleting it could orphan its transactions, so the user resolves
// that collision by hand.
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
  yield aligned

// The canonical parent for a retired account: the live parent chain nested
// under the Archive subroot, created on demand — each missing segment as a
// placeholder copy of its live counterpart (or of the archive-side parent,
// when the live segment no longer exists). In a dry run nothing is written;
// missing segments are fabricated so the rest of the plan can proceed.
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
    cursors <- livePathInit
      .foldLeftM((archive = archiveSubroot, live = Option(root))):
        (cursors, segment) =>
          for
            nextLive <- cursors.live match
              case Some(live) => live.child(segment)
              case None       => IO.pure(None)
            nextArchive <- createOrRetrieveChild(
              cursors.archive,
              segment,
              dryRun,
              placeholder = true,
              template = nextLive
            )
          yield (archive = nextArchive, live = nextLive)
  yield cursors.archive

// The live parent chain for a code-defined path, created on demand below its
// top-level account — which must already exist, and does in any freshly
// created book (GnuCash makes Assets, Expenses, Income and Liabilities).
// Intermediate segments are placeholders: only leaves take postings.
def liveParentFor(
    pathInit: List[String],
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    top <- existingAccountAt(List(pathInit.head))
    parent <- pathInit.tail.foldLeftM(top): (parent, segment) =>
      createOrRetrieveChild(parent, segment, dryRun, placeholder = true)
  yield parent

def existingAccountAt(
    path: List[String]
)(using db: Database[IO]): IO[Account] =
  Account
    .atPath(path)
    .flatMap:
      IO.fromOption(_):
        Error(s"No account at ${path.mkString(":")}")

// Get-or-create one child. A created child inherits its account type and
// commodity from `template` — by default the parent, so an Expenses child is
// an EXPENSE account and a Liabilities child a LIABILITY (the literal
// accounts.account_type values); archiveParentFor passes the live twin so
// archived mirrors match what they mirror.
// Structural path segments are created as placeholders, leaves that take
// postings are not. A dry run inserts nothing but still yields the would-be
// account, so the rest of the plan can proceed against it.
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
      case None        =>
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
