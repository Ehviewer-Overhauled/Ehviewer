/*
 * Copyright 2016 Hippo Seven
 * Rewrite with Kotlin coroutines, Tarsin Norbin 2023
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

import android.util.Log
import androidx.annotation.IntDef
import androidx.collection.LongSparseArray
import androidx.collection.set
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUrl.getGalleryDetailUrl
import com.hippo.ehviewer.client.EhUrl.getGalleryMultiPageViewerUrl
import com.hippo.ehviewer.client.EhUrl.referer
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.ehRequest
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.execute
import com.hippo.ehviewer.client.parser.GalleryDetailParser.parsePages
import com.hippo.ehviewer.client.parser.GalleryDetailParser.parsePreviewList
import com.hippo.ehviewer.client.parser.GalleryDetailParser.parsePreviewPages
import com.hippo.ehviewer.client.parser.GalleryMultiPageViewerPTokenParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import moe.tarsin.coroutines.runSuspendCatching
import splitties.init.appCtx
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SpiderQueen private constructor(val galleryInfo: GalleryInfo) : CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()

    @Volatile
    lateinit var mPageStateArray: IntArray
    lateinit var mSpiderInfo: SpiderInfo

    val mSpiderDen: SpiderDen = SpiderDen(galleryInfo)
    val mPagePercentMap = ConcurrentHashMap<Int, Float>()
    private val mPageStateLock = Any()
    private val mDownloadedPages = AtomicInteger(0)
    private val mFinishedPages = AtomicInteger(0)
    private val mPageErrorMap = ConcurrentHashMap<Int, String>()
    private val mSpiderListeners: MutableList<OnSpiderListener> = ArrayList()

    private var mReadReference = 0
    private var mDownloadReference = 0

    fun addOnSpiderListener(listener: OnSpiderListener) {
        synchronized(mSpiderListeners) { mSpiderListeners.add(listener) }
    }

    fun removeOnSpiderListener(listener: OnSpiderListener) {
        synchronized(mSpiderListeners) { mSpiderListeners.remove(listener) }
    }

    private fun notifyGetPages(pages: Int) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach { it.onGetPages(pages) }
        }
    }

    fun notifyGet509(index: Int) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach { it.onGet509(index) }
        }
    }

    fun notifyPageDownload(index: Int, contentLength: Long, receivedSize: Long, bytesRead: Int) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach {
                it.onPageDownload(
                    index,
                    contentLength,
                    receivedSize,
                    bytesRead,
                )
            }
        }
    }

    private fun notifyPageSuccess(index: Int) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach {
                it.onPageSuccess(
                    index,
                    mFinishedPages.get(),
                    mDownloadedPages.get(),
                    mPageStateArray.size,
                )
            }
        }
    }

    private fun notifyPageFailure(index: Int, error: String?) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach {
                it.onPageFailure(
                    index,
                    error,
                    mFinishedPages.get(),
                    mDownloadedPages.get(),
                    mPageStateArray.size,
                )
            }
        }
    }

    private fun notifyAllPageDownloaded() {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach {
                it.onFinish(
                    mFinishedPages.get(),
                    mDownloadedPages.get(),
                    mPageStateArray.size,
                )
            }
        }
    }

    fun notifyGetImageSuccess(index: Int, image: Image) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach {
                it.onGetImageSuccess(index, image)
            }
        }
    }

    fun notifyGetImageFailure(index: Int, error: String) {
        synchronized(mSpiderListeners) {
            mSpiderListeners.forEach {
                it.onGetImageFailure(index, error)
            }
        }
    }

    private var downloadMode = false
    val isReady
        get() = this::mSpiderInfo.isInitialized && this::mPageStateArray.isInitialized

    @Synchronized
    private fun updateMode() {
        if (!isReady) return
        val mode: Int = if (mDownloadReference > 0) {
            MODE_DOWNLOAD
        } else {
            MODE_READ
        }
        mSpiderDen.mode = mode

        // Update download page
        val intoDownloadMode = mode == MODE_DOWNLOAD
        if (intoDownloadMode && !downloadMode) {
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
        downloadMode = intoDownloadMode
    }

    private fun setMode(@Mode mode: Int) {
        when (mode) {
            MODE_READ -> mReadReference++
            MODE_DOWNLOAD -> mDownloadReference++
        }
        check(mDownloadReference <= 1) { "mDownloadReference can't more than 0" }
    }

    private fun clearMode(@Mode mode: Int) {
        when (mode) {
            MODE_READ -> mReadReference--
            MODE_DOWNLOAD -> mDownloadReference--
        }
        check(!(mReadReference < 0 || mDownloadReference < 0)) { "Mode reference < 0" }
    }

    private val prepareJob = launchIO { doPrepare() }

    private suspend fun doPrepare() {
        mSpiderInfo = readSpiderInfoFromLocal() ?: readSpiderInfoFromInternet() ?: return
        mPageStateArray = IntArray(mSpiderInfo.pages)
        notifyGetPages(mSpiderInfo.pages)
    }

    suspend fun awaitReady(): Boolean {
        prepareJob.join()
        return isReady
    }

    suspend fun awaitStartPage(): Int {
        prepareJob.join()
        if (!isReady) return 0
        return mSpiderInfo.startPage
    }

    private fun stop() {
        val queenScope = this
        launchIO {
            queenScope.cancel()
            runCatching {
                writeSpiderInfoToLocal()
            }.onFailure {
                it.printStackTrace()
            }
        }
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
        } else {
            mSpiderDen.saveToUniFile(index, file)
        }
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
        } else {
            mSpiderDen.getExtension(index)
        }
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
        } ?: readFromCache(galleryInfo.gid)?.takeIf { it.gid == galleryInfo.gid && it.token == galleryInfo.token }
    }

    @Throws(ParseException::class)
    private fun readPreviews(body: String, index: Int, spiderInfo: SpiderInfo) {
        spiderInfo.previewPages = parsePreviewPages(body)
        val (previewList, pageUrlList) = parsePreviewList(body)
        if (previewList.isNotEmpty()) {
            if (index == 0) {
                spiderInfo.previewPerPage = previewList.size
            } else {
                spiderInfo.previewPerPage = previewList[0].position / index
            }
        }
        pageUrlList.forEach {
            val result = GalleryPageUrlParser.parse(it)
            if (result != null) {
                spiderInfo.pTokenMap[result.page] = result.pToken
            }
        }
    }

    private suspend fun readSpiderInfoFromInternet(): SpiderInfo? {
        return runSuspendCatching {
            ehRequest(getGalleryDetailUrl(galleryInfo.gid, galleryInfo.token, 0, false), referer).execute {
                val bodyStr = body.string()
                val pages = parsePages(bodyStr)
                val spiderInfo = SpiderInfo(galleryInfo.gid, pages)
                spiderInfo.token = galleryInfo.token
                readPreviews(bodyStr, 0, spiderInfo)
                spiderInfo
            }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    suspend fun getPTokenFromMultiPageViewer(index: Int): String? {
        val spiderInfo = mSpiderInfo
        val url = getGalleryMultiPageViewerUrl(
            galleryInfo.gid,
            galleryInfo.token!!,
        )
        return try {
            ehRequest(url, referer).execute {
                val bodyStr = body.string()
                GalleryMultiPageViewerPTokenParser.parse(bodyStr).forEachIndexed { index, s ->
                    spiderInfo.pTokenMap[index] = s
                }
                spiderInfo.pTokenMap[index]
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            return null
        }
    }

    suspend fun getPTokenFromInternet(index: Int): String? {
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
            galleryInfo.gid,
            galleryInfo.token,
            previewIndex,
            false,
        )
        return try {
            ehRequest(url, referer).execute {
                readPreviews(body.string(), previewIndex, spiderInfo)
                spiderInfo.pTokenMap[index]
            }
        } catch (e: Throwable) {
            ExceptionUtils.throwIfFatal(e)
            null
        }
    }

    @Synchronized
    private fun writeSpiderInfoToLocal() {
        if (!isReady) return
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
            } else if (isStateDone(state)) {
                mPagePercentMap.remove(index)
            }

            // Get default error
            if (state == STATE_FAILED) {
                mPageErrorMap[index] = error ?: appCtx.getString(R.string.error_unknown)
            }
        }

        // Notify listeners
        if (state == STATE_FAILED) {
            notifyPageFailure(index, error)
        } else if (state == STATE_FINISHED) {
            notifyPageSuccess(index)
        }
        if (mDownloadedPages.get() == size) notifyAllPageDownloaded()
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
            val gid = galleryInfo.gid
            return (sQueenMap[gid] ?: SpiderQueen(galleryInfo).also { sQueenMap[gid] = it }).apply {
                setMode(mode)
                launchIO { if (awaitReady()) updateMode() }
            }
        }

        @JvmStatic
        fun releaseSpiderQueen(queen: SpiderQueen, @Mode mode: Int) {
            queen.run {
                clearMode(mode)
                if (mReadReference == 0 && mDownloadReference == 0) {
                    stop()
                    sQueenMap.remove(galleryInfo.gid)
                } else {
                    launchIO { if (awaitReady()) updateMode() }
                }
            }
        }
    }

    private val mWorkerScope = object {
        private val mFetcherJobMap = hashMapOf<Int, Job>()
        private val mSemaphore = Semaphore(Settings.multiThreadDownload)
        private val pTokenLock = Mutex()
        private var showKey: String? = null
        private val showKeyLock = Mutex()
        private val mDownloadDelay = Settings.downloadDelay
        private var isDownloadMode = false

        fun cancelDecode(index: Int) {
            decoder.cancel(index)
        }

        @Synchronized
        fun enterDownloadMode() {
            if (isDownloadMode) return
            updateRAList((0 until size).toList())
            isDownloadMode = true
        }

        fun updateRAList(list: List<Int>, cancelBounds: Pair<Int, Int> = 0 to Int.MAX_VALUE) {
            if (isDownloadMode) return
            synchronized(mFetcherJobMap) {
                mFetcherJobMap.forEach { (i, job) ->
                    if (i < cancelBounds.first || i > cancelBounds.second) {
                        job.cancel()
                    }
                }
                list.forEach {
                    if (mFetcherJobMap[it]?.isActive != true) {
                        doLaunchDownloadJob(it, false)
                    }
                }
            }
        }

        private fun doLaunchDownloadJob(index: Int, force: Boolean) {
            val state = mPageStateArray[index]
            if (!force && state == STATE_FINISHED) return
            val currentJob = mFetcherJobMap[index]
            if (force) currentJob?.cancel()
            if (currentJob?.isActive != true) {
                mFetcherJobMap[index] = launch {
                    mSemaphore.withPermit {
                        doInJob(index, force)
                    }
                }.apply {
                    invokeOnCompletion {
                        if (getPageState(index) == STATE_DOWNLOADING) {
                            Log.d(WORKER_DEBUG_TAG, "Download image cancelled $index")
                            mSpiderDen.remove(index)
                        }
                    }
                }
            }
        }

        fun launch(index: Int, force: Boolean = false) {
            check(index in 0 until size)
            if (!isDownloadMode) synchronized(mFetcherJobMap) { doLaunchDownloadJob(index, force) }
            if (force) decoder.cancel(index)
            decoder.launch(index)
        }

        private suspend fun doInJob(index: Int, force: Boolean) {
            suspend fun getPToken(index: Int): String? {
                if (index !in 0 until size) return null
                return mSpiderInfo.pTokenMap[index].takeIf { it != TOKEN_FAILED }
                    ?: getPTokenFromInternet(index)
                    ?: getPTokenFromInternet(index)
                    ?: getPTokenFromMultiPageViewer(index)
            }
            updatePageState(index, STATE_DOWNLOADING)
            if (!force && index in mSpiderDen) {
                return updatePageState(index, STATE_FINISHED)
            }
            if (force) {
                pTokenLock.withLock {
                    val pToken = mSpiderInfo.pTokenMap[index]
                    if (pToken == TOKEN_FAILED) mSpiderInfo.pTokenMap.remove(index)
                }
            }
            val previousPToken: String?
            val pToken: String

            pTokenLock.withLock {
                pToken = getPToken(index) ?: return updatePageState(
                    index,
                    STATE_FAILED,
                    PTOKEN_FAILED_MESSAGE,
                ).also {
                    mSpiderInfo.pTokenMap[index] = TOKEN_FAILED
                }
                previousPToken = getPToken(index - 1)
            }

            var skipHathKey: String? = null
            val skipHathKeys = mutableListOf<String>()
            var originImageUrl: String? = null
            var error: String? = null
            var forceHtml = false
            var leakSkipHathKey = false
            repeat(3) {
                var imageUrl: String? = null
                var localShowKey: String?

                showKeyLock.withLock {
                    localShowKey = showKey
                    if (localShowKey == null || forceHtml) {
                        if (leakSkipHathKey) return@repeat
                        var pageUrl = EhUrl.getPageUrl(mSpiderInfo.gid, index, pToken)
                        // Add skipHathKey
                        if (skipHathKey != null) {
                            pageUrl += if ("?" in pageUrl) {
                                "&nl=$skipHathKey"
                            } else {
                                "?nl=$skipHathKey"
                            }
                        }
                        runSuspendCatching {
                            EhEngine.getGalleryPage(pageUrl, mSpiderInfo.gid, mSpiderInfo.token)
                                .also {
                                    if (check509(it.imageUrl)) {
                                        // Get 509
                                        notifyGet509(index)
                                        error = ERROR_509
                                        return@repeat
                                    }
                                }
                        }.onSuccess { result ->
                            imageUrl = result.imageUrl
                            skipHathKey = result.skipHathKey?.takeIf { it.isNotBlank() }
                            originImageUrl = result.originImageUrl
                            localShowKey = result.showKey

                            if (skipHathKey != null) {
                                if (skipHathKey in skipHathKeys) {
                                    // Duplicate skip hath key
                                    leakSkipHathKey = true
                                } else {
                                    skipHathKeys.add(skipHathKey!!)
                                }
                            } else {
                                leakSkipHathKey = true
                            }

                            showKey = result.showKey
                        }.onFailure {
                            if (it is ParseException && "Key mismatch" == it.message) {
                                // Show key is wrong, enter a new loop to get the new show key
                                if (showKey == localShowKey) showKey = null
                                return@repeat
                            } else {
                                error = ExceptionUtils.getReadableString(it)
                                return@repeat
                            }
                        }
                    }
                }

                if (imageUrl == null) {
                    if (localShowKey == null) {
                        error = "ShowKey error"
                        return@repeat
                    }
                    runSuspendCatching {
                        EhEngine.getGalleryPageApi(
                            mSpiderInfo.gid,
                            index,
                            pToken,
                            localShowKey,
                            previousPToken,
                        ).also {
                            if (check509(it.imageUrl)) {
                                // Get 509
                                notifyGet509(index)
                                error = ERROR_509
                                return@repeat
                            }
                        }
                    }.onFailure {
                        if (it is ParseException && "Key mismatch" == it.message) {
                            // Show key is wrong, enter a new loop to get the new show key
                            if (showKey == localShowKey) showKey = null
                        } else {
                            error = ExceptionUtils.getReadableString(it)
                        }
                        return@repeat
                    }.onSuccess {
                        imageUrl = it.imageUrl
                        skipHathKey = it.skipHathKey
                        originImageUrl = it.originImageUrl
                    }
                }

                val targetImageUrl: String?
                val referer: String?

                if (Settings.downloadOriginImage && !originImageUrl.isNullOrBlank()) {
                    targetImageUrl = originImageUrl
                    referer = EhUrl.getPageUrl(mSpiderInfo.gid, index, pToken)
                } else {
                    targetImageUrl = imageUrl
                    referer = null
                }
                if (targetImageUrl == null) {
                    error = "TargetImageUrl error"
                    return@repeat
                }
                Log.d(WORKER_DEBUG_TAG, targetImageUrl)

                runSuspendCatching {
                    Log.d(WORKER_DEBUG_TAG, "Start download image $index")
                    val success: Boolean = mSpiderDen.makeHttpCallAndSaveImage(
                        index,
                        targetImageUrl,
                        referer,
                    ) { contentLength: Long, receivedSize: Long, bytesRead: Int ->
                        mPagePercentMap[index] = receivedSize.toFloat() / contentLength
                        notifyPageDownload(index, contentLength, receivedSize, bytesRead)
                    }

                    if (!success) {
                        Log.e(WORKER_DEBUG_TAG, "Can't download all of image data")
                        error = "Incomplete"
                        forceHtml = true
                        return@repeat
                    }

                    if (mSpiderDen.checkPlainText(index)) {
                        error = ""
                        forceHtml = true
                        return@repeat
                    }

                    Log.d(WORKER_DEBUG_TAG, "Download image succeed $index")

                    updatePageState(index, STATE_FINISHED)
                    delay(mDownloadDelay.toLong())
                    return
                }.onFailure {
                    it.printStackTrace()
                    error = NETWORK_ERROR
                    forceHtml = true
                }
                Log.d(WORKER_DEBUG_TAG, "End download image $index")
            }
            mSpiderDen.remove(index)
            updatePageState(index, STATE_FAILED, error)
        }

        private val decoder = object {
            private val mSemaphore = Semaphore(4)
            private val mDecodeJobMap = hashMapOf<Int, Job>()

            fun cancel(index: Int) {
                synchronized(mDecodeJobMap) {
                    mDecodeJobMap.remove(index)?.cancel()
                }
            }

            fun launch(index: Int) {
                synchronized(mDecodeJobMap) {
                    val currentJob = mDecodeJobMap[index]
                    if (currentJob?.isActive != true) {
                        mDecodeJobMap[index] = launch {
                            doInJob(index)
                        }
                    }
                }
            }

            private suspend fun doInJob(index: Int) {
                mFetcherJobMap[index]?.takeIf { it.isActive }?.join()
                val src = mSpiderDen.getImageSource(index) ?: return
                val image = mSemaphore.withPermit { Image.decode(src) }
                runCatching {
                    currentCoroutineContext().ensureActive()
                }.onFailure {
                    image?.recycle()
                    throw it
                }
                if (image == null) {
                    notifyGetImageFailure(index, DECODE_ERROR)
                } else {
                    notifyGetImageSuccess(index, image)
                }
            }
        }
    }
}

private val PTOKEN_FAILED_MESSAGE = appCtx.getString(R.string.error_get_ptoken_error)
private val ERROR_509 = appCtx.getString(R.string.error_509)
private val NETWORK_ERROR = appCtx.getString(R.string.error_socket)
private val DECODE_ERROR = appCtx.getString(R.string.error_decoding_failed)
private val URL_509_ARRAY = arrayOf(
    "https://ehgt.org/g/509.gif",
    "https://ehgt.org/g/509s.gif",
    "https://exhentai.org/img/509.gif",
    "https://exhentai.org/img/509s.gif",
)
private const val WORKER_DEBUG_TAG = "SpiderQueenWorker"

private fun check509(url: String) = url in URL_509_ARRAY
