package com.hippo.compat

import com.hippo.ehviewer.spider.SpiderInfo
import com.hippo.yorozuya.IOUtils
import java.io.InputStream

fun readLegacySpiderInfo(inputStream: InputStream): SpiderInfo {
    fun read(): String {
        return IOUtils.readAsciiLine(inputStream)
    }

    fun readInt(): Int {
        return read().toInt()
    }

    fun readLong(): Long {
        return read().toLong()
    }
    repeat(2) { read() } // We assert that only info v2
    val gid = readLong()
    val token = read()
    read()
    val previewPages = readInt()
    val previewPerPage = readInt()
    val pages = read().toInt()
    val pTokenMap = hashMapOf<Int, String>()
    val info = SpiderInfo(gid, pages, pTokenMap, 0, token, previewPages, previewPerPage)
    runCatching {
        val line = read()
        val pos = line.indexOf(" ")
        if (pos > 0) {
            val index = line.substring(0, pos).toInt()
            val pToken = line.substring(pos + 1)
            if (pToken.isNotEmpty()) {
                pTokenMap[index] = pToken
            }
        }
    }
    return info
}
