package macos

import macos.aliases.*

import scala.scalanative.unsafe.*

// CoreFoundation/Security expose `kSecClass` and friends as `extern const
// CFStringRef` globals. sn-bindgen only emits bindings for functions, types and
// structs — it does not emit accessors for `extern const` variables. Scala
// Native's own `var name: T = extern` reads the C global fine, so we declare
// them directly here.
//
// TODO(https://github.com/indoorvivants/sn-bindgen/issues/150): Patch
// sn-bindgen upstream to emit `var = extern` for `extern const` globals so
// this file can go away.
@extern
object Globals:

  var kSecClass: CFStringRef = extern

  var kSecClassGenericPassword: CFStringRef = extern

  var kSecAttrAccount: CFStringRef = extern

  var kSecValueData: CFStringRef = extern

  var kSecReturnData: CFStringRef = extern

  var kCFBooleanTrue: CFBooleanRef = extern
