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
)(implicit verbosity: Verbosity): IO[Unit] =
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
        restoredAccount <- nonArchiveParent
          .child(archivedAccount.name)
          .flatMap:
            case None =>
              // The common case: there is no existing non-archive mirror, so we
              // simply move the account out of the archive structure back to
              // its original location.
              archivedAccount.update(
                parent = nonArchiveParent,
                hidden = true
              )

            case Some(existingNonArchiveVersion) =>
              // TODO: Test this case.
              //
              // This is the case where a non-archive mirror already exists.
              // This happens when a child was already restored, resulting in
              // the creation of a non-archive mirror of the parent account
              // we're now restoring.
              //
              // The correct handling is to move the children of the account
              // we're now restoring to be children of the existing non-archive
              // mirror (their original parent) and to delete the newly
              // redundant archive mirror.
              for
                _ <- warn:
                  s"Non-archive mirror for $archivedAccountPath already exists."
                existingChildren <- archivedAccount.directChildren
                _ <- (IO.traverse:
                  existingChildren
                ): child =>
                  for
                    _ <- child.update(
                      parent = existingNonArchiveVersion
                    )
                    childPath <- child.pathString
                    _ <- warn:
                      s"Moved $childPath to $existingNonArchiveVersion."
                  yield ()
                _ <- archivedAccount.delete
              yield existingNonArchiveVersion
        restoredPath <- restoredAccount.pathString
        _ <- info:
          s"Restored $archivedAccountPath to $restoredPath."
      yield ()
