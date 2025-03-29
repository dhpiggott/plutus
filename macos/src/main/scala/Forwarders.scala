package macos

import scala.scalanative.unsafe.*

@extern
object Forwarders:

  // def SecUseDataProtectionKeychain: macos.aliases.CFStringRef = extern

  // def SecAttrSynchronizable: macos.aliases.CFStringRef = extern

  // def SecAttrAccessControl: macos.aliases.CFStringRef = extern

  // def SecAttrAccessibleWhenUnlocked: macos.aliases.CFStringRef = extern

  def SecClass: macos.aliases.CFStringRef = extern

  def SecClassGenericPassword: macos.aliases.CFStringRef = extern

  def SecAttrAccount: macos.aliases.CFStringRef = extern

  def SecValueData: macos.aliases.CFStringRef = extern

  def SecReturnData: macos.aliases.CFStringRef = extern

  def CFBooleanTrue: macos.aliases.CFBooleanRef = extern
