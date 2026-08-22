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

  // Wraps body in a transaction that is always rolled back, for a run that
  // is only meant to read. Without it a statement that slipped past a dry
  // run's guards would commit the moment it ran, in autocommit — and a dry
  // run takes no backup, so there would be nothing to restore from. The
  // `begin` here is deferred, unlike transact's `begin immediate`, so a run
  // that writes nothing never takes the write lock and reads exactly as it
  // did before.
  def withoutCommitting[A](body: F[A]): F[A] =
    val begin = db.execute(sql"begin".command)
    val rollback = db.execute(sql"rollback".command)
    begin *> body.attempt.flatMap: result =>
      rollback *> F.fromEither(result)

// Rows inserted, updated or deleted on this connection since it was opened —
// SQLite's own count, so no write path has to remember to report itself. The
// counter isn't decremented by a rollback, which is what makes it answer two
// different questions: read after transact it is "did this run change
// anything?", and read after withoutCommitting it is "did this run try to?".
extension [F[_]](db: Database[F])
  def rowsChanged: F[Long] =
    db.unique(sql"select total_changes()".query(porcupine.Codec.integer))
