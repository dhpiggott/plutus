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

// Move an account under the Archive subroot, mirroring its parent chain —
// shared by archive-accounts (hidden accounts) and import (deleted pots).
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
        potAccountIds = materialByAccount.collect:
          case (account, _) if account.accountType.isEmpty => account.id
        // The book is the durable home of the pot association: each pot child
        // is tagged at creation with its backing-account ID in a
        // account-level online_id slot, so a book that outlives the state
        // store (say,
        // moved to a new machine) — or a pot child since renamed or moved in
        // GnuCash — still resolves by tag.
        taggedPotAssets <- potAccountIds
          .traverse: accountId =>
            Account
              .bySlot(onlineIdSlot, accountId.value)
              .map(accountId -> _)
          .map:
            _.collect:
              case (accountId, Some(account)) => accountId -> account
            .toMap
        // Likewise fail fast on a pot the book doesn't know and no recorded
        // link names: it can't be filed into its own account, and a mis-filed
        // row would be permanent — online_id dedup skips it on every later
        // run. One run whose window spans a transfer for the pot records the
        // link.
        unnamedPots = potAccountIds.filterNot: accountId =>
          taggedPotAssets.contains(accountId) || pots.contains(accountId)
        _ <- IO.raiseUnless(unnamedPots.isEmpty):
          Error(
            s"Nothing identifies the pot(s) behind ${unnamedPots.map(_.value).mkString(", ")} — no tagged account in the book and no recorded pot link; re-run with --since spanning a transfer for each to record the link(s)."
          )
        currency <- Commodity.gbp
        // Fail fast on a pot denominated in anything but the book's currency:
        // its minor units would otherwise be posted as if they were pence.
        foreignPots = potAccountIds.flatMap: accountId =>
          pots
            .get(accountId)
            .filterNot(_.currency.value == currency.mnemonic)
            .map: pot =>
              s"${pot.name.value} (${pot.currency.value})"
        _ <- IO.raiseUnless(foreignPots.isEmpty):
          Error(
            s"Pot(s) not denominated in the book's currency (${currency.mnemonic}): ${foreignPots.mkString(", ")}."
          )
        // Resolve the fixed targets once, up front, so a missing account fails
        // before anything is written. Only the leaf accounts — category legs
        // and pot accounts — are created on demand.
        typedAssets <- materialByAccount
          .flatMap: (account, _) =>
            account.accountType.flatMap: accountType =>
              assetAccounts.byAccountType.get(accountType.value)
          .distinct
          .traverse: path =>
            existingAccountAt(path).map(path -> _)
          .map(_.toMap)
        untaggedPotAccountIds = potAccountIds.filterNot(
          taggedPotAssets.contains
        )
        potAssets <-
          if untaggedPotAccountIds.isEmpty then IO.pure(taggedPotAssets)
          else
            existingAccountAt(assetAccounts.pots).flatMap: potsParent =>
              untaggedPotAccountIds
                .traverse: accountId =>
                  createOrRetrievePotChild(
                    potsParent,
                    pots(accountId).name.value,
                    accountId,
                    dryRun
                  ).map(accountId -> _)
                .map(taggedPotAssets ++ _.toMap)
        // Monzo's categories are authoritative: each files into the account
        // categoryTarget names, created on first sight — no mapping to
        // maintain.
        categories <- materialByAccount
          .flatMap: (_, transactions) =>
            transactions
          .map(categoryTarget)
          .distinct
          .traverse: (parentPath, name) =>
            existingAccountAt(parentPath)
              .flatMap(createOrRetrieveChild(_, name, dryRun))
              .map((parentPath, name) -> _)
          .map(_.toMap)
        results <- materialByAccount.flatTraverse: (account, material) =>
          // Both lookups are total: unmapped types and unnamed pots failed the
          // run up front, and the maps were built from this same list.
          val assetAccount = account.accountType match
            case Some(accountType) =>
              typedAssets(assetAccounts.byAccountType(accountType.value))
            case None =>
              potAssets(account.id)
          material.traverse: transaction =>
            Slot
              .hasOnlineId(transaction.id)
              .flatMap:
                case true  => Imported.Skipped.pure[IO]
                case false =>
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
        // Archiving runs after posting, so an account retired mid-window
        // still receives its final transactions.
        _ <- pots.toList
          .collect:
            case (accountId, pot) if pot.deleted.value => accountId
          .traverse: accountId =>
            // The account to archive was resolved this run if the pot had
            // material transactions; otherwise look it up by tag. A pot never
            // imported has no account, so there's nothing to archive.
            val maybeAccount = potAssets.get(accountId) match
              case Some(account) => IO.pure(Some(account))
              case None => Account.bySlot(onlineIdSlot, accountId.value)
            maybeAccount.flatMap:
              case Some(account) =>
                archiveRetiredAccount(account, "its pot is deleted", dryRun)
              case None => IO.unit
        // Closed main accounts get the same treatment — /accounts keeps
        // listing them (closed: true), so the trigger persists across runs.
        // Because the mapping is type-keyed, an asset account is archived
        // only once every Monzo account of its type is closed: a closed
        // account replaced by an open one of the same type shares its asset
        // account, which must stay live.
        closedAssetPaths = byAccount
          .flatMap: (account, _) =>
            account.accountType
              .flatMap: accountType =>
                assetAccounts.byAccountType.get(accountType.value)
              .map(_ -> account)
          .groupMap(_._1)(_._2)
          .collect:
            case (path, accounts)
                if accounts.forall(_.closed.exists(_.value)) =>
              path
        _ <- closedAssetPaths.toList.traverse: path =>
          Account
            .atPath(path)
            .flatMap:
              case Some(account) =>
                archiveRetiredAccount(
                  account,
                  "its Monzo account is closed",
                  dryRun
                )
              case None => IO.unit
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

// Where a transaction's category leg posts, as (existing parent, on-demand
// child). Monzo's categories are authoritative, but not all of them are
// spending: income files under Income — as "General", mirroring the expense
// side's catch-all, since title-casing the category itself would produce
// Income:Income — and the two transfer-ish categories share one wash account
// under Assets: the two legs of a pot transfer cancel there, and what remains
// is money moved to institutions the book imports nothing from (still an
// asset, not an expense), awaiting manual re-filing. Everything else is an
// expense, named by title-casing the category (eating_out -> "Eating Out"; no
// category -> "General", Monzo's default). A refund arrives sign-flipped in
// its spending category and negates the expense, which is why the amount's
// sign plays no part here.
def categoryTarget(transaction: monzo.Transaction): (List[String], String) =
  transaction.category.fold("general")(_.value) match
    case "income"                => (List("Income"), "General")
    case "savings" | "transfers" => (List("Assets"), "Transfers")
    case category                => (List("Expenses"), titleCased(category))

def titleCased(category: String): String =
  category
    .split('_')
    .filter(_.nonEmpty)
    .map(_.capitalize)
    .mkString(" ")

// The slot a pot child is tagged with, holding its Monzo backing-account ID —
// the same account-level online_id association GnuCash's OFX importer stores,
// with the same ACCTID our OFX export emits for the pot. So an account
// associated by a past GUI import of export-transactions' OFX is found without
// re-tagging. One wrinkle: libofx builds the stored value as
// "BANKID BRANCHID ACCTID" with unconditional space separators, so
// GnuCash-written values arrive as "  acc_…"; Plutus writes the bare ID and
// Account.bySlot compares trimmed, honouring both shapes.
val onlineIdSlot: String = "online_id"

// Get-or-create a pot's asset account under the pots parent, tagging it with
// the backing-account ID so future runs (and future homes of the book) resolve
// it by tag rather than name. Retrieval tags too: a same-named child that
// predates tagging, or was created by hand, gets adopted.
def createOrRetrievePotChild(
    potsParent: Account,
    name: String,
    accountId: monzo.AccountId,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  for
    child <- createOrRetrieveChild(potsParent, name, dryRun)
    _ <- IO.unlessA(dryRun):
      Slot
        .stringSlot(
          objGuid = child.guid,
          name = onlineIdSlot,
          value = accountId.value
        )
        .insert
  yield child

// Monzo is authoritative about retirement — a deleted pot or a closed account
// can never see new activity — so the asset account is swept under Archive
// with the same mechanics as archive-accounts, and an account restored while
// its Monzo side stays retired is simply re-archived on the next run. An
// account already under Archive is left where it is: re-archiving it would
// mirror Archive inside itself.
def archiveRetiredAccount(
    account: Account,
    reason: String,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
  for
    maybeArchiveSubroot <- Account.retrieveArchiveSubroot
    underArchive <- maybeArchiveSubroot.fold(IO.pure(false)): archiveSubroot =>
      account.path.map(_.exists(_.guid == archiveSubroot.guid))
    _ <- IO.unlessA(underArchive):
      if dryRun then
        account.pathString.flatMap: path =>
          info(
            s"Would archive ${
                if path.isEmpty then account.name else path
              }: $reason."
          )
      else
        for
          root <- Account.root
          archiveSubroot <- Account.createOrRetrieveArchiveSubroot
          _ <- archiveAccount(account, root, archiveSubroot)
        yield ()
  yield ()

def existingAccountAt(
    path: List[String]
)(using db: Database[IO]): IO[Account] =
  Account
    .atPath(path)
    .flatMap:
      IO.fromOption(_):
        Error(s"No account at ${path.mkString(":")}")

// Get-or-create for the leaves Monzo data names: category accounts under
// Expenses, pot accounts under the pots parent. A created child inherits the
// parent's account type and commodity, so an Expenses child is an EXPENSE
// account and a pot child matches its parent. A dry run inserts nothing but
// still yields the would-be account, so the rest of the plan can proceed
// against it.
def createOrRetrieveChild(
    parent: Account,
    name: String,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Account] =
  parent
    .child(name)
    .flatMap:
      case Some(child) => IO.pure(child)
      case None        =>
        for
          guid <- newGuid
          parentPath <- parent.pathString
          child = parent.copy(
            guid = guid,
            name = name,
            parentGuid = Some(parent.guid),
            code = None,
            description = None,
            hidden = false,
            placeholder = false
          )
          _ <-
            if dryRun then info(s"Would create account $parentPath/$name.")
            else child.insert *> info(s"Created account $parentPath/$name.")
        yield child
