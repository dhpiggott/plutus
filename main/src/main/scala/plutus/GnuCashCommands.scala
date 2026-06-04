package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import cue4s.*
import porcupine.*
import porcupine.Codec.*

import scala.collection.immutable.SortedMap

lazy val gnucashOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "gnucash",
  help = "GnuCash housekeeping."
):
  archiveAccountsOpts orElse restoreAccountOpts orElse auditSlotsOpts

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

lazy val auditSlotsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "audit-slots",
  help = "Reconcile hidden/placeholder columns with their slots."
):
  (verbosityOpts, inputOpts, auditDryRunOpts).tupled.map:
    (verbosity, input, dryRun) => auditSlots(input, dryRun)(using verbosity)

lazy val auditDryRunOpts: Opts[Boolean] =
  Opts
    .flag(
      "dry-run",
      help = "Report inconsistencies without fixing them."
    )
    .orFalse

// Repairs files that drift between the hidden/placeholder slots and their
// cache columns: those Plutus wrote before it learned to write slots, or that
// the user toggled in the GnuCash UI (which rewrites the slot but not always
// the column Plutus reads). The slot is the source of truth — matching what
// GnuCash reads — so every fix mutates the column or deletes a stray slot,
// never the reverse.
def auditSlots(
    input: fs2.io.file.Path,
    dryRun: Boolean
)(using verbosity: Verbosity): IO[Unit] =
  Database
    .open[IO]:
      input.toString
    .use: db =>
      given Database[IO] = db

      def describe(guid: String): IO[String] =
        Account
          .get(guid)
          .flatMap:
            case Some(account) => account.pathString
            case None          => IO.pure(guid)

      def fix(guid: String, what: String, repair: IO[Unit]): IO[Unit] =
        describe(guid).flatMap: path =>
          if dryRun then
            warn:
              s"Would fix $path: $what."
          else
            repair *> info:
              s"Fixed $path: $what."

      def fixColumn(guid: String, flag: String, value: Boolean): IO[Unit] =
        flag match
          case "hidden"      => Account.setHiddenColumn(guid, value)
          case "placeholder" => Account.setPlaceholderColumn(guid, value)
          case _             => IO.unit

      for
        _ <- info:
          "Auditing hidden/placeholder slots…"
        // For each flag (as data, so one query covers both): the cache column,
        // whether a slot exists, and whether that slot is truthy.
        flagStates <- db.execute:
          sql"""
            select
              accounts.guid,
              'hidden' as flag,
              coalesce(accounts.hidden, 0) as col,
              hidden_slot.obj_guid is not null as slot_exists,
              coalesce(
                hidden_slot.string_val = 'true',
                hidden_slot.int64_val != 0,
                0
              ) as slot_truthy
            from accounts
            left join slots hidden_slot
              on hidden_slot.obj_guid = accounts.guid
              and hidden_slot.name = 'hidden'
            union all
            select
              accounts.guid,
              'placeholder',
              coalesce(accounts.placeholder, 0),
              placeholder_slot.obj_guid is not null,
              coalesce(
                placeholder_slot.string_val = 'true',
                placeholder_slot.int64_val != 0,
                0
              )
            from accounts
            left join slots placeholder_slot
              on placeholder_slot.obj_guid = accounts.guid
              and placeholder_slot.name = 'placeholder'
          """.query:
            text *: text *: boolean *: boolean *: boolean *: nil
        _ <- (IO.traverse:
          flagStates
        ):
          case (guid, flag, col, slotExists, slotTruthy) =>
            (col, slotExists, slotTruthy) match
              case (true, false, _) =>
                fix(
                  guid,
                  s"$flag column set but slot missing — added slot",
                  Slot.stringSlot(guid, flag, "true").insert
                )
              case (false, true, true) =>
                fix(
                  guid,
                  s"$flag slot set but column clear — set column",
                  fixColumn(guid, flag, true)
                )
              case (true, true, false) =>
                fix(
                  guid,
                  s"$flag slot clear but column set — cleared column",
                  fixColumn(guid, flag, false)
                )
              case _ =>
                IO.unit
        // Orphan slots: a hidden/placeholder slot whose account no longer
        // exists. Scoped to these two names because the slots table is
        // polymorphic — obj_guid can reference transactions, splits, books,
        // lots, commodities, … whose object tables we don't model, so we can't
        // safely call a slot of any other name orphaned.
        orphans <- db.execute:
          sql"""
            select slots.obj_guid, slots.name
            from slots
            left join accounts on accounts.guid = slots.obj_guid
            where accounts.guid is null
              and slots.name in ('hidden', 'placeholder')
          """.query:
            text *: text *: nil
        _ <- (IO.traverse:
          orphans
        ):
          case (objGuid, name) =>
            if dryRun then
              warn:
                s"Would fix $objGuid: orphan $name slot — deleted."
            else
              Slot.delete(objGuid, name) *> info:
                s"Fixed $objGuid: orphan $name slot — deleted."
        _ <- info:
          "Finished auditing hidden/placeholder slots."
      yield ()
