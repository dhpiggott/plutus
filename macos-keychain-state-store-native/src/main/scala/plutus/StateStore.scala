package plutus

import cats.effect.*
import macos.Forwarders.*
import macos.all.*
import smithy4s.*
import smithy4s.json.*

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

import Keychain.*

def loadState()(using verbosity: Verbosity): IO[Option[State]] =
  for
    errorOrMaybeDataString <- IO:
      val resultPtr = stackalloc[CFTypeRef]()
      (SecItemCopyMatching(
        query = toCfDictionary(
          SecClass.value.unsafeToPtr -> SecClassGenericPassword.value.unsafeToPtr,
          SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr,
          SecReturnData.value.unsafeToPtr -> CFBooleanTrue.value.unsafeToPtr
        ),
        result = resultPtr
      ).value match
        case `errSecItemNotFound` =>
          Right:
            None

        case `errSecSuccess` =>
          Right:
            Some:
              fromCString:
                CFStringGetCStringPtr(
                  theString = CFStringCreateFromExternalRepresentation(
                    alloc = defaultAllocator,
                    data = CFDataRef:
                      (!resultPtr).value.unsafeToPtr
                    ,
                    encoding = utf8
                  ),
                  encoding = utf8
                )

        case other =>
          Left:
            Error:
              s"Load state failed with status $other."
      )
    maybeState <- errorOrMaybeDataString match
      case Right(None) =>
        IO.none

      case Right(Some(dataString)) =>
        IO.fromEither:
          Json.read[State]:
            Blob:
              dataString
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
  osStatus <- IO:
    val attributes = toCfDictionary(
      SecClass.value.unsafeToPtr -> SecClassGenericPassword.value.unsafeToPtr,
      SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr,
      SecValueData.value.unsafeToPtr -> Zone(
        CFStringCreateExternalRepresentation(
          alloc = defaultAllocator,
          theString = CFStringCreateWithCString(
            alloc = defaultAllocator,
            cStr = toCString(
              Json
                .writeBlob:
                  state
                .toUTF8String,
              java.nio.charset.StandardCharsets.UTF_8
            ),
            encoding = utf8
          ),
          encoding = utf8,
          lossByte = UInt8:
            0.toUByte
        ).value.unsafeToPtr
      )
    )
    val query = toCfDictionary(
      SecClass.value.unsafeToPtr -> SecClassGenericPassword.value.unsafeToPtr,
      SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr
    )
    // Try update first; on errSecItemNotFound (first run, nothing to update)
    // fall back to add.
    val updateStatus = SecItemUpdate(
      query = query,
      attributesToUpdate = attributes
    )
    if updateStatus.value == errSecItemNotFound then
      SecItemAdd(
        attributes = attributes,
        result = null
      )
    else updateStatus
  _ <- (IO.unlessA:
    osStatus.value == errSecSuccess
  ):
    IO.raiseError:
      Error:
        s"Save state failed with status $osStatus."
  _ <- info:
    "Saved state to Keychain."
yield ()

private object Keychain:

  val defaultAllocator: CFAllocatorRef =
    CFAllocatorRef:
      null

  val utf8: CFStringEncoding =
    UInt32:
      kCFStringEncodingUTF8

  val secItemName: CFStringRef =
    CFStringCreateWithCString(
      alloc = defaultAllocator,
      cStr = c"plutus",
      encoding = utf8
    )

  extension (ptr: Ptr[?])
    /** This is an unavoidable consequence of the way sn-bindgen generates code
      * and the types the macOS APIs define interact. macOS's CFStringRef for
      * example is generated as:
      *
      * opaque type CFStringRef = Ptr[__CFString]
      *
      * with:
      *
      * opaque type __CFString = CStruct0
      *
      * This is the behaviour documented at
      * https://sn-bindgen.indoorvivants.com/semantics/index.html#structs-are-converted-to-opaque-types.
      *
      * This would be OK if macOS didn't define APIs like CFDictionaryCreate to
      * take heterogenous map inputs like this:
      *
      * keys : Ptr[Ptr[Byte]], values : Ptr[Ptr[Byte]]
      *
      * (Read this as two pointers to arrays, where each array is of
      * heterogeneous type, i.e. some values may be __CFString while others may
      * be __CFBoolean, etc).
      *
      * @param A
      * @return
      */
    def unsafeToPtr[A]: Ptr[A] = ptr.asInstanceOf[Ptr[A]]

  def toCfDictionary(
      entries: (Ptr[Byte], Ptr[Byte])*
  ): CFDictionaryRef =
    val keys = stackalloc[Ptr[Byte]]:
      entries.length.toUInt
    val values = stackalloc[Ptr[Byte]]:
      entries.length.toUInt
    entries.zipWithIndex.foreach: (entry, index) =>
      val (key, value) = entry
      keys.update(index, key)
      values.update(index, value)
    CFDictionaryCreate(
      allocator = defaultAllocator,
      keys = keys,
      values = values,
      numValues = CFIndex:
        entries.length
      ,
      keyCallBacks = null,
      valueCallBacks = null
    )
