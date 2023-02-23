package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import androidx.collection.LruCache
import com.hippo.image.Image
import com.hippo.yorozuya.MathUtils
import com.hippo.yorozuya.OSUtils
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

abstract class PageLoader {

    private val mImageCache = ImageCache()
    val mPages by lazy {
        check(size > 0)
        (0 until size).map { ReaderPage(it) }
    }

    private val mPreloads = MathUtils.clamp(com.hippo.ehviewer.Settings.getPreloadImage(), 0, 100)

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

        // val pagesAbsent = (index until (mPreloads + index).coerceAtMost(size())).toMutableList().removeAll(mImageCache.snapshot().keys)
        // Should we refresh our LruCache ?
        val pagesAbsent =
            ((index - 5).coerceAtLeast(0) until (mPreloads + index).coerceAtMost(size)).mapNotNull { it.takeIf { mImageCache[it] == null } }
        preloadPages(
            pagesAbsent, (index - 10).coerceAtLeast(0) to (mPreloads + index + 10).coerceAtMost(
                size
            )
        )
    }

    fun retryPage(index: Int) {
        notifyPageWait(index)
        onForceRequest(index)
    }

    protected abstract fun preloadPages(pages: List<Int>, pair: Pair<Int, Int>)

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int)

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
        if (mImageCache[index] != image)
            mImageCache.add(index, image)
        mPages[index].image = image
        mPages[index].status.value = Page.State.READY
    }

    fun notifyPageFailed(index: Int, error: String?) {
        mPages[index].errorMsg = error
        mPages[index].status.value = Page.State.ERROR
    }

    private class ImageCache : LruCache<Int, Image>(
        MathUtils.clamp(
            OSUtils.getTotalMemory() / 16,
            MIN_CACHE_SIZE,
            MAX_CACHE_SIZE
        ).toInt()
    ) {
        fun add(key: Int, value: Image) {
            put(key, value)
        }

        override fun sizeOf(key: Int, value: Image): Int {
            return value.size
        }

        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Image, newValue: Image?) {
            oldValue.recycle()
        }

        companion object {
            private const val MAX_CACHE_SIZE = (512 * 1024 * 1024).toLong()
            private const val MIN_CACHE_SIZE = (128 * 1024 * 1024).toLong()
        }
    }
}
