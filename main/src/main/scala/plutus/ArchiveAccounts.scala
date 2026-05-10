package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import porcupine.*

lazy val archiveAccountsOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "archive-accounts",
  help = "Archive hidden accounts."
):
  (verbosityOpts, inputOpts).tupled.map: (verbosity, input) =>
    archiveAccounts(
      input = fs2.io.file.Path.fromNioPath:
        input
    )(using verbosity)

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
            archiveParent <- hiddenAccount.createOrRetrieveArchiveParent(
              root = root,
              archiveSubroot = archiveSubroot
            )
            maybeExistingArchiveMirror <- archiveParent
              .child(hiddenAccount.name)
            _ <- (IO.traverse:
              maybeExistingArchiveMirror
            ): existingArchiveMirror =>
              // This is the case where an archive mirror already exists. This
              // happens when a child was already archived, resulting in the
              // creation of an archive mirror of the parent account we're now
              // archiving.
              //
              // The correct handling is to move the children of the existing
              // archive mirror to be children of the account we're now
              // archiving (their original parent) and to delete the newly
              // redundant archive mirror (because it will be replaced when the
              // original parent is moved into its place).
              for
                _ <- warn:
                  s"Archive mirror for $hiddenAccountPath already exists."
                existingChildren <- existingArchiveMirror.directChildren
                _ <- (IO.traverse:
                  existingChildren
                ): child =>
                  for
                    _ <- child.update(
                      parent = hiddenAccount
                    )
                    childPath <- child.pathString
                    _ <- warn:
                      s"Moved $childPath to $hiddenAccountPath."
                  yield ()
                existingArchiveMirrorPath <- existingArchiveMirror.pathString
                _ <- existingArchiveMirror.delete
                _ <- warn:
                  s"Deleted existing archive mirror $existingArchiveMirrorPath."
              yield ()
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
