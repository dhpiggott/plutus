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
    def column(i: Int): Any | Null

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
      if rc != SQLITE_OK() then
        if !db.equals(MemorySegment.NULL) then sqlite3_close(db): Unit
        throw RuntimeException(errstr(rc))
      ConnectionImpl(db)
    finally arena.close()

  // SQLITE_TRANSIENT is `(sqlite3_destructor_type)-1`. Passed as the destructor
  // for sqlite3_bind_text/blob it tells sqlite to copy the buffer, so we can
  // release the bind allocations as soon as the call returns.
  private val SqliteTransient: MemorySegment = MemorySegment.ofAddress(-1L)

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
        val rc = sqlite3_prepare_v2(
          db = db,
          zSql = sqlSeg,
          nByte = sqlBytes.length,
          ppStmt = stmtPtr,
          pzTail = MemorySegment.NULL
        )
        if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))
        StatementImpl(db, stmtPtr.get(ADDRESS, 0))
      finally arena.close()

    def close(): Unit =
      val rc = sqlite3_close(db)
      if rc != SQLITE_OK() then throw RuntimeException(errstr(rc))

  private final class StatementImpl(db: MemorySegment, stmt: MemorySegment)
      extends Statement:

    def bindNull(i: Int): Unit =
      val rc = sqlite3_bind_null(stmt, i)
      if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))

    def bindLong(i: Int, value: Long): Unit =
      val rc = sqlite3_bind_int64(stmt, i, value)
      if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))

    def bindDouble(i: Int, value: Double): Unit =
      val rc = sqlite3_bind_double(stmt, i, value)
      if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))

    def bindText(i: Int, value: String): Unit =
      val arena = Arena.ofConfined()
      try
        val bytes = value.getBytes(StandardCharsets.UTF_8)
        val seg = arena.allocate(bytes.length.toLong.max(1L))
        MemorySegment.copy(bytes, 0, seg, JAVA_BYTE, 0L, bytes.length)
        val rc = sqlite3_bind_text(stmt, i, seg, bytes.length, SqliteTransient)
        if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))
      finally arena.close()

    def bindBlob(i: Int, value: Array[Byte]): Unit =
      val arena = Arena.ofConfined()
      try
        val seg = arena.allocate(value.length.toLong.max(1L))
        MemorySegment.copy(value, 0, seg, JAVA_BYTE, 0L, value.length)
        val rc = sqlite3_bind_blob(stmt, i, seg, value.length, SqliteTransient)
        if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))
      finally arena.close()

    def step(): Boolean =
      sqlite3_step(stmt) match
        case rc if rc == SQLITE_ROW()  => true
        case rc if rc == SQLITE_DONE() => false
        case _                         => throw RuntimeException(errmsg(db))

    def reset(): Unit =
      val rc = sqlite3_reset(stmt)
      if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))

    def columnCount: Int = sqlite3_column_count(stmt)

    def column(i: Int): Any | Null =
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
      val rc = sqlite3_finalize(stmt)
      if rc != SQLITE_OK() then throw RuntimeException(errmsg(db))
