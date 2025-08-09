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
