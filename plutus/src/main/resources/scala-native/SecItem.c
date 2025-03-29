#include </Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk/System/Library/Frameworks/Security.framework/Versions/A/Headers/SecItem.h>

CFStringRef plutusKSecClass()
{
    return kSecClass;
}

CFStringRef plutusKSecClassGenericPassword()
{
    return kSecClassGenericPassword;
}

CFStringRef plutusKSecAttrAccount()
{
    return kSecAttrAccount;
}

CFStringRef plutusKSecValueData()
{
    return kSecValueData;
}

CFStringRef plutusKSecReturnData()
{
    return kSecReturnData;
}
