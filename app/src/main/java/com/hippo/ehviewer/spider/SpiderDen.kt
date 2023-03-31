/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.spider

import android.graphics.ImageDecoder
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_WRITE
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils.getSuitableTitle
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.getImageKey
import com.hippo.ehviewer.client.referer
import com.hippo.ehviewer.coil.edit
import com.hippo.ehviewer.coil.read
import com.hippo.ehviewer.gallery.SUPPORT_IMAGE_EXTENSIONS
import com.hippo.image.Image.CloseableSource
import com.hippo.image.rewriteGifSource2
import com.hippo.unifile.UniFile
import com.hippo.unifile.openOutputStream
import com.hippo.util.sendTo
import com.hippo.yorozuya.FileUtils
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.utils.io.jvm.nio.copyTo
import moe.tarsin.coroutines.runSuspendCatching
import java.io.IOException
import java.util.Locale
import kotlin.io.path.readText
import com.hippo.ehviewer.EhApplication.Companion.imageCache as sCache

private val client = EhApplication.ktorClient

class SpiderDen(private val mGalleryInfo: GalleryInfo) {
    private val mGid = mGalleryInfo.gid
    var downloadDir: UniFile? = getGalleryDownloadDir(mGid)?.takeIf { it.isDirectory }

    @Volatile
    @SpiderQueen.Mode
    var mode = SpiderQueen.MODE_READ
        set(value) {
            field = value
            if (field == SpiderQueen.MODE_DOWNLOAD) {
                val title = getSuitableTitle(mGalleryInfo)
                val dirname = FileUtils.sanitizeFilename("$mGid-$title")
                EhDB.putDownloadDirname(mGid, dirname)
                downloadDir = getGalleryDownloadDir(mGid)!!.apply { check(ensureDir()) { "Download directory $uri is not valid directory!" } }
            }
        }

    private fun containInCache(index: Int): Boolean {
        val key = getImageKey(mGid, index)
        return sCache[key]?.use { true } ?: false
    }

    private fun containInDownloadDir(index: Int): Boolean {
        val dir = downloadDir ?: return false
        return findImageFile(dir, index) != null
    }

    private fun copyFromCacheToDownloadDir(index: Int): Boolean {
        val dir = downloadDir ?: return false
        val key = getImageKey(mGid, index)
        return runCatching {
            sCache.read(key) {
                val extension = fixExtension("." + metadata.toFile().readText())
                val file = dir.createFile(perFilename(index, extension)) ?: return false
                file.openFileDescriptor("w").use { outFd ->
                    ParcelFileDescriptor.open(data.toFile(), MODE_READ_WRITE).use {
                        it sendTo outFd
                    }
                }
            }
        }.getOrElse {
            it.printStackTrace()
            false
        }
    }

    operator fun contains(index: Int): Boolean {
        return when (mode) {
            SpiderQueen.MODE_READ -> {
                containInCache(index) || containInDownloadDir(index)
            }

            SpiderQueen.MODE_DOWNLOAD -> {
                containInDownloadDir(index) || copyFromCacheToDownloadDir(index)
            }

            else -> {
                false
            }
        }
    }

    private fun removeFromCache(index: Int): Boolean {
        val key = getImageKey(mGid, index)
        return sCache.remove(key)
    }

    private fun removeFromDownloadDir(index: Int): Boolean {
        return downloadDir?.let { findImageFile(it, index)?.delete() } ?: false
    }

    fun remove(index: Int): Boolean {
        return removeFromCache(index) or removeFromDownloadDir(index)
    }

    private fun findDownloadFileForIndex(index: Int, extension: String): UniFile? {
        val dir = downloadDir ?: return null
        val ext = fixExtension(".$extension")
        return dir.createFile(perFilename(index, ext))
    }

    @Throws(IOException::class)
    suspend fun makeHttpCallAndSaveImage(
        index: Int,
        url: String,
        referer: String?,
        notifyProgress: (Long, Long, Int) -> Unit,
    ): Boolean {
        return client.prepareGet(url) {
            var state: Long = 0
            referer(referer)
            onDownload { bytesSentTotal, contentLength ->
                notifyProgress(contentLength, bytesSentTotal, (bytesSentTotal - state).toInt())
                state = bytesSentTotal
            }
        }.execute {
            if (it.status.value >= 400) return@execute false
            saveFromHttpResponse(index, it)
        }
    }

    private suspend fun saveFromHttpResponse(index: Int, body: HttpResponse): Boolean {
        val contentType = body.contentType()
        val extension = contentType?.contentSubtype ?: "jpg"
        val length = body.contentLength() ?: return false

        suspend fun doSave(outFile: UniFile): Long {
            val ret: Long
            outFile.openOutputStream().use {
                ret = body.bodyAsChannel().copyTo(it.channel)
            }
            if (contentType == ContentType.Image.GIF) {
                outFile.openFileDescriptor("rw").use {
                    rewriteGifSource2(it.fd)
                }
            }
            return ret
        }

        findDownloadFileForIndex(index, extension)?.runSuspendCatching {
            return doSave(this) == length
        }?.onFailure {
            it.printStackTrace()
            return false
        }

        // Read Mode, allow save to cache
        if (mode == SpiderQueen.MODE_READ) {
            val key = getImageKey(mGid, index)
            var received: Long = 0
            runSuspendCatching {
                sCache.edit(key) {
                    metadata.toFile().writeText(extension)
                    received = doSave(UniFile.fromFile(data.toFile())!!)
                }
            }.onFailure {
                it.printStackTrace()
            }.onSuccess {
                return received == length
            }
        }

        return false
    }

    fun saveToUniFile(index: Int, file: UniFile): Boolean {
        file.openFileDescriptor("w").use { toFd ->
            val key = getImageKey(mGid, index)

            // Read from diskCache first
            sCache[key]?.use { snapshot ->
                runCatching {
                    UniFile.fromFile(snapshot.data.toFile())!!.openFileDescriptor("r").use {
                        it sendTo toFd
                    }
                }.onSuccess {
                    return true
                }.onFailure {
                    it.printStackTrace()
                    return false
                }
            }

            // Read from download dir
            downloadDir?.let { uniFile ->
                runCatching {
                    findImageFile(uniFile, index)?.openFileDescriptor("r")?.use {
                        it sendTo toFd
                    }
                }.onFailure {
                    it.printStackTrace()
                    return false
                }.onSuccess {
                    return true
                }
            }
        }
        return false
    }

    fun checkPlainText(index: Int): Boolean {
        return false
    }

    fun getExtension(index: Int): String? {
        val key = getImageKey(mGid, index)
        return sCache[key]?.use { it.metadata.toNioPath().readText() }
            ?: downloadDir?.let { findImageFile(it, index) }
                ?.name.let { FileUtils.getExtensionFromFilename(it) }
    }

    fun getImageSource(index: Int): CloseableSource? {
        if (mode == SpiderQueen.MODE_READ) {
            val key = getImageKey(mGid, index)
            val snapshot = sCache[key]
            if (snapshot != null) {
                val source = ImageDecoder.createSource(snapshot.data.toFile())
                return object : CloseableSource, AutoCloseable by snapshot {
                    override val source = source
                }
            }
        }
        val dir = downloadDir ?: return null
        val source = findImageFile(dir, index)?.imageSource ?: return null
        return object : CloseableSource {
            override val source = source

            override fun close() {}
        }
    }

    companion object {
        fun getGalleryDownloadDir(gid: Long): UniFile? {
            val dir = Settings.downloadLocation ?: return null
            val dirname = EhDB.getDownloadDirname(gid) ?: return null
            return dir.subFile(dirname)
        }
    }
}

private val COMPAT_IMAGE_EXTENSIONS = SUPPORT_IMAGE_EXTENSIONS + ".jpeg"

/**
 * @param extension with dot
 */
fun perFilename(index: Int, extension: String?): String {
    return String.format(Locale.US, "%08d%s", index + 1, extension)
}

/**
 * @param extension with dot
 */
private fun fixExtension(extension: String): String {
    return extension.takeIf { SUPPORT_IMAGE_EXTENSIONS.contains(it) } ?: SUPPORT_IMAGE_EXTENSIONS[0]
}

private fun findImageFile(dir: UniFile, index: Int): UniFile? {
    return COMPAT_IMAGE_EXTENSIONS.firstNotNullOfOrNull { dir.findFile(perFilename(index, it)) }
}
