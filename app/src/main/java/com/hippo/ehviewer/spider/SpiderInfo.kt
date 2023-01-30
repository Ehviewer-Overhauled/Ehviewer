@file:OptIn(ExperimentalSerializationApi::class)

package com.hippo.ehviewer.spider

import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import com.hippo.compat.readLegacySpiderInfo
import com.hippo.ehviewer.EhApplication
import com.hippo.unifile.UniFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File

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
        fun readCompatFromUniFile(file: UniFile): SpiderInfo? {
            return runCatching {
                file.openInputStream().use {
                    Cbor.decodeFromByteArray<SpiderInfo>(it.readBytes())
                }
            }.getOrNull() ?: runCatching {
                file.openInputStream().use { readLegacySpiderInfo(it) }
            }.getOrNull()
        }

        const val TOKEN_FAILED = "failed"
    }
}