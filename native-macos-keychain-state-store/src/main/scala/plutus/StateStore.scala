package plutus

import cats.effect.*
import cats.effect.std.*
import smithy.api.TimestampFormat
import smithy4s.*
import smithy4s.json.*

import java.lang.Runtime
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.concurrent.duration.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object StateStore:

  def apply(implicit verbosity: Verbosity): StateStore[IO] =
    new StateStoreImpl()

final class StateStoreImpl(implicit verbosity: Verbosity)
    extends StateStore[IO]:

  override def loadState(): IO[LoadStateOutput] =
    for
      errorOrMaybeDataString <- IO:
        val resultPtr = stackalloc[macos.aliases.CFTypeRef]()
        (macos.functions
          .SecItemCopyMatching(
            query = toCfDictionary(
              macos.Forwarders.SecClass.value.unsafeToPtr -> macos.Forwarders.SecClassGenericPassword.value.unsafeToPtr,
              macos.Forwarders.SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr,
              macos.Forwarders.SecReturnData.value.unsafeToPtr -> macos.Forwarders.CFBooleanTrue.value.unsafeToPtr
            ),
            result = resultPtr
          )
          .value match
          case macos.constants.errSecItemNotFound =>
            Right:
              None

          case macos.constants.errSecSuccess =>
            Right:
              Some:
                fromCString:
                  macos.functions.CFStringGetCStringPtr(
                    theString = macos.functions
                      .CFStringCreateFromExternalRepresentation(
                        alloc = defaultAllocator,
                        data = macos.aliases.CFDataRef:
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
    yield LoadStateOutput:
      maybeState

  override def saveState(
      state: State,
      mode: SaveStateMode
  ): IO[Unit] = for
    attributes <- IO:
      toCfDictionary(
        macos.Forwarders.SecClass.value.unsafeToPtr -> macos.Forwarders.SecClassGenericPassword.value.unsafeToPtr,
        macos.Forwarders.SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr,
        macos.Forwarders.SecValueData.value.unsafeToPtr -> Zone(implicit z =>
          macos.functions
            .CFStringCreateExternalRepresentation(
              alloc = defaultAllocator,
              theString = macos.functions.CFStringCreateWithCString(
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
              lossByte = macos.aliases.UInt8:
                0.toUByte
            )
            .value
            .unsafeToPtr
        )
      )
    osStatus <- mode match
      case SaveStateMode.CREATE =>
        IO:
          macos.functions.SecItemAdd(
            attributes = attributes,
            result = null
          )

      case SaveStateMode.UPDATE =>
        IO:
          macos.functions.SecItemUpdate(
            query = toCfDictionary(
              macos.Forwarders.SecClass.value.unsafeToPtr -> macos.Forwarders.SecClassGenericPassword.value.unsafeToPtr,
              macos.Forwarders.SecAttrAccount.value.unsafeToPtr -> secItemName.value.unsafeToPtr
            ),
            attributesToUpdate = attributes
          )
    _ <- (IO.unlessA:
      osStatus.value == macos.constants.errSecSuccess
    ):
      IO.raiseError:
        Error:
          s"Save state failed with status $osStatus."
    _ <- info:
      "Saved state to Keychain."
  yield ()

  val defaultAllocator: macos.aliases.CFAllocatorRef =
    macos.aliases.CFAllocatorRef:
      null

  val utf8: macos.aliases.CFStringEncoding =
    macos.aliases.UInt32:
      macos.constants.kCFStringEncodingUTF8

  val secItemName: macos.aliases.CFStringRef =
    macos.functions
      .CFStringCreateWithCString(
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
  ): macos.aliases.CFDictionaryRef =
    val keys = stackalloc[Ptr[Byte]]:
      entries.length.toUInt
    val values = stackalloc[Ptr[Byte]]:
      entries.length.toUInt
    entries.zipWithIndex.foreach: (entry, index) =>
      val (key, value) = entry
      keys.update(index.toULong, key)
      values.update(index.toULong, value)
    macos.functions.CFDictionaryCreate(
      allocator = defaultAllocator,
      keys = keys,
      values = values,
      numValues = macos.aliases.CFIndex:
        entries.length
      ,
      keyCallBacks = null,
      valueCallBacks = null
    )
