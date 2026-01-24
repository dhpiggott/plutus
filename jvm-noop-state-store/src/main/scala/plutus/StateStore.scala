package plutus

import cats.effect.*

object StateStore:

  def make(using verbosity: Verbosity): StateStore[IO] =
    val _ = verbosity
    new StateStoreImpl()

final class StateStoreImpl extends StateStore[IO]:

  override def loadState(): IO[LoadStateOutput] =
    IO.pure:
      LoadStateOutput:
        None

  override def saveState(
      state: State,
      mode: SaveStateMode
  ): IO[Unit] =
    IO.unit
