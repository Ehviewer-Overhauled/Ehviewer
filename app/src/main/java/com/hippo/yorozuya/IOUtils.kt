package com.hippo.yorozuya

import okhttp3.ResponseBody
import java.io.File

fun ResponseBody.copyToFile(file: File) {
    val length = contentLength()
    check(length > 0) { "contentLength < 0!" }
    val os = file.outputStream()
    val channel = os.channel
    val source = source()
    try {
        channel.transferFrom(source, 0, length)
    } catch (e: Throwable) {
        throw e
    } finally {
        source.close()
        channel.close()
        os.close()
    }
}
