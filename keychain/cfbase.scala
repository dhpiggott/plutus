package cfbase

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object aliases:
  import _root_.cfbase.aliases.*
  import _root_.cfbase.structs.*
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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorAllocateCallBack = CFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]]
  object CFAllocatorAllocateCallBack: 
    given _tag: Tag[CFAllocatorAllocateCallBack] = Tag.materializeCFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorAllocateCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]]): CFAllocatorAllocateCallBack = o
    extension (v: CFAllocatorAllocateCallBack)
      inline def value: CFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorCopyDescriptionCallBack = CFuncPtr1[Ptr[Byte], CFStringRef]
  object CFAllocatorCopyDescriptionCallBack: 
    given _tag: Tag[CFAllocatorCopyDescriptionCallBack] = Tag.materializeCFuncPtr1[Ptr[Byte], CFStringRef]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorCopyDescriptionCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr1[Ptr[Byte], CFStringRef]): CFAllocatorCopyDescriptionCallBack = o
    extension (v: CFAllocatorCopyDescriptionCallBack)
      inline def value: CFuncPtr1[Ptr[Byte], CFStringRef] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorDeallocateCallBack = CFuncPtr2[Ptr[Byte], Ptr[Byte], Unit]
  object CFAllocatorDeallocateCallBack: 
    given _tag: Tag[CFAllocatorDeallocateCallBack] = Tag.materializeCFuncPtr2[Ptr[Byte], Ptr[Byte], Unit]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorDeallocateCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr2[Ptr[Byte], Ptr[Byte], Unit]): CFAllocatorDeallocateCallBack = o
    extension (v: CFAllocatorDeallocateCallBack)
      inline def value: CFuncPtr2[Ptr[Byte], Ptr[Byte], Unit] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorPreferredSizeCallBack = CFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], CFIndex]
  object CFAllocatorPreferredSizeCallBack: 
    given _tag: Tag[CFAllocatorPreferredSizeCallBack] = Tag.materializeCFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], CFIndex]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorPreferredSizeCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], CFIndex]): CFAllocatorPreferredSizeCallBack = o
    extension (v: CFAllocatorPreferredSizeCallBack)
      inline def value: CFuncPtr3[CFIndex, CFOptionFlags, Ptr[Byte], CFIndex] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorReallocateCallBack = CFuncPtr4[Ptr[Byte], CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]]
  object CFAllocatorReallocateCallBack: 
    given _tag: Tag[CFAllocatorReallocateCallBack] = Tag.materializeCFuncPtr4[Ptr[Byte], CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorReallocateCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr4[Ptr[Byte], CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]]): CFAllocatorReallocateCallBack = o
    extension (v: CFAllocatorReallocateCallBack)
      inline def value: CFuncPtr4[Ptr[Byte], CFIndex, CFOptionFlags, Ptr[Byte], Ptr[Byte]] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorRef = Ptr[__CFAllocator]
  object CFAllocatorRef: 
    given _tag: Tag[CFAllocatorRef] = Tag.Ptr[__CFAllocator](__CFAllocator._tag)
    inline def apply(inline o: Ptr[__CFAllocator]): CFAllocatorRef = o
    extension (v: CFAllocatorRef)
      inline def value: Ptr[__CFAllocator] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorReleaseCallBack = CFuncPtr1[Ptr[Byte], Unit]
  object CFAllocatorReleaseCallBack: 
    given _tag: Tag[CFAllocatorReleaseCallBack] = Tag.materializeCFuncPtr1[Ptr[Byte], Unit]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorReleaseCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr1[Ptr[Byte], Unit]): CFAllocatorReleaseCallBack = o
    extension (v: CFAllocatorReleaseCallBack)
      inline def value: CFuncPtr1[Ptr[Byte], Unit] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorRetainCallBack = CFuncPtr1[Ptr[Byte], Ptr[Byte]]
  object CFAllocatorRetainCallBack: 
    given _tag: Tag[CFAllocatorRetainCallBack] = Tag.materializeCFuncPtr1[Ptr[Byte], Ptr[Byte]]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFAllocatorRetainCallBack = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr1[Ptr[Byte], Ptr[Byte]]): CFAllocatorRetainCallBack = o
    extension (v: CFAllocatorRetainCallBack)
      inline def value: CFuncPtr1[Ptr[Byte], Ptr[Byte]] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorTypeID = CUnsignedLongLong
  object CFAllocatorTypeID: 
    given _tag: Tag[CFAllocatorTypeID] = Tag.ULong
    inline def apply(inline o: CUnsignedLongLong): CFAllocatorTypeID = o
    extension (v: CFAllocatorTypeID)
      inline def value: CUnsignedLongLong = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFComparatorFunction = CFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], CFComparisonResult]
  object CFComparatorFunction: 
    given _tag: Tag[CFComparatorFunction] = Tag.materializeCFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], CFComparisonResult]
    inline def fromPtr(ptr: Ptr[Byte] | Ptr[?]): CFComparatorFunction = CFuncPtr.fromPtr(ptr.asInstanceOf[Ptr[Byte]])
    inline def apply(inline o: CFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], CFComparisonResult]): CFComparatorFunction = o
    extension (v: CFComparatorFunction)
      inline def value: CFuncPtr3[Ptr[Byte], Ptr[Byte], Ptr[Byte], CFComparisonResult] = v
      inline def toPtr: Ptr[?] = CFuncPtr.toPtr(v)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  type CFComparisonResult = CFIndex
  object CFComparisonResult: 
    given _tag: Tag[CFComparisonResult] = CFIndex._tag
    inline def apply(inline o: CFIndex): CFComparisonResult = o
    extension (v: CFComparisonResult)
      inline def value: CFIndex = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFHashCode = CUnsignedLongInt
  object CFHashCode: 
    given _tag: Tag[CFHashCode] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFHashCode = o
    extension (v: CFHashCode)
      inline def value: CUnsignedLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFIndex = CLongInt
  object CFIndex: 
    given _tag: Tag[CFIndex] = Tag.Long
    inline def apply(inline o: CLongInt): CFIndex = o
    extension (v: CFIndex)
      inline def value: CLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFMutableStringRef = Ptr[__CFString]
  object CFMutableStringRef: 
    given _tag: Tag[CFMutableStringRef] = Tag.Ptr[__CFString](__CFString._tag)
    inline def apply(inline o: Ptr[__CFString]): CFMutableStringRef = o
    extension (v: CFMutableStringRef)
      inline def value: Ptr[__CFString] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFNullRef = Ptr[__CFNull]
  object CFNullRef: 
    given _tag: Tag[CFNullRef] = Tag.Ptr[__CFNull](__CFNull._tag)
    inline def apply(inline o: Ptr[__CFNull]): CFNullRef = o
    extension (v: CFNullRef)
      inline def value: Ptr[__CFNull] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFOptionFlags = CUnsignedLongInt
  object CFOptionFlags: 
    given _tag: Tag[CFOptionFlags] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFOptionFlags = o
    extension (v: CFOptionFlags)
      inline def value: CUnsignedLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  type CFPropertyListRef = CFTypeRef
  object CFPropertyListRef: 
    given _tag: Tag[CFPropertyListRef] = CFTypeRef._tag
    inline def apply(inline o: CFTypeRef): CFPropertyListRef = o
    extension (v: CFPropertyListRef)
      inline def value: CFTypeRef = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFStringRef = Ptr[__CFString]
  object CFStringRef: 
    given _tag: Tag[CFStringRef] = Tag.Ptr[__CFString](__CFString._tag)
    inline def apply(inline o: Ptr[__CFString]): CFStringRef = o
    extension (v: CFStringRef)
      inline def value: Ptr[__CFString] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFTypeID = CUnsignedLongInt
  object CFTypeID: 
    given _tag: Tag[CFTypeID] = Tag.ULong
    inline def apply(inline o: CUnsignedLongInt): CFTypeID = o
    extension (v: CFTypeID)
      inline def value: CUnsignedLongInt = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFTypeRef = Ptr[Byte]
  object CFTypeRef: 
    given _tag: Tag[CFTypeRef] = Tag.Ptr(Tag.Byte)
    inline def apply(inline o: Ptr[Byte]): CFTypeRef = o
    extension (v: CFTypeRef)
      inline def value: Ptr[Byte] = v

object structs:
  import _root_.cfbase.aliases.*
  import _root_.cfbase.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type CFAllocatorContext = CStruct9[CFIndex, Ptr[Byte], CFAllocatorRetainCallBack, CFAllocatorReleaseCallBack, CFAllocatorCopyDescriptionCallBack, CFAllocatorAllocateCallBack, CFAllocatorReallocateCallBack, CFAllocatorDeallocateCallBack, CFAllocatorPreferredSizeCallBack]
  object CFAllocatorContext:
    given _tag: Tag[CFAllocatorContext] = Tag.materializeCStruct9Tag[CFIndex, Ptr[Byte], CFAllocatorRetainCallBack, CFAllocatorReleaseCallBack, CFAllocatorCopyDescriptionCallBack, CFAllocatorAllocateCallBack, CFAllocatorReallocateCallBack, CFAllocatorDeallocateCallBack, CFAllocatorPreferredSizeCallBack]
    def apply()(using Zone): Ptr[CFAllocatorContext] = scala.scalanative.unsafe.alloc[CFAllocatorContext](1)
    def apply(version : CFIndex, info : Ptr[Byte], retain : CFAllocatorRetainCallBack, release : CFAllocatorReleaseCallBack, copyDescription : CFAllocatorCopyDescriptionCallBack, allocate : CFAllocatorAllocateCallBack, reallocate : CFAllocatorReallocateCallBack, deallocate : CFAllocatorDeallocateCallBack, preferredSize : CFAllocatorPreferredSizeCallBack)(using Zone): Ptr[CFAllocatorContext] = 
      val ____ptr = apply()
      (!____ptr).version = version
      (!____ptr).info = info
      (!____ptr).retain = retain
      (!____ptr).release = release
      (!____ptr).copyDescription = copyDescription
      (!____ptr).allocate = allocate
      (!____ptr).reallocate = reallocate
      (!____ptr).deallocate = deallocate
      (!____ptr).preferredSize = preferredSize
      ____ptr
    extension (struct: CFAllocatorContext)
      def version : CFIndex = struct._1
      def version_=(value: CFIndex): Unit = !struct.at1 = value
      def info : Ptr[Byte] = struct._2
      def info_=(value: Ptr[Byte]): Unit = !struct.at2 = value
      def retain : CFAllocatorRetainCallBack = struct._3
      def retain_=(value: CFAllocatorRetainCallBack): Unit = !struct.at3 = value
      def release : CFAllocatorReleaseCallBack = struct._4
      def release_=(value: CFAllocatorReleaseCallBack): Unit = !struct.at4 = value
      def copyDescription : CFAllocatorCopyDescriptionCallBack = struct._5
      def copyDescription_=(value: CFAllocatorCopyDescriptionCallBack): Unit = !struct.at5 = value
      def allocate : CFAllocatorAllocateCallBack = struct._6
      def allocate_=(value: CFAllocatorAllocateCallBack): Unit = !struct.at6 = value
      def reallocate : CFAllocatorReallocateCallBack = struct._7
      def reallocate_=(value: CFAllocatorReallocateCallBack): Unit = !struct.at7 = value
      def deallocate : CFAllocatorDeallocateCallBack = struct._8
      def deallocate_=(value: CFAllocatorDeallocateCallBack): Unit = !struct.at8 = value
      def preferredSize : CFAllocatorPreferredSizeCallBack = struct._9
      def preferredSize_=(value: CFAllocatorPreferredSizeCallBack): Unit = !struct.at9 = value

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type __CFAllocator = CStruct0
  object __CFAllocator:
    given _tag: Tag[__CFAllocator] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type __CFNull = CStruct0
  object __CFNull:
    given _tag: Tag[__CFNull] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  opaque type __CFString = CStruct0
  object __CFString:
    given _tag: Tag[__CFString] = Tag.materializeCStruct0Tag


@extern
private[cfbase] object extern_functions:
  import _root_.cfbase.aliases.*
  import _root_.cfbase.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorAllocate(allocator : CFAllocatorRef, size : CFIndex, hint : CFOptionFlags): Ptr[Byte] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorAllocateBytes(allocator : CFAllocatorRef, size : CFIndex, hint : CFOptionFlags): Ptr[Byte] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorAllocateTyped(allocator : CFAllocatorRef, size : CFIndex, descriptor : CFAllocatorTypeID, hint : CFOptionFlags): Ptr[Byte] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorCreate(allocator : CFAllocatorRef, context : Ptr[CFAllocatorContext]): CFAllocatorRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorDeallocate(allocator : CFAllocatorRef, ptr : Ptr[Byte]): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorGetContext(allocator : CFAllocatorRef, context : Ptr[CFAllocatorContext]): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorGetDefault(): CFAllocatorRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorGetPreferredSizeForSize(allocator : CFAllocatorRef, size : CFIndex, hint : CFOptionFlags): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorReallocate(allocator : CFAllocatorRef, ptr : Ptr[Byte], newsize : CFIndex, hint : CFOptionFlags): Ptr[Byte] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorReallocateBytes(allocator : CFAllocatorRef, ptr : Ptr[Byte], newsize : CFIndex, hint : CFOptionFlags): Ptr[Byte] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorReallocateTyped(allocator : CFAllocatorRef, ptr : Ptr[Byte], newsize : CFIndex, descriptor : CFAllocatorTypeID, hint : CFOptionFlags): Ptr[Byte] = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAllocatorSetDefault(allocator : CFAllocatorRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFAutorelease(arg : CFTypeRef): CFTypeRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFCopyDescription(cf : CFTypeRef): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFCopyTypeIDDescription(type_id : CFTypeID): CFStringRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFEqual(cf1 : CFTypeRef, cf2 : CFTypeRef): Boolean = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFGetAllocator(cf : CFTypeRef): CFAllocatorRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFGetRetainCount(cf : CFTypeRef): CFIndex = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFGetTypeID(cf : CFTypeRef): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFHash(cf : CFTypeRef): CFHashCode = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFMakeCollectable(cf : CFTypeRef): CFTypeRef = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFNullGetTypeID(): CFTypeID = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFRelease(cf : CFTypeRef): Unit = extern

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFRetain(cf : CFTypeRef): CFTypeRef = extern

  private[cfbase] def __sn_wrap_cfbase_CFRangeMake(loc : CFIndex, len : CFIndex, __return : Ptr[CFRange]): Unit = extern

  private[cfbase] def __sn_wrap_cfbase___CFRangeMake(loc : CFIndex, len : CFIndex, __return : Ptr[CFRange]): Unit = extern


object functions:
  import _root_.cfbase.aliases.*
  import _root_.cfbase.structs.*
  import extern_functions.*
  export extern_functions.*

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFRangeMake(loc : CFIndex, len : CFIndex)(using Zone): CFRange = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    __sn_wrap_cfbase_CFRangeMake(loc, len, (__ptr_0 + 0))
    !(__ptr_0 + 0)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def CFRangeMake(loc : CFIndex, len : CFIndex)(__return : Ptr[CFRange]): Unit = 
    __sn_wrap_cfbase_CFRangeMake(loc, len, __return)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def __CFRangeMake(loc : CFIndex, len : CFIndex)(using Zone): CFRange = 
    val __ptr_0: Ptr[CFRange] = alloc[CFRange](1)
    __sn_wrap_cfbase___CFRangeMake(loc, len, (__ptr_0 + 0))
    !(__ptr_0 + 0)

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFBase.h
  */
  def __CFRangeMake(loc : CFIndex, len : CFIndex)(__return : Ptr[CFRange]): Unit = 
    __sn_wrap_cfbase___CFRangeMake(loc, len, __return)

object constants:
  val kCFCompareLessThan: CInt = -1
  val kCFCompareEqualTo: CInt = 0
  val kCFCompareGreaterThan: CInt = 1
  
object types:
  export _root_.cfbase.structs.*
  export _root_.cfbase.aliases.*

object all:
  export _root_.cfbase.aliases.Boolean
  export _root_.cfbase.aliases.CFAllocatorAllocateCallBack
  export _root_.cfbase.aliases.CFAllocatorCopyDescriptionCallBack
  export _root_.cfbase.aliases.CFAllocatorDeallocateCallBack
  export _root_.cfbase.aliases.CFAllocatorPreferredSizeCallBack
  export _root_.cfbase.aliases.CFAllocatorReallocateCallBack
  export _root_.cfbase.aliases.CFAllocatorRef
  export _root_.cfbase.aliases.CFAllocatorReleaseCallBack
  export _root_.cfbase.aliases.CFAllocatorRetainCallBack
  export _root_.cfbase.aliases.CFAllocatorTypeID
  export _root_.cfbase.aliases.CFComparatorFunction
  export _root_.cfbase.aliases.CFComparisonResult
  export _root_.cfbase.aliases.CFHashCode
  export _root_.cfbase.aliases.CFIndex
  export _root_.cfbase.aliases.CFMutableStringRef
  export _root_.cfbase.aliases.CFNullRef
  export _root_.cfbase.aliases.CFOptionFlags
  export _root_.cfbase.aliases.CFPropertyListRef
  export _root_.cfbase.aliases.CFStringRef
  export _root_.cfbase.aliases.CFTypeID
  export _root_.cfbase.aliases.CFTypeRef
  export _root_.cfbase.structs.CFAllocatorContext
  export _root_.cfbase.structs.CFRange
  export _root_.cfbase.structs.__CFAllocator
  export _root_.cfbase.structs.__CFNull
  export _root_.cfbase.structs.__CFString
  export _root_.cfbase.functions.CFAllocatorAllocate
  export _root_.cfbase.functions.CFAllocatorAllocateBytes
  export _root_.cfbase.functions.CFAllocatorAllocateTyped
  export _root_.cfbase.functions.CFAllocatorCreate
  export _root_.cfbase.functions.CFAllocatorDeallocate
  export _root_.cfbase.functions.CFAllocatorGetContext
  export _root_.cfbase.functions.CFAllocatorGetDefault
  export _root_.cfbase.functions.CFAllocatorGetPreferredSizeForSize
  export _root_.cfbase.functions.CFAllocatorGetTypeID
  export _root_.cfbase.functions.CFAllocatorReallocate
  export _root_.cfbase.functions.CFAllocatorReallocateBytes
  export _root_.cfbase.functions.CFAllocatorReallocateTyped
  export _root_.cfbase.functions.CFAllocatorSetDefault
  export _root_.cfbase.functions.CFAutorelease
  export _root_.cfbase.functions.CFCopyDescription
  export _root_.cfbase.functions.CFCopyTypeIDDescription
  export _root_.cfbase.functions.CFEqual
  export _root_.cfbase.functions.CFGetAllocator
  export _root_.cfbase.functions.CFGetRetainCount
  export _root_.cfbase.functions.CFGetTypeID
  export _root_.cfbase.functions.CFHash
  export _root_.cfbase.functions.CFMakeCollectable
  export _root_.cfbase.functions.CFNullGetTypeID
  export _root_.cfbase.functions.CFRelease
  export _root_.cfbase.functions.CFRetain
  export _root_.cfbase.functions.CFRangeMake
  export _root_.cfbase.functions.__CFRangeMake

// TODO:
@extern
object SecItem:
  def plutusKCFAllocatorNull: cfbase.aliases.CFAllocatorRef = extern