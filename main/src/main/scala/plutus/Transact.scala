package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*

// Wraps body in one SQLite transaction: a mid-run failure rolls the book back
// to exactly its pre-run state, rather than leaving it partway through a
// multi-write command (archive-accounts, restore-account and
// import-transactions all use this). NOTE: the per-connection Mutex in
// Database.open only guards one prepared statement at a time — it's taken
// and released per statement, not held across the whole body — so it is NOT
// what makes begin/body/commit atomic here. That safety comes entirely from
// the CLI running one command to completion in a single fiber: nothing else
// can interleave a statement between this begin and its commit.
extension [F[_]](db: Database[F])(using F: MonadCancel[F, Throwable])
  def transact[A](body: F[A]): F[A] =
    val begin = db.execute(sql"begin immediate".command)
    val commit = db.execute(sql"commit".command)
    val rollback = db.execute(sql"rollback".command)
    begin *> body.attempt.flatMap:
      case Right(a) => commit.as(a)
      case Left(e)  => rollback *> F.raiseError(e)
