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

import androidx.annotation.IntDef
import androidx.collection.LongSparseArray
import androidx.collection.set
import com.hippo.ehviewer.EhApplication.Companion.okHttpClient
import com.hippo.ehviewer.GetText
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhRequestBuilder
import com.hippo.ehviewer.client.EhUrl.getGalleryDetailUrl
import com.hippo.ehviewer.client.EhUrl.getGalleryMultiPageViewerUrl
import com.hippo.ehviewer.client.EhUrl.referer
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.parser.GalleryDetailParser.parsePages
import com.hippo.ehviewer.client.parser.GalleryDetailParser.parsePreviewPages
import com.hippo.ehviewer.client.parser.GalleryDetailParser.parsePreviewSet
import com.hippo.ehviewer.client.parser.GalleryMultiPageViewerPTokenParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.image.Image
import com.hippo.unifile.UniFile
import com.hippo.util.ExceptionUtils
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchNonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.executeAsync
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class SpiderQueen private constructor(val galleryInfo: GalleryInfo) : CoroutineScope {
    val mSpiderDen: SpiderDen = SpiderDen(galleryInfo)
    lateinit var mSpiderInfo: SpiderInfo
    val mPagePercentMap = ConcurrentHashMap<Int, Float>()
    private val mPageStateLock = Any()
    private val mDownloadedPages = AtomicInteger(0)
    private val mFinishedPages = AtomicInteger(0)
    private val mPageErrorMap = ConcurrentHashMap<Int, String>()
    private val mSpiderListeners: MutableList<OnSpiderListener> = ArrayList()

    @Volatile
    lateinit var mPageStateArray: IntArray
    private var mReadReference = 0
    private var mDownloadReference = 0

    private val mWorkerScope = SpiderQueenWorker(this)

    fun addOnSpiderListener(listener: OnSpiderListener) {
        synchronized(mSpiderListeners) { mSpiderListeners.add(listener) }
    }

    fun removeOnSpiderListener(listener: OnSpiderListener) {
        synchronized(mSpiderListeners) { mSpiderListeners.remove(listener) }
    }

    private fun notifyGetPages(pages: Int) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onGetPages(pages)
            }
        }
    }

    fun notifyGet509(index: Int) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onGet509(index)
            }
        }
    }

    fun notifyPageDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onPageDownload(index, contentLength, receivedSize, bytesRead)
            }
        }
    }

    private fun notifyPageSuccess(index: Int) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onPageSuccess(
                    index,
                    mFinishedPages.get(),
                    mDownloadedPages.get(),
                    mPageStateArray.size
                )
            }
        }
    }

    private fun notifyPageFailure(index: Int, error: String?) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onPageFailure(
                    index,
                    error,
                    mFinishedPages.get(),
                    mDownloadedPages.get(),
                    mPageStateArray.size
                )
            }
        }
    }

    private fun notifyAllPageDownloaded() {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onFinish(
                    mFinishedPages.get(),
                    mDownloadedPages.get(),
                    mPageStateArray.size
                )
            }
        }
    }

    fun notifyGetImageSuccess(index: Int, image: Image) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onGetImageSuccess(index, image)
            }
        }
    }

    fun notifyGetImageFailure(index: Int, error: String) {
        synchronized(mSpiderListeners) {
            for (listener in mSpiderListeners) {
                listener.onGetImageFailure(index, error)
            }
        }
    }

    private var mInDownloadMode = false

    @Synchronized
    private fun updateMode() {
        val mode: Int = if (mDownloadReference > 0) {
            MODE_DOWNLOAD
        } else {
            MODE_READ
        }
        mSpiderDen.setMode(mode)

        // Update download page
        val intoDownloadMode = mode == MODE_DOWNLOAD
        if (intoDownloadMode && !mInDownloadMode) {
            // Clear download state
            synchronized(mPageStateLock) {
                val temp: IntArray = mPageStateArray
                var i = 0
                val n = temp.size
                while (i < n) {
                    val oldState = temp[i]
                    if (STATE_DOWNLOADING != oldState) {
                        temp[i] = STATE_NONE
                    }
                    i++
                }
                mDownloadedPages.lazySet(0)
                mFinishedPages.lazySet(0)
                mPageErrorMap.clear()
                mPagePercentMap.clear()
            }
            mWorkerScope.enterDownloadMode()
        }
        mInDownloadMode = intoDownloadMode
    }

    private fun setMode(@Mode mode: Int) {
        when (mode) {
            MODE_READ -> mReadReference++
            MODE_DOWNLOAD -> mDownloadReference++
        }
        check(mDownloadReference <= 1) { "mDownloadReference can't more than 0" }
        launchIO {
            awaitReady()
            updateMode()
        }
    }

    private fun clearMode(@Mode mode: Int) {
        when (mode) {
            MODE_READ -> mReadReference--
            MODE_DOWNLOAD -> mDownloadReference--
        }
        check(!(mReadReference < 0 || mDownloadReference < 0)) { "Mode reference < 0" }
        launchIO {
            awaitReady()
            updateMode()
        }
    }

    private var prepareJob = launchIO { doPrepare() }

    private suspend fun doPrepare() {
        mSpiderInfo = readSpiderInfoFromLocal() ?: readSpiderInfoFromInternet() ?: return
        mPageStateArray = IntArray(mSpiderInfo.pages)
    }

    suspend fun awaitReady() {
        prepareJob.join()
        notifyGetPages(mSpiderInfo.pages)
    }

    private fun stop() {
        launchNonCancellable { writeSpiderInfoToLocal() }
        cancel()
    }

    val size
        get() = mPageStateArray.size

    val error: String?
        get() = null

    fun forceRequest(index: Int) {
        request(index, true)
    }

    fun request(index: Int) {
        request(index, false)
    }

    private fun getPageState(index: Int): Int {
        synchronized(mPageStateLock) {
            return if (index >= 0 && index < mPageStateArray.size) {
                mPageStateArray[index]
            } else {
                STATE_NONE
            }
        }
    }

    fun cancelRequest(index: Int) {
        mWorkerScope.cancelDecode(index)
    }

    fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>) {
        mWorkerScope.updateRAList(pages, pair)
    }

    private fun request(index: Int, force: Boolean) {
        // Get page state
        val state = getPageState(index)

        // Fix state for force
        if (force && (state == STATE_FINISHED || state == STATE_FAILED) || state == STATE_FAILED) {
            // Update state to none at once
            updatePageState(index, STATE_NONE)
        }
        mWorkerScope.launch(index, force)
    }

    fun save(index: Int, file: UniFile): Boolean {
        val state = getPageState(index)
        return if (STATE_FINISHED != state) {
            false
        } else mSpiderDen.saveToUniFile(index, file)
    }

    fun save(index: Int, dir: UniFile, filename: String): UniFile? {
        val state = getPageState(index)
        if (STATE_FINISHED != state) {
            return null
        }
        val ext = mSpiderDen.getExtension(index)
        val dst = dir.subFile(if (null != ext) "$filename.$ext" else filename) ?: return null
        return if (!mSpiderDen.saveToUniFile(index, dst)) null else dst
    }

    fun getExtension(index: Int): String? {
        val state = getPageState(index)
        return if (STATE_FINISHED != state) {
            null
        } else mSpiderDen.getExtension(index)
    }

    val startPage: Int
        get() = mSpiderInfo.startPage

    fun putStartPage(page: Int) {
        mSpiderInfo.startPage = page
    }

    private fun readSpiderInfoFromLocal(): SpiderInfo? {
        return mSpiderDen.downloadDir?.run {
            findFile(SPIDER_INFO_FILENAME)?.let { file ->
                readCompatFromUniFile(file)?.takeIf {
                    it.gid == galleryInfo.gid && it.token == galleryInfo.token
                }
            }
        }
            ?: readFromCache(galleryInfo.gid)?.takeIf { it.gid == galleryInfo.gid && it.token == galleryInfo.token }
    }

    @Throws(ParseException::class)
    private fun readPreviews(body: String, index: Int, spiderInfo: SpiderInfo) {
        spiderInfo.previewPages = parsePreviewPages(body)
        val previewSet = parsePreviewSet(body)
        if (previewSet.size() > 0) {
            if (index == 0) {
                spiderInfo.previewPerPage = previewSet.size()
            } else {
                spiderInfo.previewPerPage = previewSet.getPosition(0) / index
            }
        }
        var i = 0
        val n = previewSet.size()
        while (i < n) {
            val result = GalleryPageUrlParser.parse(previewSet.getPageUrlAt(i))
            if (result != null) {
                spiderInfo.pTokenMap[result.page] = result.pToken
            }
            i++
        }
    }

    private suspend fun readSpiderInfoFromInternet(): SpiderInfo? {
        val request = EhRequestBuilder(
            getGalleryDetailUrl(
                galleryInfo.gid, galleryInfo.token, 0, false
            ), referer
        ).build()
        return runSuspendCatching {
            okHttpClient.newCall(request).executeAsync().use { response ->
                val body = response.body.string()
                val pages = parsePages(body)
                val spiderInfo = SpiderInfo(galleryInfo.gid, pages)
                spiderInfo.token = galleryInfo.token
                readPreviews(body, 0, spiderInfo)
                spiderInfo
            }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    fun getPTokenFromMultiPageViewer(index: Int): String? {
        val spiderInfo = mSpiderInfo
        val url = getGalleryMultiPageViewerUrl(
            galleryInfo.gid, galleryInfo.token!!
        )
        val referer = referer
        val request = EhRequestBuilder(url, referer).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                val list = GalleryMultiPageViewerPTokenParser.parse(body)
                for (i in list.indices) {
                    spiderInfo.pTokenMap[i] = list[i]
                }
                return spiderInfo.pTokenMap[index]
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            return null
        }
    }

    fun getPTokenFromInternet(index: Int): String? {
        val spiderInfo = mSpiderInfo

        // Check previewIndex
        var previewIndex: Int
        previewIndex = if (spiderInfo.previewPerPage >= 0) {
            index / spiderInfo.previewPerPage
        } else {
            0
        }
        if (spiderInfo.previewPages > 0) {
            previewIndex = previewIndex.coerceAtMost(spiderInfo.previewPages - 1)
        }
        val url = getGalleryDetailUrl(
            galleryInfo.gid, galleryInfo.token, previewIndex, false
        )
        val referer = referer
        val request = EhRequestBuilder(url, referer).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                readPreviews(body, previewIndex, spiderInfo)
                return spiderInfo.pTokenMap[index]
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            return null
        }
    }

    @Synchronized
    private fun writeSpiderInfoToLocal() {
        mSpiderDen.downloadDir?.run { createFile(SPIDER_INFO_FILENAME).also { mSpiderInfo.write(it) } }
        mSpiderInfo.saveToCache()
    }

    private fun isStateDone(state: Int): Boolean {
        return state == STATE_FINISHED || state == STATE_FAILED
    }

    @JvmOverloads
    fun updatePageState(index: Int, @State state: Int, error: String? = null) {
        var oldState: Int
        synchronized(mPageStateLock) {
            oldState = mPageStateArray[index]
            mPageStateArray[index] = state
            if (!isStateDone(oldState) && isStateDone(state)) {
                mDownloadedPages.incrementAndGet()
            } else if (isStateDone(oldState) && !isStateDone(state)) {
                mDownloadedPages.decrementAndGet()
            }
            if (oldState != STATE_FINISHED && state == STATE_FINISHED) {
                mFinishedPages.incrementAndGet()
            } else if (oldState == STATE_FINISHED && state != STATE_FINISHED) {
                mFinishedPages.decrementAndGet()
            }

            // Clear
            if (state == STATE_DOWNLOADING) {
                mPageErrorMap.remove(index)
            } else if (state == STATE_FINISHED || state == STATE_FAILED) {
                mPagePercentMap.remove(index)
            }

            // Get default error
            if (state == STATE_FAILED) {
                mPageErrorMap[index] = error ?: GetText.getString(R.string.error_unknown)
            }
        }

        // Notify listeners
        if (state == STATE_FAILED) {
            notifyPageFailure(index, error)
        } else if (state == STATE_FINISHED) {
            notifyPageSuccess(index)
        }
        if (mFinishedPages.get() == size) notifyAllPageDownloaded()
    }

    @IntDef(MODE_READ, MODE_DOWNLOAD)
    @Retention
    annotation class Mode

    @IntDef(STATE_NONE, STATE_DOWNLOADING, STATE_FINISHED, STATE_FAILED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class State
    interface OnSpiderListener {
        fun onGetPages(pages: Int)
        fun onGet509(index: Int)
        fun onPageDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int)
        fun onPageSuccess(index: Int, finished: Int, downloaded: Int, total: Int)
        fun onPageFailure(index: Int, error: String?, finished: Int, downloaded: Int, total: Int)
        fun onFinish(finished: Int, downloaded: Int, total: Int)
        fun onGetImageSuccess(index: Int, image: Image?)
        fun onGetImageFailure(index: Int, error: String?)
    }

    companion object {
        const val MODE_READ = 0
        const val MODE_DOWNLOAD = 1
        const val STATE_NONE = 0
        const val STATE_DOWNLOADING = 1
        const val STATE_FINISHED = 2
        const val STATE_FAILED = 3
        const val SPIDER_INFO_FILENAME = ".ehviewer"
        private val sQueenMap = LongSparseArray<SpiderQueen>()

        @JvmStatic
        fun obtainSpiderQueen(galleryInfo: GalleryInfo, @Mode mode: Int): SpiderQueen {
            return sQueenMap[galleryInfo.gid]?.apply { setMode(mode) }
                ?: SpiderQueen(galleryInfo).apply { setMode(mode) }
                    .also { sQueenMap[galleryInfo.gid] = it }
        }

        @JvmStatic
        fun releaseSpiderQueen(queen: SpiderQueen, @Mode mode: Int) {
            queen.clearMode(mode)
            if (queen.mReadReference == 0 && queen.mDownloadReference == 0) {
                queen.stop()
                sQueenMap.remove(queen.galleryInfo.gid)
            }
        }

        fun getStartPage(gid: Long): Int {
            val queen = sQueenMap[gid]
            var spiderInfo: SpiderInfo? = null

            // Fast Path: read existing queen
            if (queen != null) {
                spiderInfo = queen.mSpiderInfo
            }

            // Slow path, read diskcache
            if (spiderInfo == null) {
                spiderInfo = readFromCache(gid)
            }
            return spiderInfo?.startPage ?: 0
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job()
}

var PTOKEN_FAILED_MESSAGE = GetText.getString(R.string.error_get_ptoken_error)
var ERROR_509 = GetText.getString(R.string.error_509)
var NETWORK_ERROR = GetText.getString(R.string.error_socket)
var DECODE_ERROR = GetText.getString(R.string.error_decoding_failed)