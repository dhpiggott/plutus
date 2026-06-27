package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*

// One SQLite transaction around the whole run: a mid-import failure rolls the
// book back to exactly its pre-run state. NOTE: the per-connection Mutex in
// Database.open serialises individual operations but does NOT scope a
// multi-statement transaction — this is safe only because the CLI runs these
// sequentially in a single fiber, not because of any concurrency isolation.
extension [F[_]](db: Database[F])(using F: MonadCancel[F, Throwable])
  def transact[A](body: F[A]): F[A] =
    val begin = db.execute(sql"begin immediate".command)
    val commit = db.execute(sql"commit".command)
    val rollback = db.execute(sql"rollback".command)
    begin *> body.attempt.flatMap:
      case Right(a) => commit.as(a)
      case Left(e)  => rollback *> F.raiseError(e)
