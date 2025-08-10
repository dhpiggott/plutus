package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*

object Plutus
    extends CommandIOApp(
      name = "plutus",
      header = "Personal finance utility.",
      version = plutus.BuildInfo.version
    ):

  override def main: Opts[IO[ExitCode]] =
    (archiveAccountsOpts orElse exportTransactionsOpts).map:
      _.as:
        ExitCode.Success

lazy val verbosityOpts: Opts[Verbosity] =
  silentOpts orElse verboseOpts orElse debugOpts withDefault Verbosity.DEFAULT

lazy val silentOpts: Opts[Verbosity] =
  Opts
    .flag("silent", help = "Don't log anything.")
    .as:
      Verbosity.SILENT

lazy val verboseOpts: Opts[Verbosity] =
  Opts
    .flag(
      "verbose",
      help =
        "Log decoded account and transaction entities. This includes default logging."
    )
    .as:
      Verbosity.VERBOSE

lazy val debugOpts: Opts[Verbosity] =
  Opts
    .flag(
      "debug",
      help =
        "Log raw HTTP requests and responses. This includes --verbose logging."
    )
    .as:
      Verbosity.DEBUG
