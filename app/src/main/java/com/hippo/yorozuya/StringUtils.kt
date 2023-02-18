package com.hippo.yorozuya

import org.jsoup.parser.Parser

fun String.unescapeXml(): String {
    return Parser.unescapeEntities(this, true)
}
