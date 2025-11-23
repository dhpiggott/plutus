package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import cue4s.*
import porcupine.*

lazy val restoreAccountOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "restore-account",
  help = "Restore archived account."
):
  (verbosityOpts, inputOpts).tupled.map: (verbosity, input) =>
    restoreAccount(
      input = fs2.io.file.Path.fromNioPath:
        input
    )(using verbosity)

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
        archivedAccountPaths <- (IO.traverse:
          archivedAccounts
        ):
          _.pathString
        _ <- IO:
          assert(archivedAccountPaths.distinct == archivedAccountPaths)
        archivedAccountPath <- IO.blocking:
          Prompts.sync.use:
            _.singleChoice(
              "Choose account to restore:",
              archivedAccountPaths
            ).getOrThrow
        archivedAccount = archivedAccounts(
          archivedAccountPaths.indexOf(archivedAccountPath)
        )
        nonArchiveParent <- archivedAccount.createOrRetrieveNonArchiveParent(
          root = root,
          archiveSubroot = archiveSubroot
        )
        maybeExistingNonArchiveMirror <- nonArchiveParent
          .child(archivedAccount.name)
        _ <- (IO.traverse:
          maybeExistingNonArchiveMirror
        ): existingNonArchiveMirror =>
          // This is the case where a non-archive mirror already exists. This
          // happens when a child was already restored, resulting in the
          // creation of a non-archive mirror of the parent account we're now
          // restoring.
          //
          // The correct handling is to move the children of the non-archive
          // mirror to be children of the acount we're now restoring (their
          // original parent) and to delete the newly redundant non-archive
          // mirror.
          for
            _ <- warn:
              s"Non-archive mirror for $archivedAccountPath already exists."
            existingChildren <- existingNonArchiveMirror.directChildren
            _ <- (IO.traverse:
              existingChildren
            ): child =>
              for
                _ <- child.update(
                  parent = archivedAccount
                )
                childPath <- child.pathString
                _ <- warn:
                  s"Moved $childPath to $archivedAccountPath."
              yield ()
            _ <- existingNonArchiveMirror.delete
            existingNonArchiveMirrorPath <- existingNonArchiveMirror.pathString
            _ <- warn:
              s"Deleted existing non-archive mirror $existingNonArchiveMirrorPath."
          yield ()
        restoredAccount <- archivedAccount.update(
          parent = nonArchiveParent
        )
        restoredPath <- restoredAccount.pathString
        _ <- info:
          s"Restored $archivedAccountPath to $restoredPath."
      yield ()
