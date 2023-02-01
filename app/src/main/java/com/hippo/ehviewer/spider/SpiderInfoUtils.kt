package com.hippo.ehviewer.spider

import coil.disk.DiskCache
import com.hippo.compat.readLegacySpiderInfo
import com.hippo.ehviewer.EhApplication
import com.hippo.unifile.UniFile
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.io.File

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
