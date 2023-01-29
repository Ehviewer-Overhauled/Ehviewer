@file:OptIn(ExperimentalSerializationApi::class)

package com.hippo.ehviewer.spider

import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import com.hippo.ehviewer.EhApplication
import com.hippo.unifile.UniFile
import com.hippo.yorozuya.IOUtils
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File
import java.io.InputStream

@Serializable
class SpiderInfo @JvmOverloads constructor(
    val gid: Long,

    val pages: Int,

    val pTokenMap: MutableMap<Int, String> = hashMapOf(),

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

    @OptIn(ExperimentalCoilApi::class)
    fun saveToCache() {
        val entry = spiderInfoCache.edit(gid.toString()) ?: return
        runCatching {
            entry.data.toFile().writeBytes(Cbor.encodeToByteArray(this))
        }.onFailure {
            it.printStackTrace()
            entry.abort()
        }.onSuccess {
            entry.commit()
        }
    }

    companion object {
        private val spiderInfoCache by lazy {
            DiskCache.Builder()
                .directory(File(EhApplication.application.cacheDir, "spider_info_v2_1"))
                .maxSizeBytes(20 * 1024 * 1024).build()
        }

        @JvmStatic
        @OptIn(ExperimentalCoilApi::class)
        fun readFromCache(gid: Long): SpiderInfo? {
            val snapshot = spiderInfoCache[gid.toString()] ?: return null
            return runCatching {
                snapshot.use {
                    return Cbor.decodeFromByteArray(it.data.toFile().readBytes())
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }

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

        const val TOKEN_FAILED = "failed"
    }
}