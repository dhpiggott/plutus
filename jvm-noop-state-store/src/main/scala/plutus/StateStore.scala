package plutus

import cats.effect.*

object StateStore extends StateStore[IO]:

  override def loadState(verbosity: Verbosity): IO[LoadStateOutput] =
    IO.pure:
      LoadStateOutput:
        None

  override def saveState(
      state: State,
      mode: SaveStateMode,
      verbosity: Verbosity
  ): IO[Unit] =
    IO.unit
