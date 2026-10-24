package plutus

import cats.effect.*
import cue4s.*

// Interruptible rather than blocking so that Ctrl-C at a prompt exits. The
// native binary handles SIGINT by cancelling the main fiber, but a prompt only
// returns once cue4s's reading thread does, and that thread sits in getchar
// until the next key, so an uncancelable IO.blocking would leave Ctrl-C
// apparently ignored. The interrupt ends the wait, and use's finally takes the
// terminal out of raw mode. Under main3/run the JVM still waits for one more
// key before it exits.
def prompt[A](f: SyncPrompts => Completion[A]): IO[A] =
  IO.interruptible:
    Prompts.sync.use(f).getOrThrow
