package moe.tarsin.ehviewer

import platform.posix.size_t

private val MAX_ALLOWED_CONVERT_VALUE = size_t.MAX_VALUE.toLong()

actual fun Long.tosize_t(): size_t {
    check(this <= MAX_ALLOWED_CONVERT_VALUE)
    return toUInt()
}
