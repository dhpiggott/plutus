package plutus

import cats.effect.*
import com.monovore.decline.*

import java.nio.file.Path

def gnuCashOpts(
    verbosityOpts: Opts[Verbosity],
    inputOpts: Opts[Path]
): Opts[IO[Unit]] =
  archiveAccountsOpts(verbosityOpts, inputOpts) orElse restoreAccountOpts(
    verbosityOpts,
    inputOpts
  )
