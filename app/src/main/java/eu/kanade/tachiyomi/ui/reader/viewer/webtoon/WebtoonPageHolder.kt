package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.hippo.ehviewer.databinding.ReaderErrorBinding
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPageImageView
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderProgressIndicator
import eu.kanade.tachiyomi.util.system.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Holder of the webtoon reader for a single page of a chapter.
 *
 * @param frame the root view for this holder.
 * @param viewer the webtoon viewer.
 * @constructor creates a new webtoon holder.
 */
class WebtoonPageHolder(
    private val frame: ReaderPageImageView,
    viewer: WebtoonViewer,
) : WebtoonBaseHolder(frame, viewer) {

    /**
     * Loading progress bar to indicate the current progress.
     */
    private val progressIndicator = createProgressIndicator()

    /**
     * Progress bar container. Needed to keep a minimum height size of the holder, otherwise the
     * adapter would create more views to fill the screen, which is not wanted.
     */
    private lateinit var progressContainer: ViewGroup

    /**
     * Error layout to show when the image fails to load.
     */
    private var errorLayout: ReaderErrorBinding? = null

    /**
     * The default height when image not available.
     */
    private val defaultHeight
        get() = (viewer.recycler.width * 1.4125).toInt()

    /**
     * Page of a chapter.
     */
    private var page: ReaderPage? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Subscription for status changes of the page.
     */
    private var statusJob: Job? = null

    /**
     * Job for progress changes of the page.
     */
    private var progressJob: Job? = null

    init {
        refreshLayoutParams()

        frame.onImageLoaded = { onImageDecoded() }
        frame.onImageLoadError = { onImageDecodeError() }
        frame.onScaleChanged = { viewer.activity.hideMenu() }
    }

    /**
     * Binds the given [page] with this view holder, subscribing to its state.
     */
    fun bind(page: ReaderPage) {
        this.page = page
        statusJob = scope.launch(Dispatchers.Main) {
            page.status.collectLatest {
                processStatus(it)
            }
        }
        refreshLayoutParams()
    }

    private fun refreshLayoutParams() {
        frame.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            if (!viewer.isContinuous) {
                bottomMargin = 15.dpToPx
            }

            val margin = Resources.getSystem().displayMetrics.widthPixels * (viewer.config.sidePadding / 100f)
            marginEnd = margin.toInt()
            marginStart = margin.toInt()
        }
    }

    /**
     * Called when the view is recycled and added to the view pool.
     */
    override fun recycle() {
        statusJob?.cancel()
        cancelProgressJob()

        removeErrorLayout()
        frame.recycle()
        progressIndicator.setProgress(0, animated = false)
    }

    /**
     * Observes the progress of the page and updates view.
     */
    private fun launchProgressJob() {
        cancelProgressJob()

        val page = page ?: return

        progressJob = scope.launch(Dispatchers.Main) {
            page.progressFlow.collectLatest { value -> progressIndicator.setProgress(value) }
        }
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
                launchProgressJob()
                setDownloading()
            }
            Page.State.READY -> {
                page?.image?.mObtainedDrawable?.let { setImage(it) }
                cancelProgressJob()
            }
            Page.State.ERROR -> {
                setError()
                cancelProgressJob()
            }
        }
    }

    /**
     * Unsubscribes from the progress subscription.
     */
    private fun cancelProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * Called when the page is queued.
     */
    private fun setQueued() {
        progressContainer.isVisible = true
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is loading.
     */
    private fun setLoading() {
        progressContainer.isVisible = true
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is downloading
     */
    private fun setDownloading() {
        progressContainer.isVisible = true
        progressIndicator.show()
        removeErrorLayout()
    }

    /**
     * Called when the page is ready.
     */
    private fun setImage(drawable: Drawable) {
        progressIndicator.setProgress(0)
        removeErrorLayout()
        frame.setImage(drawable,
            ReaderPageImageView.Config(
            zoomDuration = viewer.config.doubleTapAnimDuration,
            minimumScaleType = SubsamplingScaleImageView.SCALE_TYPE_FIT_WIDTH,
            cropBorders = viewer.config.imageCropBorders,
        ),)
    }

    /**
     * Called when the page has an error.
     */
    private fun setError() {
        progressContainer.isVisible = false
        initErrorLayout()
    }

    /**
     * Called when the image is decoded and going to be displayed.
     */
    private fun onImageDecoded() {
        progressContainer.isVisible = false
    }

    /**
     * Called when the image fails to decode.
     */
    private fun onImageDecodeError() {
        progressContainer.isVisible = false
        initErrorLayout()
    }

    /**
     * Creates a new progress bar.
     */
    private fun createProgressIndicator(): ReaderProgressIndicator {
        progressContainer = FrameLayout(context)
        frame.addView(progressContainer, MATCH_PARENT, defaultHeight)

        val progress = ReaderProgressIndicator(context).apply {
            updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.CENTER
            }
        }
        progressContainer.addView(progress)
        return progress
    }

    /**
     * Initializes a button to retry pages.
     */
    private fun initErrorLayout(): ReaderErrorBinding {
        if (errorLayout == null) {
            errorLayout = ReaderErrorBinding.inflate(LayoutInflater.from(context), frame, true)
            errorLayout?.root?.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, defaultHeight)
            errorLayout?.actionRetry?.setOnClickListener {
                viewer.activity.mGalleryProvider?.retryPage(page!!.index)
            }
        }
        page?.errorMsg?.let { errorLayout!!.errorMessage.text = it }
        return errorLayout!!
    }

    /**
     * Removes the decode error layout from the holder, if found.
     */
    private fun removeErrorLayout() {
        errorLayout?.let {
            frame.removeView(it.root)
            errorLayout = null
        }
    }
}
