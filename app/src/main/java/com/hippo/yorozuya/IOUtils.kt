package com.hippo.yorozuya

import okhttp3.ResponseBody
import java.io.File

fun ResponseBody.copyToFile(file: File) {
    val os = file.outputStream()
    val channel = os.channel
    val source = source()
    try {
        channel.transferFrom(source, 0, contentLength())
    } catch (e: Throwable) {
        throw e
    } finally {
        source.close()
        channel.close()
        os.close()
    }
}
