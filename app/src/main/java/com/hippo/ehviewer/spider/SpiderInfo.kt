@file:OptIn(ExperimentalSerializationApi::class)

package com.hippo.ehviewer.spider

import com.hippo.unifile.UniFile
import com.hippo.yorozuya.IOUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.io.InputStream

@Suppress("ArrayInDataClass")
@Serializable
data class SpiderInfo @JvmOverloads constructor(
    val gid: Long,

    val pages: Int,

    val pTokenMap: Array<String?> = arrayOfNulls(pages),

    var startPage: Int = 0,

    var token: String? = null,

    var previewPages: Int = -1,

    var previewPerPage: Int = -1
) {
    fun write(file: UniFile) {
        file.openOutputStream().use {
            it.write(Cbor.encodeToByteArray(this))
        }
    }

    fun write(file: File) {
        file.writeBytes(Cbor.encodeToByteArray(this))
    }

    companion object {
        @JvmStatic
        fun read(file: UniFile): SpiderInfo? {
            return runCatching {
                file.openInputStream().use {
                    Cbor.decodeFromByteArray<SpiderInfo>(it.readBytes())
                }
            }.getOrNull() ?: runCatching {
                file.openInputStream().use { readV1(it) }
            }.getOrNull()
        }

        // This path is for diskcache only, no need to compatible with old data
        @JvmStatic
        fun read(file: File): SpiderInfo? {
            return Cbor.decodeFromByteArray(file.readBytes())
        }

        private fun readV1(inputStream: InputStream): SpiderInfo {
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
            val pTokenMap = arrayOfNulls<String>(pages)
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

        const val TOKEN_FAILED = "failed"
    }
}