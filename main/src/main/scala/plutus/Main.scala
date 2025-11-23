package plutus

import cats.effect.*
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*

import java.nio.file.Path

object Plutus
    extends CommandIOApp(
      name = "plutus",
      header = "Personal finance utility.",
      version = plutus.BuildInfo.version
    ):

  override def main: Opts[IO[ExitCode]] =
    (archiveAccountsOpts orElse exportTransactionsOpts orElse restoreAccountOpts)
      .map:
        _.as:
          ExitCode.Success

lazy val verbosityOpts: Opts[Verbosity] =
  errorOpts orElse warnOpts orElse infoOpts orElse verboseOpts orElse traceOpts withDefault Verbosity.INFO

lazy val inputOpts: Opts[Path] =
  Opts
    .option[Path](
      "input",
      help =
        "Path to read GnuCash SQLite3 file from. If not specified defaults to Accounts.gnucash in the current directory."
    )
    .orElse:
      Opts:
        Path.of:
          "Accounts.gnucash"

lazy val errorOpts: Opts[Verbosity] =
  Opts
    .flag("error", help = "Log errors only.")
    .as:
      Verbosity.ERROR

lazy val warnOpts: Opts[Verbosity] =
  Opts
    .flag("warn", help = "Log warnings only. Implies --error.")
    .as:
      Verbosity.WARN

lazy val infoOpts: Opts[Verbosity] =
  Opts
    .flag(
      "info",
      help = "Default log level. Implies --warn."
    )
    .as:
      Verbosity.INFO

lazy val verboseOpts: Opts[Verbosity] =
  Opts
    .flag(
      "verbose",
      help = "Log decoded account and transaction entities. Implies --info."
    )
    .as:
      Verbosity.VERBOSE

lazy val traceOpts: Opts[Verbosity] =
  Opts
    .flag(
      "trace",
      help = "Log raw HTTP requests and responses. Implies --verbose."
    )
    .as:
      Verbosity.TRACE
