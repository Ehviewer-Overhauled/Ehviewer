package com.hippo.ehviewer.spider

import coil.disk.DiskCache
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.legacy.readLegacySpiderInfo
import com.hippo.unifile.UniFile
import com.hippo.unifile.openInputStream
import com.hippo.unifile.openOutputStream
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

    var previewPerPage: Int = -1,
)

fun SpiderInfo.write(file: UniFile) {
    file.openOutputStream().use {
        it.write(Cbor.encodeToByteArray(this))
    }
}

fun SpiderInfo.saveToCache() {
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

private val spiderInfoCache by lazy {
    DiskCache.Builder()
        .directory(File(EhApplication.application.cacheDir, "spider_info_v2_1"))
        .maxSizeBytes(20 * 1024 * 1024).build()
}

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
