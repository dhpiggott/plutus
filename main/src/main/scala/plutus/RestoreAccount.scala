package plutus

import cats.effect.*
import com.monovore.decline.*
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder

lazy val restoreAccountOpts: Opts[IO[Unit]] = Opts.subcommand(
  name = "restore-account",
  help = "Restore archived account."
):
  Opts:
    restoreAccount

def restoreAccount: IO[Unit] =
  val terminal = TerminalBuilder.terminal()
  val lineReader = LineReaderBuilder
    .builder()
    .terminal(terminal)
    .build()
  val accountGuid = lineReader.readLine("Enter account GUID: ")
  IO.println:
    s"Account GUID: $accountGuid."
