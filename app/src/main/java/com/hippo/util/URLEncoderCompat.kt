package com.hippo.util

import android.os.Build
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

fun encode(s: String, charset: Charset): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        URLEncoder.encode(s, charset)
    } else {
        URLEncoder.encode(s, charset.name())
    }
}

fun encodeUTF8(s: String): String {
    return encode(s, StandardCharsets.UTF_8)
}