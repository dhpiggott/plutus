package cfdictionary

import _root_.scala.scalanative.unsafe.*
import _root_.scala.scalanative.unsigned.*
import _root_.scala.scalanative.libc.*
import _root_.scala.scalanative.*

object aliases:
  import _root_.cfdictionary.aliases.*
  import _root_.cfdictionary.structs.*
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
   * Type of the callback function used by the apply functions of CFDictionarys.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type CFDictionaryRef = Ptr[__CFDictionary]
  object CFDictionaryRef: 
    given _tag: Tag[CFDictionaryRef] = Tag.Ptr[__CFDictionary](__CFDictionary._tag)
    inline def apply(inline o: Ptr[__CFDictionary]): CFDictionaryRef = o
    extension (v: CFDictionaryRef)
      inline def value: Ptr[__CFDictionary] = v

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
   * This is the type of a reference to mutable CFDictionarys.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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

object structs:
  import _root_.cfdictionary.aliases.*
  import _root_.cfdictionary.structs.*
  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
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
  opaque type __CFAllocator = CStruct0
  object __CFAllocator:
    given _tag: Tag[__CFAllocator] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  opaque type __CFDictionary = CStruct0
  object __CFDictionary:
    given _tag: Tag[__CFDictionary] = Tag.materializeCStruct0Tag

  /**
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.sdk/System/Library/Frameworks/CoreFoundation.framework/Headers/CFBase.h
  */
  opaque type __CFString = CStruct0
  object __CFString:
    given _tag: Tag[__CFString] = Tag.materializeCStruct0Tag


@extern
private[cfdictionary] object extern_functions:
  import _root_.cfdictionary.aliases.*
  import _root_.cfdictionary.structs.*
  /**
   * Adds the key-value pair to the dictionary if no such key already exists.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryAddValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte], value : Ptr[Byte]): Unit = extern

  /**
   * Calls a function once for each value in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryApplyFunction(theDict : CFDictionaryRef, applier : CFDictionaryApplierFunction, context : Ptr[Byte]): Unit = extern

  /**
   * Reports whether or not the key is in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryContainsKey(theDict : CFDictionaryRef, key : Ptr[Byte]): Boolean = extern

  /**
   * Reports whether or not the value is in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryContainsValue(theDict : CFDictionaryRef, value : Ptr[Byte]): Boolean = extern

  /**
   * Creates a new immutable dictionary with the given values.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreate(allocator : CFAllocatorRef, keys : Ptr[Ptr[Byte]], values : Ptr[Ptr[Byte]], numValues : CFIndex, keyCallBacks : Ptr[CFDictionaryKeyCallBacks], valueCallBacks : Ptr[CFDictionaryValueCallBacks]): CFDictionaryRef = extern

  /**
   * Creates a new immutable dictionary with the key-value pairs from the given dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreateCopy(allocator : CFAllocatorRef, theDict : CFDictionaryRef): CFDictionaryRef = extern

  /**
   * Creates a new mutable dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreateMutable(allocator : CFAllocatorRef, capacity : CFIndex, keyCallBacks : Ptr[CFDictionaryKeyCallBacks], valueCallBacks : Ptr[CFDictionaryValueCallBacks]): CFMutableDictionaryRef = extern

  /**
   * Creates a new mutable dictionary with the key-value pairs from the given dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryCreateMutableCopy(allocator : CFAllocatorRef, capacity : CFIndex, theDict : CFDictionaryRef): CFMutableDictionaryRef = extern

  /**
   * Returns the number of values currently in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetCount(theDict : CFDictionaryRef): CFIndex = extern

  /**
   * Counts the number of times the given key occurs in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetCountOfKey(theDict : CFDictionaryRef, key : Ptr[Byte]): CFIndex = extern

  /**
   * Counts the number of times the given value occurs in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetCountOfValue(theDict : CFDictionaryRef, value : Ptr[Byte]): CFIndex = extern

  /**
   * Fills the two buffers with the keys and values from the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetKeysAndValues(theDict : CFDictionaryRef, keys : Ptr[Ptr[Byte]], values : Ptr[Ptr[Byte]]): Unit = extern

  /**
   * Returns the type identifier of all CFDictionary instances.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetTypeID(): CFTypeID = extern

  /**
   * Retrieves the value associated with the given key.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetValue(theDict : CFDictionaryRef, key : Ptr[Byte]): Ptr[Byte] = extern

  /**
   * Retrieves the value associated with the given key.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryGetValueIfPresent(theDict : CFDictionaryRef, key : Ptr[Byte], value : Ptr[Ptr[Byte]]): Boolean = extern

  /**
   * Removes all the values from the dictionary, making it empty.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryRemoveAllValues(theDict : CFMutableDictionaryRef): Unit = extern

  /**
   * Removes the value of the key from the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryRemoveValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte]): Unit = extern

  /**
   * Replaces the value of the key in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionaryReplaceValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte], value : Ptr[Byte]): Unit = extern

  /**
   * Sets the value of the key in the dictionary.
  
   * [bindgen] header: /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/CoreFoundation.framework/Versions/A/Headers/CFDictionary.h
  */
  def CFDictionarySetValue(theDict : CFMutableDictionaryRef, key : Ptr[Byte], value : Ptr[Byte]): Unit = extern


object functions:
  import _root_.cfdictionary.aliases.*
  import _root_.cfdictionary.structs.*
  import extern_functions.*
  export extern_functions.*

object types:
  export _root_.cfdictionary.structs.*
  export _root_.cfdictionary.aliases.*

object all:
  export _root_.cfdictionary.aliases.Boolean
  export _root_.cfdictionary.aliases.CFAllocatorRef
  export _root_.cfdictionary.aliases.CFDictionaryApplierFunction
  export _root_.cfdictionary.aliases.CFDictionaryCopyDescriptionCallBack
  export _root_.cfdictionary.aliases.CFDictionaryEqualCallBack
  export _root_.cfdictionary.aliases.CFDictionaryHashCallBack
  export _root_.cfdictionary.aliases.CFDictionaryRef
  export _root_.cfdictionary.aliases.CFDictionaryReleaseCallBack
  export _root_.cfdictionary.aliases.CFDictionaryRetainCallBack
  export _root_.cfdictionary.aliases.CFHashCode
  export _root_.cfdictionary.aliases.CFIndex
  export _root_.cfdictionary.aliases.CFMutableDictionaryRef
  export _root_.cfdictionary.aliases.CFStringRef
  export _root_.cfdictionary.aliases.CFTypeID
  export _root_.cfdictionary.structs.CFDictionaryKeyCallBacks
  export _root_.cfdictionary.structs.CFDictionaryValueCallBacks
  export _root_.cfdictionary.structs.__CFAllocator
  export _root_.cfdictionary.structs.__CFDictionary
  export _root_.cfdictionary.structs.__CFString
  export _root_.cfdictionary.functions.CFDictionaryAddValue
  export _root_.cfdictionary.functions.CFDictionaryApplyFunction
  export _root_.cfdictionary.functions.CFDictionaryContainsKey
  export _root_.cfdictionary.functions.CFDictionaryContainsValue
  export _root_.cfdictionary.functions.CFDictionaryCreate
  export _root_.cfdictionary.functions.CFDictionaryCreateCopy
  export _root_.cfdictionary.functions.CFDictionaryCreateMutable
  export _root_.cfdictionary.functions.CFDictionaryCreateMutableCopy
  export _root_.cfdictionary.functions.CFDictionaryGetCount
  export _root_.cfdictionary.functions.CFDictionaryGetCountOfKey
  export _root_.cfdictionary.functions.CFDictionaryGetCountOfValue
  export _root_.cfdictionary.functions.CFDictionaryGetKeysAndValues
  export _root_.cfdictionary.functions.CFDictionaryGetTypeID
  export _root_.cfdictionary.functions.CFDictionaryGetValue
  export _root_.cfdictionary.functions.CFDictionaryGetValueIfPresent
  export _root_.cfdictionary.functions.CFDictionaryRemoveAllValues
  export _root_.cfdictionary.functions.CFDictionaryRemoveValue
  export _root_.cfdictionary.functions.CFDictionaryReplaceValue
  export _root_.cfdictionary.functions.CFDictionarySetValue