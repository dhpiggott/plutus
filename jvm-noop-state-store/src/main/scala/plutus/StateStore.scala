package plutus

import cats.effect.*

object StateStore extends StateStore[IO]:

  def apply(implicit verbosity: Verbosity): StateStore[IO] =
    this

  override def loadState(): IO[LoadStateOutput] =
    IO.pure:
      LoadStateOutput:
        None

  override def saveState(
      state: State,
      mode: SaveStateMode
  ): IO[Unit] =
    IO.unit
