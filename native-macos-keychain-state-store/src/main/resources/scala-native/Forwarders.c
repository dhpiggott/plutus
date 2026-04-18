#include <CoreFoundation/CFNumber.h>
#include <Security/SecItem.h>

// TODO: Replace these with https://sn-bindgen.indoorvivants.com/semantics/index.html#some-macro-definitions-are-supported.
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
