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
        "Print the plan (filed / uncategorised / skipped) without writing to the book and without taking a backup."
    )
    .orFalse

def importTransactions(
    input: fs2.io.file.Path,
    since: Option[Instant],
    before: Option[Instant],
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] = for
  (now, byAccount) <- fetchTransactionsByAccount(since, before)
  // Snapshot first: a bad run becomes a restore, not a rebuild.
  _ <- IO.unlessA(dryRun):
    val backup = fs2.io.file.Path(s"$input.bak")
    fs2.io.file.Files[IO].copy(input, backup) *> info(s"Backed up to $backup.")
  _ <- Database
    .open[IO](input.toString)
    .use: db =>
      given Database[IO] = db
      val rules = ImportRules.default
      val assetAccounts = AssetAccounts.default
      val mapped = byAccount.map: (account, transactions) =>
        (account, assetAccounts.pathFor(account), transactions)
      val run = for
        currency <- Commodity.gbp
        // Resolve every account this run will write to once, up front, so a
        // missing target (asset or category) fails before anything is written.
        assets <- mapped
          .flatMap: (_, path, _) =>
            path
          .distinct
          .traverse: path =>
            Account
              .atPath(path)
              .flatMap:
                IO.fromOption(_):
                  Error(s"No account at ${path.mkString(":")}")
              .map(path -> _)
          .map(_.toMap)
        targets <- rules.resolve
        results <- mapped.flatTraverse: (account, maybePath, transactions) =>
          val material = materialTransactions(transactions)
          maybePath match
            case None =>
              warn(
                s"No asset account mapped for Monzo account type '${account.accountType
                    .fold("<none>")(_.value)}' (${account.id.value}); skipping its ${material.size} transaction(s)."
              ).as(List.empty[Imported])

            case Some(assetPath) =>
              val assetAccount = assets(assetPath)
              material.traverse: transaction =>
                Slot
                  .hasOnlineId(transaction.id)
                  .flatMap:
                    case true  => Imported.Skipped.pure[IO]
                    case false =>
                      val path = rules.accountPathFor(transaction)
                      val categoryAccount = targets(path)
                      Posting
                        .fromMonzo(
                          transaction,
                          assetAccount,
                          categoryAccount,
                          currency,
                          now
                        )
                        .flatMap: posting =>
                          IO.unlessA(dryRun)(posting.insert)
                            .as:
                              if path == rules.fallback then
                                Imported.Uncategorised
                              else Imported.Filed
        _ <- info:
          val filed = results.count(_ == Imported.Filed)
          val uncategorised = results.count(_ == Imported.Uncategorised)
          val skipped = results.count(_ == Imported.Skipped)
          s"$filed filed, $uncategorised to Uncategorised, $skipped already present."
      yield ()
      // Everything-or-nothing, unless we're only previewing.
      if dryRun then run else db.transact(run)
yield ()

enum Imported:
  case Filed, Uncategorised, Skipped
