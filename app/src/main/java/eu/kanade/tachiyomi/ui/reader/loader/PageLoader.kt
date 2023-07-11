package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.lruCache
import com.hippo.ehviewer.image.Image
import com.hippo.ehviewer.util.OSUtils
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

private const val MAX_CACHE_SIZE = 512 * 1024 * 1024
private const val MIN_CACHE_SIZE = 128 * 1024 * 1024

abstract class PageLoader {

    private val mImageCache = lruCache<Int, Image>(
        maxSize = (OSUtils.totalMemory / 16).toInt().coerceIn(MIN_CACHE_SIZE, MAX_CACHE_SIZE),
        sizeOf = { _, v -> v.size },
        onEntryRemoved = { _, _, o, _ -> o.recycle() },
    )
    val mPages by lazy {
        check(size > 0)
        (0 until size).map { ReaderPage(it) }
    }

    private val mPreloads = com.hippo.ehviewer.Settings.preloadImage.coerceIn(0, 100)

    abstract suspend fun awaitReady(): Boolean
    abstract val isReady: Boolean

    abstract fun start()

    @CallSuper
    open fun stop() {
        mImageCache.evictAll()
    }

    fun restart() {
        mImageCache.evictAll()
    }

    abstract val size: Int

    fun request(index: Int) {
        val image = mImageCache[index]
        if (image != null) {
            notifyPageSucceed(index, image)
        } else {
            notifyPageWait(index)
            onRequest(index)
        }

        val pagesAbsent = ((index - 5).coerceAtLeast(0) until (mPreloads + index).coerceAtMost(size)).mapNotNull { it.takeIf { mImageCache[it] == null } }
        preloadPages(pagesAbsent, (index - 10).coerceAtLeast(0) to (mPreloads + index + 10).coerceAtMost(size))
    }

    fun retryPage(index: Int, orgImg: Boolean = false) {
        notifyPageWait(index)
        onForceRequest(index, orgImg)
    }

    protected abstract fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int, orgImg: Boolean)

    fun cancelRequest(index: Int) {
        onCancelRequest(index)
    }

    protected abstract fun onCancelRequest(index: Int)

    fun notifyPageWait(index: Int) {
        mPages[index].status.value = Page.State.QUEUE
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        mPages[index].status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
        mPages[index].progress = (percent * 100).toInt()
    }

    fun notifyPageSucceed(index: Int, image: Image) {
        if (mImageCache[index] != image) {
            mImageCache.put(index, image)
        }
        mPages[index].image = image
        mPages[index].status.value = Page.State.READY
    }

    fun notifyPageFailed(index: Int, error: String?) {
        mPages[index].errorMsg = error
        mPages[index].status.value = Page.State.ERROR
    }
}
