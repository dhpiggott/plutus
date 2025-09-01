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
      verbosity,
      input = fs2.io.file.Path.fromNioPath:
        input
    )

def restoreAccount(
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
        archivedAccounts <- archiveSubroot.allChildren
        archivedAccountPaths <- (IO.traverse:
          archivedAccounts
        ):
          _.pathString
        _ <- IO:
          assert(archivedAccountPaths.distinct == archivedAccountPaths)
        path <- IO.blocking:
          Prompts.sync.use:
            _.singleChoice(
              "Choose account to restore:",
              archivedAccountPaths
            ).getOrThrow
        account = archivedAccounts(
          archivedAccountPaths.indexOf(path)
        )
        nonArchiveParent <- account.createOrRetrieveNonArchiveParent(
          root = root,
          archiveSubroot = archiveSubroot
        )
        // TODO: Handle the case where a non-archive equivalent already exists
        // (which should be a conflict). This happens when a child was already
        // restored, resulting in the creation of a non-archive equivalent of
        // the parent account we're now restoring...
        restoredAccount <- account.update(
          parent = nonArchiveParent,
          hidden = true
        )
        _ <- (IO.whenA:
          verbosity.intValue >= Verbosity.DEFAULT.intValue
        ):
          for
            restoredPath <- restoredAccount.pathString
            _ <- IO.println:
              fansi.Color.Green:
                s"Restored $path to $restoredPath."
          yield ()
      yield ()
