package secitem

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object aliases:
  import _root_.secitem.aliases.*
  import _root_.secitem.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFDictionary.h
  */
  opaque type CFDictionaryRef = Ptr[__CFDictionary]
  object CFDictionaryRef: 
    given _tag: Tag[CFDictionaryRef] = Tag.Ptr[__CFDictionary](__CFDictionary._tag)
    inline def apply(inline o: Ptr[__CFDictionary]): CFDictionaryRef = o
    extension (v: CFDictionaryRef)
      inline def value: Ptr[__CFDictionary] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFTypeRef = Ptr[Byte]
  object CFTypeRef: 
    given _tag: Tag[CFTypeRef] = Tag.Ptr(Tag.Byte)
    inline def apply(inline o: Ptr[Byte]): CFTypeRef = o
    extension (v: CFTypeRef)
      inline def value: Ptr[Byte] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  type OSStatus = SInt32
  object OSStatus: 
    given _tag: Tag[OSStatus] = SInt32._tag
    inline def apply(inline o: SInt32): OSStatus = o
    extension (v: OSStatus)
      inline def value: SInt32 = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type SInt32 = CInt
  object SInt32: 
    given _tag: Tag[SInt32] = Tag.Int
    inline def apply(inline o: CInt): SInt32 = o
    extension (v: SInt32)
      inline def value: CInt = v

object structs:
  import _root_.secitem.aliases.*
  import _root_.secitem.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFDictionary.h
  */
  opaque type __CFDictionary = CStruct0
  object __CFDictionary:
    given _tag: Tag[__CFDictionary] = Tag.materializeCStruct0Tag


@extern
private[secitem] object extern_functions:
  import _root_.secitem.aliases.*
  import _root_.secitem.structs.*
  /**
   * Add one or more items to a keychain.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemAdd(attributes : CFDictionaryRef, result : Ptr[CFTypeRef]): OSStatus = extern

  /**
   * Returns one or more items which match a search query.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemCopyMatching(query : CFDictionaryRef, result : Ptr[CFTypeRef]): OSStatus = extern

  /**
   * Delete zero or more items which match a search query.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemDelete(query : CFDictionaryRef): OSStatus = extern

  /**
   * Modify zero or more items which match a search query.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemUpdate(query : CFDictionaryRef, attributesToUpdate : CFDictionaryRef): OSStatus = extern


object functions:
  import _root_.secitem.aliases.*
  import _root_.secitem.structs.*
  import extern_functions.*
  export extern_functions.*

object types:
  export _root_.secitem.structs.*
  export _root_.secitem.aliases.*

object all:
  export _root_.secitem.aliases.CFDictionaryRef
  export _root_.secitem.aliases.CFTypeRef
  export _root_.secitem.aliases.OSStatus
  export _root_.secitem.aliases.SInt32
  export _root_.secitem.structs.__CFDictionary
  export _root_.secitem.functions.SecItemAdd
  export _root_.secitem.functions.SecItemCopyMatching
  export _root_.secitem.functions.SecItemDelete
  export _root_.secitem.functions.SecItemUpdate

// TODO:
@extern
object SecItem:
  def plutusKSecClass: cfstring.aliases.CFStringRef = extern
  def plutusKSecClassGenericPassword: cfstring.aliases.CFStringRef = extern
  def plutusKSecAttrAccount: cfstring.aliases.CFStringRef = extern
  def plutusKSecValueData: cfstring.aliases.CFStringRef = extern
  def plutusKSecReturnData: cfstring.aliases.CFStringRef = extern
