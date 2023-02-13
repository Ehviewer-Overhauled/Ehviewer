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
import coil.decode.DecodeUtils
import coil.decode.FrameDelayRewritingSource
import coil.decode.isGif
import coil.disk.DiskCache
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhApplication.Companion.application
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCacheKeyFactory
import com.hippo.ehviewer.client.EhRequestBuilder
import com.hippo.ehviewer.client.EhUtils.getSuitableTitle
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.gallery.PageLoader2
import com.hippo.image.Image.CloseableSource
import com.hippo.unifile.RawFile
import com.hippo.unifile.UniFile
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.MathUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.executeAsync
import okio.buffer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.io.path.readText

private val client = EhApplication.okHttpClient
private val contentResolver = application.contentResolver

class SpiderDen(private val mGalleryInfo: GalleryInfo) {
    private val mGid: Long = mGalleryInfo.gid
    private var mDownloadDir: UniFile? = getGalleryDownloadDir(mGid)

    @Volatile
    private var mMode = SpiderQueen.MODE_READ

    fun setMode(@SpiderQueen.Mode mode: Int) {
        mMode = mode
        if (mode == SpiderQueen.MODE_DOWNLOAD) {
            ensureDownloadDir()
        }
    }

    private fun ensureDownloadDir(): Boolean {
        mDownloadDir?.let { return it.ensureDir() }
        val title = getSuitableTitle(mGalleryInfo)
        val dirname = FileUtils.sanitizeFilename("$mGid-$title")
        EhDB.putDownloadDirname(mGid, dirname)
        mDownloadDir = getGalleryDownloadDir(mGid)
        return mDownloadDir?.ensureDir() ?: false
    }

    val isReady: Boolean
        get() = when (mMode) {
            SpiderQueen.MODE_READ -> true
            SpiderQueen.MODE_DOWNLOAD -> mDownloadDir?.isDirectory ?: false
            else -> false
        }
    val downloadDir: UniFile?
        get() {
            if (mDownloadDir == null) {
                mDownloadDir = getGalleryDownloadDir(mGid)
            }
            return mDownloadDir?.takeIf { it.isDirectory }
        }

    private fun containInCache(index: Int): Boolean {
        val key = EhCacheKeyFactory.getImageKey(mGid, index)
        return sCache[key]?.apply { close() } != null
    }

    private fun containInDownloadDir(index: Int): Boolean {
        val dir = downloadDir ?: return false
        return findImageFile(dir, index) != null
    }

    /**
     * @param extension with dot
     */
    private fun fixExtension(extension: String): String {
        return extension.takeIf { PageLoader2.SUPPORT_IMAGE_EXTENSIONS.contains(it) }
            ?: PageLoader2.SUPPORT_IMAGE_EXTENSIONS[0]
    }

    private fun copyFromCacheToDownloadDir(index: Int): Boolean {
        val dir = downloadDir ?: return false
        val key = EhCacheKeyFactory.getImageKey(mGid, index)
        runCatching {
            (sCache[key] ?: return false).use { snapshot ->
                val extension = fixExtension("." + snapshot.metadata.toFile().readText())
                val file = dir.createFile(generateImageFilename(index, extension)) ?: return false
                (file.openOutputStream() as FileOutputStream).use { outputStream ->
                    outputStream.channel.use { outChannel ->
                        snapshot.data.toFile().inputStream().use { inputStream ->
                            inputStream.channel.use {
                                outChannel.transferFrom(it, 0, it.size())
                            }
                        }
                    }
                }
            }
            return true
        }
        return false
    }

    operator fun contains(index: Int): Boolean {
        return when (mMode) {
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
        val key = EhCacheKeyFactory.getImageKey(mGid, index)
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
        return dir.createFile(generateImageFilename(index, ext))
    }

    @Throws(IOException::class)
    suspend fun makeHttpCallAndSaveImage(
        index: Int,
        url: String,
        referer: String?,
        notifyProgress: (Long, Long, Int) -> Unit
    ): Boolean {
        val call = client.newCall(EhRequestBuilder(url, referer).build())
        call.executeAsync().use { response ->
            if (response.code >= 400) return false
            val body = response.body
            val extension = body.contentType()?.subtype ?: "jpg"
            val length = body.contentLength()

            suspend fun doSave(chan: FileChannel): Long {
                var receivedSize: Long = 0
                body.source().use { responseSource ->
                    val source = if (DecodeUtils.isGif(responseSource)) FrameDelayRewritingSource(
                        responseSource
                    ).buffer() else responseSource
                    source.use {
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val bytesRead = chan.transferFrom(it, receivedSize, TRANSFER_BLOCK)
                            receivedSize += bytesRead
                            notifyProgress(body.contentLength(), receivedSize, bytesRead.toInt())
                            if (bytesRead.toInt() == 0) break
                        }
                    }
                }
                return receivedSize
            }

            findDownloadFileForIndex(index, extension)?.runCatching {
                openOutputStream().use { outputStream ->
                    (outputStream as FileOutputStream).channel.use { return doSave(it) == length }
                }
            }?.onFailure {
                it.printStackTrace()
                return false
            }

            // Read Mode, allow save to cache
            if (mMode == SpiderQueen.MODE_READ) {
                val key = EhCacheKeyFactory.getImageKey(mGid, index)
                val editor = sCache.edit(key) ?: return false
                var received: Long = 0
                runCatching {
                    editor.metadata.toFile().writeText(extension)
                    editor.data.toFile().outputStream().use { outputStream ->
                        outputStream.channel.use { received = doSave(it) }
                    }
                }.onFailure {
                    editor.abort()
                    it.printStackTrace()
                    return false
                }.onSuccess {
                    editor.commit()
                    return received == length
                }
            }

            return false
        }
    }

    fun saveToUniFile(index: Int, file: UniFile): Boolean {
        (file.openOutputStream() as FileOutputStream).use { outputStream ->
            outputStream.channel.use { outChannel ->
                val key = EhCacheKeyFactory.getImageKey(mGid, index)

                fun copy(inputStream: FileInputStream) {
                    inputStream.channel.use {
                        outChannel.transferFrom(it, 0, it.size())
                    }
                }

                // Read from diskCache first
                sCache[key]?.use { snapshot ->
                    runCatching {
                        snapshot.data.toFile().inputStream().use { copy(it) }
                    }.onSuccess {
                        return true
                    }.onFailure {
                        it.printStackTrace()
                        return false
                    }
                }

                downloadDir?.let { uniFile ->
                    runCatching {
                        findImageFile(uniFile, index)?.openInputStream()?.use {
                            copy(it as FileInputStream)
                        }
                    }.onFailure {
                        it.printStackTrace()
                        return false
                    }.onSuccess {
                        return true
                    }
                }
                return false
            }
        }
    }

    fun checkPlainText(index: Int): Boolean {
        return false
    }

    fun getExtension(index: Int): String? {
        val key = EhCacheKeyFactory.getImageKey(mGid, index)
        return sCache[key]?.use { it.metadata.toNioPath().readText() }
            ?: downloadDir?.let { findImageFile(it, index) }
                ?.name.let { FileUtils.getExtensionFromFilename(it) }
    }

    fun getImageSource(index: Int): CloseableSource? {
        if (mMode == SpiderQueen.MODE_READ) {
            val key = EhCacheKeyFactory.getImageKey(mGid, index)
            val snapshot: DiskCache.Snapshot? = sCache[key]
            if (snapshot != null) {
                val source = ImageDecoder.createSource(snapshot.data.toFile())
                return object : CloseableSource {
                    override val source: ImageDecoder.Source
                        get() = source

                    override fun close() {
                        snapshot.close()
                    }
                }
            }
        }
        val dir = downloadDir ?: return null
        var file = findImageFile(dir, index)
        if (file != null && mMode == SpiderQueen.MODE_DOWNLOAD) {
            if (copyFromCacheToDownloadDir(index)) {
                file = findImageFile(dir, index)
            }
        }
        if (file is RawFile) {
            val source = ImageDecoder.createSource(file.mFile)
            return object : CloseableSource {
                override val source: ImageDecoder.Source
                    get() = source

                override fun close() {}
            }
        }
        if (file != null) {
            val source = ImageDecoder.createSource(contentResolver, file.uri)
            return object : CloseableSource {
                override val source: ImageDecoder.Source
                    get() = source

                override fun close() {}
            }
        }
        return null
    }

    companion object {
        private const val TRANSFER_BLOCK: Long = 8192

        // We use data to store image file, and metadata for image type
        private val sCache by lazy {
            DiskCache.Builder().directory(File(application.cacheDir, "gallery_image"))
                .maxSizeBytes(
                    MathUtils.clamp(Settings.getReadCacheSize(), 40, 1280).toLong() * 1024 * 1024
                ).build()
        }

        fun getGalleryDownloadDir(gid: Long): UniFile? {
            val dir = Settings.getDownloadLocation()
            // Read from DB
            var dirname = EhDB.getDownloadDirname(gid)
            return if (dir != null && dirname != null) {
                // Some dirname may be invalid in some version
                dirname = FileUtils.sanitizeFilename(dirname)
                EhDB.putDownloadDirname(gid, dirname)
                dir.subFile(dirname)
            } else {
                null
            }
        }

        /**
         * @param extension with dot
         */
        fun generateImageFilename(index: Int, extension: String?): String {
            return String.format(Locale.US, "%08d%s", index + 1, extension)
        }

        private fun findImageFile(dir: UniFile, index: Int): UniFile? {
            for (extension in PageLoader2.SUPPORT_IMAGE_EXTENSIONS) {
                val filename = generateImageFilename(index, extension)
                val file = dir.findFile(filename)
                if (file != null) {
                    return file
                }
            }
            return null
        }
    }
}
