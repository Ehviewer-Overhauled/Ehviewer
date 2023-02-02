package moe.tarsin.ehviewer

import platform.posix.size_t

actual fun Long.tosize_t(): size_t {
    return toULong()
}
