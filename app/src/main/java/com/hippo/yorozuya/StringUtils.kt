package com.hippo.yorozuya

fun String.unescapeXml(): String {
    return StringUtils.unescapeXml(this)
}
