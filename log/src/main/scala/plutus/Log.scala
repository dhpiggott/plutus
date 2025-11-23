package plutus

import cats.effect.*
import cats.*

def error(message: String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.ERROR):
    fansi.Color.Red:
      message

def warn(message: String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.WARN):
    fansi.Color.Yellow:
      message

def info(message: String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.INFO):
    fansi.Color.Green:
      message

def verbose(message: String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.VERBOSE):
    fansi.Color.Blue:
      message

def trace(message: String)(using verbosity: Verbosity): IO[Unit] =
  log(Verbosity.TRACE):
    fansi.Color.White:
      message

def log[A](level: Verbosity)(
    a: A
)(implicit S: Show[A] = Show.fromToString[A], verbosity: Verbosity): IO[Unit] =
  (IO.whenA:
    verbosity.intValue >= level.intValue
  ):
    IO.println:
      a
