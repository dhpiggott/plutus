package plutus

import cats.effect.*
import smithy4s.*
import smithy4s.json.*

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.nio.charset.StandardCharsets

import macos.macos_h.*
import macos.macos_h_1.*

import Keychain.*

def loadState()(using verbosity: Verbosity): IO[Option[State]] =
  for
    errorOrMaybeJsonBytes <- IO.blocking:
      val arena = Arena.ofConfined()
      try
        val query = makeDict(
          arena,
          kSecClass() -> kSecClassGenericPassword(),
          kSecAttrAccount() -> secItemName,
          kSecReturnData() -> kCFBooleanTrue()
        )
        val resultPtr = arena.allocate(ADDRESS)
        val status = SecItemCopyMatching(query, resultPtr)
        CFRelease(query)
        status match
          case `notFound` =>
            Right:
              None

          case `success` =>
            val data = resultPtr.get(ADDRESS, 0)
            val length = CFDataGetLength(data)
            val bytesPtr = CFDataGetBytePtr(data)
            val bytes = bytesPtr.reinterpret(length).toArray(JAVA_BYTE)
            CFRelease(data)
            Right:
              Some:
                bytes

          case other =>
            Left:
              Error:
                s"Load state failed with status $other."
      finally arena.close()
    maybeState <- errorOrMaybeJsonBytes match
      case Right(None) =>
        IO.none

      case Right(Some(bytes)) =>
        IO.fromEither:
          Json.read[State]:
            Blob:
              bytes
        .option

      case Left(error) =>
        IO.raiseError:
          error
    _ <-
      if maybeState.isDefined then
        info:
          "Loaded state from Keychain."
      else
        warn:
          "Couldn't load state from Keychain."
  yield maybeState

def saveState(state: State)(using verbosity: Verbosity): IO[Unit] = for
  status <- IO.blocking:
    val arena = Arena.ofConfined()
    try
      val data = makeData(
        arena,
        Json
          .writeBlob:
            state
          .toArray
      )
      val attributes = makeDict(
        arena,
        kSecClass() -> kSecClassGenericPassword(),
        kSecAttrAccount() -> secItemName,
        kSecValueData() -> data
      )
      val query = makeDict(
        arena,
        kSecClass() -> kSecClassGenericPassword(),
        kSecAttrAccount() -> secItemName
      )
      // Try update first; on errSecItemNotFound (first run, nothing to update)
      // fall back to add.
      val updateStatus = SecItemUpdate(query, attributes)
      val status =
        if updateStatus == notFound then
          SecItemAdd(attributes, MemorySegment.NULL)
        else updateStatus
      CFRelease(query)
      CFRelease(attributes)
      CFRelease(data)
      status
    finally arena.close()
  _ <- (IO.unlessA:
    status == success
  ):
    IO.raiseError:
      Error:
        s"Save state failed with status $status."
  _ <- info:
    "Saved state to Keychain."
yield ()

private object Keychain:

  // Pattern-match guards need a stable val, and jextract emits these as
  // static methods rather than constants. Renamed to avoid clashing with the
  // imported methods.
  val success: Int = errSecSuccess()
  val notFound: Int = errSecItemNotFound()

  def makeCFString(arena: Arena, s: String): MemorySegment =
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    val buf = arena.allocate(bytes.length)
    MemorySegment.copy(bytes, 0, buf, JAVA_BYTE, 0L, bytes.length)
    CFStringCreateWithBytes(
      MemorySegment.NULL,
      buf,
      bytes.length.toLong,
      kCFStringEncodingUTF8(),
      0.toByte
    )

  def makeData(arena: Arena, bytes: Array[Byte]): MemorySegment =
    val buf = arena.allocate(bytes.length.max(1).toLong)
    MemorySegment.copy(bytes, 0, buf, JAVA_BYTE, 0L, bytes.length)
    CFDataCreate(MemorySegment.NULL, buf, bytes.length.toLong)

  def makeDict(
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
      MemorySegment.NULL,
      keys,
      values,
      entries.length.toLong,
      MemorySegment.NULL,
      MemorySegment.NULL
    )

  // Process-lifetime CFString — held forever so every query/attributes dict
  // can reference it without per-call retain/release.
  val secItemName: MemorySegment =
    val arena = Arena.ofConfined()
    try makeCFString(arena, "plutus")
    finally arena.close()
