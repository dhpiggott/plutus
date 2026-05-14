package macos

import scala.scalanative.unsafe.*

// TODO: Lift these and any other common code into a shared module.
@extern
object Forwarders:

  def SecClass: macos.aliases.CFStringRef = extern

  def SecClassGenericPassword: macos.aliases.CFStringRef = extern

  def SecAttrAccount: macos.aliases.CFStringRef = extern

  def SecValueData: macos.aliases.CFStringRef = extern

  def SecReturnData: macos.aliases.CFStringRef = extern

  def CFBooleanTrue: macos.aliases.CFBooleanRef = extern
