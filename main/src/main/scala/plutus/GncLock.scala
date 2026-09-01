package plutus

import cats.effect.*
import cats.syntax.all.*
import porcupine.*
import porcupine.Codec.*

object GncLock:

  // GnuCash's own cooperative lock, so the CLI and the GUI can't both be
  // writing the same book: opening a book read/write, GnuCash inserts its
  // hostname and PID here, prompts when it finds a row that isn't its own, and
  // deletes its row on close (which is why a GnuCash that crashed leaves one
  // behind). Nothing at the SQLite level enforces any of it — a client that
  // ignores the table writes anyway, silently, which is what this CLI did
  // until now.
  //
  // Held for the whole of withBook rather than for the transaction inside it,
  // so that restore-account's prompt — deliberately outside the write lock,
  // since it waits on stdin — is covered too.
  def hold(
      input: fs2.io.file.Path,
      dryRun: Boolean,
      ignoreLock: Boolean
  )(using db: Database[IO], verbosity: Verbosity): Resource[IO, Unit] =
    Resource
      .make(take(input, dryRun, ignoreLock)):
        // Exactly the row we inserted, matched on both columns, so a GnuCash
        // holding the book alongside us — because --ignore-lock let us in on
        // top of it, or because it arrived after we looked — keeps its own.
        // Released here rather than at the end of withBook so that an
        // interrupted run still gives the book back.
        case None       => IO.unit
        case Some(lock) =>
          lock.delete *> verbose(s"Released the lock on $input.")
      .void

  // None when nothing was taken and so nothing has to be released: a dry run,
  // or a book with no gnclock table at all.
  private def take(
      input: fs2.io.file.Path,
      dryRun: Boolean,
      ignoreLock: Boolean
  )(using db: Database[IO], verbosity: Verbosity): IO[Option[GncLock]] =
    tableExists.flatMap:
      // A book GnuCash has never opened with a SQL backend new enough to
      // create the table. There is then nobody to conflict with by this
      // convention, and adding the table would be editing the schema of
      // someone else's book on their behalf — so the run proceeds, and says
      // that it is proceeding unprotected.
      case false =>
        warn(
          s"$input has no gnclock table, so nothing here can say whether GnuCash has it open, and this run can't announce itself. Close GnuCash before running."
        ).as(None)
      // A dry run reads the lock and stops there. Taking one would be a write:
      // it would need the write lock that withoutCommitting's deferred begin
      // exists to avoid, and withBook would then report the run as having
      // changed rows. Reading it is still worth doing — a plan computed
      // against a book somebody else has open is a plan against a book that
      // may not look like that by the time it is acted on.
      case true if dryRun =>
        refuseIfHeld(input, ignoreLock).as(None)
      case true =>
        ours.flatMap: lock =>
          db
            // Looked at and taken in one begin immediate, so two writers
            // starting together can't both read an empty table and both
            // conclude the book is theirs.
            .transact:
              refuseIfHeld(input, ignoreLock) *>
                lock.insert *>
                verbose(s"Locked $input as ${lock.holder}.")
            .as(Some(lock))

  // No code elsewhere introspects the schema, because every other table this
  // CLI touches is one GnuCash cannot have a book without. gnclock can be
  // absent, and querying an absent table is an error rather than an empty
  // result, so it has to be asked for first.
  private def tableExists(using db: Database[IO]): IO[Boolean] =
    db
      .unique:
        sql"""
          select count(*) from sqlite_master
          where type = 'table' and name = 'gnclock'
        """.query(integer)
      .map(_ > 0)

  // Only the user can tell a live GnuCash from one that died holding the book,
  // so --ignore-lock is the whole of the override: naming the holder is what
  // lets them make that call.
  private def refuseIfHeld(
      input: fs2.io.file.Path,
      ignoreLock: Boolean
  )(using db: Database[IO], verbosity: Verbosity): IO[Unit] =
    holders.flatMap:
      case Nil     => IO.unit
      case holders =>
        val who = holders.map(_.holder).mkString(", ")
        if ignoreLock then
          warn(
            s"$input is locked by $who; --ignore-lock was passed, so continuing anyway."
          )
        else
          IO.raiseError:
            BookInUse(
              s"$input is open in GnuCash on $who. Close it and run again, or pass --ignore-lock if that process is gone — a GnuCash that crashed leaves its row behind."
            )

  private def holders(using db: Database[IO]): IO[List[GncLock]] =
    db.execute:
      sql"select Hostname, PID from gnclock".query:
        (text *: integer *: nil).pmap[GncLock]

  // The row this run takes the book with. getLocalHost is a name lookup, so on
  // a machine with unhelpful DNS it can be slow or fail, and the value only has
  // to be readable by a person and matched by our own delete — so a hostname we
  // can't determine degrades rather than failing the command. GnuCash asks
  // g_get_host_name() for its own, which needn't agree with this and doesn't
  // have to: nothing compares the two.
  private def ours: IO[GncLock] =
    IO
      .delay(java.net.InetAddress.getLocalHost.getHostName)
      .handleError(_ => "unknown")
      .map(GncLock(_, Host.pid))

/** sqlite> .schema gnclock CREATE TABLE gnclock ( Hostname varchar(255), PID
  * int );
  */
// Both columns are nullable there, but GnuCash writes both and so do we, so
// they are modelled as required: a NULL fails the decode, which refuses the run
// rather than reading a lock it can't understand as an absent one.
final case class GncLock(hostname: String, pid: Long):

  def holder: String = s"$hostname (PID $pid)"

  def insert(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        insert into gnclock (Hostname, PID)
        values ($text, $integer)
      """.command,
      args = (hostname, pid)
    )

  def delete(using db: Database[IO]): IO[Unit] =
    db.execute(
      query = sql"""
        delete from gnclock
        where Hostname = $text
          and PID = $integer
      """.command,
      args = (hostname, pid)
    )

// Its own type because withBook has to tell it from every other failure: those
// are runs that started and went wrong, and want the copy of the book taken
// just before them kept as a backup. This one is a run that was never allowed
// to start, and would otherwise leave behind a backup of a book nothing
// touched.
final class BookInUse(message: String) extends Error(message)
