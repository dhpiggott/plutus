package plutus

import cats.effect.*
import macos.Globals.*
import macos.all.*

import scala.scalanative.libc.string.memcpy
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object Keychain:

  def load(account: String): IO[Option[Array[Byte]]] = IO.blocking:
    Zone:
      val accountStr = makeCFString(account)
      val query = makeDict(
        kSecClass.value.asCFTypeRef -> kSecClassGenericPassword.value.asCFTypeRef,
        kSecAttrAccount.value.asCFTypeRef -> accountStr.value.asCFTypeRef,
        kSecReturnData.value.asCFTypeRef -> kCFBooleanTrue.value.asCFTypeRef
      )
      val resultPtr = stackalloc[CFTypeRef]()
      val status = SecItemCopyMatching(
        query = query,
        result = resultPtr
      )
      CFRelease(query.value.asCFTypeRef)
      CFRelease(accountStr.value.asCFTypeRef)
      status.value match
        case `errSecItemNotFound` =>
          None

        case `errSecSuccess` =>
          val data = (!resultPtr).asInstanceOf[CFDataRef]
          val length = CFDataGetLength(data).value.toInt
          val bytesPtr = CFDataGetBytePtr(data)
          val bytes = new Array[Byte](length)
          var i = 0
          while i < length do
            bytes(i) = bytesPtr(i).value.toByte
            i += 1
          CFRelease(data.value.asCFTypeRef)
          Some:
            bytes

        case other =>
          throw Error:
            s"Keychain SecItemCopyMatching failed with status $other."

  def save(account: String, bytes: Array[Byte]): IO[Unit] = for
    osStatus <- IO.blocking:
      Zone:
        val accountStr = makeCFString(account)
        val data = makeData(bytes)
        val attributes = makeDict(
          kSecClass.value.asCFTypeRef -> kSecClassGenericPassword.value.asCFTypeRef,
          kSecAttrAccount.value.asCFTypeRef -> accountStr.value.asCFTypeRef,
          kSecValueData.value.asCFTypeRef -> data.value.asCFTypeRef
        )
        val query = makeDict(
          kSecClass.value.asCFTypeRef -> kSecClassGenericPassword.value.asCFTypeRef,
          kSecAttrAccount.value.asCFTypeRef -> accountStr.value.asCFTypeRef
        )
        // Try update first; on errSecItemNotFound (first run, nothing to
        // update) fall back to add.
        val updateStatus = SecItemUpdate(
          query = query,
          attributesToUpdate = attributes
        )
        val status =
          if updateStatus.value == errSecItemNotFound then
            SecItemAdd(
              attributes = attributes,
              result = null
            )
          else updateStatus
        CFRelease(query.value.asCFTypeRef)
        CFRelease(attributes.value.asCFTypeRef)
        CFRelease(data.value.asCFTypeRef)
        CFRelease(accountStr.value.asCFTypeRef)
        status
    _ <- (IO.raiseUnless:
      osStatus.value == errSecSuccess
    ):
      Error:
        s"Keychain SecItem update/add failed with status ${osStatus.value}."
  yield ()

  private def makeCFString(s: String)(using Zone): CFStringRef =
    val bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    // `.max(1)` so we never call `alloc[UInt8](0)`: zero-length allocations
    // can hand back an implementation-defined pointer, and CFStringCreateWithBytes
    // expects either NULL with numBytes 0 or a real backing pointer. The slack
    // byte is never read: the call uses `bytes.length`.
    val buf = alloc[UInt8](bytes.length.max(1))
    if bytes.nonEmpty then
      memcpy(buf, bytes.at(0), bytes.length.toSize.toCSize): Unit
    CFStringCreateWithBytes(
      alloc = CFAllocatorRef:
        null
      ,
      bytes = buf,
      numBytes = CFIndex:
        bytes.length
      ,
      encoding = UInt32:
        kCFStringEncodingUTF8
      ,
      isExternalRepresentation = macos.aliases.Boolean:
        0.toUByte
    )

  private def makeData(bytes: Array[Byte])(using Zone): CFDataRef =
    // Same `.max(1)` rationale as `makeCFString`.
    val buf = alloc[UInt8](bytes.length.max(1))
    if bytes.nonEmpty then
      memcpy(buf, bytes.at(0), bytes.length.toSize.toCSize): Unit
    CFDataCreate(
      allocator = CFAllocatorRef:
        null
      ,
      bytes = buf,
      length = CFIndex:
        bytes.length
    )

  private def makeDict(
      entries: (CFTypeRef, CFTypeRef)*
  ): CFDictionaryRef =
    val keys = stackalloc[Ptr[Byte]]:
      entries.length.toUInt
    val values = stackalloc[Ptr[Byte]]:
      entries.length.toUInt
    entries.zipWithIndex.foreach: (entry, index) =>
      val (key, value) = entry
      keys.update(index, key.value)
      values.update(index, value.value)
    CFDictionaryCreate(
      allocator = CFAllocatorRef:
        null
      ,
      keys = keys,
      values = values,
      numValues = CFIndex:
        entries.length
      ,
      keyCallBacks = null,
      valueCallBacks = null
    )

  extension (ptr: Ptr[?])
    /** Bridges sn-bindgen's per-CF-type opaque pointers (`CFStringRef =
      * Ptr[__CFString]`, `CFDataRef = Ptr[__CFData]`, …) to CoreFoundation's
      * universal `CFTypeRef = Ptr[Byte]`. That's the type `CFRelease` accepts
      * and what each slot of the heterogeneous keys/values arrays inside
      * `CFDictionaryCreate` carries — sn-bindgen's strict opaque types don't
      * grant any subtype relation between them, so a pointer-cast is the only
      * way across. See
      * https://sn-bindgen.indoorvivants.com/semantics/index.html#structs-are-converted-to-opaque-types.
      */
    private def asCFTypeRef: CFTypeRef = ptr.asInstanceOf[CFTypeRef]
