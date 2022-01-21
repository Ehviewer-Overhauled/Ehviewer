#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <fcntl.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <unistd.h>
#include <dlfcn.h>
#include <sys/system_properties.h>
#include <inttypes.h>
#include <android/log.h>
#include <errno.h>

#include "plt.h"
#include "inline.h"
#include "apk-sign-v2.h"
#include "am-proxy.h"
#include "pm.h"
#include "common.h"
#include "openat.h"
#include "path.h"

#ifdef CHECK_MOUNT
#include "mount.h"
#endif

static int genuine = CHECK_TRUE;

static int sdk;

static int uid;

static inline void fill_maps(char v[]) {
    static unsigned int m = 0;
    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }
    v[0x0] = '-';
    v[0x1] = 's';
    v[0x2] = 'v';
    v[0x3] = 'j';
    v[0x4] = 'e';
    v[0x5] = '(';
    v[0x6] = '{';
    v[0x7] = 'l';
    v[0x8] = 'f';
    v[0x9] = 'm';
    v[0xa] = '#';
    v[0xb] = 'm';
    v[0xc] = '`';
    v[0xd] = 'r';
    v[0xe] = 'p';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
}

#ifndef NO_CHECK_MAPS
static inline void fill_cannot_open_proc_self_maps(char v[]) {
    // cannot open /proc/self/maps
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'g';
    v[0x1] = 'd';
    v[0x2] = 'h';
    v[0x3] = 'i';
    v[0x4] = 'g';
    v[0x5] = '}';
    v[0x6] = '*';
    v[0x7] = 'd';
    v[0x8] = '|';
    v[0x9] = 'h';
    v[0xa] = '`';
    v[0xb] = '/';
    v[0xc] = '?';
    v[0xd] = 'a';
    v[0xe] = '`';
    v[0xf] = '|';
    v[0x10] = 'w';
    v[0x11] = ':';
    v[0x12] = 'e';
    v[0x13] = 'e';
    v[0x14] = 'm';
    v[0x15] = 'd';
    v[0x16] = ',';
    v[0x17] = 'i';
    v[0x18] = 'd';
    v[0x19] = 'v';
    v[0x1a] = 't';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

static inline void fill_r(char v[]) {
    static unsigned int m = 0;

    if (m == 0) {
        m = 2;
    } else if (m == 3) {
        m = 5;
    }

    v[0x0] = 's';
    for (unsigned int i = 0; i < 0x1; ++i) {
        v[i] ^= ((i + 0x1) % m);
    }
    v[0x1] = '\0';
}

static inline void rstrip(char *line) {
    char *path = line;
    if (line != NULL) {
        while (*path && *path != '\r' && *path != '\n') {
            ++path;
        }
        if (*path) {
            *path = '\0';
        }
    }
}

static inline bool isapk(const char *str) {
    const char *dot = strrchr(str, '.');
    return dot != NULL
           && *++dot == 'a'
           && *++dot == 'p'
           && *++dot == 'k'
           && (*++dot == '\0' || *dot == '\r' || *dot == '\n');
}

static inline bool isdex(const char *str) {
    const char *dot = strrchr(str, '.');
    return dot != NULL
           && *++dot == 'd'
           && *++dot == 'e'
           && *++dot == 'x'
           && (*++dot == '\0' || *dot == '\r' || *dot == '\n');
}

static inline bool isodex(const char *str) {
    const char *dot = strrchr(str, '.');
    return dot != NULL
           && *++dot == 'o'
           && *++dot == 'd'
           && *++dot == 'e'
           && *++dot == 'x'
           && (*++dot == '\0' || *dot == '\r' || *dot == '\n');
}

static inline bool isso(const char *str) {
    const char *dot = strrchr(str, '.');
    return dot != NULL
           && *++dot == 's'
           && *++dot == 'o'
           && (*++dot == '\0' || *dot == '\r' || *dot == '\n');
}

#ifdef ANTI_ODEX
static inline size_t fill_dex2oat_cmdline(char v[]) {
    // dex2oat-cmdline
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'f';
    v[0x1] = 'f';
    v[0x2] = '|';
    v[0x3] = '7';
    v[0x4] = 'i';
    v[0x5] = 'f';
    v[0x6] = '|';
    v[0x7] = '$';
    v[0x8] = 'i';
    v[0x9] = 'f';
    v[0xa] = 'h';
    v[0xb] = 'l';
    v[0xc] = 'h';
    v[0xd] = 'l';
    v[0xe] = 'f';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
    return 0xf;
}

static inline size_t fill_dex_file(char v[]) {
    // --dex-file
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = '.';
    v[0x1] = ')';
    v[0x2] = 'a';
    v[0x3] = 'c';
    v[0x4] = 'x';
    v[0x5] = ',';
    v[0x6] = 'd';
    v[0x7] = 'j';
    v[0x8] = 'h';
    v[0x9] = '`';
    for (unsigned int i = 0; i < 0xa; ++i) {
        v[i] ^= ((i + 0xa) % m);
    }
    v[0xa] = '\0';
    return 0xa;
}

static inline int checkOdex(const char *path) {
    size_t len;
    char *cmdline;
    char buffer[0x400], find[64];

    int ret = 0;
    int fd = open(path, (unsigned) O_RDONLY | (unsigned) O_CLOEXEC);
    if (fd == -1) {
        return 1;
    }

    lseek(fd, 0x1000, SEEK_SET);
    read(fd, buffer, 0x400);

    cmdline = buffer;
    len = fill_dex2oat_cmdline(find) + 1;
    for (int i = 0; i < 0x200; ++i, ++cmdline) {
        if (memcmp(cmdline, find, len) == 0) {
            cmdline += len;
            fill_dex_file(find);
            if ((ret = (strstr(cmdline, find) != NULL))) {
                fill_dex2oat_cmdline(find);
                LOGE(find);
                LOGE(cmdline);
            }
            break;
        }
    }

    close(fd);

    return ret;
}
#endif

static inline bool isSameFile(const char *path1, const char *path2) {
    struct stat stat1, stat2;
    if (path1 == NULL || path2 == NULL) {
        return false;
    }
    if (lstat(path1, &stat1)) {
        return false;
    }
    if (lstat(path2, &stat2)) {
        return false;
    }
    return stat1.st_dev == stat2.st_dev && stat1.st_ino == stat2.st_ino;
}

static inline void fill_ba88(char v[]) {
    // ba887869b4a4a6d1e915d383fad97c49
    static unsigned int m = 0;
    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }
    v[0x0] = 'f';
    v[0x1] = 'o';
    v[0x2] = '[';
    v[0x3] = 'l';
    v[0x4] = 'r';
    v[0x5] = 'b';
    v[0x6] = 'z';
    v[0x7] = 'h';
    v[0x8] = '~';
    v[0x9] = 'n';
    v[0xa] = 'S';
    v[0xb] = 'p';
    v[0xc] = 'i';
    v[0xd] = 'f';
    v[0xe] = 'q';
    for (unsigned int i = 0; i < 0xf; ++i) {
        v[i] ^= ((i + 0xf) % m);
    }
    v[0xf] = '\0';
}

static inline bool isSame(const char *path1, const char *path2) {
    if (path1[0] == '/') {
        return strcmp(path1, path2) == 0;
    } else {
        return strcmp(path1, strrchr(path2, '/') + 1) == 0;
    }
}

static inline void fill_cannot_find_s(char v[]) {
    // cannot find %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'b';
    v[0x1] = 'c';
    v[0x2] = 'm';
    v[0x3] = 'j';
    v[0x4] = 'j';
    v[0x5] = 'r';
    v[0x6] = '\'';
    v[0x7] = 'n';
    v[0x8] = '`';
    v[0x9] = 'd';
    v[0xa] = 'o';
    v[0xb] = ',';
    v[0xc] = '%';
    v[0xd] = 'r';
    for (unsigned int i = 0; i < 0xe; ++i) {
        v[i] ^= ((i + 0xe) % m);
    }
    v[0xe] = '\0';
}

enum {
    TYPE_NON,
    TYPE_APK,
    TYPE_DEX,
    TYPE_SO,
};

static inline int checkMaps(const char *packageName, const char *packagePath) {
    FILE *fp = NULL;
    char line[PATH_MAX];
    char maps[0x10];
    int check = genuine;
    bool loaded = false;

    fill_maps(maps);
    int fd = (int) openAt(AT_FDCWD, maps, O_RDONLY);
#ifdef DEBUG_OPENAT
    LOGI("openat %s returns %d", maps, fd);
#endif
    if (fd < 0) {
        fill_cannot_open_proc_self_maps(line);
        LOGE(line);
        return CHECK_ERROR;
    }

    Symbol symbol;
    fill_ba88(line);
    memset(&symbol, 0, sizeof(Symbol));
    symbol.check = (PLT_CHECK_PLT_APP | PLT_CHECK_NAME);
    symbol.symbol_name = (char *) line;
    if (dl_iterate_phdr_symbol(&symbol)) {
        LOGE(line);
        check = CHECK_ERROR;
        goto clean2;
    }

#if __ANDROID_API__ >= 21 || !defined(__arm__)
    if (symbol.size == 0) {
        LOGE(line);
        check = CHECK_ERROR;
        goto clean2;
    }
#endif

    char mode[0x2];
    fill_r(mode);

    fp = fdopen(fd, mode);
    if (fp == NULL) {
        fill_cannot_open_proc_self_maps(line);
        LOGE(line);
        check = CHECK_ERROR;
        goto clean3;
    }

    while (fgets(line, PATH_MAX - 1, fp) != NULL) {
        int type;
        char *path = line;
        if (strchr(line, '/') == NULL) {
            continue;
        }
        while (*path != '/') {
            ++path;
        }
        rstrip(path);
        if (isapk(path)) {
            type = TYPE_APK;
        } else if (isodex(path) || isdex(path)) {
            type = TYPE_DEX;
        } else if (isso(path)) {
            type = TYPE_SO;
            if (symbol.size > 0) {
                bool found = false;
                for (int i = 0; i < symbol.size; ++i) {
                    if (symbol.names[i] != NULL) {
                        if (isSame(symbol.names[i], path)) {
                            free(symbol.names[i]);
                            symbol.names[i] = NULL;
                        } else {
                            found = true;
                        }
                    }
                }
                if (!found) {
                    symbol.size = 0;
                }
            }
        } else {
            type = TYPE_NON;
        }
#ifdef DEBUG_MAPS
        LOGI("type: %d, line: %s", type, line);
#endif
        if (strstr(path, packageName) != NULL && access(path, F_OK) == 0) {
            if (type == TYPE_APK) {
#ifdef DEBUG
                LOGI("check %s", path);
#endif
                if (isSameFile(path, packagePath)) {
                    loaded = true;
                } else {
                    LOGW(path);
                }
            } else if (type == TYPE_DEX) {
#ifdef ANTI_ODEX
#ifdef DEBUG
                LOGI("check %s", path);
#endif
                if (checkOdex(path)) {
                    LOGE(path);
                    check = CHECK_ODEX;
                }
#endif
            }
        } else if (isDataApp(path)) {
            if (type == TYPE_DEX) {
                LOGE(path);
                check = CHECK_DEX;
            } else if (type == TYPE_APK) {
#ifdef ANTI_OVERLAY
                LOGE(path);
                check = CHECK_OVERLAY;
#endif
            }
        }
    }

    fclose(fp);

clean3:
    close(fd);

clean2:
    fill_cannot_find_s(line);
    if (!loaded) {
        LOGE(line, packagePath);
        check = CHECK_ERROR;
    }
    for (int i = 0; i < symbol.size; ++i) {
        if (symbol.names[i] != NULL) {
            if (strchr(symbol.names[i], '!') == NULL) {
                LOGE(line, symbol.names[i]);
                check = CHECK_ERROR;
            }
            free(symbol.names[i]);
            symbol.names[i] = NULL;
        }
    }
    if (symbol.names != NULL) {
        free(symbol.names);
        symbol.names = NULL;
    }

    return check;
}
#endif

#ifdef CHECK_HOOK
static inline void fill_jniRegisterNativeMethods(char v[]) {
    // jniRegisterNativeMethods
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'k';
    v[0x1] = 'l';
    v[0x2] = 'j';
    v[0x3] = 'V';
    v[0x4] = '`';
    v[0x5] = 'a';
    v[0x6] = 'n';
    v[0x7] = '{';
    v[0x8] = '}';
    v[0x9] = 'o';
    v[0xa] = 'y';
    v[0xb] = 'B';
    v[0xc] = 'l';
    v[0xd] = 'z';
    v[0xe] = 'f';
    v[0xf] = 'f';
    v[0x10] = 't';
    v[0x11] = '_';
    v[0x12] = 'v';
    v[0x13] = '`';
    v[0x14] = '}';
    v[0x15] = 'y';
    v[0x16] = 'd';
    v[0x17] = 'r';
    for (unsigned int i = 0; i < 0x18; ++i) {
        v[i] ^= ((i + 0x18) % m);
    }
    v[0x18] = '\0';
}

static inline void fill___openat(char v[]) {
    // __openat
    static unsigned int m = 0;

    if (m == 0) {
        m = 7;
    } else if (m == 11) {
        m = 13;
    }

    v[0x0] = '^';
    v[0x1] = ']';
    v[0x2] = 'l';
    v[0x3] = 't';
    v[0x4] = '`';
    v[0x5] = 'h';
    v[0x6] = 'a';
    v[0x7] = 'u';
    for (unsigned int i = 0; i < 0x8; ++i) {
        v[i] ^= ((i + 0x8) % m);
    }
    v[0x8] = '\0';
}

static inline void fill_openat(char v[]) {
    // openat
    static unsigned int m = 0;

    if (m == 0) {
        m = 5;
    } else if (m == 7) {
        m = 11;
    }

    v[0x0] = 'n';
    v[0x1] = 'r';
    v[0x2] = 'f';
    v[0x3] = 'j';
    v[0x4] = 'a';
    v[0x5] = 'u';
    for (unsigned int i = 0; i < 0x6; ++i) {
        v[i] ^= ((i + 0x6) % m);
    }
    v[0x6] = '\0';
}

static inline void fill_open(char v[]) {
    // open
    static unsigned int m = 0;

    if (m == 0) {
        m = 3;
    } else if (m == 5) {
        m = 7;
    }

    v[0x0] = 'n';
    v[0x1] = 'r';
    v[0x2] = 'e';
    v[0x3] = 'o';
    for (unsigned int i = 0; i < 0x4; ++i) {
        v[i] ^= ((i + 0x4) % m);
    }
    v[0x4] = '\0';
}

#endif

static inline void fill_sdk_d_genuine_d(char v[]) {
    // sdk: %d, genuine: %d
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'r';
    v[0x1] = 'f';
    v[0x2] = 'h';
    v[0x3] = '>';
    v[0x4] = '%';
    v[0x5] = '#';
    v[0x6] = 'c';
    v[0x7] = '$';
    v[0x8] = ')';
    v[0x9] = 'm';
    v[0xa] = 'n';
    v[0xb] = 'b';
    v[0xc] = 'x';
    v[0xd] = 'g';
    v[0xe] = 'a';
    v[0xf] = 'u';
    v[0x10] = '+';
    v[0x11] = '2';
    v[0x12] = '%';
    v[0x13] = 'e';
    for (unsigned int i = 0; i < 0x14; ++i) {
        v[i] ^= ((i + 0x14) % m);
    }
    v[0x14] = '\0';
}

static inline void fill_add_sigcont(char v[]) {
    // add sigcont handler
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 'c';
    v[0x1] = 'g';
    v[0x2] = '`';
    v[0x3] = '%';
    v[0x4] = 'u';
    v[0x5] = 'n';
    v[0x6] = 'o';
    v[0x7] = 'j';
    v[0x8] = 'e';
    v[0x9] = 'e';
    v[0xa] = 'x';
    v[0xb] = '-';
    v[0xc] = 'f';
    v[0xd] = 'n';
    v[0xe] = '~';
    v[0xf] = 'd';
    v[0x10] = 'm';
    v[0x11] = 'g';
    v[0x12] = 'q';
    for (unsigned int i = 0; i < 0x13; ++i) {
        v[i] ^= ((i + 0x13) % m);
    }
    v[0x13] = '\0';
}

static inline void fill_received_sigcont(char v[]) {
    // received sigcont
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'q';
    v[0x1] = 'a';
    v[0x2] = 'f';
    v[0x3] = 'c';
    v[0x4] = 'n';
    v[0x5] = '~';
    v[0x6] = 'l';
    v[0x7] = 'n';
    v[0x8] = '+';
    v[0x9] = '\x7f';
    v[0xa] = 'i';
    v[0xb] = 'f';
    v[0xc] = 'a';
    v[0xd] = 'l';
    v[0xe] = 'j';
    v[0xf] = 'q';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

#if defined(CHECK_ARM64) && defined(__arm__)
static inline void fill_ro_product_cpu_abi(char v[]) {
    // ro.product.cpu.abi
    static unsigned int m = 0;

    if (m == 0) {
        m = 17;
    } else if (m == 19) {
        m = 23;
    }

    v[0x0] = 's';
    v[0x1] = 'm';
    v[0x2] = '-';
    v[0x3] = 't';
    v[0x4] = 'w';
    v[0x5] = 'i';
    v[0x6] = 'c';
    v[0x7] = '}';
    v[0x8] = 'j';
    v[0x9] = '~';
    v[0xa] = '%';
    v[0xb] = 'o';
    v[0xc] = '}';
    v[0xd] = '{';
    v[0xe] = '!';
    v[0xf] = 'q';
    v[0x10] = 'b';
    v[0x11] = 'h';
    for (unsigned int i = 0; i < 0x12; ++i) {
        v[i] ^= ((i + 0x12) % m);
    }
    v[0x12] = '\0';
}

static inline bool isArm64V8a(const char *str) {
    return str != NULL
           && *str == 'a'
           && *++str == 'r'
           && *++str == 'm'
           && *++str == '6'
           && *++str == '4'
           && *++str == '-'
           && *++str == 'v'
           && *++str == '8'
           && *++str == 'a';
}

static inline void fill_32_64(char v[]) {
    // run in 32 on 64 machine
    static unsigned int m = 0;

    if (m == 0) {
        m = 19;
    } else if (m == 23) {
        m = 29;
    }

    v[0x0] = 'v';
    v[0x1] = 'p';
    v[0x2] = 'h';
    v[0x3] = '\'';
    v[0x4] = 'a';
    v[0x5] = 'g';
    v[0x6] = '*';
    v[0x7] = '8';
    v[0x8] = '>';
    v[0x9] = '-';
    v[0xa] = 'a';
    v[0xb] = 'a';
    v[0xc] = '0';
    v[0xd] = '\'';
    v[0xe] = '&';
    v[0xf] = ' ';
    v[0x10] = 'l';
    v[0x11] = 'c';
    v[0x12] = '`';
    v[0x13] = 'l';
    v[0x14] = 'l';
    v[0x15] = 'h';
    v[0x16] = 'b';
    for (unsigned int i = 0; i < 0x17; ++i) {
        v[i] ^= ((i + 0x17) % m);
    }
    v[0x17] = '\0';
}
#endif

static void handler(int sig __unused) {
    char v[0x11];
    fill_received_sigcont(v);
    LOGI(v);
}

static inline void fill_invalid_signature_path_s(char v[]) {
    // invalid signature, path: %s
    static unsigned int m = 0;

    if (m == 0) {
        m = 23;
    } else if (m == 29) {
        m = 31;
    }

    v[0x0] = 'm';
    v[0x1] = 'k';
    v[0x2] = 'p';
    v[0x3] = 'f';
    v[0x4] = 'd';
    v[0x5] = '`';
    v[0x6] = 'n';
    v[0x7] = '+';
    v[0x8] = '\x7f';
    v[0x9] = 'd';
    v[0xa] = 'i';
    v[0xb] = 'a';
    v[0xc] = 'q';
    v[0xd] = 'e';
    v[0xe] = 'g';
    v[0xf] = 'a';
    v[0x10] = 'q';
    v[0x11] = '9';
    v[0x12] = '6';
    v[0x13] = 'p';
    v[0x14] = '`';
    v[0x15] = 'v';
    v[0x16] = 'k';
    v[0x17] = '>';
    v[0x18] = '%';
    v[0x19] = '#';
    v[0x1a] = 't';
    for (unsigned int i = 0; i < 0x1b; ++i) {
        v[i] ^= ((i + 0x1b) % m);
    }
    v[0x1b] = '\0';
}

#ifdef DEBUG_HOOK_IO

static void check_hook_function(void *handle, const char *name) {
    void *symbol = dlsym(handle, name);
#ifdef DEBUG
    void *symbol2 = plt_dlsym(name, NULL);
    LOGI("symbol: %s, dlsym: %p, dl_iterate_phdr: %p", name, symbol, symbol2);
#endif
    if (symbol != NULL && setRead(symbol) && isInlineHooked(symbol)) {
        LOGI("%s is hooked", name);
    }
}

#define HOOK_SYMBOL(x, y) check_hook_function(x, #y)

static void check_inline_hook_io() {
    void *handle = dlopen("libc.so", RTLD_NOW);
    if (handle) {
        HOOK_SYMBOL(handle, faccessat);
        HOOK_SYMBOL(handle, __openat);
        HOOK_SYMBOL(handle, fchmodat);
        HOOK_SYMBOL(handle, fchownat);
        HOOK_SYMBOL(handle, renameat);
        HOOK_SYMBOL(handle, fstatat64);
        HOOK_SYMBOL(handle, __statfs);
        HOOK_SYMBOL(handle, __statfs64);
        HOOK_SYMBOL(handle, mkdirat);
        HOOK_SYMBOL(handle, mknodat);
        HOOK_SYMBOL(handle, truncate);
        HOOK_SYMBOL(handle, linkat);
        HOOK_SYMBOL(handle, readlinkat);
        HOOK_SYMBOL(handle, unlinkat);
        HOOK_SYMBOL(handle, symlinkat);
        HOOK_SYMBOL(handle, utimensat);
        HOOK_SYMBOL(handle, __getcwd);
        HOOK_SYMBOL(handle, chdir);
        HOOK_SYMBOL(handle, execve);
    }
    dlclose(handle);
}

#endif

static inline void fill_openat_is_hooked(char v[]) {
    // openAt is hooked
    static unsigned int m = 0;

    if (m == 0) {
        m = 13;
    } else if (m == 17) {
        m = 19;
    }

    v[0x0] = 'l';
    v[0x1] = 't';
    v[0x2] = '`';
    v[0x3] = 'h';
    v[0x4] = 'F';
    v[0x5] = '|';
    v[0x6] = ')';
    v[0x7] = 'c';
    v[0x8] = 'x';
    v[0x9] = ',';
    v[0xa] = 'h';
    v[0xb] = 'n';
    v[0xc] = 'm';
    v[0xd] = 'h';
    v[0xe] = 'a';
    v[0xf] = 'a';
    for (unsigned int i = 0; i < 0x10; ++i) {
        v[i] ^= ((i + 0x10) % m);
    }
    v[0x10] = '\0';
}

bool has_native_libs() {
#if __ANDROID_API__ >= 24
    return true;
#else
    Symbol symbol;
    char v[0x10];
    fill_ba88(v);
    memset(&symbol, 0, sizeof(Symbol));
    symbol.check = (PLT_CHECK_PLT_APP | PLT_CHECK_NAME);
    symbol.symbol_name = (char *) v;
    if (dl_iterate_phdr_symbol(&symbol)) {
#ifdef DEBUG_NATIVE
        LOGW("cannot dl_iterate_phdr_symbol");
#endif
        return false;
    }

    bool extractNativeLibs = true;
    for (int i = 0; i < symbol.size; ++i) {
        if (symbol.names[i] != NULL) {
#ifdef DEBUG_NATIVE
            LOGW("%s: %s", v, symbol.names[i]);
#endif
            if (extractNativeLibs && strstr(symbol.names[i], "apk!") != NULL) {
                extractNativeLibs = false;
            }
            free(symbol.names[i]);
            symbol.names[i] = NULL;
        }
    }
    if (symbol.names != NULL) {
        free(symbol.names);
        symbol.names = NULL;
    }

#ifdef DEBUG_NATIVE
    LOGW("has_native_libs: %s", extractNativeLibs ? "true" : "false");
#endif
    return extractNativeLibs;
#endif
}

bool checkGenuine(JNIEnv *env) {
    char v1[0x20];

    signal(SIGCONT, handler);
    fill_add_sigcont(v1);
    LOGI(v1); // 0x14

    sdk = getSdk();

    uid = getuid();

#ifdef DEBUG_HOOK_SELF
    LOGI("+++ whale +++");
    check_inline_hook_whale();
    LOGI("--- whale ----");
#if defined(__arm__) || defined(__aarch64__)
    LOGI("+++ hookzz-b +++");
    check_inline_hook_hookzz_b();
    LOGI("--- hookzz-b ---");
    LOGI("+++ hookzz +++");
    check_inline_hook_hookzz();
    LOGI("--- hookzz ---");
#endif
#if defined(__arm__)
    LOGI("+++ hookzz-b +++");
    check_inline_hook_hookzz_b();
    LOGI("--- hookzz-b ---");
    LOGI("+++ substrate +++");
    check_inline_hook_substrate();
    LOGI("--- substrate ---");
#endif
#endif

#ifdef DEBUG
    LOGI("JNI_OnLoad start, sdk: %d, uid: %d", sdk, uid);
#endif

#ifdef DEBUG_HOOK_IO
    check_inline_hook_io();
#endif

#ifdef DEBUG_MAPS
    LOGI("openAt: %p", &openAt);
#endif
    if (isInlineHooked(&openAt)) {
        fill_openat_is_hooked(v1);
        LOGE(v1);
        genuine = CHECK_FATAL;
        goto done;
    }

    char *packageName = getGenuinePackageName();
    char *packagePath = getPath(env, uid, packageName);
    if (packageName == NULL) {
        genuine = CHECK_TRUE;
        goto clean;
    }
    if (packagePath == NULL) {
        fill_cannot_find_s(v1);
        LOGE(v1, packageName);
        genuine = CHECK_FAKE;
        goto clean;
    } else {
        int sign = checkSignature(packagePath);
        if (sign) {
            fill_invalid_signature_path_s(v1);
            LOGE(v1, packagePath);
#ifndef DEBUG_FAKE
            genuine = sign < 0 ? CHECK_NOAPK : CHECK_FAKE;
            goto clean;
#endif
        }
    }

    if (uid < 10000) {
        goto clean;
    }

#ifdef CHECK_MOUNT
    checkMount(maps);
#endif

#ifndef NO_CHECK_MAPS
#ifdef DEBUG
    LOGI("checkMaps start");
#endif
    genuine = checkMaps(packageName, packagePath);
#endif

#ifdef CHECK_HOOK
#define CHECK_HOOK_SYMBOL(x) do {\
    if (genuine == CHECK_TRUE) {\
        fill_##x(v1);\
        if (isPltHooked(v1, true)) {\
            genuine = CHECK_FALSE;\
            }\
        }\
    } while(0);
    CHECK_HOOK_SYMBOL(open);
    CHECK_HOOK_SYMBOL(openat);
    CHECK_HOOK_SYMBOL(__openat);
    CHECK_HOOK_SYMBOL(jniRegisterNativeMethods);
#endif

#if defined(CHECK_ARM64) && defined(__arm__)
    if (genuine == CHECK_TRUE) {
        fill_ro_product_cpu_abi(v1);
        __system_property_get(v1, prop);
        if (isArm64V8a(prop)) {
            fill_32_64(v1);
            LOGW(v1);
            genuine = CHECK_FALSE;
        }
    }
#endif

clean:
#ifdef GENUINE_NAME
    free(packageName);
#endif
    free(packagePath);
#ifdef DEBUG
    LOGI("JNI_OnLoad end, genuine: %d", genuine);
#endif

    if (genuine != CHECK_PROXY && isAmProxy(env, sdk)) {
        genuine = CHECK_PROXY;
    }

done:
    fill_sdk_d_genuine_d(v1); // 0x15
    LOGI(v1, sdk, genuine);

    return setGenuine(env, genuine);
}
