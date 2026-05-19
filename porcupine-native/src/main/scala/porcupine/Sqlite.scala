package porcupine

import libsqlite.all.*

import scala.scalanative.libc.string.memcpy
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object Sqlite:

  trait Connection extends AutoCloseable:
    def prepare(sql: String): Statement

  trait Statement extends AutoCloseable:
    def bindNull(i: Int): Unit
    def bindLong(i: Int, value: Long): Unit
    def bindDouble(i: Int, value: Double): Unit
    def bindText(i: Int, value: String): Unit
    def bindBlob(i: Int, value: Array[Byte]): Unit
    def step(): Boolean
    def reset(): Unit
    def columnCount: Int
    def column(i: Int): Long | Double | String | Array[Byte] | Null

  def open(filename: String): Connection =
    val fn = (filename + 0.toChar).getBytes
    val dbPtr = stackalloc[Ptr[sqlite3]]()
    val flags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_NOMUTEX
    try guard(sqlite3_open_v2(fn.at(0), dbPtr, flags, null))
    catch
      case t: Throwable =>
        if !dbPtr ne null then sqlite3_close(!dbPtr): Unit
        throw t
    ConnectionImpl(!dbPtr)

  inline private def guard(rc: CInt): Unit =
    if rc != SQLITE_OK then
      throw RuntimeException(fromCString(sqlite3_errstr(rc)))

  inline private def guard(db: Ptr[sqlite3])(rc: CInt): Unit =
    if rc != SQLITE_OK then
      throw RuntimeException(fromCString(sqlite3_errmsg(db)))

  private final class ConnectionImpl(db: Ptr[sqlite3]) extends Connection:
    def prepare(sql: String): Statement =
      val sqlBytes = sql.getBytes
      val stmtPtr = stackalloc[Ptr[sqlite3_stmt]]()
      guard(db):
        sqlite3_prepare_v2(db, sqlBytes.at(0), sqlBytes.length, stmtPtr, null)
      StatementImpl(db, !stmtPtr)

    def close(): Unit = guard(sqlite3_close(db))

  private final class StatementImpl(db: Ptr[sqlite3], stmt: Ptr[sqlite3_stmt])
      extends Statement:
    // sqlite3_bind_text/blob receive raw pointers into these arrays with the
    // SQLITE_STATIC destructor (passed as `null` below). The arrays must
    // outlive the statement's last `step()`; `reset()` clears them.
    private var bindRefs: List[Array[Byte]] = Nil

    def bindNull(i: Int): Unit = guard(db)(sqlite3_bind_null(stmt, i))

    def bindLong(i: Int, value: Long): Unit =
      guard(db)(sqlite3_bind_int64(stmt, i, sqlite_int64(value)))

    def bindDouble(i: Int, value: Double): Unit =
      guard(db)(sqlite3_bind_double(stmt, i, value))

    def bindText(i: Int, value: String): Unit =
      val b = value.getBytes
      bindRefs = b :: bindRefs
      guard(db):
        sqlite3_bind_text(stmt, i, b.at(0), b.length, null)

    def bindBlob(i: Int, value: Array[Byte]): Unit =
      bindRefs = value :: bindRefs
      guard(db):
        sqlite3_bind_blob64(
          stmt,
          i,
          value.at(0),
          sqlite_uint64(value.length.toULong),
          null
        )

    def step(): Boolean =
      sqlite3_step(stmt) match
        case SQLITE_ROW  => true
        case SQLITE_DONE => false
        case other       =>
          guard(db)(other)
          false

    def reset(): Unit =
      bindRefs = Nil
      guard(db)(sqlite3_reset(stmt))

    def columnCount: Int = sqlite3_column_count(stmt)

    def column(i: Int): Long | Double | String | Array[Byte] | Null =
      sqlite3_column_type(stmt, i) match
        case SQLITE_NULL    => null
        case SQLITE_INTEGER => sqlite3_column_int64(stmt, i).value
        case SQLITE_FLOAT   => sqlite3_column_double(stmt, i)
        case SQLITE_TEXT    =>
          fromCString:
            sqlite3_column_text(stmt, i).asInstanceOf[CString]
        case SQLITE_BLOB =>
          val len = sqlite3_column_bytes(stmt, i)
          val arr = new Array[Byte](len)
          if len > 0 then
            memcpy(arr.at(0), sqlite3_column_blob(stmt, i), len.toCSize): Unit
          arr

    def close(): Unit = guard(db)(sqlite3_finalize(stmt))
