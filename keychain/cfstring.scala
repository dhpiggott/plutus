package cfstring

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object aliases:
  import _root_.cfstring.aliases.*
  import _root_.cfstring.structs.*

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type Boolean = CUnsignedChar
  object Boolean:
    given _tag: Tag[Boolean] = Tag.UByte
    inline def apply(inline o: CUnsignedChar): Boolean = o
    extension (v: Boolean) inline def value: CUnsignedChar = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFAllocatorRef = Ptr[__CFAllocator]
  object CFAllocatorRef:
    given _tag: Tag[CFAllocatorRef] = Tag.Ptr[__CFAllocator](__CFAllocator._tag)
    inline def apply(inline o: Ptr[__CFAllocator]): CFAllocatorRef = o
    extension (v: CFAllocatorRef) inline def value: Ptr[__CFAllocator] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFArray.h
    */
  opaque type CFArrayRef = Ptr[__CFArray]
  object CFArrayRef:
    given _tag: Tag[CFArrayRef] = Tag.Ptr[__CFArray](__CFArray._tag)
    inline def apply(inline o: Ptr[__CFArray]): CFArrayRef = o
    extension (v: CFArrayRef) inline def value: Ptr[__CFArray] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFCharacterSet.h
    */
  opaque type CFCharacterSetRef = Ptr[__CFCharacterSet]
  object CFCharacterSetRef:
    given _tag: Tag[CFCharacterSetRef] =
      Tag.Ptr[__CFCharacterSet](__CFCharacterSet._tag)
    inline def apply(inline o: Ptr[__CFCharacterSet]): CFCharacterSetRef = o
    extension (v: CFCharacterSetRef) inline def value: Ptr[__CFCharacterSet] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  type CFComparisonResult = CFIndex
  object CFComparisonResult:
    given _tag: Tag[CFComparisonResult] = CFIndex._tag
    inline def apply(inline o: CFIndex): CFComparisonResult = o
    extension (v: CFComparisonResult) inline def value: CFIndex = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFData.h
    */
  opaque type CFDataRef = Ptr[__CFData]
  object CFDataRef:
    given _tag: Tag[CFDataRef] = Tag.Ptr[__CFData](__CFData._tag)
    inline def apply(inline o: Ptr[__CFData]): CFDataRef = o
    extension (v: CFDataRef) inline def value: Ptr[__CFData] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFDictionary.h
    */
  opaque type CFDictionaryRef = Ptr[__CFDictionary]
  object CFDictionaryRef:
    given _tag: Tag[CFDictionaryRef] =
      Tag.Ptr[__CFDictionary](__CFDictionary._tag)
    inline def apply(inline o: Ptr[__CFDictionary]): CFDictionaryRef = o
    extension (v: CFDictionaryRef) inline def value: Ptr[__CFDictionary] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFError.h
    */
  opaque type CFErrorRef = Ptr[__CFError]
  object CFErrorRef:
    given _tag: Tag[CFErrorRef] = Tag.Ptr[__CFError](__CFError._tag)
    inline def apply(inline o: Ptr[__CFError]): CFErrorRef = o
    extension (v: CFErrorRef) inline def value: Ptr[__CFError] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFIndex = CLongInt
  object CFIndex:
    given _tag: Tag[CFIndex] = Tag.Long
    inline def apply(inline o: CLongInt): CFIndex = o
    extension (v: CFIndex) inline def value: CLongInt = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFLocale.h
    */
  opaque type CFLocaleRef = Ptr[__CFLocale]
  object CFLocaleRef:
    given _tag: Tag[CFLocaleRef] = Tag.Ptr[__CFLocale](__CFLocale._tag)
    inline def apply(inline o: Ptr[__CFLocale]): CFLocaleRef = o
    extension (v: CFLocaleRef) inline def value: Ptr[__CFLocale] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFMutableStringRef = Ptr[__CFString]
  object CFMutableStringRef:
    given _tag: Tag[CFMutableStringRef] = Tag.Ptr[__CFString](__CFString._tag)
    inline def apply(inline o: Ptr[__CFString]): CFMutableStringRef = o
    extension (v: CFMutableStringRef) inline def value: Ptr[__CFString] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFOptionFlags = CUnsignedLongInt
  object CFOptionFlags:
    given _tag: Tag[CFOptionFlags] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFOptionFlags = o
    extension (v: CFOptionFlags) inline def value: CUnsignedLongInt = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  type CFStringBuiltInEncodings = CFStringEncoding
  object CFStringBuiltInEncodings:
    given _tag: Tag[CFStringBuiltInEncodings] = CFStringEncoding._tag
    inline def apply(inline o: CFStringEncoding): CFStringBuiltInEncodings = o
    extension (v: CFStringBuiltInEncodings)
      inline def value: CFStringEncoding = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  type CFStringCompareFlags = CFOptionFlags
  object CFStringCompareFlags:
    given _tag: Tag[CFStringCompareFlags] = CFOptionFlags._tag
    inline def apply(inline o: CFOptionFlags): CFStringCompareFlags = o
    extension (v: CFStringCompareFlags) inline def value: CFOptionFlags = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  type CFStringEncoding = UInt32
  object CFStringEncoding:
    given _tag: Tag[CFStringEncoding] = UInt32._tag
    inline def apply(inline o: UInt32): CFStringEncoding = o
    extension (v: CFStringEncoding) inline def value: UInt32 = v

  /** This is the type of Unicode normalization forms as described in Unicode
    * Technical Report #15. To normalize for use with file system calls, use
    * CFStringGetFileSystemRepresentation().
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  type CFStringNormalizationForm = CFIndex
  object CFStringNormalizationForm:
    given _tag: Tag[CFStringNormalizationForm] = CFIndex._tag
    inline def apply(inline o: CFIndex): CFStringNormalizationForm = o
    extension (v: CFStringNormalizationForm) inline def value: CFIndex = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFStringRef = Ptr[__CFString]
  object CFStringRef:
    given _tag: Tag[CFStringRef] = Tag.Ptr[__CFString](__CFString._tag)
    inline def apply(inline o: Ptr[__CFString]): CFStringRef = o
    extension (v: CFStringRef) inline def value: Ptr[__CFString] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFTypeID = CUnsignedLongInt
  object CFTypeID:
    given _tag: Tag[CFTypeID] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFTypeID = o
    extension (v: CFTypeID) inline def value: CUnsignedLongInt = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFTypeRef = Ptr[Byte]
  object CFTypeRef:
    given _tag: Tag[CFTypeRef] = Tag.Ptr(Tag.Byte)
    inline def apply(inline o: Ptr[Byte]): CFTypeRef = o
    extension (v: CFTypeRef) inline def value: Ptr[Byte] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type ConstStr255Param = Ptr[CUnsignedChar]
  object ConstStr255Param:
    given _tag: Tag[ConstStr255Param] = Tag.Ptr[CUnsignedChar](Tag.UByte)
    inline def apply(inline o: Ptr[CUnsignedChar]): ConstStr255Param = o
    extension (v: ConstStr255Param) inline def value: Ptr[CUnsignedChar] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type ConstStringPtr = Ptr[CUnsignedChar]
  object ConstStringPtr:
    given _tag: Tag[ConstStringPtr] = Tag.Ptr[CUnsignedChar](Tag.UByte)
    inline def apply(inline o: Ptr[CUnsignedChar]): ConstStringPtr = o
    extension (v: ConstStringPtr) inline def value: Ptr[CUnsignedChar] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type SInt32 = CInt
  object SInt32:
    given _tag: Tag[SInt32] = Tag.Int
    inline def apply(inline o: CInt): SInt32 = o
    extension (v: SInt32) inline def value: CInt = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type StringPtr = Ptr[CUnsignedChar]
  object StringPtr:
    given _tag: Tag[StringPtr] = Tag.Ptr[CUnsignedChar](Tag.UByte)
    inline def apply(inline o: Ptr[CUnsignedChar]): StringPtr = o
    extension (v: StringPtr) inline def value: Ptr[CUnsignedChar] = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type UInt16 = CUnsignedShort
  object UInt16:
    given _tag: Tag[UInt16] = Tag.UShort
    inline def apply(inline o: CUnsignedShort): UInt16 = o
    extension (v: UInt16) inline def value: CUnsignedShort = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type UInt32 = CUnsignedInt
  object UInt32:
    given _tag: Tag[UInt32] = Tag.UInt
    inline def apply(inline o: CUnsignedInt): UInt32 = o
    extension (v: UInt32) inline def value: CUnsignedInt = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  opaque type UInt8 = CUnsignedChar
  object UInt8:
    given _tag: Tag[UInt8] = Tag.UByte
    inline def apply(inline o: CUnsignedChar): UInt8 = o
    extension (v: UInt8) inline def value: CUnsignedChar = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  type UTF32Char = UInt32
  object UTF32Char:
    given _tag: Tag[UTF32Char] = UInt32._tag
    inline def apply(inline o: UInt32): UTF32Char = o
    extension (v: UTF32Char) inline def value: UInt32 = v

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/usr/include/MacTypes.h
    */
  type UniChar = UInt16
  object UniChar:
    given _tag: Tag[UniChar] = UInt16._tag
    inline def apply(inline o: UInt16): UniChar = o
    extension (v: UniChar) inline def value: UInt16 = v

  type va_list = unsafe.CVarArgList
  object va_list:
    val _tag: Tag[va_list] = summon[Tag[unsafe.CVarArgList]]
    inline def apply(inline o: unsafe.CVarArgList): va_list = o
    extension (v: va_list) inline def value: unsafe.CVarArgList = v

object structs:
  import _root_.cfstring.aliases.*
  import _root_.cfstring.structs.*

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type CFRange = CStruct2[CFIndex, CFIndex]
  object CFRange:
    given _tag: Tag[CFRange] = Tag.materializeCStruct2Tag[CFIndex, CFIndex]
    def apply()(using Zone): Ptr[CFRange] =
      scala.scalanative.unsafe.alloc[CFRange](1)
    def apply(location: CFIndex, length: CFIndex)(using Zone): Ptr[CFRange] =
      val ____ptr = apply()
      (!____ptr).location = location
      (!____ptr).length = length
      ____ptr
    extension (struct: CFRange)
      def location: CFIndex = struct._1
      def location_=(value: CFIndex): Unit = !struct.at1 = value
      def length: CFIndex = struct._2
      def length_=(value: CFIndex): Unit = !struct.at2 = value

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  opaque type CFStringInlineBuffer =
    CStruct7[CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]], CFStringRef, Ptr[
      UniChar
    ], CString, CFRange, CFIndex, CFIndex]
  object CFStringInlineBuffer:
    given _tag: Tag[CFStringInlineBuffer] = Tag.materializeCStruct7Tag[CArray[
      UniChar,
      Nat.Digit2[Nat._6, Nat._4]
    ], CFStringRef, Ptr[UniChar], CString, CFRange, CFIndex, CFIndex]
    def apply()(using Zone): Ptr[CFStringInlineBuffer] =
      scala.scalanative.unsafe.alloc[CFStringInlineBuffer](1)
    def apply(
        buffer: CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]],
        theString: CFStringRef,
        directUniCharBuffer: Ptr[UniChar],
        directCStringBuffer: CString,
        rangeToBuffer: CFRange,
        bufferedRangeStart: CFIndex,
        bufferedRangeEnd: CFIndex
    )(using Zone): Ptr[CFStringInlineBuffer] =
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
      def buffer: CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]] = struct._1
      def buffer_=(value: CArray[UniChar, Nat.Digit2[Nat._6, Nat._4]]): Unit =
        !struct.at1 = value
      def theString: CFStringRef = struct._2
      def theString_=(value: CFStringRef): Unit = !struct.at2 = value
      def directUniCharBuffer: Ptr[UniChar] = struct._3
      def directUniCharBuffer_=(value: Ptr[UniChar]): Unit = !struct.at3 = value
      def directCStringBuffer: CString = struct._4
      def directCStringBuffer_=(value: CString): Unit = !struct.at4 = value
      def rangeToBuffer: CFRange = struct._5
      def rangeToBuffer_=(value: CFRange): Unit = !struct.at5 = value
      def bufferedRangeStart: CFIndex = struct._6
      def bufferedRangeStart_=(value: CFIndex): Unit = !struct.at6 = value
      def bufferedRangeEnd: CFIndex = struct._7
      def bufferedRangeEnd_=(value: CFIndex): Unit = !struct.at7 = value

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type __CFAllocator = CStruct0
  object __CFAllocator:
    given _tag: Tag[__CFAllocator] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFArray.h
    */
  opaque type __CFArray = CStruct0
  object __CFArray:
    given _tag: Tag[__CFArray] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFCharacterSet.h
    */
  opaque type __CFCharacterSet = CStruct0
  object __CFCharacterSet:
    given _tag: Tag[__CFCharacterSet] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFData.h
    */
  opaque type __CFData = CStruct0
  object __CFData:
    given _tag: Tag[__CFData] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFDictionary.h
    */
  opaque type __CFDictionary = CStruct0
  object __CFDictionary:
    given _tag: Tag[__CFDictionary] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFError.h
    */
  opaque type __CFError = CStruct0
  object __CFError:
    given _tag: Tag[__CFError] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFLocale.h
    */
  opaque type __CFLocale = CStruct0
  object __CFLocale:
    given _tag: Tag[__CFLocale] = Tag.materializeCStruct0Tag

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
    */
  opaque type __CFString = CStruct0
  object __CFString:
    given _tag: Tag[__CFString] = Tag.materializeCStruct0Tag

@extern
private[cfstring] object extern_functions:
  import _root_.cfstring.aliases.*
  import _root_.cfstring.structs.*

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFShow(obj: CFTypeRef): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFShowStr(str: CFStringRef): Unit = extern

  /** * MutableString functions **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringAppend(
      theString: CFMutableStringRef,
      appendedString: CFStringRef
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringAppendCString(
      theString: CFMutableStringRef,
      cStr: CString,
      encoding: CFStringEncoding
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringAppendCharacters(
      theString: CFMutableStringRef,
      chars: Ptr[UniChar],
      numChars: CFIndex
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringAppendFormat(
      theString: CFMutableStringRef,
      formatOptions: CFDictionaryRef,
      format: CFStringRef,
      rest: Any*
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringAppendFormatAndArguments(
      theString: CFMutableStringRef,
      formatOptions: CFDictionaryRef,
      format: CFStringRef,
      arguments: va_list
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringAppendPascalString(
      theString: CFMutableStringRef,
      pStr: ConstStr255Param,
      encoding: CFStringEncoding
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCapitalize(
      theString: CFMutableStringRef,
      locale: CFLocaleRef
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCompare(
      theString1: CFStringRef,
      theString2: CFStringRef,
      compareOptions: CFStringCompareFlags
  ): CFComparisonResult = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringConvertEncodingToIANACharSetName(
      encoding: CFStringEncoding
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringConvertEncodingToNSStringEncoding(
      encoding: CFStringEncoding
  ): CUnsignedLongInt = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringConvertEncodingToWindowsCodepage(
      encoding: CFStringEncoding
  ): UInt32 = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringConvertIANACharSetNameToEncoding(
      theString: CFStringRef
  ): CFStringEncoding = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringConvertNSStringEncodingToEncoding(
      encoding: CUnsignedLongInt
  ): CFStringEncoding = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringConvertWindowsCodepageToEncoding(
      codepage: UInt32
  ): CFStringEncoding = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateArrayBySeparatingStrings(
      alloc: CFAllocatorRef,
      theString: CFStringRef,
      separatorString: CFStringRef
  ): CFArrayRef = extern

  /** * Exploding and joining strings with a separator string **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateByCombiningStrings(
      alloc: CFAllocatorRef,
      theArray: CFArrayRef,
      separatorString: CFStringRef
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateCopy(
      alloc: CFAllocatorRef,
      theString: CFStringRef
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateExternalRepresentation(
      alloc: CFAllocatorRef,
      theString: CFStringRef,
      encoding: CFStringEncoding,
      lossByte: UInt8
  ): CFDataRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateFromExternalRepresentation(
      alloc: CFAllocatorRef,
      data: CFDataRef,
      encoding: CFStringEncoding
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateMutable(
      alloc: CFAllocatorRef,
      maxLength: CFIndex
  ): CFMutableStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateMutableCopy(
      alloc: CFAllocatorRef,
      maxLength: CFIndex,
      theString: CFStringRef
  ): CFMutableStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateMutableWithExternalCharactersNoCopy(
      alloc: CFAllocatorRef,
      chars: Ptr[UniChar],
      numChars: CFIndex,
      capacity: CFIndex,
      externalCharactersAllocator: CFAllocatorRef
  ): CFMutableStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateStringWithValidatedFormat(
      alloc: CFAllocatorRef,
      formatOptions: CFDictionaryRef,
      validFormatSpecifiers: CFStringRef,
      format: CFStringRef,
      errorPtr: Ptr[CFErrorRef],
      rest: Any*
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateStringWithValidatedFormatAndArguments(
      alloc: CFAllocatorRef,
      formatOptions: CFDictionaryRef,
      validFormatSpecifiers: CFStringRef,
      format: CFStringRef,
      arguments: va_list,
      errorPtr: Ptr[CFErrorRef]
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithBytes(
      alloc: CFAllocatorRef,
      bytes: Ptr[UInt8],
      numBytes: CFIndex,
      encoding: CFStringEncoding,
      isExternalRepresentation: Boolean
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithBytesNoCopy(
      alloc: CFAllocatorRef,
      bytes: Ptr[UInt8],
      numBytes: CFIndex,
      encoding: CFStringEncoding,
      isExternalRepresentation: Boolean,
      contentsDeallocator: CFAllocatorRef
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithCString(
      alloc: CFAllocatorRef,
      cStr: CString,
      encoding: CFStringEncoding
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithCStringNoCopy(
      alloc: CFAllocatorRef,
      cStr: CString,
      encoding: CFStringEncoding,
      contentsDeallocator: CFAllocatorRef
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithCharacters(
      alloc: CFAllocatorRef,
      chars: Ptr[UniChar],
      numChars: CFIndex
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithCharactersNoCopy(
      alloc: CFAllocatorRef,
      chars: Ptr[UniChar],
      numChars: CFIndex,
      contentsDeallocator: CFAllocatorRef
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithFileSystemRepresentation(
      alloc: CFAllocatorRef,
      buffer: CString
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithFormat(
      alloc: CFAllocatorRef,
      formatOptions: CFDictionaryRef,
      format: CFStringRef,
      rest: Any*
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithFormatAndArguments(
      alloc: CFAllocatorRef,
      formatOptions: CFDictionaryRef,
      format: CFStringRef,
      arguments: va_list
  ): CFStringRef = extern

  /** * Immutable string creation functions **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithPascalString(
      alloc: CFAllocatorRef,
      pStr: ConstStr255Param,
      encoding: CFStringEncoding
  ): CFStringRef = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithPascalStringNoCopy(
      alloc: CFAllocatorRef,
      pStr: ConstStr255Param,
      encoding: CFStringEncoding,
      contentsDeallocator: CFAllocatorRef
  ): CFStringRef = extern

  /** Folds the string into the form specified by the flags. Character foldings
    * are operations that convert any of a set of characters sharing similar
    * semantics into a single representative from that set. This function can be
    * used to preprocess strings that are to be compared, searched, or indexed.
    * Note that folding does not include normalization, so it is necessary to
    * use CFStringNormalize in addition to CFStringFold in order to obtain the
    * effect of kCFCompareNonliteral.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFold(
      theString: CFMutableStringRef,
      theFlags: CFStringCompareFlags,
      theLocale: CFLocaleRef
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCString(
      theString: CFStringRef,
      buffer: CString,
      bufferSize: CFIndex,
      encoding: CFStringEncoding
  ): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCStringPtr(
      theString: CFStringRef,
      encoding: CFStringEncoding
  ): CString = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCharacterAtIndex(
      theString: CFStringRef,
      idx: CFIndex
  ): UniChar = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCharacterFromInlineBuffer(
      buf: Ptr[CFStringInlineBuffer],
      idx: CFIndex
  ): UniChar = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCharactersPtr(theString: CFStringRef): Ptr[UniChar] = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetDoubleValue(str: CFStringRef): Double = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetFastestEncoding(theString: CFStringRef): CFStringEncoding =
    extern

  /** * FileSystem path conversion functions **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetFileSystemRepresentation(
      string: CFStringRef,
      buffer: CString,
      maxBufLen: CFIndex
  ): Boolean = extern

  /** * Parsing non-localized numbers from strings **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetIntValue(str: CFStringRef): SInt32 = extern

  /** * Basic accessors for the contents **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetLength(theString: CFStringRef): CFIndex = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetListOfAvailableEncodings(): Ptr[CFStringEncoding] = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetLongCharacterForSurrogatePair(
      surrogateHigh: UniChar,
      surrogateLow: UniChar
  ): UTF32Char = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetMaximumSizeForEncoding(
      length: CFIndex,
      encoding: CFStringEncoding
  ): CFIndex = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetMaximumSizeOfFileSystemRepresentation(
      string: CFStringRef
  ): CFIndex = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetMostCompatibleMacStringEncoding(
      encoding: CFStringEncoding
  ): CFStringEncoding = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetNameOfEncoding(encoding: CFStringEncoding): CFStringRef =
    extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetPascalString(
      theString: CFStringRef,
      buffer: StringPtr,
      bufferSize: CFIndex,
      encoding: CFStringEncoding
  ): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetPascalStringPtr(
      theString: CFStringRef,
      encoding: CFStringEncoding
  ): ConstStringPtr = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetSmallestEncoding(theString: CFStringRef): CFStringEncoding =
    extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetSurrogatePairForLongCharacter(
      character: UTF32Char,
      surrogates: Ptr[UniChar]
  ): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetSystemEncoding(): CFStringEncoding = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetTypeID(): CFTypeID = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringHasPrefix(theString: CFStringRef, prefix: CFStringRef): Boolean =
    extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringHasSuffix(theString: CFStringRef, suffix: CFStringRef): Boolean =
    extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringInsert(
      str: CFMutableStringRef,
      idx: CFIndex,
      insertedStr: CFStringRef
  ): Unit = extern

  /** * General encoding related functionality **
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringIsEncodingAvailable(encoding: CFStringEncoding): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringIsHyphenationAvailableForLocale(locale: CFLocaleRef): Boolean =
    extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringIsSurrogateHighCharacter(character: UniChar): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringIsSurrogateLowCharacter(character: UniChar): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringLowercase(
      theString: CFMutableStringRef,
      locale: CFLocaleRef
  ): Unit = extern

  /** Normalizes the string into the specified form as described in Unicode
    * Technical Report #15.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringNormalize(
      theString: CFMutableStringRef,
      theForm: CFStringNormalizationForm
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringPad(
      theString: CFMutableStringRef,
      padString: CFStringRef,
      length: CFIndex,
      indexIntoPad: CFIndex
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringReplaceAll(
      theString: CFMutableStringRef,
      replacement: CFStringRef
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringSetExternalCharactersNoCopy(
      theString: CFMutableStringRef,
      chars: Ptr[UniChar],
      length: CFIndex,
      capacity: CFIndex
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringTransform(
      string: CFMutableStringRef,
      range: Ptr[CFRange],
      transform: CFStringRef,
      reverse: Boolean
  ): Boolean = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringTrim(
      theString: CFMutableStringRef,
      trimString: CFStringRef
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringTrimWhitespace(theString: CFMutableStringRef): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringUppercase(
      theString: CFMutableStringRef,
      locale: CFLocaleRef
  ): Unit = extern

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def __CFStringMakeConstantString(cStr: CString): CFStringRef = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringCompareWithOptions(
      theString1: CFStringRef,
      theString2: CFStringRef,
      rangeToCompare: Ptr[CFRange],
      compareOptions: CFStringCompareFlags
  ): CFComparisonResult = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringCompareWithOptionsAndLocale(
      theString1: CFStringRef,
      theString2: CFStringRef,
      rangeToCompare: Ptr[CFRange],
      compareOptions: CFStringCompareFlags,
      locale: CFLocaleRef
  ): CFComparisonResult = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringCreateArrayWithFindResults(
      alloc: CFAllocatorRef,
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      compareOptions: CFStringCompareFlags
  ): CFArrayRef = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringCreateWithSubstring(
      alloc: CFAllocatorRef,
      str: CFStringRef,
      range: Ptr[CFRange]
  ): CFStringRef = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringDelete(
      theString: CFMutableStringRef,
      range: Ptr[CFRange]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringFind(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      compareOptions: CFStringCompareFlags,
      __return: Ptr[CFRange]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringFindAndReplace(
      theString: CFMutableStringRef,
      stringToFind: CFStringRef,
      replacementString: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      compareOptions: CFStringCompareFlags
  ): CFIndex = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringFindCharacterFromSet(
      theString: CFStringRef,
      theSet: CFCharacterSetRef,
      rangeToSearch: Ptr[CFRange],
      searchOptions: CFStringCompareFlags,
      result: Ptr[CFRange]
  ): Boolean = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringFindWithOptions(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      searchOptions: CFStringCompareFlags,
      result: Ptr[CFRange]
  ): Boolean = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringFindWithOptionsAndLocale(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      searchOptions: CFStringCompareFlags,
      locale: CFLocaleRef,
      result: Ptr[CFRange]
  ): Boolean = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringGetBytes(
      theString: CFStringRef,
      range: Ptr[CFRange],
      encoding: CFStringEncoding,
      lossByte: UInt8,
      isExternalRepresentation: Boolean,
      buffer: Ptr[UInt8],
      maxBufLen: CFIndex,
      usedBufLen: Ptr[CFIndex]
  ): CFIndex = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringGetCharacters(
      theString: CFStringRef,
      range: Ptr[CFRange],
      buffer: Ptr[UniChar]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringGetHyphenationLocationBeforeIndex(
      string: CFStringRef,
      location: CFIndex,
      limitRange: Ptr[CFRange],
      options: CFOptionFlags,
      locale: CFLocaleRef,
      character: Ptr[UTF32Char]
  ): CFIndex = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringGetLineBounds(
      theString: CFStringRef,
      range: Ptr[CFRange],
      lineBeginIndex: Ptr[CFIndex],
      lineEndIndex: Ptr[CFIndex],
      contentsEndIndex: Ptr[CFIndex]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringGetParagraphBounds(
      string: CFStringRef,
      range: Ptr[CFRange],
      parBeginIndex: Ptr[CFIndex],
      parEndIndex: Ptr[CFIndex],
      contentsEndIndex: Ptr[CFIndex]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringGetRangeOfComposedCharactersAtIndex(
      theString: CFStringRef,
      theIndex: CFIndex,
      __return: Ptr[CFRange]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringInitInlineBuffer(
      str: CFStringRef,
      buf: Ptr[CFStringInlineBuffer],
      range: Ptr[CFRange]
  ): Unit = extern

  private[cfstring] def __sn_wrap_cfstring_CFStringReplace(
      theString: CFMutableStringRef,
      range: Ptr[CFRange],
      replacement: CFStringRef
  ): Unit = extern

object functions:
  import _root_.cfstring.aliases.*
  import _root_.cfstring.structs.*
  import extern_functions.*
  export extern_functions.*

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCompareWithOptions(
      theString1: CFStringRef,
      theString2: CFStringRef,
      rangeToCompare: CFRange,
      compareOptions: CFStringCompareFlags
  )(using Zone): CFComparisonResult =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToCompare
    __sn_wrap_cfstring_CFStringCompareWithOptions(
      theString1,
      theString2,
      (__ptr_0 + 0),
      compareOptions
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCompareWithOptions(
      theString1: CFStringRef,
      theString2: CFStringRef,
      rangeToCompare: Ptr[CFRange],
      compareOptions: CFStringCompareFlags
  ): CFComparisonResult =
    __sn_wrap_cfstring_CFStringCompareWithOptions(
      theString1,
      theString2,
      rangeToCompare,
      compareOptions
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCompareWithOptionsAndLocale(
      theString1: CFStringRef,
      theString2: CFStringRef,
      rangeToCompare: CFRange,
      compareOptions: CFStringCompareFlags,
      locale: CFLocaleRef
  )(using Zone): CFComparisonResult =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToCompare
    __sn_wrap_cfstring_CFStringCompareWithOptionsAndLocale(
      theString1,
      theString2,
      (__ptr_0 + 0),
      compareOptions,
      locale
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCompareWithOptionsAndLocale(
      theString1: CFStringRef,
      theString2: CFStringRef,
      rangeToCompare: Ptr[CFRange],
      compareOptions: CFStringCompareFlags,
      locale: CFLocaleRef
  ): CFComparisonResult =
    __sn_wrap_cfstring_CFStringCompareWithOptionsAndLocale(
      theString1,
      theString2,
      rangeToCompare,
      compareOptions,
      locale
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateArrayWithFindResults(
      alloc: CFAllocatorRef,
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      compareOptions: CFStringCompareFlags
  ): CFArrayRef =
    __sn_wrap_cfstring_CFStringCreateArrayWithFindResults(
      alloc,
      theString,
      stringToFind,
      rangeToSearch,
      compareOptions
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateArrayWithFindResults(
      __alloc: CFAllocatorRef,
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: CFRange,
      compareOptions: CFStringCompareFlags
  )(using Zone): CFArrayRef =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_cfstring_CFStringCreateArrayWithFindResults(
      __alloc,
      theString,
      stringToFind,
      (__ptr_0 + 0),
      compareOptions
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithSubstring(
      __alloc: CFAllocatorRef,
      str: CFStringRef,
      range: CFRange
  )(using Zone): CFStringRef =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringCreateWithSubstring(__alloc, str, (__ptr_0 + 0))

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringCreateWithSubstring(
      alloc: CFAllocatorRef,
      str: CFStringRef,
      range: Ptr[CFRange]
  ): CFStringRef =
    __sn_wrap_cfstring_CFStringCreateWithSubstring(alloc, str, range)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringDelete(theString: CFMutableStringRef, range: CFRange)(using
      Zone
  ): Unit =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringDelete(theString, (__ptr_0 + 0))

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringDelete(theString: CFMutableStringRef, range: Ptr[CFRange]): Unit =
    __sn_wrap_cfstring_CFStringDelete(theString, range)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFind(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      compareOptions: CFStringCompareFlags
  )(using Zone): CFRange =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    __sn_wrap_cfstring_CFStringFind(
      theString,
      stringToFind,
      compareOptions,
      (__ptr_0 + 0)
    )
    !(__ptr_0 + 0)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFind(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      compareOptions: CFStringCompareFlags
  )(__return: Ptr[CFRange]): Unit =
    __sn_wrap_cfstring_CFStringFind(
      theString,
      stringToFind,
      compareOptions,
      __return
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindAndReplace(
      theString: CFMutableStringRef,
      stringToFind: CFStringRef,
      replacementString: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      compareOptions: CFStringCompareFlags
  ): CFIndex =
    __sn_wrap_cfstring_CFStringFindAndReplace(
      theString,
      stringToFind,
      replacementString,
      rangeToSearch,
      compareOptions
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindAndReplace(
      theString: CFMutableStringRef,
      stringToFind: CFStringRef,
      replacementString: CFStringRef,
      rangeToSearch: CFRange,
      compareOptions: CFStringCompareFlags
  )(using Zone): CFIndex =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_cfstring_CFStringFindAndReplace(
      theString,
      stringToFind,
      replacementString,
      (__ptr_0 + 0),
      compareOptions
    )

  /** Query the range of the first character contained in the specified
    * character set.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindCharacterFromSet(
      theString: CFStringRef,
      theSet: CFCharacterSetRef,
      rangeToSearch: CFRange,
      searchOptions: CFStringCompareFlags,
      result: Ptr[CFRange]
  )(using Zone): Boolean =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_cfstring_CFStringFindCharacterFromSet(
      theString,
      theSet,
      (__ptr_0 + 0),
      searchOptions,
      result
    )

  /** Query the range of the first character contained in the specified
    * character set.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindCharacterFromSet(
      theString: CFStringRef,
      theSet: CFCharacterSetRef,
      rangeToSearch: Ptr[CFRange],
      searchOptions: CFStringCompareFlags,
      result: Ptr[CFRange]
  ): Boolean =
    __sn_wrap_cfstring_CFStringFindCharacterFromSet(
      theString,
      theSet,
      rangeToSearch,
      searchOptions,
      result
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindWithOptions(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      searchOptions: CFStringCompareFlags,
      result: Ptr[CFRange]
  ): Boolean =
    __sn_wrap_cfstring_CFStringFindWithOptions(
      theString,
      stringToFind,
      rangeToSearch,
      searchOptions,
      result
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindWithOptions(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: CFRange,
      searchOptions: CFStringCompareFlags,
      result: Ptr[CFRange]
  )(using Zone): Boolean =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_cfstring_CFStringFindWithOptions(
      theString,
      stringToFind,
      (__ptr_0 + 0),
      searchOptions,
      result
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindWithOptionsAndLocale(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: Ptr[CFRange],
      searchOptions: CFStringCompareFlags,
      locale: CFLocaleRef,
      result: Ptr[CFRange]
  ): Boolean =
    __sn_wrap_cfstring_CFStringFindWithOptionsAndLocale(
      theString,
      stringToFind,
      rangeToSearch,
      searchOptions,
      locale,
      result
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringFindWithOptionsAndLocale(
      theString: CFStringRef,
      stringToFind: CFStringRef,
      rangeToSearch: CFRange,
      searchOptions: CFStringCompareFlags,
      locale: CFLocaleRef,
      result: Ptr[CFRange]
  )(using Zone): Boolean =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = rangeToSearch
    __sn_wrap_cfstring_CFStringFindWithOptionsAndLocale(
      theString,
      stringToFind,
      (__ptr_0 + 0),
      searchOptions,
      locale,
      result
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetBytes(
      theString: CFStringRef,
      range: Ptr[CFRange],
      encoding: CFStringEncoding,
      lossByte: UInt8,
      isExternalRepresentation: Boolean,
      buffer: Ptr[UInt8],
      maxBufLen: CFIndex,
      usedBufLen: Ptr[CFIndex]
  ): CFIndex =
    __sn_wrap_cfstring_CFStringGetBytes(
      theString,
      range,
      encoding,
      lossByte,
      isExternalRepresentation,
      buffer,
      maxBufLen,
      usedBufLen
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetBytes(
      theString: CFStringRef,
      range: CFRange,
      encoding: CFStringEncoding,
      lossByte: UInt8,
      isExternalRepresentation: Boolean,
      buffer: Ptr[UInt8],
      maxBufLen: CFIndex,
      usedBufLen: Ptr[CFIndex]
  )(using Zone): CFIndex =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringGetBytes(
      theString,
      (__ptr_0 + 0),
      encoding,
      lossByte,
      isExternalRepresentation,
      buffer,
      maxBufLen,
      usedBufLen
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCharacters(
      theString: CFStringRef,
      range: Ptr[CFRange],
      buffer: Ptr[UniChar]
  ): Unit =
    __sn_wrap_cfstring_CFStringGetCharacters(theString, range, buffer)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetCharacters(
      theString: CFStringRef,
      range: CFRange,
      buffer: Ptr[UniChar]
  )(using Zone): Unit =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringGetCharacters(theString, (__ptr_0 + 0), buffer)

  /** Retrieve the first potential hyphenation location found before the
    * specified location.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetHyphenationLocationBeforeIndex(
      string: CFStringRef,
      location: CFIndex,
      limitRange: Ptr[CFRange],
      options: CFOptionFlags,
      locale: CFLocaleRef,
      character: Ptr[UTF32Char]
  ): CFIndex =
    __sn_wrap_cfstring_CFStringGetHyphenationLocationBeforeIndex(
      string,
      location,
      limitRange,
      options,
      locale,
      character
    )

  /** Retrieve the first potential hyphenation location found before the
    * specified location.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetHyphenationLocationBeforeIndex(
      string: CFStringRef,
      location: CFIndex,
      limitRange: CFRange,
      options: CFOptionFlags,
      locale: CFLocaleRef,
      character: Ptr[UTF32Char]
  )(using Zone): CFIndex =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = limitRange
    __sn_wrap_cfstring_CFStringGetHyphenationLocationBeforeIndex(
      string,
      location,
      (__ptr_0 + 0),
      options,
      locale,
      character
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetLineBounds(
      theString: CFStringRef,
      range: Ptr[CFRange],
      lineBeginIndex: Ptr[CFIndex],
      lineEndIndex: Ptr[CFIndex],
      contentsEndIndex: Ptr[CFIndex]
  ): Unit =
    __sn_wrap_cfstring_CFStringGetLineBounds(
      theString,
      range,
      lineBeginIndex,
      lineEndIndex,
      contentsEndIndex
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetLineBounds(
      theString: CFStringRef,
      range: CFRange,
      lineBeginIndex: Ptr[CFIndex],
      lineEndIndex: Ptr[CFIndex],
      contentsEndIndex: Ptr[CFIndex]
  )(using Zone): Unit =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringGetLineBounds(
      theString,
      (__ptr_0 + 0),
      lineBeginIndex,
      lineEndIndex,
      contentsEndIndex
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetParagraphBounds(
      string: CFStringRef,
      range: CFRange,
      parBeginIndex: Ptr[CFIndex],
      parEndIndex: Ptr[CFIndex],
      contentsEndIndex: Ptr[CFIndex]
  )(using Zone): Unit =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringGetParagraphBounds(
      string,
      (__ptr_0 + 0),
      parBeginIndex,
      parEndIndex,
      contentsEndIndex
    )

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetParagraphBounds(
      string: CFStringRef,
      range: Ptr[CFRange],
      parBeginIndex: Ptr[CFIndex],
      parEndIndex: Ptr[CFIndex],
      contentsEndIndex: Ptr[CFIndex]
  ): Unit =
    __sn_wrap_cfstring_CFStringGetParagraphBounds(
      string,
      range,
      parBeginIndex,
      parEndIndex,
      contentsEndIndex
    )

  /** Returns the range of the composed character sequence at the specified
    * index.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetRangeOfComposedCharactersAtIndex(
      theString: CFStringRef,
      theIndex: CFIndex
  )(__return: Ptr[CFRange]): Unit =
    __sn_wrap_cfstring_CFStringGetRangeOfComposedCharactersAtIndex(
      theString,
      theIndex,
      __return
    )

  /** Returns the range of the composed character sequence at the specified
    * index.
    *
    * [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringGetRangeOfComposedCharactersAtIndex(
      theString: CFStringRef,
      theIndex: CFIndex
  )(using Zone): CFRange =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    __sn_wrap_cfstring_CFStringGetRangeOfComposedCharactersAtIndex(
      theString,
      theIndex,
      (__ptr_0 + 0)
    )
    !(__ptr_0 + 0)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringInitInlineBuffer(
      str: CFStringRef,
      buf: Ptr[CFStringInlineBuffer],
      range: Ptr[CFRange]
  ): Unit =
    __sn_wrap_cfstring_CFStringInitInlineBuffer(str, buf, range)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringInitInlineBuffer(
      str: CFStringRef,
      buf: Ptr[CFStringInlineBuffer],
      range: CFRange
  )(using Zone): Unit =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringInitInlineBuffer(str, buf, (__ptr_0 + 0))

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringReplace(
      theString: CFMutableStringRef,
      range: CFRange,
      replacement: CFStringRef
  )(using Zone): Unit =
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    !(__ptr_0 + 0) = range
    __sn_wrap_cfstring_CFStringReplace(theString, (__ptr_0 + 0), replacement)

  /** [bindgen] header:
    * /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFString.h
    */
  def CFStringReplace(
      theString: CFMutableStringRef,
      range: Ptr[CFRange],
      replacement: CFStringRef
  ): Unit =
    __sn_wrap_cfstring_CFStringReplace(theString, range, replacement)

object constants:
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

  val kCFCompareCaseInsensitive: CUnsignedInt = 1.toUInt
  val kCFCompareBackwards: CUnsignedInt = 4.toUInt
  val kCFCompareAnchored: CUnsignedInt = 8.toUInt
  val kCFCompareNonliteral: CUnsignedInt = 16.toUInt
  val kCFCompareLocalized: CUnsignedInt = 32.toUInt
  val kCFCompareNumerically: CUnsignedInt = 64.toUInt
  val kCFCompareDiacriticInsensitive: CUnsignedInt = 128.toUInt
  val kCFCompareWidthInsensitive: CUnsignedInt = 256.toUInt
  val kCFCompareForcedOrdering: CUnsignedInt = 512.toUInt

  val kCFStringNormalizationFormD: CUnsignedInt = 0.toUInt
  val kCFStringNormalizationFormKD: CUnsignedInt = 1.toUInt
  val kCFStringNormalizationFormC: CUnsignedInt = 2.toUInt
  val kCFStringNormalizationFormKC: CUnsignedInt = 3.toUInt

object types:
  export _root_.cfstring.structs.*
  export _root_.cfstring.aliases.*

object all:
  export _root_.cfstring.aliases.Boolean
  export _root_.cfstring.aliases.CFAllocatorRef
  export _root_.cfstring.aliases.CFArrayRef
  export _root_.cfstring.aliases.CFCharacterSetRef
  export _root_.cfstring.aliases.CFComparisonResult
  export _root_.cfstring.aliases.CFDataRef
  export _root_.cfstring.aliases.CFDictionaryRef
  export _root_.cfstring.aliases.CFErrorRef
  export _root_.cfstring.aliases.CFIndex
  export _root_.cfstring.aliases.CFLocaleRef
  export _root_.cfstring.aliases.CFMutableStringRef
  export _root_.cfstring.aliases.CFOptionFlags
  export _root_.cfstring.aliases.CFStringBuiltInEncodings
  export _root_.cfstring.aliases.CFStringCompareFlags
  export _root_.cfstring.aliases.CFStringEncoding
  export _root_.cfstring.aliases.CFStringNormalizationForm
  export _root_.cfstring.aliases.CFStringRef
  export _root_.cfstring.aliases.CFTypeID
  export _root_.cfstring.aliases.CFTypeRef
  export _root_.cfstring.aliases.ConstStr255Param
  export _root_.cfstring.aliases.ConstStringPtr
  export _root_.cfstring.aliases.SInt32
  export _root_.cfstring.aliases.StringPtr
  export _root_.cfstring.aliases.UInt16
  export _root_.cfstring.aliases.UInt32
  export _root_.cfstring.aliases.UInt8
  export _root_.cfstring.aliases.UTF32Char
  export _root_.cfstring.aliases.UniChar
  export _root_.cfstring.aliases.va_list
  export _root_.cfstring.structs.CFRange
  export _root_.cfstring.structs.CFStringInlineBuffer
  export _root_.cfstring.structs.__CFAllocator
  export _root_.cfstring.structs.__CFArray
  export _root_.cfstring.structs.__CFCharacterSet
  export _root_.cfstring.structs.__CFData
  export _root_.cfstring.structs.__CFDictionary
  export _root_.cfstring.structs.__CFError
  export _root_.cfstring.structs.__CFLocale
  export _root_.cfstring.structs.__CFString
  export _root_.cfstring.functions.CFShow
  export _root_.cfstring.functions.CFShowStr
  export _root_.cfstring.functions.CFStringAppend
  export _root_.cfstring.functions.CFStringAppendCString
  export _root_.cfstring.functions.CFStringAppendCharacters
  export _root_.cfstring.functions.CFStringAppendFormat
  export _root_.cfstring.functions.CFStringAppendFormatAndArguments
  export _root_.cfstring.functions.CFStringAppendPascalString
  export _root_.cfstring.functions.CFStringCapitalize
  export _root_.cfstring.functions.CFStringCompare
  export _root_.cfstring.functions.CFStringConvertEncodingToIANACharSetName
  export _root_.cfstring.functions.CFStringConvertEncodingToNSStringEncoding
  export _root_.cfstring.functions.CFStringConvertEncodingToWindowsCodepage
  export _root_.cfstring.functions.CFStringConvertIANACharSetNameToEncoding
  export _root_.cfstring.functions.CFStringConvertNSStringEncodingToEncoding
  export _root_.cfstring.functions.CFStringConvertWindowsCodepageToEncoding
  export _root_.cfstring.functions.CFStringCreateArrayBySeparatingStrings
  export _root_.cfstring.functions.CFStringCreateByCombiningStrings
  export _root_.cfstring.functions.CFStringCreateCopy
  export _root_.cfstring.functions.CFStringCreateExternalRepresentation
  export _root_.cfstring.functions.CFStringCreateFromExternalRepresentation
  export _root_.cfstring.functions.CFStringCreateMutable
  export _root_.cfstring.functions.CFStringCreateMutableCopy
  export _root_.cfstring.functions.CFStringCreateMutableWithExternalCharactersNoCopy
  export _root_.cfstring.functions.CFStringCreateStringWithValidatedFormat
  export _root_.cfstring.functions.CFStringCreateStringWithValidatedFormatAndArguments
  export _root_.cfstring.functions.CFStringCreateWithBytes
  export _root_.cfstring.functions.CFStringCreateWithBytesNoCopy
  export _root_.cfstring.functions.CFStringCreateWithCString
  export _root_.cfstring.functions.CFStringCreateWithCStringNoCopy
  export _root_.cfstring.functions.CFStringCreateWithCharacters
  export _root_.cfstring.functions.CFStringCreateWithCharactersNoCopy
  export _root_.cfstring.functions.CFStringCreateWithFileSystemRepresentation
  export _root_.cfstring.functions.CFStringCreateWithFormat
  export _root_.cfstring.functions.CFStringCreateWithFormatAndArguments
  export _root_.cfstring.functions.CFStringCreateWithPascalString
  export _root_.cfstring.functions.CFStringCreateWithPascalStringNoCopy
  export _root_.cfstring.functions.CFStringFold
  export _root_.cfstring.functions.CFStringGetCString
  export _root_.cfstring.functions.CFStringGetCStringPtr
  export _root_.cfstring.functions.CFStringGetCharacterAtIndex
  export _root_.cfstring.functions.CFStringGetCharacterFromInlineBuffer
  export _root_.cfstring.functions.CFStringGetCharactersPtr
  export _root_.cfstring.functions.CFStringGetDoubleValue
  export _root_.cfstring.functions.CFStringGetFastestEncoding
  export _root_.cfstring.functions.CFStringGetFileSystemRepresentation
  export _root_.cfstring.functions.CFStringGetIntValue
  export _root_.cfstring.functions.CFStringGetLength
  export _root_.cfstring.functions.CFStringGetListOfAvailableEncodings
  export _root_.cfstring.functions.CFStringGetLongCharacterForSurrogatePair
  export _root_.cfstring.functions.CFStringGetMaximumSizeForEncoding
  export _root_.cfstring.functions.CFStringGetMaximumSizeOfFileSystemRepresentation
  export _root_.cfstring.functions.CFStringGetMostCompatibleMacStringEncoding
  export _root_.cfstring.functions.CFStringGetNameOfEncoding
  export _root_.cfstring.functions.CFStringGetPascalString
  export _root_.cfstring.functions.CFStringGetPascalStringPtr
  export _root_.cfstring.functions.CFStringGetSmallestEncoding
  export _root_.cfstring.functions.CFStringGetSurrogatePairForLongCharacter
  export _root_.cfstring.functions.CFStringGetSystemEncoding
  export _root_.cfstring.functions.CFStringGetTypeID
  export _root_.cfstring.functions.CFStringHasPrefix
  export _root_.cfstring.functions.CFStringHasSuffix
  export _root_.cfstring.functions.CFStringInsert
  export _root_.cfstring.functions.CFStringIsEncodingAvailable
  export _root_.cfstring.functions.CFStringIsHyphenationAvailableForLocale
  export _root_.cfstring.functions.CFStringIsSurrogateHighCharacter
  export _root_.cfstring.functions.CFStringIsSurrogateLowCharacter
  export _root_.cfstring.functions.CFStringLowercase
  export _root_.cfstring.functions.CFStringNormalize
  export _root_.cfstring.functions.CFStringPad
  export _root_.cfstring.functions.CFStringReplaceAll
  export _root_.cfstring.functions.CFStringSetExternalCharactersNoCopy
  export _root_.cfstring.functions.CFStringTransform
  export _root_.cfstring.functions.CFStringTrim
  export _root_.cfstring.functions.CFStringTrimWhitespace
  export _root_.cfstring.functions.CFStringUppercase
  export _root_.cfstring.functions.__CFStringMakeConstantString
  export _root_.cfstring.functions.CFStringCompareWithOptions
  export _root_.cfstring.functions.CFStringCompareWithOptionsAndLocale
  export _root_.cfstring.functions.CFStringCreateArrayWithFindResults
  export _root_.cfstring.functions.CFStringCreateWithSubstring
  export _root_.cfstring.functions.CFStringDelete
  export _root_.cfstring.functions.CFStringFind
  export _root_.cfstring.functions.CFStringFindAndReplace
  export _root_.cfstring.functions.CFStringFindCharacterFromSet
  export _root_.cfstring.functions.CFStringFindWithOptions
  export _root_.cfstring.functions.CFStringFindWithOptionsAndLocale
  export _root_.cfstring.functions.CFStringGetBytes
  export _root_.cfstring.functions.CFStringGetCharacters
  export _root_.cfstring.functions.CFStringGetHyphenationLocationBeforeIndex
  export _root_.cfstring.functions.CFStringGetLineBounds
  export _root_.cfstring.functions.CFStringGetParagraphBounds
  export _root_.cfstring.functions.CFStringGetRangeOfComposedCharactersAtIndex
  export _root_.cfstring.functions.CFStringInitInlineBuffer
  export _root_.cfstring.functions.CFStringReplace
