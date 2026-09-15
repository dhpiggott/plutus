package plutus

import cats.effect.*

def error(message: => String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.ERROR):
    fansi.Color.Red:
      message

def warn(message: => String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.WARN):
    fansi.Color.Yellow:
      message

def info(message: => String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.INFO):
    fansi.Color.Green:
      message

def verbose(message: => String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.VERBOSE):
    fansi.Color.Blue:
      message

def trace(message: => String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.TRACE):
    fansi.Color.White:
      message

// By name the whole way down, so a message costs nothing below its level: the
// JSON dumps in the sources encode every transaction they were given, which is
// a whole account's history in the CSV case, and the default verbosity discards
// it. The wrappers above are by name for the same reason rather than a smaller
// one — an interpolated string is cheap but not free, and a caller that has to
// think about which of the two forms defers its argument is a caller that will
// get it wrong.
def log(level: Verbosity)(a: => fansi.Str)(using
    verbosity: Verbosity
): IO[Unit] =
  (IO.whenA:
    verbosity.ordinal >= level.ordinal
  ):
    IO.println:
      a.render
