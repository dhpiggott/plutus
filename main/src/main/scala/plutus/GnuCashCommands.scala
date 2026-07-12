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
          for
            hiddenAccountPath <- hiddenAccount.pathString
            archiveParent <- hiddenAccount.createOrRetrieveMirrorParent(
              from = root,
              to = archiveSubroot
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
          mirrorKind = "Non-archive"
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
  (now, byAccount, potNames) <- fetchTransactionsByAccount(since, before)
  // Snapshot first: a bad run becomes a restore, not a rebuild.
  _ <- IO.unlessA(dryRun):
    val backup = fs2.io.file.Path(s"$input.bak")
    fs2.io.file.Files[IO].copy(input, backup) *> info(s"Backed up to $backup.")
  _ <- Database
    .open[IO](input.toString)
    .use: db =>
      given Database[IO] = db
      val assetAccounts = AssetAccounts.default
      val run = for
        currency <- Commodity.gbp
        // Resolve the fixed targets once, up front, so a missing account fails
        // before anything is written. Only the leaf accounts — category legs
        // and pot accounts — are created on demand.
        typedAssets <- byAccount
          .flatMap: (account, _) =>
            account.accountType.flatMap: accountType =>
              assetAccounts.byAccountType.get(accountType.value)
          .distinct
          .traverse: path =>
            existingAccountAt(path).map(path -> _)
          .map(_.toMap)
        potsParent <-
          if byAccount.exists((account, _) => account.accountType.isEmpty) then
            existingAccountAt(assetAccounts.pots).map(Some(_))
          else IO.pure(None)
        // Monzo's categories are authoritative: each files into the account
        // categoryTarget names, created on first sight — no mapping to
        // maintain.
        categories <- byAccount
          .flatMap: (_, transactions) =>
            materialTransactions(transactions)
          .map(categoryTarget)
          .distinct
          .traverse: (parentPath, name) =>
            existingAccountAt(parentPath)
              .flatMap(createOrRetrieveChild(_, name, dryRun))
              .map((parentPath, name) -> _)
          .map(_.toMap)
        results <- byAccount.flatTraverse: (account, transactions) =>
          val material = materialTransactions(transactions)
          val maybeAssetAccount: IO[Option[Account]] =
            account.accountType match
              case Some(accountType) =>
                assetAccounts.byAccountType.get(accountType.value) match
                  case Some(path) => IO.pure(Some(typedAssets(path)))
                  case None       =>
                    warn(
                      s"No asset account mapped for Monzo account type '${accountType.value}' (${account.id.value}); skipping its ${material.size} transaction(s)."
                    ).as(None)

              case None =>
                potNames.get(account.id) match
                  case Some(potName) =>
                    // potsParent is present by construction: a typeless
                    // account is a pot, so the branch above resolved it.
                    createOrRetrieveChild(potsParent.get, potName, dryRun)
                      .map(Some(_))

                  case None =>
                    warn(
                      s"No recorded pot link names the pot behind ${account.id.value}; posting to ${assetAccounts.pots.mkString(":")} itself. A run whose window spans a transfer for the pot records the link."
                    ).as(potsParent)
          maybeAssetAccount.flatMap:
            case None               => IO.pure(List.empty[Imported])
            case Some(assetAccount) =>
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
// spending: income is income, and the two transfer-ish categories share one
// wash account under Assets — the two legs of a pot transfer cancel there,
// and what remains is money moved to institutions the book imports nothing
// from (still an asset, not an expense), awaiting manual re-filing.
// Everything else is an expense, named by title-casing the category
// (eating_out -> "Eating Out"; no category -> "General", Monzo's default). A
// refund arrives sign-flipped in its spending category and negates the
// expense, which is why the amount's sign plays no part here.
def categoryTarget(transaction: monzo.Transaction): (List[String], String) =
  transaction.category.fold("general")(_.value) match
    case "transfers" | "savings" => (List("Assets"), "Transfers")
    case "income"                => (List("Income"), "Income")
    case category                => (List("Expenses"), titleCased(category))

def titleCased(category: String): String =
  category
    .split('_')
    .filter(_.nonEmpty)
    .map(_.capitalize)
    .mkString(" ")

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
