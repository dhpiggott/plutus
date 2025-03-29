package cfnumber

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object aliases:
  import _root_.cfnumber.aliases.*
  import _root_.cfnumber.structs.*
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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type CFBooleanRef = Ptr[__CFBoolean]
  object CFBooleanRef: 
    given _tag: Tag[CFBooleanRef] = Tag.Ptr[__CFBoolean](__CFBoolean._tag)
    inline def apply(inline o: Ptr[__CFBoolean]): CFBooleanRef = o
    extension (v: CFBooleanRef)
      inline def value: Ptr[__CFBoolean] = v

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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type CFIndex = CLongInt
  object CFIndex: 
    given _tag: Tag[CFIndex] = Tag.Long
    inline def apply(inline o: CLongInt): CFIndex = o
    extension (v: CFIndex)
      inline def value: CLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type CFNumberRef = Ptr[__CFNumber]
  object CFNumberRef: 
    given _tag: Tag[CFNumberRef] = Tag.Ptr[__CFNumber](__CFNumber._tag)
    inline def apply(inline o: Ptr[__CFNumber]): CFNumberRef = o
    extension (v: CFNumberRef)
      inline def value: Ptr[__CFNumber] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
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
  opaque type CFTypeID = CUnsignedLongInt
  object CFTypeID: 
    given _tag: Tag[CFTypeID] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFTypeID = o
    extension (v: CFTypeID)
      inline def value: CUnsignedLongInt = v

object structs:
  import _root_.cfnumber.aliases.*
  import _root_.cfnumber.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type __CFAllocator = CStruct0
  object __CFAllocator:
    given _tag: Tag[__CFAllocator] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type __CFBoolean = CStruct0
  object __CFBoolean:
    given _tag: Tag[__CFBoolean] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  opaque type __CFNumber = CStruct0
  object __CFNumber:
    given _tag: Tag[__CFNumber] = Tag.materializeCStruct0Tag


@extern
private[cfnumber] object extern_functions:
  import _root_.cfnumber.aliases.*
  import _root_.cfnumber.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFBooleanGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFBooleanGetValue(boolean : CFBooleanRef): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberCompare(number : CFNumberRef, otherNumber : CFNumberRef, context : Ptr[Byte]): CFComparisonResult = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberCreate(allocator : CFAllocatorRef, theType : CFNumberType, valuePtr : Ptr[Byte]): CFNumberRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetByteSize(number : CFNumberRef): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetType(number : CFNumberRef): CFNumberType = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberGetValue(number : CFNumberRef, theType : CFNumberType, valuePtr : Ptr[Byte]): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFNumber.h
  */
  def CFNumberIsFloatType(number : CFNumberRef): Boolean = extern


object functions:
  import _root_.cfnumber.aliases.*
  import _root_.cfnumber.structs.*
  import extern_functions.*
  export extern_functions.*

object constants:
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
  export _root_.cfnumber.structs.*
  export _root_.cfnumber.aliases.*

object all:
  export _root_.cfnumber.aliases.Boolean
  export _root_.cfnumber.aliases.CFAllocatorRef
  export _root_.cfnumber.aliases.CFBooleanRef
  export _root_.cfnumber.aliases.CFComparisonResult
  export _root_.cfnumber.aliases.CFIndex
  export _root_.cfnumber.aliases.CFNumberRef
  export _root_.cfnumber.aliases.CFNumberType
  export _root_.cfnumber.aliases.CFTypeID
  export _root_.cfnumber.structs.__CFAllocator
  export _root_.cfnumber.structs.__CFBoolean
  export _root_.cfnumber.structs.__CFNumber
  export _root_.cfnumber.functions.CFBooleanGetTypeID
  export _root_.cfnumber.functions.CFBooleanGetValue
  export _root_.cfnumber.functions.CFNumberCompare
  export _root_.cfnumber.functions.CFNumberCreate
  export _root_.cfnumber.functions.CFNumberGetByteSize
  export _root_.cfnumber.functions.CFNumberGetType
  export _root_.cfnumber.functions.CFNumberGetTypeID
  export _root_.cfnumber.functions.CFNumberGetValue
  export _root_.cfnumber.functions.CFNumberIsFloatType

// TODO:
@extern
object CFNumber:
  def plutusKCFBooleanTrue: cfnumber.aliases.CFBooleanRef = extern
