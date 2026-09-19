package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import cue4s.*
import porcupine.*

import scala.collection.immutable.SortedMap

lazy val archiveAccountsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "archive-accounts",
  help = "Archive hidden accounts."
):
  (verbosityOpts, inputOpts, archiveDryRunOpts, ignoreLockOpts).tupled.map:
    (verbosity, input, dryRun, ignoreLock) =>
      archiveAccounts(input, dryRun, ignoreLock)(using verbosity)

lazy val archiveDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print what would be archived without writing to the book and without taking a backup."
    )
    .orFalse

// Shared by all three commands rather than spelled out per command like
// --dry-run above, whose help text differs by what each one would print: this
// one says the same thing whichever command carries it.
lazy val ignoreLockOpts: Opts[Boolean] =
  Opts
    .flag(
      "ignore-lock",
      help =
        "Open the book even though GnuCash's gnclock table says another process has it open. Only for a lock left behind by a GnuCash that crashed — a live one will overwrite whatever this run writes."
    )
    .orFalse

def archiveAccounts(
    input: fs2.io.file.Path,
    dryRun: Boolean,
    ignoreLock: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  for
    now <- IO.realTimeInstant
    _ <- withBook(input, now, dryRun, ignoreLock): db =>
      given Database[IO] = db
      for
        // TODO: Change this to accept a single account to archive, like
        // restore-account does?
        _ <- info:
          "Finding hidden accounts…"
        // Whole run in one transaction, the Archive subroot's own creation
        // included: a failure partway through rolls every account archived so
        // far in this run — and, on a book's first archive, the subroot it
        // created to hold them — back to where it started, rather than
        // leaving some archived and others not.
        _ <- db.transactOrRollBack(dryRun):
          for
            roots <- BookRoots.creating(dryRun)
            archiveSubroot <- roots.archiveSubroot
            hiddenAccounts <- hiddenAccountsToArchive(
              roots.root,
              archiveSubroot
            )
            _ <- (IO.traverse:
              hiddenAccounts
            ): hiddenAccount =>
              for
                hiddenAccountPath <- hiddenAccount.pathString
                livePathInit <- hiddenAccount.pathInitBelow(roots.root)
                archiveParent <- boundaryParentFor(
                  livePathInit,
                  retired = true,
                  roots,
                  dryRun
                )
                // Where the account will sit, spelled out rather than read
                // back: in a dry run its parents are would-be accounts that
                // were never inserted, so pathString would come back empty.
                archivedPath = canonicalPathString(
                  livePathInit :+ hiddenAccount.name,
                  retired = true
                )
                _ <- cleanUpRedundantMirror(
                  original = hiddenAccount,
                  originalPath = hiddenAccountPath,
                  mirrorParent = archiveParent,
                  mirrorKind = "Archive",
                  dryRun
                )
                _ <- IO.unlessA(dryRun):
                  hiddenAccount.update(parent = archiveParent).void
                _ <- info:
                  val verb = if dryRun then "Would archive" else "Archived"
                  s"$verb $hiddenAccountPath to $archivedPath."
              yield ()
          yield ()
        _ <- info:
          "Finished archiving hidden accounts."
      yield ()
  yield ()

lazy val restoreAccountOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "restore-account",
  help = "Restore archived account."
):
  (verbosityOpts, inputOpts, restoreDryRunOpts, ignoreLockOpts).tupled.map:
    (verbosity, input, dryRun, ignoreLock) =>
      restoreAccount(input, dryRun, ignoreLock)(using verbosity)

lazy val restoreDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help =
        "Print what would be restored without writing to the book and without taking a backup."
    )
    .orFalse

def restoreAccount(
    input: fs2.io.file.Path,
    dryRun: Boolean,
    ignoreLock: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  for
    now <- IO.realTimeInstant
    _ <- withBook(input, now, dryRun, ignoreLock): db =>
      given Database[IO] = db
      for
        root <- Account.root
        nothingToRestore = Error("No archived accounts to restore.")
        // Retrieved, never created: a book that has never archived anything
        // has no Archive subroot and nothing to restore, and creating one
        // here would be a write outside the transaction below, on behalf of a
        // command that is about to do nothing. Which is also why the roots
        // this command hands the mirror below are both already resolved —
        // there is nothing left for them to bring into being.
        archiveSubroot <- root
          .child(ArchiveName)
          .flatMap:
            IO.fromOption(_):
              nothingToRestore
        roots = BookRoots(root, IO.pure(archiveSubroot))
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
        _ <- db.transactOrRollBack(dryRun):
          for
            archivePathInit <- archivedAccount.pathInitBelow(archiveSubroot)
            nonArchiveParent <- boundaryParentFor(
              archivePathInit,
              retired = false,
              roots,
              dryRun
            )
            restoredPath = canonicalPathString(
              archivePathInit :+ archivedAccount.name,
              retired = false
            )
            _ <- cleanUpRedundantMirror(
              original = archivedAccount,
              originalPath = archivedAccountPath,
              mirrorParent = nonArchiveParent,
              mirrorKind = "Non-archive",
              dryRun
            )
            _ <- IO.unlessA(dryRun):
              archivedAccount.update(parent = nonArchiveParent).void
            _ <- info:
              val verb = if dryRun then "Would restore" else "Restored"
              s"$verb $archivedAccountPath to $restoredPath."
          yield ()
      yield ()
  yield ()

// The hidden accounts archive-accounts will move: the frontier of hidden
// accounts below `root`, stopping at the first hidden account on each branch
// (its children are implicitly hidden, so listing them too would archive a
// subtree a node at a time) and skipping the Archive subroot, since the point
// of the scan is to find what still needs moving into it.
//
// One recursive query and an in-memory walk, rather than a directChildren query
// per account: the walk visits exactly the nodes that query returned, and a
// book whose tree is mostly not hidden used to pay a round trip for each.
def hiddenAccountsToArchive(root: Account, archiveSubroot: Account)(using
    db: Database[IO]
): IO[List[Account]] =
  root.allChildren.map: descendants =>
    val byParentGuid = descendants.groupBy(_.parentGuid)
    // By name, as the directChildren query this replaces ordered them.
    def frontier(account: Account): List[Account] =
      byParentGuid
        .getOrElse(Some(account.guid), Nil)
        .sortBy(_.name)
        .flatMap: child =>
          if child.guid == archiveSubroot.guid then Nil
          else if child.hidden then List(child)
          else frontier(child)
    frontier(root)

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
    mirrorKind: String,
    dryRun: Boolean
)(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
  for
    maybeExistingMirror <- mirrorParent.child(original.name)
    _ <- (IO.traverse:
      maybeExistingMirror
    ): existingMirror =>
      for
        _ <- warn:
          s"$mirrorKind mirror for $originalPath already exists."
        // Read before anything moves, and read rather than spelled out: the
        // mirror is always a real account here — a fabricated dry-run parent
        // has no children to find one under — but where it sits is only known
        // to the book.
        existingMirrorPath <- existingMirror.pathString
        existingChildren <- existingMirror.directChildren
        _ <- (IO.traverse:
          existingChildren
        ): child =>
          val childPath = s"$existingMirrorPath/${child.name}"
          // Where the child ends up, spelled out rather than read back after
          // the move: a dry run doesn't move it, and the path would otherwise
          // have to be queried twice to say the same thing.
          val movedChildPath = s"$originalPath/${child.name}"
          IO.unlessA(dryRun)(child.update(parent = original).void) *> warn:
            val verb = if dryRun then "Would move" else "Moved"
            s"$verb $childPath to $movedChildPath."
        _ <- IO.unlessA(dryRun)(existingMirror.delete)
        _ <- warn:
          val verb = if dryRun then "Would delete" else "Deleted"
          s"$verb existing ${mirrorKind.toLowerCase} mirror $existingMirrorPath."
      yield ()
  yield ()
