// kSecClass, kCFBooleanTrue and friends are declared in the SDK headers as
// `extern const CFStringRef` (etc.) globals exported by the framework binaries.
// sn-bindgen only generates Scala bindings for functions, types and structs —
// it does not emit accessors for `extern const` variables, so those symbols are
// unreachable from Scala Native directly. These trivial C forwarders wrap each
// constant in a function so sn-bindgen has something it will bind to.
#include <CoreFoundation/CFNumber.h>
#include <Security/SecItem.h>

CFStringRef SecClass()
{
    return kSecClass;
}

CFStringRef SecClassGenericPassword()
{
    return kSecClassGenericPassword;
}

CFStringRef SecAttrAccount()
{
    return kSecAttrAccount;
}

CFStringRef SecValueData()
{
    return kSecValueData;
}

CFStringRef SecReturnData()
{
    return kSecReturnData;
}

CFBooleanRef CFBooleanTrue()
{
    return kCFBooleanTrue;
}
