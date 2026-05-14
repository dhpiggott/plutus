package plutus

import cats.effect.*
import smithy4s.*
import smithy4s.json.*

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets

object StateStore:

  def make(using verbosity: Verbosity): StateStore[IO] =
    new StateStoreImpl()

final class StateStoreImpl(using verbosity: Verbosity) extends StateStore[IO]:

  import Keychain.*

  override def loadState(): IO[LoadStateOutput] =
    for
      errorOrMaybeJsonBytes <- IO.blocking:
        val arena = Arena.ofConfined()
        try
          val query = makeDict(
            arena,
            kSecClass -> kSecClassGenericPassword,
            kSecAttrAccount -> secItemName,
            kSecReturnData -> kCFBooleanTrue
          )
          val resultPtr = arena.allocate(ADDRESS)
          val status = SecItemCopyMatching
            .invokeWithArguments(query, resultPtr)
            .asInstanceOf[Integer]
            .intValue
          CFRelease.invokeWithArguments(query)
          status match
            case `errSecItemNotFound` =>
              Right:
                None

            case `errSecSuccess` =>
              val data = resultPtr.get(ADDRESS, 0)
              val length = CFDataGetLength
                .invokeWithArguments(data)
                .asInstanceOf[java.lang.Long]
                .longValue
              val bytesPtr = CFDataGetBytePtr
                .invokeWithArguments(data)
                .asInstanceOf[MemorySegment]
              val bytes = bytesPtr.reinterpret(length).toArray(JAVA_BYTE)
              CFRelease.invokeWithArguments(data)
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
    yield LoadStateOutput:
      maybeState

  override def saveState(
      state: State,
      mode: SaveStateMode
  ): IO[Unit] = for
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
          kSecClass -> kSecClassGenericPassword,
          kSecAttrAccount -> secItemName,
          kSecValueData -> data
        )
        val status = mode match
          case SaveStateMode.CREATE =>
            SecItemAdd
              .invokeWithArguments(attributes, MemorySegment.NULL)
              .asInstanceOf[Integer]
              .intValue

          case SaveStateMode.UPDATE =>
            val query = makeDict(
              arena,
              kSecClass -> kSecClassGenericPassword,
              kSecAttrAccount -> secItemName
            )
            val status = SecItemUpdate
              .invokeWithArguments(query, attributes)
              .asInstanceOf[Integer]
              .intValue
            CFRelease.invokeWithArguments(query)
            status
        CFRelease.invokeWithArguments(attributes)
        CFRelease.invokeWithArguments(data)
        status
      finally arena.close()
    _ <- (IO.unlessA:
      status == errSecSuccess
    ):
      IO.raiseError:
        Error:
          s"Save state failed with status $status."
    _ <- info:
      "Saved state to Keychain."
  yield ()

  // Process-lifetime CFString — held forever so every query/attributes dict
  // can reference it without per-call retain/release.
  private val secItemName: MemorySegment =
    val arena = Arena.ofConfined()
    try makeCFString(arena, "plutus")
    finally arena.close()

  private def makeCFString(arena: Arena, s: String): MemorySegment =
    val bytes = s.getBytes(StandardCharsets.UTF_8)
    val buf = arena.allocate(bytes.length)
    MemorySegment.copy(bytes, 0, buf, JAVA_BYTE, 0L, bytes.length)
    CFStringCreateWithBytes
      .invokeWithArguments(
        MemorySegment.NULL,
        buf,
        java.lang.Long.valueOf(bytes.length),
        java.lang.Integer.valueOf(kCFStringEncodingUTF8),
        java.lang.Byte.valueOf(0.toByte)
      )
      .asInstanceOf[MemorySegment]

  private def makeData(arena: Arena, bytes: Array[Byte]): MemorySegment =
    val buf = arena.allocate(bytes.length.max(1).toLong)
    MemorySegment.copy(bytes, 0, buf, JAVA_BYTE, 0L, bytes.length)
    CFDataCreate
      .invokeWithArguments(
        MemorySegment.NULL,
        buf,
        java.lang.Long.valueOf(bytes.length)
      )
      .asInstanceOf[MemorySegment]

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
    CFDictionaryCreate
      .invokeWithArguments(
        MemorySegment.NULL,
        keys,
        values,
        java.lang.Long.valueOf(entries.length),
        MemorySegment.NULL,
        MemorySegment.NULL
      )
      .asInstanceOf[MemorySegment]

private object Keychain:

  private val linker = Linker.nativeLinker()

  private val coreFoundation = SymbolLookup.libraryLookup(
    "/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation",
    Arena.global
  )
  private val security = SymbolLookup.libraryLookup(
    "/System/Library/Frameworks/Security.framework/Security",
    Arena.global
  )

  private def downcall(
      lookup: SymbolLookup,
      name: String,
      descriptor: FunctionDescriptor
  ): MethodHandle =
    linker.downcallHandle(lookupOrThrow(lookup, name), descriptor)

  // `extern const CFStringRef`/`extern const CFBooleanRef` globals: the symbol
  // address points at a pointer-sized cell holding the actual CFTypeRef value,
  // so dereference once to get the value the macOS APIs expect at call sites.
  private def globalCFTypeRef(
      lookup: SymbolLookup,
      name: String
  ): MemorySegment =
    lookupOrThrow(lookup, name).reinterpret(ADDRESS.byteSize).get(ADDRESS, 0)

  private def lookupOrThrow(
      lookup: SymbolLookup,
      name: String
  ): MemorySegment =
    lookup
      .find(name)
      .orElseThrow(() => UnsatisfiedLinkError(s"Symbol $name not found"))

  val CFStringCreateWithBytes: MethodHandle = downcall(
    coreFoundation,
    "CFStringCreateWithBytes",
    FunctionDescriptor.of(
      ADDRESS,
      ADDRESS,
      ADDRESS,
      JAVA_LONG,
      JAVA_INT,
      JAVA_BYTE
    )
  )

  val CFDataCreate: MethodHandle = downcall(
    coreFoundation,
    "CFDataCreate",
    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG)
  )

  val CFDataGetLength: MethodHandle = downcall(
    coreFoundation,
    "CFDataGetLength",
    FunctionDescriptor.of(JAVA_LONG, ADDRESS)
  )

  val CFDataGetBytePtr: MethodHandle = downcall(
    coreFoundation,
    "CFDataGetBytePtr",
    FunctionDescriptor.of(ADDRESS, ADDRESS)
  )

  val CFDictionaryCreate: MethodHandle = downcall(
    coreFoundation,
    "CFDictionaryCreate",
    FunctionDescriptor.of(
      ADDRESS,
      ADDRESS,
      ADDRESS,
      ADDRESS,
      JAVA_LONG,
      ADDRESS,
      ADDRESS
    )
  )

  val CFRelease: MethodHandle = downcall(
    coreFoundation,
    "CFRelease",
    FunctionDescriptor.ofVoid(ADDRESS)
  )

  val SecItemCopyMatching: MethodHandle = downcall(
    security,
    "SecItemCopyMatching",
    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
  )

  val SecItemAdd: MethodHandle = downcall(
    security,
    "SecItemAdd",
    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
  )

  val SecItemUpdate: MethodHandle = downcall(
    security,
    "SecItemUpdate",
    FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
  )

  val kSecClass: MemorySegment = globalCFTypeRef(security, "kSecClass")
  val kSecClassGenericPassword: MemorySegment =
    globalCFTypeRef(security, "kSecClassGenericPassword")
  val kSecAttrAccount: MemorySegment =
    globalCFTypeRef(security, "kSecAttrAccount")
  val kSecValueData: MemorySegment = globalCFTypeRef(security, "kSecValueData")
  val kSecReturnData: MemorySegment =
    globalCFTypeRef(security, "kSecReturnData")
  val kCFBooleanTrue: MemorySegment =
    globalCFTypeRef(coreFoundation, "kCFBooleanTrue")

  // SecBase.h
  val errSecSuccess: Int = 0
  val errSecItemNotFound: Int = -25300

  // CFString.h
  val kCFStringEncodingUTF8: Int = 0x08000100
