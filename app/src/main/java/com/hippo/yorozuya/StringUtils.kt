package com.hippo.yorozuya

import org.jsoup.parser.Parser

fun String.unescapeXml(): String {
    return Parser.unescapeEntities(this, true)
}

inline infix fun <T> CharSequence.trimAnd(block: CharSequence.() -> T): T {
    return block(trim())
}
