package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.hippo.ehviewer.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * View of the ViewPager that contains a page of a chapter.
 */
@SuppressLint("ViewConstructor")
class PagerPageHolder(
    readerThemedContext: Context,
    val viewer: PagerViewer,
    val page: ReaderPage,
) : ReaderPageImageView(readerThemedContext), ViewPagerAdapter.PositionableView {

    /**
     * Item that identifies this view. Needed by the adapter to not recreate views.
     */
    override val item
        get() = page

    /**
     * Loading progress bar to indicate the current progress.
     */
    private val progressIndicator: ReaderProgressIndicator =
        ReaderProgressIndicator(readerThemedContext).apply {
            updateLayoutParams<LayoutParams> {
                gravity = Gravity.CENTER
            }
        }

    /**
     * Error layout to show when the image fails to load.
     */
    private var errorLayout: ReaderErrorBinding? = null

    private var scope = CoroutineScope(Dispatchers.IO)

    /**
     * Subscription for status changes of the page.
     */
    private var statusJob: Job? = null

    /**
     * Subscription for progress changes of the page.
     */
    private var progressJob: Job? = null

    init {
        addView(progressIndicator)
        progressJob = scope.launch(Dispatchers.Main) {
            page.progress.collect {
                progressIndicator.setProgress(it.toInt())
            }
        }
        statusJob = scope.launch(Dispatchers.Main) {
            page.status.collectLatest {
                processStatus(it)
            }
        }
    }

    /**
     * Called when this view is detached from the window. Unsubscribes any active subscription.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unsubscribeProgress()
        unsubscribeStatus()
    }

    /**
     * Called when the status of the page changes.
     *
     * @param status the new status of the page.
     */
    private fun processStatus(status: Page.State) {
        when (status) {
            Page.State.QUEUE -> setQueued()
            Page.State.LOAD_PAGE -> setLoading()
            Page.State.DOWNLOAD_IMAGE -> {
                setDownloading()
            }

            Page.State.READY -> {
                page.image?.mObtainedDrawable?.let { setImage(it) }
                unsubscribeProgress()
            }

            Page.State.ERROR -> {
                setError()
                unsubscribeProgress()
            }
        }
    }

    /**
     * Unsubscribes from the status subscription.
     */
    private fun unsubscribeStatus() {
        statusJob?.cancel()
    }

    /**
     * Unsubscribes from the progress subscription.
     */
    private fun unsubscribeProgress() {
        progressJob?.cancel()
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progressIndicator.show()
        errorLayout?.root?.isVisible = false
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progressIndicator.show()
        errorLayout?.root?.isVisible = false
    }

    /**
     * Called when the page is downloading.
     */
    private fun setDownloading() {
        progressIndicator.show()
        errorLayout?.root?.isVisible = false
    }

    /**
     * Called when the page is ready.
     */
    private fun setImage(drawable: Drawable) {
        progressIndicator.setProgress(0)
        errorLayout?.root?.isVisible = false
        setImage(drawable,
            Config(
                zoomDuration = viewer.config.doubleTapAnimDuration,
                minimumScaleType = viewer.config.imageScaleType,
                cropBorders = viewer.config.imageCropBorders,
                zoomStartPosition = viewer.config.imageZoomType,
                landscapeZoom = viewer.config.landscapeZoom,
            ),)
        if (drawable !is Animatable)
            pageBackground = background
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progressIndicator.hide()
        showErrorLayout()
    }

    override fun onImageLoaded() {
        super.onImageLoaded()
        progressIndicator.hide()
    }

    /**
     * Called when an image fails to decode.
     */
    override fun onImageLoadError() {
        super.onImageLoadError()
        progressIndicator.hide()
        showErrorLayout()
    }

    /**
     * Called when an image is zoomed in/out.
     */
    override fun onScaleChanged(newScale: Float) {
        super.onScaleChanged(newScale)
        viewer.activity.hideMenu()
    }

    private fun showErrorLayout(): ReaderErrorBinding {
        if (errorLayout == null) {
            errorLayout = ReaderErrorBinding.inflate(LayoutInflater.from(context), this, true)
            errorLayout?.actionRetry?.viewer = viewer
            errorLayout?.actionRetry?.setOnClickListener {
                viewer.activity.mGalleryProvider?.request(page.index)
            }
        }
        errorLayout?.root?.isVisible = true
        return errorLayout!!
    }
}
