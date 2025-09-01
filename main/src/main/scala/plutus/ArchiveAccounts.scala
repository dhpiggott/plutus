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
      verbosity,
      input = fs2.io.file.Path.fromNioPath:
        input
    )

def archiveAccounts(
    verbosity: Verbosity,
    input: fs2.io.file.Path
): IO[Unit] =
  Database
    .open[IO]:
      input.toString
    .use: db =>
      given Database[IO] = db
      for
        root <- Account.root
        archiveSubroot <- Account.createOrRetrieveArchiveSubroot
        _ <- (IO.whenA:
          verbosity.intValue >= Verbosity.DEFAULT.intValue
        ):
          IO.println:
            fansi.Color.Green:
              "Finding hidden accounts…"
        hiddenAccounts <- root.hiddenChildren:
          archiveSubroot
        _ <- (IO.traverse:
          hiddenAccounts
        ): hiddenAccount =>
          for
            archiveParent <- hiddenAccount.createOrRetrieveArchiveParent(
              root = root,
              archiveSubroot = archiveSubroot
            )
            // TODO: Handle the case where an archive equivalent already exists
            // (which should be a conflict). This happens when a child was
            // already archived, resulting in the creation of an archive
            // equivalent of the parent account we're now archiving. The correct
            // handling is to move the children of the existing archive
            // equivalent to be children of the "real" parent and to delete the
            // newly redundant archive equivalent of the parent account we're
            // moving.
            archivedAccount <- hiddenAccount.update(
              parent = archiveParent,
              hidden = false
            )
            _ <- (IO.whenA:
              verbosity.intValue >= Verbosity.DEFAULT.intValue
            ):
              for
                path <- hiddenAccount.pathString
                archivedPath <- archivedAccount.pathString
                _ <- IO.println:
                  fansi.Color.Green:
                    s"Archived $path to $archivedPath."
              yield ()
          yield ()
        _ <- (IO.whenA:
          verbosity.intValue >= Verbosity.DEFAULT.intValue
        ):
          IO.println:
            fansi.Color.Green:
              "Finished archiving hidden accounts."
      yield ()
