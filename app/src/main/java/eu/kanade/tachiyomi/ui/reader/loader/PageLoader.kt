package eu.kanade.tachiyomi.ui.reader.loader

import androidx.annotation.CallSuper
import com.hippo.image.Image
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.MutableStateFlow

abstract class PageLoader {
    val mPages by lazy {
        check(size() > 0)
        (0 until size()).map { ReaderPage(it) }
    }

    var state = MutableStateFlow(STATE_WAIT)

    abstract val error: String

    abstract fun start()

    @CallSuper
    open fun stop() {
        mPages.forEach { it.image?.recycle() }
    }

    abstract fun size(): Int

    fun request(index: Int) {
        if (mPages[index].image?.isRecycled == false)
            notifyPageSucceed(index, mPages[index].image!!)
        else
            onRequest(index)
    }

    fun retryPage(index: Int) {
        onForceRequest(index)
    }

    protected abstract fun onRequest(index: Int)

    protected abstract fun onForceRequest(index: Int)

    fun cancelRequest(index: Int) {
        onCancelRequest(index)
    }

    protected abstract fun onCancelRequest(index: Int)

    fun notifyDataChanged() {
        if (size() == STATE_ERROR)
            state.value = STATE_ERROR
        else
            state.compareAndSet(STATE_WAIT, STATE_READY)
    }

    fun notifyPageWait(index: Int) {
        mPages[index].status.value = Page.State.QUEUE
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        mPages[index].status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
        mPages[index].progress = (percent * 100).toInt()
    }

    fun notifyPageSucceed(index: Int, image: Image) {
        val priv = if (image != mPages[index].image) mPages[index].image else null
        mPages[index].image = image
        mPages[index].status.value = Page.State.READY
        priv?.recycle()
    }

    fun notifyPageFailed(index: Int) {
        mPages[index].status.value = Page.State.ERROR
    }

    companion object {
        const val STATE_WAIT = -1
        const val STATE_ERROR = -2
        const val STATE_READY = -3
    }
}
