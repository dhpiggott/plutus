package plutus

import cats.effect.*
import com.monovore.decline.*
import com.monovore.decline.effect.*

object Plutus
    extends CommandIOApp(
      name = "plutus",
      header = "Personal finance utility.",
      version = plutus.BuildInfo.version
    ):

  override def main: Opts[IO[ExitCode]] =
    (exportTransactionsOpts orElse readGnucashOpts).map:
      _.as:
        ExitCode.Success
