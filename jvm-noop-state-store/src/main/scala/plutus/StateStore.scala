package plutus

import cats.effect.*

import scala.annotation.unused

object StateStore:

  def make(using @unused verbosity: Verbosity): StateStore[IO] =
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
