package plutus

import cats.effect.*

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.nio.charset.StandardCharsets

import macos.macos_h.*
import macos.macos_h_1.*

object Keychain:

  def load(account: String): IO[Option[Array[Byte]]] = IO.blocking:
    val arena = Arena.ofConfined()
    try
      val accountStr = makeCFString(arena, account)
      val query = makeDict(
        arena,
        kSecClass() -> kSecClassGenericPassword(),
        kSecAttrAccount() -> accountStr,
        kSecReturnData() -> kCFBooleanTrue()
      )
      val resultPtr = arena.allocate(ADDRESS)
      val status = SecItemCopyMatching(
        query = query,
        result = resultPtr
      )
      CFRelease(cf = query)
      CFRelease(cf = accountStr)
      status match
        case `notFound` =>
          None

        case `success` =>
          val data = resultPtr.get(ADDRESS, 0)
          val length = CFDataGetLength(theData = data)
          val bytesPtr = CFDataGetBytePtr(theData = data)
          val bytes = bytesPtr.reinterpret(length).toArray(JAVA_BYTE)
          CFRelease(cf = data)
          Some:
            bytes

        case other =>
          throw Error:
            s"Keychain SecItemCopyMatching failed with status $other."
    finally arena.close()

  def save(account: String, bytes: Array[Byte]): IO[Unit] = for
    status <- IO.blocking:
      val arena = Arena.ofConfined()
      try
        val accountStr = makeCFString(arena, account)
        val data = makeData(arena, bytes)
        val attributes = makeDict(
          arena,
          kSecClass() -> kSecClassGenericPassword(),
          kSecAttrAccount() -> accountStr,
          kSecValueData() -> data
        )
        val query = makeDict(
          arena,
          kSecClass() -> kSecClassGenericPassword(),
          kSecAttrAccount() -> accountStr
        )
        // Try update first; on errSecItemNotFound (first run, nothing to
        // update) fall back to add.
        val updateStatus = SecItemUpdate(
          query = query,
          attributesToUpdate = attributes
        )
        val status =
          if updateStatus == notFound then
            SecItemAdd(
              attributes = attributes,
              result = MemorySegment.NULL
            )
          else updateStatus
        CFRelease(cf = query)
        CFRelease(cf = attributes)
        CFRelease(cf = data)
        CFRelease(cf = accountStr)
        status
      finally arena.close()
    _ <- (IO.raiseUnless:
      status == success
    ):
      Error:
        s"Keychain SecItem update/add failed with status $status."
  yield ()

  // Pattern-match guards need a stable val, and jextract emits these as
  // static methods rather than constants. Renamed to avoid clashing with the
  // imported methods.
  private val success: Int = errSecSuccess()
  private val notFound: Int = errSecItemNotFound()

  private def makeCFString(arena: Arena, s: String): MemorySegment =
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    val buf = arena.allocate(bytes.length)
    MemorySegment.copy(bytes, 0, buf, JAVA_BYTE, 0L, bytes.length)
    CFStringCreateWithBytes(
      alloc = MemorySegment.NULL,
      bytes = buf,
      numBytes = bytes.length.toLong,
      encoding = kCFStringEncodingUTF8(),
      isExternalRepresentation = 0.toByte
    )

  private def makeData(arena: Arena, bytes: Array[Byte]): MemorySegment =
    // `.max(1)` avoids `arena.allocate(0L)`, which returns a zero-length
    // MemorySegment whose base address is implementation-defined (commonly
    // NULL). CFDataCreate's contract for an empty payload is NULL + length 0,
    // not implementation-defined-pointer + length 0. The slack byte is never
    // read: the copy and the CFDataCreate length both use `bytes.length`.
    val buf = arena.allocate(bytes.length.max(1).toLong)
    MemorySegment.copy(bytes, 0, buf, JAVA_BYTE, 0L, bytes.length)
    CFDataCreate(
      allocator = MemorySegment.NULL,
      bytes = buf,
      length = bytes.length.toLong
    )

  private def makeDict(
      arena: Arena,
      entries: (MemorySegment, MemorySegment)*
  ): MemorySegment =
    val keys = arena.allocate(ADDRESS, entries.length.toLong)
    val values = arena.allocate(ADDRESS, entries.length.toLong)
    entries.zipWithIndex.foreach: (entry, index) =>
      val (key, value) = entry
      keys.setAtIndex(ADDRESS, index, key)
      values.setAtIndex(ADDRESS, index, value)
    CFDictionaryCreate(
      allocator = MemorySegment.NULL,
      keys = keys,
      values = values,
      numValues = entries.length.toLong,
      keyCallBacks = MemorySegment.NULL,
      valueCallBacks = MemorySegment.NULL
    )
