package com.hippo.yorozuya

import okhttp3.ResponseBody
import java.io.File

fun ResponseBody.copyToFile(file: File) {
    file.outputStream().use { os ->
        source().use {
            os.channel.transferFrom(it, 0, Long.MAX_VALUE)
        }
    }
}
