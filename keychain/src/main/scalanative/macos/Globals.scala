package macos

import macos.aliases.*

import scala.scalanative.unsafe.*

// CoreFoundation/Security expose `kSecClass` and friends as `extern const
// CFStringRef` globals. sn-bindgen only emits bindings for functions, types and
// structs — it does not emit accessors for `extern const` variables. Scala
// Native's own `var name: T = extern` reads the C global fine, so we declare
// them directly here.
//
// sn-bindgen may grow native support for `extern const` globals
// (https://github.com/indoorvivants/sn-bindgen/pull/409), which would let this
// file go away.
@extern
object Globals:

  var kSecClass: CFStringRef = extern

  var kSecClassGenericPassword: CFStringRef = extern

  var kSecAttrAccount: CFStringRef = extern

  var kSecValueData: CFStringRef = extern

  var kSecReturnData: CFStringRef = extern

  var kCFBooleanTrue: CFBooleanRef = extern
