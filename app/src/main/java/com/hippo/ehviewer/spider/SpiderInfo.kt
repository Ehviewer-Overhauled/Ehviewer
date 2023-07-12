package com.hippo.ehviewer.spider

import com.hippo.ehviewer.coil.edit
import com.hippo.ehviewer.coil.read
import com.hippo.ehviewer.ktbuilder.diskCache
import com.hippo.ehviewer.legacy.readLegacySpiderInfo
import com.hippo.unifile.UniFile
import com.hippo.unifile.openInputStream
import com.hippo.unifile.openOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import moe.tarsin.coroutines.runSuspendCatching
import splitties.init.appCtx
import java.io.File

@Serializable
class SpiderInfo(
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
    runSuspendCatching {
        spiderInfoCache.edit(gid.toString()) {
            data.toFile().writeBytes(Cbor.encodeToByteArray(this@saveToCache))
        }
    }.onFailure {
        it.printStackTrace()
    }
}

private val spiderInfoCache by lazy {
    diskCache {
        directory(File(appCtx.cacheDir, "spider_info_v2_1"))
        maxSizeBytes(20 * 1024 * 1024)
    }
}

fun readFromCache(gid: Long): SpiderInfo? {
    return spiderInfoCache.read(gid.toString()) {
        runCatching {
            Cbor.decodeFromByteArray<SpiderInfo>(data.toFile().readBytes())
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }
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
