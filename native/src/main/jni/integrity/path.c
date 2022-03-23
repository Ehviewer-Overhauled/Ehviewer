//
// Created by Thom on 2019-05-03.
//

#include "path.h"
#include <stdio.h>

static inline bool isSystem(const char *str) {
    return str != NULL
           && *str == '/'
           && *++str == 's'
           && *++str == 'y'
           && *++str == 's'
           && *++str == 't'
           && *++str == 'e'
           && *++str == 'm'
           && *++str == '/';
}

static inline bool isVendor(const char *str) {
    return str != NULL
           && *str == '/'
           && *++str == 'v'
           && *++str == 'e'
           && *++str == 'n'
           && *++str == 'd'
           && *++str == 'o'
           && *++str == 'r'
           && *++str == '/';
}

static inline bool isOem(const char *str) {
    return str != NULL
           && *str == '/'
           && *++str == 'o'
           && *++str == 'e'
           && *++str == 'm'
           && *++str == '/';
}

bool isThirdParty(const char *str) {
    if (isSystem(str) || isVendor(str) || isOem(str)) {
        return false;
    } else {
        return true;
    }
}

bool isDataApp(const char *str) {
    return str != NULL
           && *str == '/'
           && *++str == 'd'
           && *++str == 'a'
           && *++str == 't'
           && *++str == 'a'
           && *++str == '/'
           && *++str == 'a'
           && *++str == 'p'
           && *++str == 'p'
           && *++str == '/';
}

bool isAsecApp(const char *str) {
    return str != NULL
           && *str == '/'
           && *++str == 'm'
           && *++str == 'n'
           && *++str == 't'
           && *++str == '/'
           && *++str == 'a'
           && *++str == 's'
           && *++str == 'e'
           && *++str == 'c'
           && *++str == '/';
}

bool isExpandApp(const char *str) {
    return str != NULL
           && *str == '/'
           && *++str == 'm'
           && *++str == 'n'
           && *++str == 't'
           && *++str == '/'
           && *++str == 'e'
           && *++str == 'x'
           && *++str == 'p'
           && *++str == 'a'
           && *++str == 'n'
           && *++str == 'd'
           && *++str == '/';
}

bool isExternalSdApp(const char *str) {
    return isAsecApp(str) || isExpandApp(str);
}