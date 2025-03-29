package macos

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object aliases:
  import _root_.macos.aliases.*
  import _root_.macos.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type Boolean = CUnsignedChar
  object Boolean: 
    given _tag: Tag[Boolean] = Tag.UByte
    inline def apply(inline o: CUnsignedChar): Boolean = o
    extension (v: Boolean)
      inline def value: CUnsignedChar = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFAllocatorRef = Ptr[__CFAllocator]
  object CFAllocatorRef: 
    given _tag: Tag[CFAllocatorRef] = Tag.Ptr[__CFAllocator](__CFAllocator._tag)
    inline def apply(inline o: Ptr[__CFAllocator]): CFAllocatorRef = o
    extension (v: CFAllocatorRef)
      inline def value: Ptr[__CFAllocator] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFArray.h
  */
  opaque type CFArrayRef = Ptr[__CFArray]
  object CFArrayRef: 
    given _tag: Tag[CFArrayRef] = Tag.Ptr[__CFArray](__CFArray._tag)
    inline def apply(inline o: Ptr[__CFArray]): CFArrayRef = o
    extension (v: CFArrayRef)
      inline def value: Ptr[__CFArray] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type CFBooleanRef = Ptr[__CFBoolean]
  object CFBooleanRef: 
    given _tag: Tag[CFBooleanRef] = Tag.Ptr[__CFBoolean](__CFBoolean._tag)
    inline def apply(inline o: Ptr[__CFBoolean]): CFBooleanRef = o
    extension (v: CFBooleanRef)
      inline def value: Ptr[__CFBoolean] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFCharacterSet.h
  */
  opaque type CFCharacterSetRef = Ptr[__CFCharacterSet]
  object CFCharacterSetRef: 
    given _tag: Tag[CFCharacterSetRef] = Tag.Ptr[__CFCharacterSet](__CFCharacterSet._tag)
    inline def apply(inline o: Ptr[__CFCharacterSet]): CFCharacterSetRef = o
    extension (v: CFCharacterSetRef)
      inline def value: Ptr[__CFCharacterSet] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  type CFComparisonResult = CFIndex
  object CFComparisonResult: 
    given _tag: Tag[CFComparisonResult] = CFIndex._tag
    inline def apply(inline o: CFIndex): CFComparisonResult = o
    extension (v: CFComparisonResult)
      inline def value: CFIndex = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFData.h
  */
  opaque type CFDataRef = Ptr[__CFData]
  object CFDataRef: 
    given _tag: Tag[CFDataRef] = Tag.Ptr[__CFData](__CFData._tag)
    inline def apply(inline o: Ptr[__CFData]): CFDataRef = o
    extension (v: CFDataRef)
      inline def value: Ptr[__CFData] = v

  /**
   * Type of the callback function used by the apply functions of CFDictionarys.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryApplierFunction = CFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], Unit]
  object CFDictionaryApplierFunction: 
    given _tag: Tag[CFDictionaryApplierFunction] = Tag.materializeCFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], Unit]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFDictionaryApplierFunction = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], Unit]): CFDictionaryApplierFunction = o
    extension (v: CFDictionaryApplierFunction)
      inline def value: CFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], Unit] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryCopyDescriptionCallBack = CFuncPtr1[Ptr[Byte], CFStringRef]
  object CFDictionaryCopyDescriptionCallBack: 
    given _tag: Tag[CFDictionaryCopyDescriptionCallBack] = Tag.materializeCFuncPtr1[Ptr[Byte], CFStringRef]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFDictionaryCopyDescriptionCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr1[Ptr[Byte], CFStringRef]): CFDictionaryCopyDescriptionCallBack = o
    extension (v: CFDictionaryCopyDescriptionCallBack)
      inline def value: CFuncPtr1[Ptr[Byte], CFStringRef] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryEqualCallBack = CFuncPtr2[Ptr[Byte], Ptr[Byte], Boolean]
  object CFDictionaryEqualCallBack: 
    given _tag: Tag[CFDictionaryEqualCallBack] = Tag.materializeCFuncPtr2[Ptr[Byte], Ptr[Byte], Boolean]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFDictionaryEqualCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], Ptr[Byte], Boolean]): CFDictionaryEqualCallBack = o
    extension (v: CFDictionaryEqualCallBack)
      inline def value: CFuncPtr2[Ptr[Byte], Ptr[Byte], Boolean] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryHashCallBack = CFuncPtr1[Ptr[Byte], CFHashCode]
  object CFDictionaryHashCallBack: 
    given _tag: Tag[CFDictionaryHashCallBack] = Tag.materializeCFuncPtr1[Ptr[Byte], CFHashCode]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFDictionaryHashCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr1[Ptr[Byte], CFHashCode]): CFDictionaryHashCallBack = o
    extension (v: CFDictionaryHashCallBack)
      inline def value: CFuncPtr1[Ptr[Byte], CFHashCode] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * This is the type of a reference to immutable CFDictionarys.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryRef = Ptr[__CFDictionary]
  object CFDictionaryRef: 
    given _tag: Tag[CFDictionaryRef] = Tag.Ptr[__CFDictionary](__CFDictionary._tag)
    inline def apply(inline o: Ptr[__CFDictionary]): CFDictionaryRef = o
    extension (v: CFDictionaryRef)
      inline def value: Ptr[__CFDictionary] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryReleaseCallBack = CFuncPtr2[CFAllocatorRef, Ptr[Byte], Unit]
  object CFDictionaryReleaseCallBack: 
    given _tag: Tag[CFDictionaryReleaseCallBack] = Tag.materializeCFuncPtr2[CFAllocatorRef, Ptr[Byte], Unit]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFDictionaryReleaseCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[CFAllocatorRef, Ptr[Byte], Unit]): CFDictionaryReleaseCallBack = o
    extension (v: CFDictionaryReleaseCallBack)
      inline def value: CFuncPtr2[CFAllocatorRef, Ptr[Byte], Unit] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * Structure containing the callbacks for keys of a CFDictionary. version The version number of the structure type being passed in as a parameter to the CFDictionary creation functions. This structure is version 0. retain The callback used to add a retain for the dictionary on keys as they are used to put values into the dictionary. This callback returns the value to use as the key in the dictionary, which is usually the value parameter passed to this callback, but may be a different value if a different value should be used as the key. The dictionary's allocator is passed as the first argument. release The callback used to remove a retain previously added for the dictionary from keys as their values are removed from the dictionary. The dictionary's allocator is passed as the first argument. copyDescription The callback used to create a descriptive string representation of each key in the dictionary. This is used by the CFCopyDescription() function. equal The callback used to compare keys in the dictionary for equality. hash The callback used to compute a hash code for keys as they are used to access, add, or remove values in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryRetainCallBack = CFuncPtr2[CFAllocatorRef, Ptr[Byte], Ptr[Byte]]
  object CFDictionaryRetainCallBack: 
    given _tag: Tag[CFDictionaryRetainCallBack] = Tag.materializeCFuncPtr2[CFAllocatorRef, Ptr[Byte], Ptr[Byte]]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFDictionaryRetainCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[CFAllocatorRef, Ptr[Byte], Ptr[Byte]]): CFDictionaryRetainCallBack = o
    extension (v: CFDictionaryRetainCallBack)
      inline def value: CFuncPtr2[CFAllocatorRef, Ptr[Byte], Ptr[Byte]] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFError.h
  */
  opaque type CFErrorRef = Ptr[__CFError]
  object CFErrorRef: 
    given _tag: Tag[CFErrorRef] = Tag.Ptr[__CFError](__CFError._tag)
    inline def apply(inline o: Ptr[__CFError]): CFErrorRef = o
    extension (v: CFErrorRef)
      inline def value: Ptr[__CFError] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFHashCode = CUnsignedLongInt
  object CFHashCode: 
    given _tag: Tag[CFHashCode] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFHashCode = o
    extension (v: CFHashCode)
      inline def value: CUnsignedLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFIndex = CLongInt
  object CFIndex: 
    given _tag: Tag[CFIndex] = Tag.Long
    inline def apply(inline o: CLongInt): CFIndex = o
    extension (v: CFIndex)
      inline def value: CLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFLocale.h
  */
  opaque type CFLocaleRef = Ptr[__CFLocale]
  object CFLocaleRef: 
    given _tag: Tag[CFLocaleRef] = Tag.Ptr[__CFLocale](__CFLocale._tag)
    inline def apply(inline o: Ptr[__CFLocale]): CFLocaleRef = o
    extension (v: CFLocaleRef)
      inline def value: Ptr[__CFLocale] = v

  /**
   * This is the type of a reference to mutable CFDictionarys.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFMutableDictionaryRef = Ptr[__CFDictionary]
  object CFMutableDictionaryRef: 
    given _tag: Tag[CFMutableDictionaryRef] = Tag.Ptr[__CFDictionary](__CFDictionary._tag)
    inline def apply(inline o: Ptr[__CFDictionary]): CFMutableDictionaryRef = o
    extension (v: CFMutableDictionaryRef)
      inline def value: Ptr[__CFDictionary] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFMutableStringRef = Ptr[__CFString]
  object CFMutableStringRef: 
    given _tag: Tag[CFMutableStringRef] = Tag.Ptr[__CFString](__CFString._tag)
    inline def apply(inline o: Ptr[__CFString]): CFMutableStringRef = o
    extension (v: CFMutableStringRef)
      inline def value: Ptr[__CFString] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type CFNumberRef = Ptr[__CFNumber]
  object CFNumberRef: 
    given _tag: Tag[CFNumberRef] = Tag.Ptr[__CFNumber](__CFNumber._tag)
    inline def apply(inline o: Ptr[__CFNumber]): CFNumberRef = o
    extension (v: CFNumberRef)
      inline def value: Ptr[__CFNumber] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  type CFNumberType = CFIndex
  object CFNumberType: 
    given _tag: Tag[CFNumberType] = CFIndex._tag
    inline def apply(inline o: CFIndex): CFNumberType = o
    extension (v: CFNumberType)
      inline def value: CFIndex = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFOptionFlags = CUnsignedLongInt
  object CFOptionFlags: 
    given _tag: Tag[CFOptionFlags] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFOptionFlags = o
    extension (v: CFOptionFlags)
      inline def value: CUnsignedLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  type CFStringBuiltInEncodings = CFStringEncoding
  object CFStringBuiltInEncodings: 
    given _tag: Tag[CFStringBuiltInEncodings] = CFStringEncoding._tag
    inline def apply(inline o: CFStringEncoding): CFStringBuiltInEncodings = o
    extension (v: CFStringBuiltInEncodings)
      inline def value: CFStringEncoding = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  type CFStringCompareFlags = CFOptionFlags
  object CFStringCompareFlags: 
    given _tag: Tag[CFStringCompareFlags] = CFOptionFlags._tag
    inline def apply(inline o: CFOptionFlags): CFStringCompareFlags = o
    extension (v: CFStringCompareFlags)
      inline def value: CFOptionFlags = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  type CFStringEncoding = UInt32
  object CFStringEncoding: 
    given _tag: Tag[CFStringEncoding] = UInt32._tag
    inline def apply(inline o: UInt32): CFStringEncoding = o
    extension (v: CFStringEncoding)
      inline def value: UInt32 = v

  /**
   * This is the type of Unicode normalization forms as described in Unicode Technical Report #15. To normalize for use with file system calls, use CFStringGetFileSystemRepresentation().
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  type CFStringNormalizationForm = CFIndex
  object CFStringNormalizationForm: 
    given _tag: Tag[CFStringNormalizationForm] = CFIndex._tag
    inline def apply(inline o: CFIndex): CFStringNormalizationForm = o
    extension (v: CFStringNormalizationForm)
      inline def value: CFIndex = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFStringRef = Ptr[__CFString]
  object CFStringRef: 
    given _tag: Tag[CFStringRef] = Tag.Ptr[__CFString](__CFString._tag)
    inline def apply(inline o: Ptr[__CFString]): CFStringRef = o
    extension (v: CFStringRef)
      inline def value: Ptr[__CFString] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFTypeID = CUnsignedLongInt
  object CFTypeID: 
    given _tag: Tag[CFTypeID] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFTypeID = o
    extension (v: CFTypeID)
      inline def value: CUnsignedLongInt = v

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
  opaque type ConstStr255Param = Ptr[CUnsignedChar]
  object ConstStr255Param: 
    given _tag: Tag[ConstStr255Param] = Tag.Ptr[CUnsignedChar](Tag.UByte)
    inline def apply(inline o: Ptr[CUnsignedChar]): ConstStr255Param = o
    extension (v: ConstStr255Param)
      inline def value: Ptr[CUnsignedChar] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type ConstStringPtr = Ptr[CUnsignedChar]
  object ConstStringPtr: 
    given _tag: Tag[ConstStringPtr] = Tag.Ptr[CUnsignedChar](Tag.UByte)
    inline def apply(inline o: Ptr[CUnsignedChar]): ConstStringPtr = o
    extension (v: ConstStringPtr)
      inline def value: Ptr[CUnsignedChar] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  type FourCharCode = UInt32
  object FourCharCode: 
    given _tag: Tag[FourCharCode] = UInt32._tag
    inline def apply(inline o: UInt32): FourCharCode = o
    extension (v: FourCharCode)
      inline def value: UInt32 = v

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
  type OSType = FourCharCode
  object OSType: 
    given _tag: Tag[OSType] = FourCharCode._tag
    inline def apply(inline o: FourCharCode): OSType = o
    extension (v: OSType)
      inline def value: FourCharCode = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type SInt32 = CInt
  object SInt32: 
    given _tag: Tag[SInt32] = Tag.Int
    inline def apply(inline o: CInt): SInt32 = o
    extension (v: SInt32)
      inline def value: CInt = v

  /**
   * Contains information about an access control list (ACL) entry.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecACLRef = Ptr[__SecACL]
  object SecACLRef: 
    given _tag: Tag[SecACLRef] = Tag.Ptr[__SecACL](__SecACL._tag)
    inline def apply(inline o: Ptr[__SecACL]): SecACLRef = o
    extension (v: SecACLRef)
      inline def value: Ptr[__SecACL] = v

  /**
   * CFType representing access control for an item. SecAccessControl.h for details.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecAccessControlRef = Ptr[__SecAccessControl]
  object SecAccessControlRef: 
    given _tag: Tag[SecAccessControlRef] = Tag.Ptr[__SecAccessControl](__SecAccessControl._tag)
    inline def apply(inline o: Ptr[__SecAccessControl]): SecAccessControlRef = o
    extension (v: SecAccessControlRef)
      inline def value: Ptr[__SecAccessControl] = v

  /**
   * Contains information about an access.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecAccessRef = Ptr[__SecAccess]
  object SecAccessRef: 
    given _tag: Tag[SecAccessRef] = Tag.Ptr[__SecAccess](__SecAccess._tag)
    inline def apply(inline o: Ptr[__SecAccess]): SecAccessRef = o
    extension (v: SecAccessRef)
      inline def value: Ptr[__SecAccess] = v

  /**
   * CFType representing a X.509 certificate. See SecCertificate.h for details.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecCertificateRef = Ptr[__SecCertificate]
  object SecCertificateRef: 
    given _tag: Tag[SecCertificateRef] = Tag.Ptr[__SecCertificate](__SecCertificate._tag)
    inline def apply(inline o: Ptr[__SecCertificate]): SecCertificateRef = o
    extension (v: SecCertificateRef)
      inline def value: Ptr[__SecCertificate] = v

  /**
   * CFType representing an identity, which contains a SecKeyRef and an associated SecCertificateRef. See SecIdentity.h for details.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecIdentityRef = Ptr[__SecIdentity]
  object SecIdentityRef: 
    given _tag: Tag[SecIdentityRef] = Tag.Ptr[__SecIdentity](__SecIdentity._tag)
    inline def apply(inline o: Ptr[__SecIdentity]): SecIdentityRef = o
    extension (v: SecIdentityRef)
      inline def value: Ptr[__SecIdentity] = v

  /**
   * CFType representing a cryptographic key. See SecKey.h for details.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeyRef = Ptr[__SecKey]
  object SecKeyRef: 
    given _tag: Tag[SecKeyRef] = Tag.Ptr[__SecKey](__SecKey._tag)
    inline def apply(inline o: Ptr[__SecKey]): SecKeyRef = o
    extension (v: SecKeyRef)
      inline def value: Ptr[__SecKey] = v

  /**
   * Represents a keychain attribute type.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  type SecKeychainAttrType = OSType
  object SecKeychainAttrType: 
    given _tag: Tag[SecKeychainAttrType] = OSType._tag
    inline def apply(inline o: OSType): SecKeychainAttrType = o
    extension (v: SecKeychainAttrType)
      inline def value: OSType = v

  /**
   * Represents a pointer to a keychain attribute structure.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainAttributePtr = Ptr[SecKeychainAttribute]
  object SecKeychainAttributePtr: 
    given _tag: Tag[SecKeychainAttributePtr] = Tag.Ptr[SecKeychainAttribute](SecKeychainAttribute._tag)
    inline def apply(inline o: Ptr[SecKeychainAttribute]): SecKeychainAttributePtr = o
    extension (v: SecKeychainAttributePtr)
      inline def value: Ptr[SecKeychainAttribute] = v

  /**
   * Contains information about a keychain item.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainItemRef = Ptr[__SecKeychainItem]
  object SecKeychainItemRef: 
    given _tag: Tag[SecKeychainItemRef] = Tag.Ptr[__SecKeychainItem](__SecKeychainItem._tag)
    inline def apply(inline o: Ptr[__SecKeychainItem]): SecKeychainItemRef = o
    extension (v: SecKeychainItemRef)
      inline def value: Ptr[__SecKeychainItem] = v

  /**
   * Contains information about a keychain.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainRef = Ptr[__SecKeychain]
  object SecKeychainRef: 
    given _tag: Tag[SecKeychainRef] = Tag.Ptr[__SecKeychain](__SecKeychain._tag)
    inline def apply(inline o: Ptr[__SecKeychain]): SecKeychainRef = o
    extension (v: SecKeychainRef)
      inline def value: Ptr[__SecKeychain] = v

  /**
   * Contains information about a keychain search.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainSearchRef = Ptr[__SecKeychainSearch]
  object SecKeychainSearchRef: 
    given _tag: Tag[SecKeychainSearchRef] = Tag.Ptr[__SecKeychainSearch](__SecKeychainSearch._tag)
    inline def apply(inline o: Ptr[__SecKeychainSearch]): SecKeychainSearchRef = o
    extension (v: SecKeychainSearchRef)
      inline def value: Ptr[__SecKeychainSearch] = v

  /**
   * Represents the status of a keychain.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  type SecKeychainStatus = UInt32
  object SecKeychainStatus: 
    given _tag: Tag[SecKeychainStatus] = UInt32._tag
    inline def apply(inline o: UInt32): SecKeychainStatus = o
    extension (v: SecKeychainStatus)
      inline def value: UInt32 = v

  /**
   * Contains information about a password.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecPasswordRef = Ptr[__SecPassword]
  object SecPasswordRef: 
    given _tag: Tag[SecPasswordRef] = Tag.Ptr[__SecPassword](__SecPassword._tag)
    inline def apply(inline o: Ptr[__SecPassword]): SecPasswordRef = o
    extension (v: SecPasswordRef)
      inline def value: Ptr[__SecPassword] = v

  /**
   * CFType representing a X.509 certificate trust policy. See SecPolicy.h for details.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecPolicyRef = Ptr[__SecPolicy]
  object SecPolicyRef: 
    given _tag: Tag[SecPolicyRef] = Tag.Ptr[__SecPolicy](__SecPolicy._tag)
    inline def apply(inline o: Ptr[__SecPolicy]): SecPolicyRef = o
    extension (v: SecPolicyRef)
      inline def value: Ptr[__SecPolicy] = v

  /**
   * Contains information about a trusted application.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecTrustedApplicationRef = Ptr[__SecTrustedApplication]
  object SecTrustedApplicationRef: 
    given _tag: Tag[SecTrustedApplicationRef] = Tag.Ptr[__SecTrustedApplication](__SecTrustedApplication._tag)
    inline def apply(inline o: Ptr[__SecTrustedApplication]): SecTrustedApplicationRef = o
    extension (v: SecTrustedApplicationRef)
      inline def value: Ptr[__SecTrustedApplication] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type StringPtr = Ptr[CUnsignedChar]
  object StringPtr: 
    given _tag: Tag[StringPtr] = Tag.Ptr[CUnsignedChar](Tag.UByte)
    inline def apply(inline o: Ptr[CUnsignedChar]): StringPtr = o
    extension (v: StringPtr)
      inline def value: Ptr[CUnsignedChar] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type UInt16 = CUnsignedShort
  object UInt16: 
    given _tag: Tag[UInt16] = Tag.UShort
    inline def apply(inline o: CUnsignedShort): UInt16 = o
    extension (v: UInt16)
      inline def value: CUnsignedShort = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type UInt32 = CUnsignedInt
  object UInt32: 
    given _tag: Tag[UInt32] = Tag.UInt
    inline def apply(inline o: CUnsignedInt): UInt32 = o
    extension (v: UInt32)
      inline def value: CUnsignedInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  opaque type UInt8 = CUnsignedChar
  object UInt8: 
    given _tag: Tag[UInt8] = Tag.UByte
    inline def apply(inline o: CUnsignedChar): UInt8 = o
    extension (v: UInt8)
      inline def value: CUnsignedChar = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  type UTF32Char = UInt32
  object UTF32Char: 
    given _tag: Tag[UTF32Char] = UInt32._tag
    inline def apply(inline o: UInt32): UTF32Char = o
    extension (v: UTF32Char)
      inline def value: UInt32 = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
  */
  type UniChar = UInt16
  object UniChar: 
    given _tag: Tag[UniChar] = UInt16._tag
    inline def apply(inline o: UInt16): UniChar = o
    extension (v: UniChar)
      inline def value: UInt16 = v

  type va_list = unsafe.CVarArgList
  object va_list: 
    val _tag: Tag[va_list] = summon[Tag[unsafe.CVarArgList]]
    inline def apply(inline o: unsafe.CVarArgList): va_list = o
    extension (v: va_list)
      inline def value: unsafe.CVarArgList = v

object structs:
  import _root_.macos.aliases.*
  import _root_.macos.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryKeyCallBacks = CStruct6[CFIndex, CFDictionaryRetainCallBack, CFDictionaryReleaseCallBack, CFDictionaryCopyDescriptionCallBack, CFDictionaryEqualCallBack, CFDictionaryHashCallBack]
  object CFDictionaryKeyCallBacks:
    given _tag: Tag[CFDictionaryKeyCallBacks] = Tag.materializeCStruct6Tag[CFIndex, CFDictionaryRetainCallBack, CFDictionaryReleaseCallBack, CFDictionaryCopyDescriptionCallBack, CFDictionaryEqualCallBack, CFDictionaryHashCallBack]
    def apply()(using Zone): Ptr[CFDictionaryKeyCallBacks] = scala.scalanative.unsafe.alloc[CFDictionaryKeyCallBacks](1)
    def apply(version : CFIndex, retain : CFDictionaryRetainCallBack, release : CFDictionaryReleaseCallBack, copyDescription : CFDictionaryCopyDescriptionCallBack, equal : CFDictionaryEqualCallBack, hash : CFDictionaryHashCallBack)(using Zone): Ptr[CFDictionaryKeyCallBacks] = 
      val ____ptr = apply()
      (!____ptr).version = version
      (!____ptr).retain = retain
      (!____ptr).release = release
      (!____ptr).copyDescription = copyDescription
      (!____ptr).equal = equal
      (!____ptr).hash = hash
      ____ptr
    extension (struct: CFDictionaryKeyCallBacks)
      def version : CFIndex = struct._1
      def version_=(value: CFIndex): Unit = !struct.at1 = value
      def retain : CFDictionaryRetainCallBack = struct._2
      def retain_=(value: CFDictionaryRetainCallBack): Unit = !struct.at2 = value
      def release : CFDictionaryReleaseCallBack = struct._3
      def release_=(value: CFDictionaryReleaseCallBack): Unit = !struct.at3 = value
      def copyDescription : CFDictionaryCopyDescriptionCallBack = struct._4
      def copyDescription_=(value: CFDictionaryCopyDescriptionCallBack): Unit = !struct.at4 = value
      def equal : CFDictionaryEqualCallBack = struct._5
      def equal_=(value: CFDictionaryEqualCallBack): Unit = !struct.at5 = value
      def hash : CFDictionaryHashCallBack = struct._6
      def hash_=(value: CFDictionaryHashCallBack): Unit = !struct.at6 = value

  /**
   * Structure containing the callbacks for values of a CFDictionary. version The version number of the structure type being passed in as a parameter to the CFDictionary creation functions. This structure is version 0. retain The callback used to add a retain for the dictionary on values as they are put into the dictionary. This callback returns the value to use as the value in the dictionary, which is usually the value parameter passed to this callback, but may be a different value if a different value should be added to the dictionary. The dictionary's allocator is passed as the first argument. release The callback used to remove a retain previously added for the dictionary from values as they are removed from the dictionary. The dictionary's allocator is passed as the first argument. copyDescription The callback used to create a descriptive string representation of each value in the dictionary. This is used by the CFCopyDescription() function. equal The callback used to compare values in the dictionary for equality in some operations.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryValueCallBacks = CStruct5[CFIndex, CFDictionaryRetainCallBack, CFDictionaryReleaseCallBack, CFDictionaryCopyDescriptionCallBack, CFDictionaryEqualCallBack]
  object CFDictionaryValueCallBacks:
    given _tag: Tag[CFDictionaryValueCallBacks] = Tag.materializeCStruct5Tag[CFIndex, CFDictionaryRetainCallBack, CFDictionaryReleaseCallBack, CFDictionaryCopyDescriptionCallBack, CFDictionaryEqualCallBack]
    def apply()(using Zone): Ptr[CFDictionaryValueCallBacks] = scala.scalanative.unsafe.alloc[CFDictionaryValueCallBacks](1)
    def apply(version : CFIndex, retain : CFDictionaryRetainCallBack, release : CFDictionaryReleaseCallBack, copyDescription : CFDictionaryCopyDescriptionCallBack, equal : CFDictionaryEqualCallBack)(using Zone): Ptr[CFDictionaryValueCallBacks] = 
      val ____ptr = apply()
      (!____ptr).version = version
      (!____ptr).retain = retain
      (!____ptr).release = release
      (!____ptr).copyDescription = copyDescription
      (!____ptr).equal = equal
      ____ptr
    extension (struct: CFDictionaryValueCallBacks)
      def version : CFIndex = struct._1
      def version_=(value: CFIndex): Unit = !struct.at1 = value
      def retain : CFDictionaryRetainCallBack = struct._2
      def retain_=(value: CFDictionaryRetainCallBack): Unit = !struct.at2 = value
      def release : CFDictionaryReleaseCallBack = struct._3
      def release_=(value: CFDictionaryReleaseCallBack): Unit = !struct.at3 = value
      def copyDescription : CFDictionaryCopyDescriptionCallBack = struct._4
      def copyDescription_=(value: CFDictionaryCopyDescriptionCallBack): Unit = !struct.at4 = value
      def equal : CFDictionaryEqualCallBack = struct._5
      def equal_=(value: CFDictionaryEqualCallBack): Unit = !struct.at5 = value

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFRange = CStruct2[CFIndex, CFIndex]
  object CFRange:
    given _tag: Tag[CFRange] = Tag.materializeCStruct2Tag[CFIndex, CFIndex]
    def apply()(using Zone): Ptr[CFRange] = scala.scalanative.unsafe.alloc[CFRange](1)
    def apply(location : CFIndex, length : CFIndex)(using Zone): Ptr[CFRange] = 
      val ____ptr = apply()
      (!____ptr).location = location
      (!____ptr).length = length
      ____ptr
    extension (struct: CFRange)
      def location : CFIndex = struct._1
      def location_=(value: CFIndex): Unit = !struct.at1 = value
      def length : CFIndex = struct._2
      def length_=(value: CFIndex): Unit = !struct.at2 = value

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  opaque type CFStringInlineBuffer = CStruct7[CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]], CFStringRef, Ptr[UniChar], CString, CFRange, CFIndex, CFIndex]
  object CFStringInlineBuffer:
    given _tag: Tag[CFStringInlineBuffer] = Tag.materializeCStruct7Tag[CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]], CFStringRef, Ptr[UniChar], CString, CFRange, CFIndex, CFIndex]
    def apply()(using Zone): Ptr[CFStringInlineBuffer] = scala.scalanative.unsafe.alloc[CFStringInlineBuffer](1)
    def apply(buffer : CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]], theString : CFStringRef, directUniCharBuffer : Ptr[UniChar], directCStringBuffer : CString, rangeToBuffer : CFRange, bufferedRangeStart : CFIndex, bufferedRangeEnd : CFIndex)(using Zone): Ptr[CFStringInlineBuffer] = 
      val ____ptr = apply()
      (!____ptr).buffer = buffer
      (!____ptr).theString = theString
      (!____ptr).directUniCharBuffer = directUniCharBuffer
      (!____ptr).directCStringBuffer = directCStringBuffer
      (!____ptr).rangeToBuffer = rangeToBuffer
      (!____ptr).bufferedRangeStart = bufferedRangeStart
      (!____ptr).bufferedRangeEnd = bufferedRangeEnd
      ____ptr
    extension (struct: CFStringInlineBuffer)
      def buffer : CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]] = struct._1
      def buffer_=(value: CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]]): Unit = !struct.at1 = value
      def theString : CFStringRef = struct._2
      def theString_=(value: CFStringRef): Unit = !struct.at2 = value
      def directUniCharBuffer : Ptr[UniChar] = struct._3
      def directUniCharBuffer_=(value: Ptr[UniChar]): Unit = !struct.at3 = value
      def directCStringBuffer : CString = struct._4
      def directCStringBuffer_=(value: CString): Unit = !struct.at4 = value
      def rangeToBuffer : CFRange = struct._5
      def rangeToBuffer_=(value: CFRange): Unit = !struct.at5 = value
      def bufferedRangeStart : CFIndex = struct._6
      def bufferedRangeStart_=(value: CFIndex): Unit = !struct.at6 = value
      def bufferedRangeEnd : CFIndex = struct._7
      def bufferedRangeEnd_=(value: CFIndex): Unit = !struct.at7 = value

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type OpaqueSecAccessRef = CStruct0
  object OpaqueSecAccessRef:
    given _tag: Tag[OpaqueSecAccessRef] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type OpaqueSecCertificateRef = CStruct0
  object OpaqueSecCertificateRef:
    given _tag: Tag[OpaqueSecCertificateRef] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type OpaqueSecIdentityRef = CStruct0
  object OpaqueSecIdentityRef:
    given _tag: Tag[OpaqueSecIdentityRef] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type OpaqueSecKeyRef = CStruct0
  object OpaqueSecKeyRef:
    given _tag: Tag[OpaqueSecKeyRef] = Tag.materializeCStruct0Tag

  /**
   * Contains keychain attributes. tag A 4-byte attribute tag. length The length of the buffer pointed to by data. data A pointer to the attribute data.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainAttribute = CStruct3[SecKeychainAttrType, UInt32, Ptr[Byte]]
  object SecKeychainAttribute:
    given _tag: Tag[SecKeychainAttribute] = Tag.materializeCStruct3Tag[SecKeychainAttrType, UInt32, Ptr[Byte]]
    def apply()(using Zone): Ptr[SecKeychainAttribute] = scala.scalanative.unsafe.alloc[SecKeychainAttribute](1)
    def apply(tag : SecKeychainAttrType, length : UInt32, data : Ptr[Byte])(using Zone): Ptr[SecKeychainAttribute] = 
      val ____ptr = apply()
      (!____ptr).tag = tag
      (!____ptr).length = length
      (!____ptr).data = data
      ____ptr
    extension (struct: SecKeychainAttribute)
      def tag : SecKeychainAttrType = struct._1
      def tag_=(value: SecKeychainAttrType): Unit = !struct.at1 = value
      def length : UInt32 = struct._2
      def length_=(value: UInt32): Unit = !struct.at2 = value
      def data : Ptr[Byte] = struct._3
      def data_=(value: Ptr[Byte]): Unit = !struct.at3 = value

  /**
   * Represents an attribute. count The number of tag-format pairs in the respective arrays. tag A pointer to the first attribute tag in the array. format A pointer to the first CSSM_DB_ATTRIBUTE_FORMAT in the array.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainAttributeInfo = CStruct3[UInt32, Ptr[UInt32], Ptr[UInt32]]
  object SecKeychainAttributeInfo:
    given _tag: Tag[SecKeychainAttributeInfo] = Tag.materializeCStruct3Tag[UInt32, Ptr[UInt32], Ptr[UInt32]]
    def apply()(using Zone): Ptr[SecKeychainAttributeInfo] = scala.scalanative.unsafe.alloc[SecKeychainAttributeInfo](1)
    def apply(count : UInt32, tag : Ptr[UInt32], format : Ptr[UInt32])(using Zone): Ptr[SecKeychainAttributeInfo] = 
      val ____ptr = apply()
      (!____ptr).count = count
      (!____ptr).tag = tag
      (!____ptr).format = format
      ____ptr
    extension (struct: SecKeychainAttributeInfo)
      def count : UInt32 = struct._1
      def count_=(value: UInt32): Unit = !struct.at1 = value
      def tag : Ptr[UInt32] = struct._2
      def tag_=(value: Ptr[UInt32]): Unit = !struct.at2 = value
      def format : Ptr[UInt32] = struct._3
      def format_=(value: Ptr[UInt32]): Unit = !struct.at3 = value

  /**
   * Represents a list of keychain attributes. count An unsigned 32-bit integer that represents the number of keychain attributes in the array. attr A pointer to the first keychain attribute in the array.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type SecKeychainAttributeList = CStruct2[UInt32, Ptr[SecKeychainAttribute]]
  object SecKeychainAttributeList:
    given _tag: Tag[SecKeychainAttributeList] = Tag.materializeCStruct2Tag[UInt32, Ptr[SecKeychainAttribute]]
    def apply()(using Zone): Ptr[SecKeychainAttributeList] = scala.scalanative.unsafe.alloc[SecKeychainAttributeList](1)
    def apply(count : UInt32, attr : Ptr[SecKeychainAttribute])(using Zone): Ptr[SecKeychainAttributeList] = 
      val ____ptr = apply()
      (!____ptr).count = count
      (!____ptr).attr = attr
      ____ptr
    extension (struct: SecKeychainAttributeList)
      def count : UInt32 = struct._1
      def count_=(value: UInt32): Unit = !struct.at1 = value
      def attr : Ptr[SecKeychainAttribute] = struct._2
      def attr_=(value: Ptr[SecKeychainAttribute]): Unit = !struct.at2 = value

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type __CFAllocator = CStruct0
  object __CFAllocator:
    given _tag: Tag[__CFAllocator] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFArray.h
  */
  opaque type __CFArray = CStruct0
  object __CFArray:
    given _tag: Tag[__CFArray] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type __CFBoolean = CStruct0
  object __CFBoolean:
    given _tag: Tag[__CFBoolean] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFCharacterSet.h
  */
  opaque type __CFCharacterSet = CStruct0
  object __CFCharacterSet:
    given _tag: Tag[__CFCharacterSet] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFData.h
  */
  opaque type __CFData = CStruct0
  object __CFData:
    given _tag: Tag[__CFData] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type __CFDictionary = CStruct0
  object __CFDictionary:
    given _tag: Tag[__CFDictionary] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFError.h
  */
  opaque type __CFError = CStruct0
  object __CFError:
    given _tag: Tag[__CFError] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFLocale.h
  */
  opaque type __CFLocale = CStruct0
  object __CFLocale:
    given _tag: Tag[__CFLocale] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type __CFNumber = CStruct0
  object __CFNumber:
    given _tag: Tag[__CFNumber] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type __CFString = CStruct0
  object __CFString:
    given _tag: Tag[__CFString] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecACL = CStruct0
  object __SecACL:
    given _tag: Tag[__SecACL] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecAccess = CStruct0
  object __SecAccess:
    given _tag: Tag[__SecAccess] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecAccessControl = CStruct0
  object __SecAccessControl:
    given _tag: Tag[__SecAccessControl] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecCertificate = CStruct0
  object __SecCertificate:
    given _tag: Tag[__SecCertificate] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecIdentity = CStruct0
  object __SecIdentity:
    given _tag: Tag[__SecIdentity] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecKey = CStruct0
  object __SecKey:
    given _tag: Tag[__SecKey] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecKeychain = CStruct0
  object __SecKeychain:
    given _tag: Tag[__SecKeychain] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecKeychainItem = CStruct0
  object __SecKeychainItem:
    given _tag: Tag[__SecKeychainItem] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecKeychainSearch = CStruct0
  object __SecKeychainSearch:
    given _tag: Tag[__SecKeychainSearch] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecPassword = CStruct0
  object __SecPassword:
    given _tag: Tag[__SecPassword] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecPolicy = CStruct0
  object __SecPolicy:
    given _tag: Tag[__SecPolicy] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  opaque type __SecTrustedApplication = CStruct0
  object __SecTrustedApplication:
    given _tag: Tag[__SecTrustedApplication] = Tag.materializeCStruct0Tag


@extern
private[macos] object extern_functions:
  import _root_.macos.aliases.*
  import _root_.macos.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFBooleanGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFBooleanGetValue(boolean : CFBooleanRef): Boolean = extern

  /**
   * Adds the key-value pair to the dictionary if no such key already exists.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryAddValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte], value : Ptr[Byte]): Unit = extern

  /**
   * Calls a function once for each value in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryApplyFunction(theDict : CFDictionaryRef, applier : CFDictionaryApplierFunction, context : Ptr[Byte]): Unit = extern

  /**
   * Reports whether or not the key is in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryContainsKey(theDict : CFDictionaryRef, key : Ptr[Byte]): Boolean = extern

  /**
   * Reports whether or not the value is in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryContainsValue(theDict : CFDictionaryRef, value : Ptr[Byte]): Boolean = extern

  /**
   * Creates a new immutable dictionary with the given values.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreate(allocator : CFAllocatorRef, keys : Ptr[Ptr[Byte]], values : Ptr[Ptr[Byte]], numValues : CFIndex, keyCallBacks : Ptr[CFDictionaryKeyCallBacks], valueCallBacks : Ptr[CFDictionaryValueCallBacks]): CFDictionaryRef = extern

  /**
   * Creates a new immutable dictionary with the key-value pairs from the given dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreateCopy(allocator : CFAllocatorRef, theDict : CFDictionaryRef): CFDictionaryRef = extern

  /**
   * Creates a new mutable dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreateMutable(allocator : CFAllocatorRef, capacity : CFIndex, keyCallBacks : Ptr[CFDictionaryKeyCallBacks], valueCallBacks : Ptr[CFDictionaryValueCallBacks]): CFMutableDictionaryRef = extern

  /**
   * Creates a new mutable dictionary with the key-value pairs from the given dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreateMutableCopy(allocator : CFAllocatorRef, capacity : CFIndex, theDict : CFDictionaryRef): CFMutableDictionaryRef = extern

  /**
   * Returns the number of values currently in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetCount(theDict : CFDictionaryRef): CFIndex = extern

  /**
   * Counts the number of times the given key occurs in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetCountOfKey(theDict : CFDictionaryRef, key : Ptr[Byte]): CFIndex = extern

  /**
   * Counts the number of times the given value occurs in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetCountOfValue(theDict : CFDictionaryRef, value : Ptr[Byte]): CFIndex = extern

  /**
   * Fills the two buffers with the keys and values from the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetKeysAndValues(theDict : CFDictionaryRef, keys : Ptr[Ptr[Byte]], values : Ptr[Ptr[Byte]]): Unit = extern

  /**
   * Returns the type identifier of all CFDictionary instances.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetTypeID(): CFTypeID = extern

  /**
   * Retrieves the value associated with the given key.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetValue(theDict : CFDictionaryRef, key : Ptr[Byte]): Ptr[Byte] = extern

  /**
   * Retrieves the value associated with the given key.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetValueIfPresent(theDict : CFDictionaryRef, key : Ptr[Byte], value : Ptr[Ptr[Byte]]): Boolean = extern

  /**
   * Removes all the values from the dictionary, making it empty.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryRemoveAllValues(theDict : CFMutableDictionaryRef): Unit = extern

  /**
   * Removes the value of the key from the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryRemoveValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte]): Unit = extern

  /**
   * Replaces the value of the key in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryReplaceValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte], value : Ptr[Byte]): Unit = extern

  /**
   * Sets the value of the key in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionarySetValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte], value : Ptr[Byte]): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberCompare(number : CFNumberRef, otherNumber : CFNumberRef, context : Ptr[Byte]): CFComparisonResult = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberCreate(allocator : CFAllocatorRef, theType : CFNumberType, valuePtr : Ptr[Byte]): CFNumberRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetByteSize(number : CFNumberRef): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetType(number : CFNumberRef): CFNumberType = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetValue(number : CFNumberRef, theType : CFNumberType, valuePtr : Ptr[Byte]): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberIsFloatType(number : CFNumberRef): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFShow(obj : CFTypeRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFShowStr(str : CFStringRef): Unit = extern

  /**
   * * MutableString functions **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringAppend(theString : CFMutableStringRef, appendedString : CFStringRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringAppendCString(theString : CFMutableStringRef, cStr : CString, encoding : CFStringEncoding): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringAppendCharacters(theString : CFMutableStringRef, chars : Ptr[UniChar], numChars : CFIndex): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringAppendFormat(theString : CFMutableStringRef, formatOptions : CFDictionaryRef, format : CFStringRef, rest: Any*): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringAppendFormatAndArguments(theString : CFMutableStringRef, formatOptions : CFDictionaryRef, format : CFStringRef, arguments : va_list): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringAppendPascalString(theString : CFMutableStringRef, pStr : ConstStr255Param, encoding : CFStringEncoding): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCapitalize(theString : CFMutableStringRef, locale : CFLocaleRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCompare(theString1 : CFStringRef, theString2 : CFStringRef, compareOptions : CFStringCompareFlags): CFComparisonResult = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringConvertEncodingToIANACharSetName(encoding : CFStringEncoding): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringConvertEncodingToNSStringEncoding(encoding : CFStringEncoding): CUnsignedLongInt = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringConvertEncodingToWindowsCodepage(encoding : CFStringEncoding): UInt32 = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringConvertIANACharSetNameToEncoding(theString : CFStringRef): CFStringEncoding = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringConvertNSStringEncodingToEncoding(encoding : CUnsignedLongInt): CFStringEncoding = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringConvertWindowsCodepageToEncoding(codepage : UInt32): CFStringEncoding = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateArrayBySeparatingStrings(alloc : CFAllocatorRef, theString : CFStringRef, separatorString : CFStringRef): CFArrayRef = extern

  /**
   * * Exploding and joining strings with a separator string **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateByCombiningStrings(alloc : CFAllocatorRef, theArray : CFArrayRef, separatorString : CFStringRef): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateCopy(alloc : CFAllocatorRef, theString : CFStringRef): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateExternalRepresentation(alloc : CFAllocatorRef, theString : CFStringRef, encoding : CFStringEncoding, lossByte : UInt8): CFDataRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateFromExternalRepresentation(alloc : CFAllocatorRef, data : CFDataRef, encoding : CFStringEncoding): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateMutable(alloc : CFAllocatorRef, maxLength : CFIndex): CFMutableStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateMutableCopy(alloc : CFAllocatorRef, maxLength : CFIndex, theString : CFStringRef): CFMutableStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateMutableWithExternalCharactersNoCopy(alloc : CFAllocatorRef, chars : Ptr[UniChar], numChars : CFIndex, capacity : CFIndex, externalCharactersAllocator : CFAllocatorRef): CFMutableStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateStringWithValidatedFormat(alloc : CFAllocatorRef, formatOptions : CFDictionaryRef, validFormatSpecifiers : CFStringRef, format : CFStringRef, errorPtr : Ptr[CFErrorRef], rest: Any*): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateStringWithValidatedFormatAndArguments(alloc : CFAllocatorRef, formatOptions : CFDictionaryRef, validFormatSpecifiers : CFStringRef, format : CFStringRef, arguments : va_list, errorPtr : Ptr[CFErrorRef]): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithBytes(alloc : CFAllocatorRef, bytes : Ptr[UInt8], numBytes : CFIndex, encoding : CFStringEncoding, isExternalRepresentation : Boolean): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithBytesNoCopy(alloc : CFAllocatorRef, bytes : Ptr[UInt8], numBytes : CFIndex, encoding : CFStringEncoding, isExternalRepresentation : Boolean, contentsDeallocator : CFAllocatorRef): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithCString(alloc : CFAllocatorRef, cStr : CString, encoding : CFStringEncoding): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithCStringNoCopy(alloc : CFAllocatorRef, cStr : CString, encoding : CFStringEncoding, contentsDeallocator : CFAllocatorRef): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithCharacters(alloc : CFAllocatorRef, chars : Ptr[UniChar], numChars : CFIndex): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithCharactersNoCopy(alloc : CFAllocatorRef, chars : Ptr[UniChar], numChars : CFIndex, contentsDeallocator : CFAllocatorRef): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithFileSystemRepresentation(alloc : CFAllocatorRef, buffer : CString): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithFormat(alloc : CFAllocatorRef, formatOptions : CFDictionaryRef, format : CFStringRef, rest: Any*): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithFormatAndArguments(alloc : CFAllocatorRef, formatOptions : CFDictionaryRef, format : CFStringRef, arguments : va_list): CFStringRef = extern

  /**
   * * Immutable string creation functions **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithPascalString(alloc : CFAllocatorRef, pStr : ConstStr255Param, encoding : CFStringEncoding): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithPascalStringNoCopy(alloc : CFAllocatorRef, pStr : ConstStr255Param, encoding : CFStringEncoding, contentsDeallocator : CFAllocatorRef): CFStringRef = extern

  /**
   * Folds the string into the form specified by the flags. Character foldings are operations that convert any of a set of characters sharing similar semantics into a single representative from that set. This function can be used to preprocess strings that are to be compared, searched, or indexed. Note that folding does not include normalization, so it is necessary to use CFStringNormalize in addition to CFStringFold in order to obtain the effect of kCFCompareNonliteral.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFold(theString : CFMutableStringRef, theFlags : CFStringCompareFlags, theLocale : CFLocaleRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCString(theString : CFStringRef, buffer : CString, bufferSize : CFIndex, encoding : CFStringEncoding): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCStringPtr(theString : CFStringRef, encoding : CFStringEncoding): CString = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCharacterAtIndex(theString : CFStringRef, idx : CFIndex): UniChar = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCharacterFromInlineBuffer(buf : Ptr[CFStringInlineBuffer], idx : CFIndex): UniChar = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCharactersPtr(theString : CFStringRef): Ptr[UniChar] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetDoubleValue(str : CFStringRef): Double = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetFastestEncoding(theString : CFStringRef): CFStringEncoding = extern

  /**
   * * FileSystem path conversion functions **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetFileSystemRepresentation(string : CFStringRef, buffer : CString, maxBufLen : CFIndex): Boolean = extern

  /**
   * * Parsing non-localized numbers from strings **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetIntValue(str : CFStringRef): SInt32 = extern

  /**
   * * Basic accessors for the contents **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetLength(theString : CFStringRef): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetListOfAvailableEncodings(): Ptr[CFStringEncoding] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetLongCharacterForSurrogatePair(surrogateHigh : UniChar, surrogateLow : UniChar): UTF32Char = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetMaximumSizeForEncoding(length : CFIndex, encoding : CFStringEncoding): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetMaximumSizeOfFileSystemRepresentation(string : CFStringRef): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetMostCompatibleMacStringEncoding(encoding : CFStringEncoding): CFStringEncoding = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetNameOfEncoding(encoding : CFStringEncoding): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetPascalString(theString : CFStringRef, buffer : StringPtr, bufferSize : CFIndex, encoding : CFStringEncoding): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetPascalStringPtr(theString : CFStringRef, encoding : CFStringEncoding): ConstStringPtr = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetSmallestEncoding(theString : CFStringRef): CFStringEncoding = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetSurrogatePairForLongCharacter(character : UTF32Char, surrogates : Ptr[UniChar]): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetSystemEncoding(): CFStringEncoding = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringHasPrefix(theString : CFStringRef, prefix : CFStringRef): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringHasSuffix(theString : CFStringRef, suffix : CFStringRef): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringInsert(str : CFMutableStringRef, idx : CFIndex, insertedStr : CFStringRef): Unit = extern

  /**
   * * General encoding related functionality **
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringIsEncodingAvailable(encoding : CFStringEncoding): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringIsHyphenationAvailableForLocale(locale : CFLocaleRef): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringIsSurrogateHighCharacter(character : UniChar): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringIsSurrogateLowCharacter(character : UniChar): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringLowercase(theString : CFMutableStringRef, locale : CFLocaleRef): Unit = extern

  /**
   * Normalizes the string into the specified form as described in Unicode Technical Report #15.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringNormalize(theString : CFMutableStringRef, theForm : CFStringNormalizationForm): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringPad(theString : CFMutableStringRef, padString : CFStringRef, length : CFIndex, indexIntoPad : CFIndex): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringReplaceAll(theString : CFMutableStringRef, replacement : CFStringRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringSetExternalCharactersNoCopy(theString : CFMutableStringRef, chars : Ptr[UniChar], length : CFIndex, capacity : CFIndex): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringTransform(string : CFMutableStringRef, range : Ptr[CFRange], transform : CFStringRef, reverse : Boolean): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringTrim(theString : CFMutableStringRef, trimString : CFStringRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringTrimWhitespace(theString : CFMutableStringRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringUppercase(theString : CFMutableStringRef, locale : CFLocaleRef): Unit = extern

  /**
   * Returns a string describing the specified error result code.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecBase.h
  */
  def SecCopyErrorMessageString(status : OSStatus, reserved : Ptr[Byte]): CFStringRef = extern

  /**
   * Add one or more items to a keychain.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemAdd(attributes : CFDictionaryRef, result : Ptr[CFTypeRef]): OSStatus = extern

  /**
   * Returns one or more items which match a search query.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemCopyMatching(query : CFDictionaryRef, result : Ptr[CFTypeRef]): OSStatus = extern

  /**
   * Delete zero or more items which match a search query.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemDelete(query : CFDictionaryRef): OSStatus = extern

  /**
   * Modify zero or more items which match a search query.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h
  */
  def SecItemUpdate(query : CFDictionaryRef, attributesToUpdate : CFDictionaryRef): OSStatus = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def __CFStringMakeConstantString(cStr : CString): CFStringRef = extern

  private[macos] def __sn_wrap_macos_CFStringCompareWithOptions(theString1 : CFStringRef, theString2 : CFStringRef, rangeToCompare : Ptr[CFRange], compareOptions : CFStringCompareFlags): CFComparisonResult = extern

  private[macos] def __sn_wrap_macos_CFStringCompareWithOptionsAndLocale(theString1 : CFStringRef, theString2 : CFStringRef, rangeToCompare : Ptr[CFRange], compareOptions : CFStringCompareFlags, locale : CFLocaleRef): CFComparisonResult = extern

  private[macos] def __sn_wrap_macos_CFStringCreateArrayWithFindResults(alloc : CFAllocatorRef, theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : Ptr[CFRange], compareOptions : CFStringCompareFlags): CFArrayRef = extern

  private[macos] def __sn_wrap_macos_CFStringCreateWithSubstring(alloc : CFAllocatorRef, str : CFStringRef, range : Ptr[CFRange]): CFStringRef = extern

  private[macos] def __sn_wrap_macos_CFStringDelete(theString : CFMutableStringRef, range : Ptr[CFRange]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringFind(theString : CFStringRef, stringToFind : CFStringRef, compareOptions : CFStringCompareFlags, __return : Ptr[CFRange]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringFindAndReplace(theString : CFMutableStringRef, stringToFind : CFStringRef, replacementString : CFStringRef, rangeToSearch : Ptr[CFRange], compareOptions : CFStringCompareFlags): CFIndex = extern

  private[macos] def __sn_wrap_macos_CFStringFindCharacterFromSet(theString : CFStringRef, theSet : CFCharacterSetRef, rangeToSearch : Ptr[CFRange], searchOptions : CFStringCompareFlags, result : Ptr[CFRange]): Boolean = extern

  private[macos] def __sn_wrap_macos_CFStringFindWithOptions(theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : Ptr[CFRange], searchOptions : CFStringCompareFlags, result : Ptr[CFRange]): Boolean = extern

  private[macos] def __sn_wrap_macos_CFStringFindWithOptionsAndLocale(theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : Ptr[CFRange], searchOptions : CFStringCompareFlags, locale : CFLocaleRef, result : Ptr[CFRange]): Boolean = extern

  private[macos] def __sn_wrap_macos_CFStringGetBytes(theString : CFStringRef, range : Ptr[CFRange], encoding : CFStringEncoding, lossByte : UInt8, isExternalRepresentation : Boolean, buffer : Ptr[UInt8], maxBufLen : CFIndex, usedBufLen : Ptr[CFIndex]): CFIndex = extern

  private[macos] def __sn_wrap_macos_CFStringGetCharacters(theString : CFStringRef, range : Ptr[CFRange], buffer : Ptr[UniChar]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringGetHyphenationLocationBeforeIndex(string : CFStringRef, location : CFIndex, limitRange : Ptr[CFRange], options : CFOptionFlags, locale : CFLocaleRef, character : Ptr[UTF32Char]): CFIndex = extern

  private[macos] def __sn_wrap_macos_CFStringGetLineBounds(theString : CFStringRef, range : Ptr[CFRange], lineBeginIndex : Ptr[CFIndex], lineEndIndex : Ptr[CFIndex], contentsEndIndex : Ptr[CFIndex]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringGetParagraphBounds(string : CFStringRef, range : Ptr[CFRange], parBeginIndex : Ptr[CFIndex], parEndIndex : Ptr[CFIndex], contentsEndIndex : Ptr[CFIndex]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringGetRangeOfComposedCharactersAtIndex(theString : CFStringRef, theIndex : CFIndex, __return : Ptr[CFRange]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringInitInlineBuffer(str : CFStringRef, buf : Ptr[CFStringInlineBuffer], range : Ptr[CFRange]): Unit = extern

  private[macos] def __sn_wrap_macos_CFStringReplace(theString : CFMutableStringRef, range : Ptr[CFRange], replacement : CFStringRef): Unit = extern


object functions:
  import _root_.macos.aliases.*
  import _root_.macos.structs.*
  import extern_functions.*
  export extern_functions.*

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCompareWithOptions(theString1 : CFStringRef, theString2 : CFStringRef, rangeToCompare : Ptr[CFRange], compareOptions : CFStringCompareFlags): CFComparisonResult = 
    __sn_wrap_macos_CFStringCompareWithOptions(theString1, theString2, rangeToCompare, compareOptions)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCompareWithOptions(theString1 : CFStringRef, theString2 : CFStringRef, rangeToCompare : CFRange, compareOptions : CFStringCompareFlags)(using Zone): CFComparisonResult = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToCompare
    __sn_wrap_macos_CFStringCompareWithOptions(theString1, theString2, (__ptr_0 + 0), compareOptions)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCompareWithOptionsAndLocale(theString1 : CFStringRef, theString2 : CFStringRef, rangeToCompare : Ptr[CFRange], compareOptions : CFStringCompareFlags, locale : CFLocaleRef): CFComparisonResult = 
    __sn_wrap_macos_CFStringCompareWithOptionsAndLocale(theString1, theString2, rangeToCompare, compareOptions, locale)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCompareWithOptionsAndLocale(theString1 : CFStringRef, theString2 : CFStringRef, rangeToCompare : CFRange, compareOptions : CFStringCompareFlags, locale : CFLocaleRef)(using Zone): CFComparisonResult = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToCompare
    __sn_wrap_macos_CFStringCompareWithOptionsAndLocale(theString1, theString2, (__ptr_0 + 0), compareOptions, locale)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateArrayWithFindResults(_alloc : CFAllocatorRef, theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : CFRange, compareOptions : CFStringCompareFlags)(using Zone): CFArrayRef = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_macos_CFStringCreateArrayWithFindResults(_alloc, theString, stringToFind, (__ptr_0 + 0), compareOptions)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateArrayWithFindResults(alloc : CFAllocatorRef, theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : Ptr[CFRange], compareOptions : CFStringCompareFlags): CFArrayRef = 
    __sn_wrap_macos_CFStringCreateArrayWithFindResults(alloc, theString, stringToFind, rangeToSearch, compareOptions)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithSubstring(alloc : CFAllocatorRef, str : CFStringRef, range : Ptr[CFRange]): CFStringRef = 
    __sn_wrap_macos_CFStringCreateWithSubstring(alloc, str, range)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringCreateWithSubstring(_alloc : CFAllocatorRef, str : CFStringRef, range : CFRange)(using Zone): CFStringRef = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringCreateWithSubstring(_alloc, str, (__ptr_0 + 0))

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringDelete(theString : CFMutableStringRef, range : Ptr[CFRange]): Unit = 
    __sn_wrap_macos_CFStringDelete(theString, range)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringDelete(theString : CFMutableStringRef, range : CFRange)(using Zone): Unit = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringDelete(theString, (__ptr_0 + 0))

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFind(theString : CFStringRef, stringToFind : CFStringRef, compareOptions : CFStringCompareFlags)(__return : Ptr[CFRange]): Unit = 
    __sn_wrap_macos_CFStringFind(theString, stringToFind, compareOptions, __return)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFind(theString : CFStringRef, stringToFind : CFStringRef, compareOptions : CFStringCompareFlags)(using Zone): CFRange = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    __sn_wrap_macos_CFStringFind(theString, stringToFind, compareOptions, (__ptr_0 + 0))
    !(__ptr_0 + 0)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindAndReplace(theString : CFMutableStringRef, stringToFind : CFStringRef, replacementString : CFStringRef, rangeToSearch : Ptr[CFRange], compareOptions : CFStringCompareFlags): CFIndex = 
    __sn_wrap_macos_CFStringFindAndReplace(theString, stringToFind, replacementString, rangeToSearch, compareOptions)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindAndReplace(theString : CFMutableStringRef, stringToFind : CFStringRef, replacementString : CFStringRef, rangeToSearch : CFRange, compareOptions : CFStringCompareFlags)(using Zone): CFIndex = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_macos_CFStringFindAndReplace(theString, stringToFind, replacementString, (__ptr_0 + 0), compareOptions)

  /**
   * Query the range of the first character contained in the specified character set.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindCharacterFromSet(theString : CFStringRef, theSet : CFCharacterSetRef, rangeToSearch : CFRange, searchOptions : CFStringCompareFlags, result : Ptr[CFRange])(using Zone): Boolean = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_macos_CFStringFindCharacterFromSet(theString, theSet, (__ptr_0 + 0), searchOptions, result)

  /**
   * Query the range of the first character contained in the specified character set.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindCharacterFromSet(theString : CFStringRef, theSet : CFCharacterSetRef, rangeToSearch : Ptr[CFRange], searchOptions : CFStringCompareFlags, result : Ptr[CFRange]): Boolean = 
    __sn_wrap_macos_CFStringFindCharacterFromSet(theString, theSet, rangeToSearch, searchOptions, result)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindWithOptions(theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : CFRange, searchOptions : CFStringCompareFlags, result : Ptr[CFRange])(using Zone): Boolean = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_macos_CFStringFindWithOptions(theString, stringToFind, (__ptr_0 + 0), searchOptions, result)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindWithOptions(theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : Ptr[CFRange], searchOptions : CFStringCompareFlags, result : Ptr[CFRange]): Boolean = 
    __sn_wrap_macos_CFStringFindWithOptions(theString, stringToFind, rangeToSearch, searchOptions, result)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindWithOptionsAndLocale(theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : CFRange, searchOptions : CFStringCompareFlags, locale : CFLocaleRef, result : Ptr[CFRange])(using Zone): Boolean = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_macos_CFStringFindWithOptionsAndLocale(theString, stringToFind, (__ptr_0 + 0), searchOptions, locale, result)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringFindWithOptionsAndLocale(theString : CFStringRef, stringToFind : CFStringRef, rangeToSearch : Ptr[CFRange], searchOptions : CFStringCompareFlags, locale : CFLocaleRef, result : Ptr[CFRange]): Boolean = 
    __sn_wrap_macos_CFStringFindWithOptionsAndLocale(theString, stringToFind, rangeToSearch, searchOptions, locale, result)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetBytes(theString : CFStringRef, range : CFRange, encoding : CFStringEncoding, lossByte : UInt8, isExternalRepresentation : Boolean, buffer : Ptr[UInt8], maxBufLen : CFIndex, usedBufLen : Ptr[CFIndex])(using Zone): CFIndex = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringGetBytes(theString, (__ptr_0 + 0), encoding, lossByte, isExternalRepresentation, buffer, maxBufLen, usedBufLen)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetBytes(theString : CFStringRef, range : Ptr[CFRange], encoding : CFStringEncoding, lossByte : UInt8, isExternalRepresentation : Boolean, buffer : Ptr[UInt8], maxBufLen : CFIndex, usedBufLen : Ptr[CFIndex]): CFIndex = 
    __sn_wrap_macos_CFStringGetBytes(theString, range, encoding, lossByte, isExternalRepresentation, buffer, maxBufLen, usedBufLen)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCharacters(theString : CFStringRef, range : Ptr[CFRange], buffer : Ptr[UniChar]): Unit = 
    __sn_wrap_macos_CFStringGetCharacters(theString, range, buffer)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetCharacters(theString : CFStringRef, range : CFRange, buffer : Ptr[UniChar])(using Zone): Unit = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringGetCharacters(theString, (__ptr_0 + 0), buffer)

  /**
   * Retrieve the first potential hyphenation location found before the specified location.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetHyphenationLocationBeforeIndex(string : CFStringRef, location : CFIndex, limitRange : Ptr[CFRange], options : CFOptionFlags, locale : CFLocaleRef, character : Ptr[UTF32Char]): CFIndex = 
    __sn_wrap_macos_CFStringGetHyphenationLocationBeforeIndex(string, location, limitRange, options, locale, character)

  /**
   * Retrieve the first potential hyphenation location found before the specified location.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetHyphenationLocationBeforeIndex(string : CFStringRef, location : CFIndex, limitRange : CFRange, options : CFOptionFlags, locale : CFLocaleRef, character : Ptr[UTF32Char])(using Zone): CFIndex = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = limitRange
    __sn_wrap_macos_CFStringGetHyphenationLocationBeforeIndex(string, location, (__ptr_0 + 0), options, locale, character)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetLineBounds(theString : CFStringRef, range : Ptr[CFRange], lineBeginIndex : Ptr[CFIndex], lineEndIndex : Ptr[CFIndex], contentsEndIndex : Ptr[CFIndex]): Unit = 
    __sn_wrap_macos_CFStringGetLineBounds(theString, range, lineBeginIndex, lineEndIndex, contentsEndIndex)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetLineBounds(theString : CFStringRef, range : CFRange, lineBeginIndex : Ptr[CFIndex], lineEndIndex : Ptr[CFIndex], contentsEndIndex : Ptr[CFIndex])(using Zone): Unit = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringGetLineBounds(theString, (__ptr_0 + 0), lineBeginIndex, lineEndIndex, contentsEndIndex)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetParagraphBounds(string : CFStringRef, range : Ptr[CFRange], parBeginIndex : Ptr[CFIndex], parEndIndex : Ptr[CFIndex], contentsEndIndex : Ptr[CFIndex]): Unit = 
    __sn_wrap_macos_CFStringGetParagraphBounds(string, range, parBeginIndex, parEndIndex, contentsEndIndex)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetParagraphBounds(string : CFStringRef, range : CFRange, parBeginIndex : Ptr[CFIndex], parEndIndex : Ptr[CFIndex], contentsEndIndex : Ptr[CFIndex])(using Zone): Unit = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringGetParagraphBounds(string, (__ptr_0 + 0), parBeginIndex, parEndIndex, contentsEndIndex)

  /**
   * Returns the range of the composed character sequence at the specified index.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetRangeOfComposedCharactersAtIndex(theString : CFStringRef, theIndex : CFIndex)(using Zone): CFRange = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    __sn_wrap_macos_CFStringGetRangeOfComposedCharactersAtIndex(theString, theIndex, (__ptr_0 + 0))
    !(__ptr_0 + 0)

  /**
   * Returns the range of the composed character sequence at the specified index.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringGetRangeOfComposedCharactersAtIndex(theString : CFStringRef, theIndex : CFIndex)(__return : Ptr[CFRange]): Unit = 
    __sn_wrap_macos_CFStringGetRangeOfComposedCharactersAtIndex(theString, theIndex, __return)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringInitInlineBuffer(str : CFStringRef, buf : Ptr[CFStringInlineBuffer], range : Ptr[CFRange]): Unit = 
    __sn_wrap_macos_CFStringInitInlineBuffer(str, buf, range)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringInitInlineBuffer(str : CFStringRef, buf : Ptr[CFStringInlineBuffer], range : CFRange)(using Zone): Unit = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringInitInlineBuffer(str, buf, (__ptr_0 + 0))

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringReplace(theString : CFMutableStringRef, range : CFRange, replacement : CFStringRef)(using Zone): Unit = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_macos_CFStringReplace(theString, (__ptr_0 + 0), replacement)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
  */
  def CFStringReplace(theString : CFMutableStringRef, range : Ptr[CFRange], replacement : CFStringRef): Unit = 
    __sn_wrap_macos_CFStringReplace(theString, range, replacement)

object constants:
  val kCFCompareCaseInsensitive: CUnsignedInt = 1.toUInt
  val kCFCompareBackwards: CUnsignedInt = 4.toUInt
  val kCFCompareAnchored: CUnsignedInt = 8.toUInt
  val kCFCompareNonliteral: CUnsignedInt = 16.toUInt
  val kCFCompareLocalized: CUnsignedInt = 32.toUInt
  val kCFCompareNumerically: CUnsignedInt = 64.toUInt
  val kCFCompareDiacriticInsensitive: CUnsignedInt = 128.toUInt
  val kCFCompareWidthInsensitive: CUnsignedInt = 256.toUInt
  val kCFCompareForcedOrdering: CUnsignedInt = 512.toUInt
  
  val errSSLProtocol: CInt = -9800
  val errSSLNegotiation: CInt = -9801
  val errSSLFatalAlert: CInt = -9802
  val errSSLWouldBlock: CInt = -9803
  val errSSLSessionNotFound: CInt = -9804
  val errSSLClosedGraceful: CInt = -9805
  val errSSLClosedAbort: CInt = -9806
  val errSSLXCertChainInvalid: CInt = -9807
  val errSSLBadCert: CInt = -9808
  val errSSLCrypto: CInt = -9809
  val errSSLInternal: CInt = -9810
  val errSSLModuleAttach: CInt = -9811
  val errSSLUnknownRootCert: CInt = -9812
  val errSSLNoRootCert: CInt = -9813
  val errSSLCertExpired: CInt = -9814
  val errSSLCertNotYetValid: CInt = -9815
  val errSSLClosedNoNotify: CInt = -9816
  val errSSLBufferOverflow: CInt = -9817
  val errSSLBadCipherSuite: CInt = -9818
  val errSSLPeerUnexpectedMsg: CInt = -9819
  val errSSLPeerBadRecordMac: CInt = -9820
  val errSSLPeerDecryptionFail: CInt = -9821
  val errSSLPeerRecordOverflow: CInt = -9822
  val errSSLPeerDecompressFail: CInt = -9823
  val errSSLPeerHandshakeFail: CInt = -9824
  val errSSLPeerBadCert: CInt = -9825
  val errSSLPeerUnsupportedCert: CInt = -9826
  val errSSLPeerCertRevoked: CInt = -9827
  val errSSLPeerCertExpired: CInt = -9828
  val errSSLPeerCertUnknown: CInt = -9829
  val errSSLIllegalParam: CInt = -9830
  val errSSLPeerUnknownCA: CInt = -9831
  val errSSLPeerAccessDenied: CInt = -9832
  val errSSLPeerDecodeError: CInt = -9833
  val errSSLPeerDecryptError: CInt = -9834
  val errSSLPeerExportRestriction: CInt = -9835
  val errSSLPeerProtocolVersion: CInt = -9836
  val errSSLPeerInsufficientSecurity: CInt = -9837
  val errSSLPeerInternalError: CInt = -9838
  val errSSLPeerUserCancelled: CInt = -9839
  val errSSLPeerNoRenegotiation: CInt = -9840
  val errSSLPeerAuthCompleted: CInt = -9841
  val errSSLClientCertRequested: CInt = -9842
  val errSSLHostNameMismatch: CInt = -9843
  val errSSLConnectionRefused: CInt = -9844
  val errSSLDecryptionFail: CInt = -9845
  val errSSLBadRecordMac: CInt = -9846
  val errSSLRecordOverflow: CInt = -9847
  val errSSLBadConfiguration: CInt = -9848
  val errSSLUnexpectedRecord: CInt = -9849
  val errSSLWeakPeerEphemeralDHKey: CInt = -9850
  val errSSLClientHelloReceived: CInt = -9851
  val errSSLTransportReset: CInt = -9852
  val errSSLNetworkTimeout: CInt = -9853
  val errSSLConfigurationFailed: CInt = -9854
  val errSSLUnsupportedExtension: CInt = -9855
  val errSSLUnexpectedMessage: CInt = -9856
  val errSSLDecompressFail: CInt = -9857
  val errSSLHandshakeFail: CInt = -9858
  val errSSLDecodeError: CInt = -9859
  val errSSLInappropriateFallback: CInt = -9860
  val errSSLMissingExtension: CInt = -9861
  val errSSLBadCertificateStatusResponse: CInt = -9862
  val errSSLCertificateRequired: CInt = -9863
  val errSSLUnknownPSKIdentity: CInt = -9864
  val errSSLUnrecognizedName: CInt = -9865
  val errSSLATSViolation: CInt = -9880
  val errSSLATSMinimumVersionViolation: CInt = -9881
  val errSSLATSCiphersuiteViolation: CInt = -9882
  val errSSLATSMinimumKeySizeViolation: CInt = -9883
  val errSSLATSLeafCertificateHashAlgorithmViolation: CInt = -9884
  val errSSLATSCertificateHashAlgorithmViolation: CInt = -9885
  val errSSLATSCertificateTrustViolation: CInt = -9886
  val errSSLEarlyDataRejected: CInt = -9890
  
  val kCFStringEncodingMacRoman: CUnsignedInt = 0.toUInt
  val kCFStringEncodingWindowsLatin1: CUnsignedInt = 1280.toUInt
  val kCFStringEncodingISOLatin1: CUnsignedInt = 513.toUInt
  val kCFStringEncodingNextStepLatin: CUnsignedInt = 2817.toUInt
  val kCFStringEncodingASCII: CUnsignedInt = 1536.toUInt
  val kCFStringEncodingUnicode: CUnsignedInt = 256.toUInt
  val kCFStringEncodingUTF8: CUnsignedInt = 134217984.toUInt
  val kCFStringEncodingNonLossyASCII: CUnsignedInt = 3071.toUInt
  val kCFStringEncodingUTF16: CUnsignedInt = 256.toUInt
  val kCFStringEncodingUTF16BE: CUnsignedInt = 268435712.toUInt
  val kCFStringEncodingUTF16LE: CUnsignedInt = 335544576.toUInt
  val kCFStringEncodingUTF32: CUnsignedInt = 201326848.toUInt
  val kCFStringEncodingUTF32BE: CUnsignedInt = 402653440.toUInt
  val kCFStringEncodingUTF32LE: CUnsignedInt = 469762304.toUInt
  
  val errSecSuccess: CInt = 0
  val errSecUnimplemented: CInt = -4
  val errSecDiskFull: CInt = -34
  val errSecDskFull: CInt = -34
  val errSecIO: CInt = -36
  val errSecOpWr: CInt = -49
  val errSecParam: CInt = -50
  val errSecWrPerm: CInt = -61
  val errSecAllocate: CInt = -108
  val errSecUserCanceled: CInt = -128
  val errSecBadReq: CInt = -909
  val errSecInternalComponent: CInt = -2070
  val errSecCoreFoundationUnknown: CInt = -4960
  val errSecMissingEntitlement: CInt = -34018
  val errSecRestrictedAPI: CInt = -34020
  val errSecNotAvailable: CInt = -25291
  val errSecReadOnly: CInt = -25292
  val errSecAuthFailed: CInt = -25293
  val errSecNoSuchKeychain: CInt = -25294
  val errSecInvalidKeychain: CInt = -25295
  val errSecDuplicateKeychain: CInt = -25296
  val errSecDuplicateCallback: CInt = -25297
  val errSecInvalidCallback: CInt = -25298
  val errSecDuplicateItem: CInt = -25299
  val errSecItemNotFound: CInt = -25300
  val errSecBufferTooSmall: CInt = -25301
  val errSecDataTooLarge: CInt = -25302
  val errSecNoSuchAttr: CInt = -25303
  val errSecInvalidItemRef: CInt = -25304
  val errSecInvalidSearchRef: CInt = -25305
  val errSecNoSuchClass: CInt = -25306
  val errSecNoDefaultKeychain: CInt = -25307
  val errSecInteractionNotAllowed: CInt = -25308
  val errSecReadOnlyAttr: CInt = -25309
  val errSecWrongSecVersion: CInt = -25310
  val errSecKeySizeNotAllowed: CInt = -25311
  val errSecNoStorageModule: CInt = -25312
  val errSecNoCertificateModule: CInt = -25313
  val errSecNoPolicyModule: CInt = -25314
  val errSecInteractionRequired: CInt = -25315
  val errSecDataNotAvailable: CInt = -25316
  val errSecDataNotModifiable: CInt = -25317
  val errSecCreateChainFailed: CInt = -25318
  val errSecInvalidPrefsDomain: CInt = -25319
  val errSecInDarkWake: CInt = -25320
  val errSecACLNotSimple: CInt = -25240
  val errSecPolicyNotFound: CInt = -25241
  val errSecInvalidTrustSetting: CInt = -25242
  val errSecNoAccessForItem: CInt = -25243
  val errSecInvalidOwnerEdit: CInt = -25244
  val errSecTrustNotAvailable: CInt = -25245
  val errSecUnsupportedFormat: CInt = -25256
  val errSecUnknownFormat: CInt = -25257
  val errSecKeyIsSensitive: CInt = -25258
  val errSecMultiplePrivKeys: CInt = -25259
  val errSecPassphraseRequired: CInt = -25260
  val errSecInvalidPasswordRef: CInt = -25261
  val errSecInvalidTrustSettings: CInt = -25262
  val errSecNoTrustSettings: CInt = -25263
  val errSecPkcs12VerifyFailure: CInt = -25264
  val errSecNotSigner: CInt = -26267
  val errSecDecode: CInt = -26275
  val errSecServiceNotAvailable: CInt = -67585
  val errSecInsufficientClientID: CInt = -67586
  val errSecDeviceReset: CInt = -67587
  val errSecDeviceFailed: CInt = -67588
  val errSecAppleAddAppACLSubject: CInt = -67589
  val errSecApplePublicKeyIncomplete: CInt = -67590
  val errSecAppleSignatureMismatch: CInt = -67591
  val errSecAppleInvalidKeyStartDate: CInt = -67592
  val errSecAppleInvalidKeyEndDate: CInt = -67593
  val errSecConversionError: CInt = -67594
  val errSecAppleSSLv2Rollback: CInt = -67595
  val errSecQuotaExceeded: CInt = -67596
  val errSecFileTooBig: CInt = -67597
  val errSecInvalidDatabaseBlob: CInt = -67598
  val errSecInvalidKeyBlob: CInt = -67599
  val errSecIncompatibleDatabaseBlob: CInt = -67600
  val errSecIncompatibleKeyBlob: CInt = -67601
  val errSecHostNameMismatch: CInt = -67602
  val errSecUnknownCriticalExtensionFlag: CInt = -67603
  val errSecNoBasicConstraints: CInt = -67604
  val errSecNoBasicConstraintsCA: CInt = -67605
  val errSecInvalidAuthorityKeyID: CInt = -67606
  val errSecInvalidSubjectKeyID: CInt = -67607
  val errSecInvalidKeyUsageForPolicy: CInt = -67608
  val errSecInvalidExtendedKeyUsage: CInt = -67609
  val errSecInvalidIDLinkage: CInt = -67610
  val errSecPathLengthConstraintExceeded: CInt = -67611
  val errSecInvalidRoot: CInt = -67612
  val errSecCRLExpired: CInt = -67613
  val errSecCRLNotValidYet: CInt = -67614
  val errSecCRLNotFound: CInt = -67615
  val errSecCRLServerDown: CInt = -67616
  val errSecCRLBadURI: CInt = -67617
  val errSecUnknownCertExtension: CInt = -67618
  val errSecUnknownCRLExtension: CInt = -67619
  val errSecCRLNotTrusted: CInt = -67620
  val errSecCRLPolicyFailed: CInt = -67621
  val errSecIDPFailure: CInt = -67622
  val errSecSMIMEEmailAddressesNotFound: CInt = -67623
  val errSecSMIMEBadExtendedKeyUsage: CInt = -67624
  val errSecSMIMEBadKeyUsage: CInt = -67625
  val errSecSMIMEKeyUsageNotCritical: CInt = -67626
  val errSecSMIMENoEmailAddress: CInt = -67627
  val errSecSMIMESubjAltNameNotCritical: CInt = -67628
  val errSecSSLBadExtendedKeyUsage: CInt = -67629
  val errSecOCSPBadResponse: CInt = -67630
  val errSecOCSPBadRequest: CInt = -67631
  val errSecOCSPUnavailable: CInt = -67632
  val errSecOCSPStatusUnrecognized: CInt = -67633
  val errSecEndOfData: CInt = -67634
  val errSecIncompleteCertRevocationCheck: CInt = -67635
  val errSecNetworkFailure: CInt = -67636
  val errSecOCSPNotTrustedToAnchor: CInt = -67637
  val errSecRecordModified: CInt = -67638
  val errSecOCSPSignatureError: CInt = -67639
  val errSecOCSPNoSigner: CInt = -67640
  val errSecOCSPResponderMalformedReq: CInt = -67641
  val errSecOCSPResponderInternalError: CInt = -67642
  val errSecOCSPResponderTryLater: CInt = -67643
  val errSecOCSPResponderSignatureRequired: CInt = -67644
  val errSecOCSPResponderUnauthorized: CInt = -67645
  val errSecOCSPResponseNonceMismatch: CInt = -67646
  val errSecCodeSigningBadCertChainLength: CInt = -67647
  val errSecCodeSigningNoBasicConstraints: CInt = -67648
  val errSecCodeSigningBadPathLengthConstraint: CInt = -67649
  val errSecCodeSigningNoExtendedKeyUsage: CInt = -67650
  val errSecCodeSigningDevelopment: CInt = -67651
  val errSecResourceSignBadCertChainLength: CInt = -67652
  val errSecResourceSignBadExtKeyUsage: CInt = -67653
  val errSecTrustSettingDeny: CInt = -67654
  val errSecInvalidSubjectName: CInt = -67655
  val errSecUnknownQualifiedCertStatement: CInt = -67656
  val errSecMobileMeRequestQueued: CInt = -67657
  val errSecMobileMeRequestRedirected: CInt = -67658
  val errSecMobileMeServerError: CInt = -67659
  val errSecMobileMeServerNotAvailable: CInt = -67660
  val errSecMobileMeServerAlreadyExists: CInt = -67661
  val errSecMobileMeServerServiceErr: CInt = -67662
  val errSecMobileMeRequestAlreadyPending: CInt = -67663
  val errSecMobileMeNoRequestPending: CInt = -67664
  val errSecMobileMeCSRVerifyFailure: CInt = -67665
  val errSecMobileMeFailedConsistencyCheck: CInt = -67666
  val errSecNotInitialized: CInt = -67667
  val errSecInvalidHandleUsage: CInt = -67668
  val errSecPVCReferentNotFound: CInt = -67669
  val errSecFunctionIntegrityFail: CInt = -67670
  val errSecInternalError: CInt = -67671
  val errSecMemoryError: CInt = -67672
  val errSecInvalidData: CInt = -67673
  val errSecMDSError: CInt = -67674
  val errSecInvalidPointer: CInt = -67675
  val errSecSelfCheckFailed: CInt = -67676
  val errSecFunctionFailed: CInt = -67677
  val errSecModuleManifestVerifyFailed: CInt = -67678
  val errSecInvalidGUID: CInt = -67679
  val errSecInvalidHandle: CInt = -67680
  val errSecInvalidDBList: CInt = -67681
  val errSecInvalidPassthroughID: CInt = -67682
  val errSecInvalidNetworkAddress: CInt = -67683
  val errSecCRLAlreadySigned: CInt = -67684
  val errSecInvalidNumberOfFields: CInt = -67685
  val errSecVerificationFailure: CInt = -67686
  val errSecUnknownTag: CInt = -67687
  val errSecInvalidSignature: CInt = -67688
  val errSecInvalidName: CInt = -67689
  val errSecInvalidCertificateRef: CInt = -67690
  val errSecInvalidCertificateGroup: CInt = -67691
  val errSecTagNotFound: CInt = -67692
  val errSecInvalidQuery: CInt = -67693
  val errSecInvalidValue: CInt = -67694
  val errSecCallbackFailed: CInt = -67695
  val errSecACLDeleteFailed: CInt = -67696
  val errSecACLReplaceFailed: CInt = -67697
  val errSecACLAddFailed: CInt = -67698
  val errSecACLChangeFailed: CInt = -67699
  val errSecInvalidAccessCredentials: CInt = -67700
  val errSecInvalidRecord: CInt = -67701
  val errSecInvalidACL: CInt = -67702
  val errSecInvalidSampleValue: CInt = -67703
  val errSecIncompatibleVersion: CInt = -67704
  val errSecPrivilegeNotGranted: CInt = -67705
  val errSecInvalidScope: CInt = -67706
  val errSecPVCAlreadyConfigured: CInt = -67707
  val errSecInvalidPVC: CInt = -67708
  val errSecEMMLoadFailed: CInt = -67709
  val errSecEMMUnloadFailed: CInt = -67710
  val errSecAddinLoadFailed: CInt = -67711
  val errSecInvalidKeyRef: CInt = -67712
  val errSecInvalidKeyHierarchy: CInt = -67713
  val errSecAddinUnloadFailed: CInt = -67714
  val errSecLibraryReferenceNotFound: CInt = -67715
  val errSecInvalidAddinFunctionTable: CInt = -67716
  val errSecInvalidServiceMask: CInt = -67717
  val errSecModuleNotLoaded: CInt = -67718
  val errSecInvalidSubServiceID: CInt = -67719
  val errSecAttributeNotInContext: CInt = -67720
  val errSecModuleManagerInitializeFailed: CInt = -67721
  val errSecModuleManagerNotFound: CInt = -67722
  val errSecEventNotificationCallbackNotFound: CInt = -67723
  val errSecInputLengthError: CInt = -67724
  val errSecOutputLengthError: CInt = -67725
  val errSecPrivilegeNotSupported: CInt = -67726
  val errSecDeviceError: CInt = -67727
  val errSecAttachHandleBusy: CInt = -67728
  val errSecNotLoggedIn: CInt = -67729
  val errSecAlgorithmMismatch: CInt = -67730
  val errSecKeyUsageIncorrect: CInt = -67731
  val errSecKeyBlobTypeIncorrect: CInt = -67732
  val errSecKeyHeaderInconsistent: CInt = -67733
  val errSecUnsupportedKeyFormat: CInt = -67734
  val errSecUnsupportedKeySize: CInt = -67735
  val errSecInvalidKeyUsageMask: CInt = -67736
  val errSecUnsupportedKeyUsageMask: CInt = -67737
  val errSecInvalidKeyAttributeMask: CInt = -67738
  val errSecUnsupportedKeyAttributeMask: CInt = -67739
  val errSecInvalidKeyLabel: CInt = -67740
  val errSecUnsupportedKeyLabel: CInt = -67741
  val errSecInvalidKeyFormat: CInt = -67742
  val errSecUnsupportedVectorOfBuffers: CInt = -67743
  val errSecInvalidInputVector: CInt = -67744
  val errSecInvalidOutputVector: CInt = -67745
  val errSecInvalidContext: CInt = -67746
  val errSecInvalidAlgorithm: CInt = -67747
  val errSecInvalidAttributeKey: CInt = -67748
  val errSecMissingAttributeKey: CInt = -67749
  val errSecInvalidAttributeInitVector: CInt = -67750
  val errSecMissingAttributeInitVector: CInt = -67751
  val errSecInvalidAttributeSalt: CInt = -67752
  val errSecMissingAttributeSalt: CInt = -67753
  val errSecInvalidAttributePadding: CInt = -67754
  val errSecMissingAttributePadding: CInt = -67755
  val errSecInvalidAttributeRandom: CInt = -67756
  val errSecMissingAttributeRandom: CInt = -67757
  val errSecInvalidAttributeSeed: CInt = -67758
  val errSecMissingAttributeSeed: CInt = -67759
  val errSecInvalidAttributePassphrase: CInt = -67760
  val errSecMissingAttributePassphrase: CInt = -67761
  val errSecInvalidAttributeKeyLength: CInt = -67762
  val errSecMissingAttributeKeyLength: CInt = -67763
  val errSecInvalidAttributeBlockSize: CInt = -67764
  val errSecMissingAttributeBlockSize: CInt = -67765
  val errSecInvalidAttributeOutputSize: CInt = -67766
  val errSecMissingAttributeOutputSize: CInt = -67767
  val errSecInvalidAttributeRounds: CInt = -67768
  val errSecMissingAttributeRounds: CInt = -67769
  val errSecInvalidAlgorithmParms: CInt = -67770
  val errSecMissingAlgorithmParms: CInt = -67771
  val errSecInvalidAttributeLabel: CInt = -67772
  val errSecMissingAttributeLabel: CInt = -67773
  val errSecInvalidAttributeKeyType: CInt = -67774
  val errSecMissingAttributeKeyType: CInt = -67775
  val errSecInvalidAttributeMode: CInt = -67776
  val errSecMissingAttributeMode: CInt = -67777
  val errSecInvalidAttributeEffectiveBits: CInt = -67778
  val errSecMissingAttributeEffectiveBits: CInt = -67779
  val errSecInvalidAttributeStartDate: CInt = -67780
  val errSecMissingAttributeStartDate: CInt = -67781
  val errSecInvalidAttributeEndDate: CInt = -67782
  val errSecMissingAttributeEndDate: CInt = -67783
  val errSecInvalidAttributeVersion: CInt = -67784
  val errSecMissingAttributeVersion: CInt = -67785
  val errSecInvalidAttributePrime: CInt = -67786
  val errSecMissingAttributePrime: CInt = -67787
  val errSecInvalidAttributeBase: CInt = -67788
  val errSecMissingAttributeBase: CInt = -67789
  val errSecInvalidAttributeSubprime: CInt = -67790
  val errSecMissingAttributeSubprime: CInt = -67791
  val errSecInvalidAttributeIterationCount: CInt = -67792
  val errSecMissingAttributeIterationCount: CInt = -67793
  val errSecInvalidAttributeDLDBHandle: CInt = -67794
  val errSecMissingAttributeDLDBHandle: CInt = -67795
  val errSecInvalidAttributeAccessCredentials: CInt = -67796
  val errSecMissingAttributeAccessCredentials: CInt = -67797
  val errSecInvalidAttributePublicKeyFormat: CInt = -67798
  val errSecMissingAttributePublicKeyFormat: CInt = -67799
  val errSecInvalidAttributePrivateKeyFormat: CInt = -67800
  val errSecMissingAttributePrivateKeyFormat: CInt = -67801
  val errSecInvalidAttributeSymmetricKeyFormat: CInt = -67802
  val errSecMissingAttributeSymmetricKeyFormat: CInt = -67803
  val errSecInvalidAttributeWrappedKeyFormat: CInt = -67804
  val errSecMissingAttributeWrappedKeyFormat: CInt = -67805
  val errSecStagedOperationInProgress: CInt = -67806
  val errSecStagedOperationNotStarted: CInt = -67807
  val errSecVerifyFailed: CInt = -67808
  val errSecQuerySizeUnknown: CInt = -67809
  val errSecBlockSizeMismatch: CInt = -67810
  val errSecPublicKeyInconsistent: CInt = -67811
  val errSecDeviceVerifyFailed: CInt = -67812
  val errSecInvalidLoginName: CInt = -67813
  val errSecAlreadyLoggedIn: CInt = -67814
  val errSecInvalidDigestAlgorithm: CInt = -67815
  val errSecInvalidCRLGroup: CInt = -67816
  val errSecCertificateCannotOperate: CInt = -67817
  val errSecCertificateExpired: CInt = -67818
  val errSecCertificateNotValidYet: CInt = -67819
  val errSecCertificateRevoked: CInt = -67820
  val errSecCertificateSuspended: CInt = -67821
  val errSecInsufficientCredentials: CInt = -67822
  val errSecInvalidAction: CInt = -67823
  val errSecInvalidAuthority: CInt = -67824
  val errSecVerifyActionFailed: CInt = -67825
  val errSecInvalidCertAuthority: CInt = -67826
  val errSecInvalidCRLAuthority: CInt = -67827
  val errSecInvaldCRLAuthority: CInt = -67827
  val errSecInvalidCRLEncoding: CInt = -67828
  val errSecInvalidCRLType: CInt = -67829
  val errSecInvalidCRL: CInt = -67830
  val errSecInvalidFormType: CInt = -67831
  val errSecInvalidID: CInt = -67832
  val errSecInvalidIdentifier: CInt = -67833
  val errSecInvalidIndex: CInt = -67834
  val errSecInvalidPolicyIdentifiers: CInt = -67835
  val errSecInvalidTimeString: CInt = -67836
  val errSecInvalidReason: CInt = -67837
  val errSecInvalidRequestInputs: CInt = -67838
  val errSecInvalidResponseVector: CInt = -67839
  val errSecInvalidStopOnPolicy: CInt = -67840
  val errSecInvalidTuple: CInt = -67841
  val errSecMultipleValuesUnsupported: CInt = -67842
  val errSecNotTrusted: CInt = -67843
  val errSecNoDefaultAuthority: CInt = -67844
  val errSecRejectedForm: CInt = -67845
  val errSecRequestLost: CInt = -67846
  val errSecRequestRejected: CInt = -67847
  val errSecUnsupportedAddressType: CInt = -67848
  val errSecUnsupportedService: CInt = -67849
  val errSecInvalidTupleGroup: CInt = -67850
  val errSecInvalidBaseACLs: CInt = -67851
  val errSecInvalidTupleCredentials: CInt = -67852
  val errSecInvalidTupleCredendtials: CInt = -67852
  val errSecInvalidEncoding: CInt = -67853
  val errSecInvalidValidityPeriod: CInt = -67854
  val errSecInvalidRequestor: CInt = -67855
  val errSecRequestDescriptor: CInt = -67856
  val errSecInvalidBundleInfo: CInt = -67857
  val errSecInvalidCRLIndex: CInt = -67858
  val errSecNoFieldValues: CInt = -67859
  val errSecUnsupportedFieldFormat: CInt = -67860
  val errSecUnsupportedIndexInfo: CInt = -67861
  val errSecUnsupportedLocality: CInt = -67862
  val errSecUnsupportedNumAttributes: CInt = -67863
  val errSecUnsupportedNumIndexes: CInt = -67864
  val errSecUnsupportedNumRecordTypes: CInt = -67865
  val errSecFieldSpecifiedMultiple: CInt = -67866
  val errSecIncompatibleFieldFormat: CInt = -67867
  val errSecInvalidParsingModule: CInt = -67868
  val errSecDatabaseLocked: CInt = -67869
  val errSecDatastoreIsOpen: CInt = -67870
  val errSecMissingValue: CInt = -67871
  val errSecUnsupportedQueryLimits: CInt = -67872
  val errSecUnsupportedNumSelectionPreds: CInt = -67873
  val errSecUnsupportedOperator: CInt = -67874
  val errSecInvalidDBLocation: CInt = -67875
  val errSecInvalidAccessRequest: CInt = -67876
  val errSecInvalidIndexInfo: CInt = -67877
  val errSecInvalidNewOwner: CInt = -67878
  val errSecInvalidModifyMode: CInt = -67879
  val errSecMissingRequiredExtension: CInt = -67880
  val errSecExtendedKeyUsageNotCritical: CInt = -67881
  val errSecTimestampMissing: CInt = -67882
  val errSecTimestampInvalid: CInt = -67883
  val errSecTimestampNotTrusted: CInt = -67884
  val errSecTimestampServiceNotAvailable: CInt = -67885
  val errSecTimestampBadAlg: CInt = -67886
  val errSecTimestampBadRequest: CInt = -67887
  val errSecTimestampBadDataFormat: CInt = -67888
  val errSecTimestampTimeNotAvailable: CInt = -67889
  val errSecTimestampUnacceptedPolicy: CInt = -67890
  val errSecTimestampUnacceptedExtension: CInt = -67891
  val errSecTimestampAddInfoNotAvailable: CInt = -67892
  val errSecTimestampSystemFailure: CInt = -67893
  val errSecSigningTimeMissing: CInt = -67894
  val errSecTimestampRejection: CInt = -67895
  val errSecTimestampWaiting: CInt = -67896
  val errSecTimestampRevocationWarning: CInt = -67897
  val errSecTimestampRevocationNotification: CInt = -67898
  val errSecCertificatePolicyNotAllowed: CInt = -67899
  val errSecCertificateNameNotAllowed: CInt = -67900
  val errSecCertificateValidityPeriodTooLong: CInt = -67901
  val errSecCertificateIsCA: CInt = -67902
  val errSecCertificateDuplicateExtension: CInt = -67903
  val errSecMissingQualifiedCertStatement: CInt = -67904
  
  val kCFStringNormalizationFormD: CUnsignedInt = 0.toUInt
  val kCFStringNormalizationFormKD: CUnsignedInt = 1.toUInt
  val kCFStringNormalizationFormC: CUnsignedInt = 2.toUInt
  val kCFStringNormalizationFormKC: CUnsignedInt = 3.toUInt
  
  val kCFNumberSInt8Type: CUnsignedInt = 1.toUInt
  val kCFNumberSInt16Type: CUnsignedInt = 2.toUInt
  val kCFNumberSInt32Type: CUnsignedInt = 3.toUInt
  val kCFNumberSInt64Type: CUnsignedInt = 4.toUInt
  val kCFNumberFloat32Type: CUnsignedInt = 5.toUInt
  val kCFNumberFloat64Type: CUnsignedInt = 6.toUInt
  val kCFNumberCharType: CUnsignedInt = 7.toUInt
  val kCFNumberShortType: CUnsignedInt = 8.toUInt
  val kCFNumberIntType: CUnsignedInt = 9.toUInt
  val kCFNumberLongType: CUnsignedInt = 10.toUInt
  val kCFNumberLongLongType: CUnsignedInt = 11.toUInt
  val kCFNumberFloatType: CUnsignedInt = 12.toUInt
  val kCFNumberDoubleType: CUnsignedInt = 13.toUInt
  val kCFNumberCFIndexType: CUnsignedInt = 14.toUInt
  val kCFNumberNSIntegerType: CUnsignedInt = 15.toUInt
  val kCFNumberCGFloatType: CUnsignedInt = 16.toUInt
  val kCFNumberMaxType: CUnsignedInt = 16.toUInt
  
object types:
  export _root_.macos.structs.*
  export _root_.macos.aliases.*

object all:
  export _root_.macos.aliases.Boolean
  export _root_.macos.aliases.CFAllocatorRef
  export _root_.macos.aliases.CFArrayRef
  export _root_.macos.aliases.CFBooleanRef
  export _root_.macos.aliases.CFCharacterSetRef
  export _root_.macos.aliases.CFComparisonResult
  export _root_.macos.aliases.CFDataRef
  export _root_.macos.aliases.CFDictionaryApplierFunction
  export _root_.macos.aliases.CFDictionaryCopyDescriptionCallBack
  export _root_.macos.aliases.CFDictionaryEqualCallBack
  export _root_.macos.aliases.CFDictionaryHashCallBack
  export _root_.macos.aliases.CFDictionaryRef
  export _root_.macos.aliases.CFDictionaryReleaseCallBack
  export _root_.macos.aliases.CFDictionaryRetainCallBack
  export _root_.macos.aliases.CFErrorRef
  export _root_.macos.aliases.CFHashCode
  export _root_.macos.aliases.CFIndex
  export _root_.macos.aliases.CFLocaleRef
  export _root_.macos.aliases.CFMutableDictionaryRef
  export _root_.macos.aliases.CFMutableStringRef
  export _root_.macos.aliases.CFNumberRef
  export _root_.macos.aliases.CFNumberType
  export _root_.macos.aliases.CFOptionFlags
  export _root_.macos.aliases.CFStringBuiltInEncodings
  export _root_.macos.aliases.CFStringCompareFlags
  export _root_.macos.aliases.CFStringEncoding
  export _root_.macos.aliases.CFStringNormalizationForm
  export _root_.macos.aliases.CFStringRef
  export _root_.macos.aliases.CFTypeID
  export _root_.macos.aliases.CFTypeRef
  export _root_.macos.aliases.ConstStr255Param
  export _root_.macos.aliases.ConstStringPtr
  export _root_.macos.aliases.FourCharCode
  export _root_.macos.aliases.OSStatus
  export _root_.macos.aliases.OSType
  export _root_.macos.aliases.SInt32
  export _root_.macos.aliases.SecACLRef
  export _root_.macos.aliases.SecAccessControlRef
  export _root_.macos.aliases.SecAccessRef
  export _root_.macos.aliases.SecCertificateRef
  export _root_.macos.aliases.SecIdentityRef
  export _root_.macos.aliases.SecKeyRef
  export _root_.macos.aliases.SecKeychainAttrType
  export _root_.macos.aliases.SecKeychainAttributePtr
  export _root_.macos.aliases.SecKeychainItemRef
  export _root_.macos.aliases.SecKeychainRef
  export _root_.macos.aliases.SecKeychainSearchRef
  export _root_.macos.aliases.SecKeychainStatus
  export _root_.macos.aliases.SecPasswordRef
  export _root_.macos.aliases.SecPolicyRef
  export _root_.macos.aliases.SecTrustedApplicationRef
  export _root_.macos.aliases.StringPtr
  export _root_.macos.aliases.UInt16
  export _root_.macos.aliases.UInt32
  export _root_.macos.aliases.UInt8
  export _root_.macos.aliases.UTF32Char
  export _root_.macos.aliases.UniChar
  export _root_.macos.aliases.va_list
  export _root_.macos.structs.CFDictionaryKeyCallBacks
  export _root_.macos.structs.CFDictionaryValueCallBacks
  export _root_.macos.structs.CFRange
  export _root_.macos.structs.CFStringInlineBuffer
  export _root_.macos.structs.OpaqueSecAccessRef
  export _root_.macos.structs.OpaqueSecCertificateRef
  export _root_.macos.structs.OpaqueSecIdentityRef
  export _root_.macos.structs.OpaqueSecKeyRef
  export _root_.macos.structs.SecKeychainAttribute
  export _root_.macos.structs.SecKeychainAttributeInfo
  export _root_.macos.structs.SecKeychainAttributeList
  export _root_.macos.structs.__CFAllocator
  export _root_.macos.structs.__CFArray
  export _root_.macos.structs.__CFBoolean
  export _root_.macos.structs.__CFCharacterSet
  export _root_.macos.structs.__CFData
  export _root_.macos.structs.__CFDictionary
  export _root_.macos.structs.__CFError
  export _root_.macos.structs.__CFLocale
  export _root_.macos.structs.__CFNumber
  export _root_.macos.structs.__CFString
  export _root_.macos.structs.__SecACL
  export _root_.macos.structs.__SecAccess
  export _root_.macos.structs.__SecAccessControl
  export _root_.macos.structs.__SecCertificate
  export _root_.macos.structs.__SecIdentity
  export _root_.macos.structs.__SecKey
  export _root_.macos.structs.__SecKeychain
  export _root_.macos.structs.__SecKeychainItem
  export _root_.macos.structs.__SecKeychainSearch
  export _root_.macos.structs.__SecPassword
  export _root_.macos.structs.__SecPolicy
  export _root_.macos.structs.__SecTrustedApplication
  export _root_.macos.functions.CFBooleanGetTypeID
  export _root_.macos.functions.CFBooleanGetValue
  export _root_.macos.functions.CFDictionaryAddValue
  export _root_.macos.functions.CFDictionaryApplyFunction
  export _root_.macos.functions.CFDictionaryContainsKey
  export _root_.macos.functions.CFDictionaryContainsValue
  export _root_.macos.functions.CFDictionaryCreate
  export _root_.macos.functions.CFDictionaryCreateCopy
  export _root_.macos.functions.CFDictionaryCreateMutable
  export _root_.macos.functions.CFDictionaryCreateMutableCopy
  export _root_.macos.functions.CFDictionaryGetCount
  export _root_.macos.functions.CFDictionaryGetCountOfKey
  export _root_.macos.functions.CFDictionaryGetCountOfValue
  export _root_.macos.functions.CFDictionaryGetKeysAndValues
  export _root_.macos.functions.CFDictionaryGetTypeID
  export _root_.macos.functions.CFDictionaryGetValue
  export _root_.macos.functions.CFDictionaryGetValueIfPresent
  export _root_.macos.functions.CFDictionaryRemoveAllValues
  export _root_.macos.functions.CFDictionaryRemoveValue
  export _root_.macos.functions.CFDictionaryReplaceValue
  export _root_.macos.functions.CFDictionarySetValue
  export _root_.macos.functions.CFNumberCompare
  export _root_.macos.functions.CFNumberCreate
  export _root_.macos.functions.CFNumberGetByteSize
  export _root_.macos.functions.CFNumberGetType
  export _root_.macos.functions.CFNumberGetTypeID
  export _root_.macos.functions.CFNumberGetValue
  export _root_.macos.functions.CFNumberIsFloatType
  export _root_.macos.functions.CFShow
  export _root_.macos.functions.CFShowStr
  export _root_.macos.functions.CFStringAppend
  export _root_.macos.functions.CFStringAppendCString
  export _root_.macos.functions.CFStringAppendCharacters
  export _root_.macos.functions.CFStringAppendFormat
  export _root_.macos.functions.CFStringAppendFormatAndArguments
  export _root_.macos.functions.CFStringAppendPascalString
  export _root_.macos.functions.CFStringCapitalize
  export _root_.macos.functions.CFStringCompare
  export _root_.macos.functions.CFStringConvertEncodingToIANACharSetName
  export _root_.macos.functions.CFStringConvertEncodingToNSStringEncoding
  export _root_.macos.functions.CFStringConvertEncodingToWindowsCodepage
  export _root_.macos.functions.CFStringConvertIANACharSetNameToEncoding
  export _root_.macos.functions.CFStringConvertNSStringEncodingToEncoding
  export _root_.macos.functions.CFStringConvertWindowsCodepageToEncoding
  export _root_.macos.functions.CFStringCreateArrayBySeparatingStrings
  export _root_.macos.functions.CFStringCreateByCombiningStrings
  export _root_.macos.functions.CFStringCreateCopy
  export _root_.macos.functions.CFStringCreateExternalRepresentation
  export _root_.macos.functions.CFStringCreateFromExternalRepresentation
  export _root_.macos.functions.CFStringCreateMutable
  export _root_.macos.functions.CFStringCreateMutableCopy
  export _root_.macos.functions.CFStringCreateMutableWithExternalCharactersNoCopy
  export _root_.macos.functions.CFStringCreateStringWithValidatedFormat
  export _root_.macos.functions.CFStringCreateStringWithValidatedFormatAndArguments
  export _root_.macos.functions.CFStringCreateWithBytes
  export _root_.macos.functions.CFStringCreateWithBytesNoCopy
  export _root_.macos.functions.CFStringCreateWithCString
  export _root_.macos.functions.CFStringCreateWithCStringNoCopy
  export _root_.macos.functions.CFStringCreateWithCharacters
  export _root_.macos.functions.CFStringCreateWithCharactersNoCopy
  export _root_.macos.functions.CFStringCreateWithFileSystemRepresentation
  export _root_.macos.functions.CFStringCreateWithFormat
  export _root_.macos.functions.CFStringCreateWithFormatAndArguments
  export _root_.macos.functions.CFStringCreateWithPascalString
  export _root_.macos.functions.CFStringCreateWithPascalStringNoCopy
  export _root_.macos.functions.CFStringFold
  export _root_.macos.functions.CFStringGetCString
  export _root_.macos.functions.CFStringGetCStringPtr
  export _root_.macos.functions.CFStringGetCharacterAtIndex
  export _root_.macos.functions.CFStringGetCharacterFromInlineBuffer
  export _root_.macos.functions.CFStringGetCharactersPtr
  export _root_.macos.functions.CFStringGetDoubleValue
  export _root_.macos.functions.CFStringGetFastestEncoding
  export _root_.macos.functions.CFStringGetFileSystemRepresentation
  export _root_.macos.functions.CFStringGetIntValue
  export _root_.macos.functions.CFStringGetLength
  export _root_.macos.functions.CFStringGetListOfAvailableEncodings
  export _root_.macos.functions.CFStringGetLongCharacterForSurrogatePair
  export _root_.macos.functions.CFStringGetMaximumSizeForEncoding
  export _root_.macos.functions.CFStringGetMaximumSizeOfFileSystemRepresentation
  export _root_.macos.functions.CFStringGetMostCompatibleMacStringEncoding
  export _root_.macos.functions.CFStringGetNameOfEncoding
  export _root_.macos.functions.CFStringGetPascalString
  export _root_.macos.functions.CFStringGetPascalStringPtr
  export _root_.macos.functions.CFStringGetSmallestEncoding
  export _root_.macos.functions.CFStringGetSurrogatePairForLongCharacter
  export _root_.macos.functions.CFStringGetSystemEncoding
  export _root_.macos.functions.CFStringGetTypeID
  export _root_.macos.functions.CFStringHasPrefix
  export _root_.macos.functions.CFStringHasSuffix
  export _root_.macos.functions.CFStringInsert
  export _root_.macos.functions.CFStringIsEncodingAvailable
  export _root_.macos.functions.CFStringIsHyphenationAvailableForLocale
  export _root_.macos.functions.CFStringIsSurrogateHighCharacter
  export _root_.macos.functions.CFStringIsSurrogateLowCharacter
  export _root_.macos.functions.CFStringLowercase
  export _root_.macos.functions.CFStringNormalize
  export _root_.macos.functions.CFStringPad
  export _root_.macos.functions.CFStringReplaceAll
  export _root_.macos.functions.CFStringSetExternalCharactersNoCopy
  export _root_.macos.functions.CFStringTransform
  export _root_.macos.functions.CFStringTrim
  export _root_.macos.functions.CFStringTrimWhitespace
  export _root_.macos.functions.CFStringUppercase
  export _root_.macos.functions.SecCopyErrorMessageString
  export _root_.macos.functions.SecItemAdd
  export _root_.macos.functions.SecItemCopyMatching
  export _root_.macos.functions.SecItemDelete
  export _root_.macos.functions.SecItemUpdate
  export _root_.macos.functions.__CFStringMakeConstantString
  export _root_.macos.functions.CFStringCompareWithOptions
  export _root_.macos.functions.CFStringCompareWithOptionsAndLocale
  export _root_.macos.functions.CFStringCreateArrayWithFindResults
  export _root_.macos.functions.CFStringCreateWithSubstring
  export _root_.macos.functions.CFStringDelete
  export _root_.macos.functions.CFStringFind
  export _root_.macos.functions.CFStringFindAndReplace
  export _root_.macos.functions.CFStringFindCharacterFromSet
  export _root_.macos.functions.CFStringFindWithOptions
  export _root_.macos.functions.CFStringFindWithOptionsAndLocale
  export _root_.macos.functions.CFStringGetBytes
  export _root_.macos.functions.CFStringGetCharacters
  export _root_.macos.functions.CFStringGetHyphenationLocationBeforeIndex
  export _root_.macos.functions.CFStringGetLineBounds
  export _root_.macos.functions.CFStringGetParagraphBounds
  export _root_.macos.functions.CFStringGetRangeOfComposedCharactersAtIndex
  export _root_.macos.functions.CFStringInitInlineBuffer
  export _root_.macos.functions.CFStringReplace