package porcupine

import libsqlite.libsqlite_h_1.*

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.nio.charset.StandardCharsets

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
    val arena = Arena.ofConfined()
    try
      val fnBytes = filename.getBytes(StandardCharsets.UTF_8)
      val fnSeg = arena.allocate(fnBytes.length.toLong + 1L)
      MemorySegment.copy(fnBytes, 0, fnSeg, JAVA_BYTE, 0L, fnBytes.length)
      val dbPtr = arena.allocate(ADDRESS)
      val flags =
        SQLITE_OPEN_READWRITE() | SQLITE_OPEN_CREATE() | SQLITE_OPEN_NOMUTEX()
      val rc = sqlite3_open_v2(
        filename = fnSeg,
        ppDb = dbPtr,
        flags = flags,
        zVfs = MemorySegment.NULL
      )
      val db = dbPtr.get(ADDRESS, 0)
      if rc != SQLITE_OK() && !db.equals(MemorySegment.NULL) then
        sqlite3_close(db): Unit
      guard(rc)
      ConnectionImpl(db)
    finally arena.close()

  private def guard(rc: Int): Unit =
    if rc != SQLITE_OK() then throw RuntimeException(errstr(rc))

  private def guard(db: MemorySegment)(rc: Int): Unit =
    if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))

  private def errstr(rc: Int): String =
    val msg = sqlite3_errstr(rc)
    if msg.equals(MemorySegment.NULL) then s"sqlite3 error $rc"
    else msg.reinterpret(Long.MaxValue).getString(0L)

  private def errmsg(db: MemorySegment): String =
    val msg = sqlite3_errmsg(db)
    if msg.equals(MemorySegment.NULL) then "sqlite3 error"
    else msg.reinterpret(Long.MaxValue).getString(0L)

  private final class ConnectionImpl(db: MemorySegment) extends Connection:
    def prepare(sql: String): Statement =
      val arena = Arena.ofConfined()
      try
        val sqlBytes = sql.getBytes(StandardCharsets.UTF_8)
        val sqlSeg = arena.allocate(sqlBytes.length.toLong.max(1L))
        MemorySegment.copy(sqlBytes, 0, sqlSeg, JAVA_BYTE, 0L, sqlBytes.length)
        val stmtPtr = arena.allocate(ADDRESS)
        guard(db):
          sqlite3_prepare_v2(
            db = db,
            zSql = sqlSeg,
            nByte = sqlBytes.length,
            ppStmt = stmtPtr,
            pzTail = MemorySegment.NULL
          )
        StatementImpl(db, stmtPtr.get(ADDRESS, 0))
      finally arena.close()

    def close(): Unit = guard(sqlite3_close(db))

  private final class StatementImpl(db: MemorySegment, stmt: MemorySegment)
      extends Statement:
    // sqlite3_bind_text/blob receive raw pointers into these segments with the
    // SQLITE_STATIC destructor (MemorySegment.NULL below). The segments must
    // outlive the statement's last step(); reset() closes the arena. Heap
    // arrays can't take their place — JVM GC moves them, so any pointer
    // sqlite saved would dangle as soon as the bind call returned.
    private var bindArena: Arena = Arena.ofConfined()

    def bindNull(i: Int): Unit = guard(db)(sqlite3_bind_null(stmt, i))

    def bindLong(i: Int, value: Long): Unit =
      guard(db)(sqlite3_bind_int64(stmt, i, value))

    def bindDouble(i: Int, value: Double): Unit =
      guard(db)(sqlite3_bind_double(stmt, i, value))

    def bindText(i: Int, value: String): Unit =
      val bytes = value.getBytes(StandardCharsets.UTF_8)
      val seg = bindArena.allocate(bytes.length.toLong.max(1L))
      MemorySegment.copy(bytes, 0, seg, JAVA_BYTE, 0L, bytes.length)
      guard(db)(sqlite3_bind_text(stmt, i, seg, bytes.length, MemorySegment.NULL))

    def bindBlob(i: Int, value: Array[Byte]): Unit =
      val seg = bindArena.allocate(value.length.toLong.max(1L))
      MemorySegment.copy(value, 0, seg, JAVA_BYTE, 0L, value.length)
      guard(db)(sqlite3_bind_blob(stmt, i, seg, value.length, MemorySegment.NULL))

    def step(): Boolean =
      sqlite3_step(stmt) match
        case rc if rc == SQLITE_ROW()  => true
        case rc if rc == SQLITE_DONE() => false
        case other                     =>
          guard(db)(other)
          false

    def reset(): Unit =
      guard(db)(sqlite3_reset(stmt))
      bindArena.close()
      bindArena = Arena.ofConfined()

    def columnCount: Int = sqlite3_column_count(stmt)

    def column(i: Int): Long | Double | String | Array[Byte] | Null =
      sqlite3_column_type(stmt, i) match
        case t if t == SQLITE_NULL()    => null
        case t if t == SQLITE_INTEGER() => sqlite3_column_int64(stmt, i)
        case t if t == SQLITE_FLOAT()   => sqlite3_column_double(stmt, i)
        case t if t == SQLITE_TEXT()    =>
          val len = sqlite3_column_bytes(stmt, i)
          if len == 0 then ""
          else
            val ptr = sqlite3_column_text(stmt, i).reinterpret(len.toLong)
            new String(ptr.toArray(JAVA_BYTE), StandardCharsets.UTF_8)
        case t if t == SQLITE_BLOB() =>
          val len = sqlite3_column_bytes(stmt, i)
          if len == 0 then new Array[Byte](0)
          else
            sqlite3_column_blob(stmt, i).reinterpret(len.toLong).toArray(JAVA_BYTE)

    def close(): Unit =
      guard(db)(sqlite3_finalize(stmt))
      bindArena.close()
