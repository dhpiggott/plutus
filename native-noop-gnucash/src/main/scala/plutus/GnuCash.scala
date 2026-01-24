package plutus

import com.monovore.decline.*

import java.nio.file.Path

def gnuCashOpts(
    verbosityOpts: Opts[Verbosity],
    inputOpts: Opts[Path]
): Opts[Nothing] =
  val _ = (verbosityOpts, inputOpts)
  Opts.never
