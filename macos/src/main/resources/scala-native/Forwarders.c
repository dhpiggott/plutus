#include <CoreFoundation/CFNumber.h>
#include <Security/SecItem.h>

// CFStringRef SecUseDataProtectionKeychain()
// {
//     return kSecUseDataProtectionKeychain;
// }

// CFStringRef SecAttrSynchronizable()
// {
//     return kSecAttrSynchronizable;
// }

// CFStringRef SecAttrAccessControl()
// {
//     return kSecAttrAccessControl;
// }

// CFStringRef SecAttrAccessibleWhenUnlocked()
// {
//     return kSecAttrAccessibleWhenUnlocked;
// }

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
