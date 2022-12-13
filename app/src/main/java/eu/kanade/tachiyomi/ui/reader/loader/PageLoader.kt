package eu.kanade.tachiyomi.ui.reader.loader

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

    abstract fun stop()

    abstract fun size(): Int

    fun request(index: Int) {
        onRequest(index)
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

    fun notifyDataChanged(index: Int) {
        onRequest(index)
    }

    fun notifyPageWait(index: Int) {
        mPages[index].status.value = Page.State.QUEUE
    }

    fun notifyPagePercent(index: Int, percent: Float) {
        mPages[index].status.compareAndSet(Page.State.QUEUE, Page.State.DOWNLOAD_IMAGE)
        mPages[index].progress.value = percent
    }

    fun notifyPageSucceed(index: Int, image: Image) {
        val priv = mPages[index].image
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
