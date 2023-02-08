package com.hippo.ehviewer.spider

import android.util.Log
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine.getGalleryPage
import com.hippo.ehviewer.client.EhEngine.getGalleryPageApi
import com.hippo.ehviewer.client.EhUrl.getPageUrl
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.spider.SpiderQueen.ERROR_509
import com.hippo.ehviewer.spider.SpiderQueen.NETWORK_ERROR
import com.hippo.ehviewer.spider.SpiderQueen.PTOKEN_FAILED_MESSAGE
import com.hippo.ehviewer.spider.SpiderQueen.STATE_FAILED
import com.hippo.ehviewer.spider.SpiderQueen.STATE_FINISHED
import com.hippo.util.ExceptionUtils
import com.hippo.yorozuya.StringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import moe.tarsin.coroutines.runSuspendCatching
import kotlin.coroutines.CoroutineContext

class SpiderQueenWorker(private val queen: SpiderQueen) : CoroutineScope {
    private val spiderDen
        get() = queen.mSpiderDen
    private val spiderInfo by lazy { queen.mSpiderInfo.get() }
    private val mJobMap = hashMapOf<Int, Job>()
    private val mSemaphore = Semaphore(Settings.getMultiThreadDownload())
    private val pTokenLock = Mutex()
    private var showKey: String? = null
    private val showKeyLock = Mutex()
    private val mDownloadDelay = Settings.getDownloadDelay()
    private var isDownloadMode = false
    private val size
        get() = queen.size()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + Job() // No SupervisorJob here since uncaught exception would cause semaphore leak

    fun cancel(index: Int) {
        if (isDownloadMode) return
        synchronized(mJobMap) {
            mJobMap.remove(index)?.cancel()
        }
    }

    @Synchronized
    fun enterDownloadMode() {
        if (isDownloadMode) return
        updateRAList((0 until size).toList())
        isDownloadMode = true
    }

    fun updateRAList(list: List<Int>) {
        if (isDownloadMode) return
        synchronized(mJobMap) {
            sequence {
                mJobMap.forEach { (i, job) ->
                    if (i !in list) {
                        job.cancel()
                    }
                    if (!job.isActive) yield(i)
                }
            }.toSet().forEach { mJobMap.remove(it) }
            list.forEach {
                if (mJobMap[it]?.isActive != true)
                    launch(it)
            }
        }
    }

    @JvmOverloads
    fun launch(index: Int, force: Boolean = false) {
        if (isDownloadMode) return
        check(index in 0 until size)
        val state = queen.mPageStateArray[index]
        if (!force && state == STATE_FINISHED) return

        synchronized(mJobMap) {
            val currentJob = mJobMap[index]
            if (force) currentJob?.cancel()
            if (currentJob?.isActive != true) {
                mJobMap[index] = launch {
                    mSemaphore.withPermit {
                        doInJob(index, force)
                    }
                }
            }
        }
    }

    private suspend fun doInJob(index: Int, force: Boolean) {
        fun getPToken(index: Int): String? {
            if (index !in 0 until size) return null
            return spiderInfo.pTokenMap[index].takeIf { it != TOKEN_FAILED }
                ?: queen.getPTokenFromInternet(index)
                ?: queen.getPTokenFromInternet(index)
                ?: queen.getPTokenFromMultiPageViewer(index)
        }
        queen.updatePageState(index, SpiderQueen.STATE_DOWNLOADING)
        if (!force && index in spiderDen) {
            return queen.updatePageState(index, STATE_FINISHED)
        }
        if (force) {
            pTokenLock.withLock {
                val pToken = spiderInfo.pTokenMap[index]
                if (pToken == TOKEN_FAILED) spiderInfo.pTokenMap.remove(index)
            }
        }
        val previousPToken: String?
        val pToken: String

        pTokenLock.withLock {
            pToken = getPToken(index) ?: return queen.updatePageState(
                index,
                STATE_FAILED,
                PTOKEN_FAILED_MESSAGE
            ).also {
                spiderInfo.pTokenMap[index] = TOKEN_FAILED
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
                    var pageUrl = getPageUrl(spiderInfo.gid, index, pToken)
                    // Add skipHathKey
                    if (skipHathKey != null) {
                        pageUrl += if ("?" in pageUrl) {
                            "&nl=$skipHathKey"
                        } else {
                            "?nl=$skipHathKey"
                        }
                    }
                    runSuspendCatching {
                        getGalleryPage(pageUrl, spiderInfo.gid, spiderInfo.token).also {
                            if (StringUtils.endsWith(it.imageUrl, URL_509_SUFFIX_ARRAY)) {
                                // Get 509
                                queen.notifyGet509(index)
                                error = ERROR_509
                                return@repeat
                            }
                        }
                    }.onSuccess { result ->
                        imageUrl = result.imageUrl
                        skipHathKey = result.skipHathKey.takeIf { it.isNotBlank() }
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
                    getGalleryPageApi(
                        spiderInfo.gid,
                        index,
                        pToken,
                        localShowKey,
                        previousPToken
                    ).also {
                        if (StringUtils.endsWith(it.imageUrl, URL_509_SUFFIX_ARRAY)) {
                            // Get 509
                            queen.notifyGet509(index)
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

            if (Settings.getDownloadOriginImage() && !originImageUrl.isNullOrBlank()) {
                targetImageUrl = originImageUrl
                referer = getPageUrl(spiderInfo.gid, index, pToken)
            } else {
                targetImageUrl = imageUrl
                referer = null
            }
            if (targetImageUrl == null) {
                error = "TargetImageUrl error"
                return@repeat
            }
            Log.d(DEBUG_TAG, targetImageUrl)

            runSuspendCatching {
                Log.d(DEBUG_TAG, "Start download image $index")
                val success: Boolean = spiderDen.makeHttpCallAndSaveImage(
                    index,
                    targetImageUrl,
                    referer
                ) { contentLength: Long, receivedSize: Long, bytesRead: Int ->
                    queen.mPagePercentMap[index] = receivedSize.toFloat() / contentLength
                    queen.notifyPageDownload(index, contentLength, receivedSize, bytesRead)
                }

                if (!success) {
                    Log.e(DEBUG_TAG, "Can't download all of image data")
                    error = "Incomplete"
                    forceHtml = true
                    return@repeat
                }

                if (spiderDen.checkPlainText(index)) {
                    error = ""
                    forceHtml = true
                    return@repeat
                }

                Log.d(DEBUG_TAG, "Download image succeed $index")

                queen.updatePageState(index, STATE_FINISHED)
                delay(mDownloadDelay.toLong())
                return
            }.onFailure {
                it.printStackTrace()
                error = NETWORK_ERROR
            }
            Log.d(DEBUG_TAG, "End download image $index")
        }
        spiderDen.remove(index)
        queen.updatePageState(index, STATE_FAILED, error)
    }

    companion object {
        private val URL_509_SUFFIX_ARRAY = arrayOf(
            "/509.gif",
            "/509s.gif"
        )

        private const val DEBUG_TAG = "SpiderQueenWorker"
    }
}
