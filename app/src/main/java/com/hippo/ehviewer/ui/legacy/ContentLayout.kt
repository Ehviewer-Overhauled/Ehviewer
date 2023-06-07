/*
 * Copyright 2015 Hippo Seven
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
package com.hippo.ehviewer.ui.legacy

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.databinding.WidgetContentLayoutBinding
import com.hippo.ehviewer.ui.legacy.LayoutManagerUtils.OnScrollToPositionListener
import com.hippo.ehviewer.ui.legacy.ViewTransition.OnShowViewListener
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.yorozuya.IntIdGenerator
import com.hippo.ehviewer.yorozuya.LayoutUtils
import com.hippo.ehviewer.yorozuya.collect.IntList
import rikka.core.res.resolveColor

class ContentLayout(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private lateinit var mContentHelper: ContentHelper
    private val mRecyclerViewOriginBottom: Int
    private val mFastScrollerOriginBottom: Int
    private val binding: WidgetContentLayoutBinding

    init {
        binding = WidgetContentLayoutBinding.inflate((context as Activity).layoutInflater, this)
        clipChildren = false
        clipToPadding = false
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        val drawable = HandlerDrawable()
        drawable.setColor(context.getTheme().resolveColor(androidx.appcompat.R.attr.colorPrimary))
        binding.fastScroller.setHandlerDrawable(drawable)
        mRecyclerViewOriginBottom = binding.recyclerView.paddingBottom
        mFastScrollerOriginBottom = binding.fastScroller.paddingBottom
    }

    val recyclerView
        get() = binding.recyclerView

    val fastScroller
        get() = binding.fastScroller

    fun setHelper(helper: ContentHelper) {
        mContentHelper = helper
        helper.init(this)
    }

    fun hideFastScroll() {
        binding.fastScroller.detachedFromRecyclerView()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, 0)
        setFitPaddingBottom(bottom)
    }

    fun setFitPaddingTop(fitPaddingTop: Int) {
        // RefreshLayout
        binding.refreshLayout.setProgressViewOffset(
            true,
            0,
            fitPaddingTop + LayoutUtils.dp2pix(context, 32f),
        ) // TODO
    }

    private fun setFitPaddingBottom(fitPaddingBottom: Int) {
        // RecyclerView
        binding.recyclerView.setPadding(
            binding.recyclerView.paddingLeft,
            binding.recyclerView.paddingTop,
            binding.recyclerView.paddingRight,
            mRecyclerViewOriginBottom + fitPaddingBottom,
        )
        binding.tip.setPadding(
            binding.tip.paddingLeft,
            binding.tip.paddingTop,
            binding.tip.paddingRight,
            fitPaddingBottom,
        )
        binding.progress.setPadding(
            binding.progress.paddingLeft,
            binding.progress.paddingTop,
            binding.progress.paddingRight,
            fitPaddingBottom,
        )
        binding.fastScroller.setPadding(
            binding.fastScroller.paddingLeft,
            binding.fastScroller.paddingTop,
            binding.fastScroller.paddingRight,
            mFastScrollerOriginBottom + fitPaddingBottom,
        )
        if (fitPaddingBottom > LayoutUtils.dp2pix(context, 16f)) {
            binding.bottomProgress.setPadding(0, 0, 0, fitPaddingBottom)
        } else {
            binding.bottomProgress.setPadding(0, 0, 0, 0)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return mContentHelper.saveInstanceState(super.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(mContentHelper.restoreInstanceState(state))
    }

    abstract class ContentHelper : OnShowViewListener {
        /**
         * Generate task id
         */
        private val mIdGenerator = IntIdGenerator()
        private val mOnScrollToPositionListener =
            OnScrollToPositionListener { position: Int -> onScrollToPosition(position) }
        protected var mPrev: String? = null
        protected var mNext: String? = null
        private lateinit var mViewTransition: ViewTransition
        private lateinit var binding: WidgetContentLayoutBinding

        /**
         * Store data
         */
        private var mData = ArrayList<GalleryInfo>()

        /**
         * Store the page divider index
         *
         *
         * For example, the data contain page 3, page 4, page 5,
         * page 3 size is 7, page 4 size is 8, page 5 size is 9,
         * so `mPageDivider` contain 7, 15, 24.
         */
        private var mPageDivider = IntList()

        /**
         * The first page in `mData`
         */
        private var mStartPage = 0

        /**
         * The last page + 1 in `mData`
         */
        private var mEndPage = 0

        /**
         * The available page count.
         */
        var pages = 0
            private set
        private var mNextPage = 0
        private var mCurrentTaskId = 0
        private var mCurrentTaskType = 0
        private var mCurrentTaskPage = 0
        private val mOnRefreshListener = OnRefreshListener {
            if (mPrev != null || mStartPage > 0) {
                mCurrentTaskId = mIdGenerator.nextId()
                mCurrentTaskType = TYPE_PRE_PAGE_KEEP_POS
                mCurrentTaskPage = mStartPage - 1
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage, mPrev, false)
            } else {
                doRefresh()
            }
        }
        private var mNextPageScrollSize = 0
        private var mEmptyString = "No hint"
        private val mOnScrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!binding.refreshLayout.isRefreshing && !recyclerView.canScrollVertically(1)) {
                        if (mNext != null || mEndPage < pages) {
                            binding.bottomProgress.show()
                            // Get next page
                            // Fill pages before NextPage with empty list
                            while (mNextPage > mEndPage && mEndPage < pages) {
                                mCurrentTaskId = mIdGenerator.nextId()
                                mCurrentTaskType = TYPE_NEXT_PAGE_KEEP_POS
                                mCurrentTaskPage = mEndPage
                                onGetPageData(
                                    mCurrentTaskId,
                                    pages,
                                    mNextPage,
                                    null,
                                    null,
                                    emptyList(),
                                )
                            }
                            mCurrentTaskId = mIdGenerator.nextId()
                            mCurrentTaskType = TYPE_NEXT_PAGE_KEEP_POS
                            mCurrentTaskPage = mEndPage
                            getPageData(
                                mCurrentTaskId,
                                mCurrentTaskType,
                                mCurrentTaskPage,
                                mNext,
                                true,
                            )
                        } else if (mStartPage > 0 && mEndPage == pages) {
                            binding.bottomProgress.show()
                            // Refresh last page
                            mCurrentTaskId = mIdGenerator.nextId()
                            mCurrentTaskType = TYPE_REFRESH_PAGE
                            mCurrentTaskPage = mEndPage - 1
                            getPageData(
                                mCurrentTaskId,
                                mCurrentTaskType,
                                mCurrentTaskPage,
                                null,
                                true,
                            )
                        }
                    }
                }
            }
        fun init(contentLayout: ContentLayout) {
            binding = contentLayout.binding
            mNextPageScrollSize = LayoutUtils.dp2pix(contentLayout.context, 48f)
            val drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid)!!
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            binding.tip.setCompoundDrawables(null, drawable, null, null)
            mViewTransition = ViewTransition(
                binding.contentView,
                binding.progress,
                binding.tip,
            )
            mViewTransition.setOnShowViewListener(this)
            binding.recyclerView.addOnScrollListener(mOnScrollListener)
            binding.refreshLayout.setOnRefreshListener(mOnRefreshListener)
            binding.tip.setOnClickListener { refresh() }
        }

        /**
         * Call [.onGetPageData] when get data
         *
         * @param taskId task id
         * @param page   the page to get
         * @param index  the index to get
         * @param isNext the index is next or prev
         */
        protected abstract fun getPageData(
            taskId: Int,
            type: Int,
            page: Int,
            index: String?,
            isNext: Boolean,
        )

        protected abstract val context: Context
        protected open fun notifyDataSetChanged(callback: () -> Unit) {
            callback()
        }

        protected abstract fun notifyDataSetChanged()
        protected abstract fun notifyItemRangeInserted(positionStart: Int, itemCount: Int)
        protected open fun onScrollToPosition(position: Int) {}
        override fun onShowView(hiddenView: View, shownView: View) {}
        val shownViewIndex: Int
            get() = mViewTransition.shownViewIndex

        fun setRefreshLayoutEnable(enable: Boolean) {
            binding.refreshLayout.isEnabled = enable
        }

        fun setEmptyString(str: String) {
            mEmptyString = str
        }

        val data: List<GalleryInfo>
            get() = mData

        fun getDataAtEx(location: Int): GalleryInfo? {
            return if (location >= 0 && location < mData.size) {
                mData[location]
            } else {
                null
            }
        }

        val firstVisibleItem: GalleryInfo?
            get() = getDataAtEx(LayoutManagerUtils.getFirstVisibleItemPosition(binding.recyclerView.layoutManager))

        fun size(): Int {
            return mData.size
        }

        fun isCurrentTask(taskId: Int): Boolean {
            return mCurrentTaskId == taskId
        }

        protected abstract fun isDuplicate(d1: GalleryInfo, d2: GalleryInfo): Boolean
        private fun removeDuplicateData(data: List<GalleryInfo>, start: Int, end: Int) {
            val slicedData = mData.slice(start.coerceAtLeast(0) until end.coerceAtMost(mData.size))
            data.dropWhile { d1 ->
                slicedData.any { d2 ->
                    isDuplicate(d1, d2)
                }
            }
        }

        protected open fun onAddData(data: List<GalleryInfo>) {}
        protected open fun onRemoveData(data: List<GalleryInfo>) {}
        protected open fun onClearData() {}
        fun onGetPageData(
            taskId: Int,
            pages: Int,
            nextPage: Int,
            prev: String?,
            next: String?,
            data: List<GalleryInfo>,
        ) {
            if (mCurrentTaskId == taskId) {
                val dataSize: Int
                when (mCurrentTaskType) {
                    TYPE_REFRESH -> {
                        mStartPage = 0
                        mEndPage = 1
                        this.pages = pages
                        mNextPage = nextPage
                        mPrev = prev
                        mNext = next
                        mPageDivider.clear()
                        mPageDivider.add(data.size)
                        if (data.isEmpty()) {
                            mData.clear()
                            onClearData()
                            notifyDataSetChanged()

                            notifyDataSetChanged {
                                // Not found
                                // Ui change, show empty string
                                showEmptyString()
                            }
                        } else {
                            mData.clear()
                            onClearData()
                            mData.addAll(data)
                            onAddData(data)
                            notifyDataSetChanged()

                            notifyDataSetChanged {
                                // Ui change, show content
                                showContent()

                                // RecyclerView scroll
                                if (binding.recyclerView.isAttachedToWindow) {
                                    scrollToPosition(0)
                                }
                            }
                        }
                    }

                    TYPE_PRE_PAGE, TYPE_PRE_PAGE_KEEP_POS -> {
                        removeDuplicateData(data, 0, CHECK_DUPLICATE_RANGE)
                        dataSize = data.size
                        var i = 0
                        val n = mPageDivider.size
                        while (i < n) {
                            mPageDivider[i] = mPageDivider[i] + dataSize
                            i++
                        }
                        mPageDivider.add(0, dataSize)
                        mStartPage--
                        this.pages = pages.coerceAtLeast(mEndPage)
                        mPrev = prev
                        // assert mStartPage >= 0
                        if (data.isEmpty()) {
                            // OK, that's all
                            if (mData.isEmpty()) {
                                // Ui change, show empty string
                                showEmptyString()
                            } else {
                                // Ui change, show content
                                showContent()
                                if (mCurrentTaskType == TYPE_PRE_PAGE && binding.recyclerView.isAttachedToWindow) {
                                    // RecyclerView scroll, to top
                                    scrollToPosition(0)
                                }
                            }
                        } else {
                            mData.addAll(0, data)
                            onAddData(data)
                            notifyItemRangeInserted(0, data.size)

                            notifyDataSetChanged {
                                // Ui change, show content
                                showContent()
                                if (binding.recyclerView.isAttachedToWindow) {
                                    // RecyclerView scroll
                                    if (mCurrentTaskType == TYPE_PRE_PAGE_KEEP_POS) {
                                        binding.recyclerView.stopScroll()
                                        LayoutManagerUtils.scrollToPositionProperly(
                                            binding.recyclerView.layoutManager,
                                            context,
                                            dataSize - 1,
                                            mOnScrollToPositionListener,
                                        )
                                    } else {
                                        scrollToPosition(0)
                                    }
                                }
                            }
                        }
                    }

                    TYPE_NEXT_PAGE, TYPE_NEXT_PAGE_KEEP_POS -> {
                        removeDuplicateData(data, mData.size - CHECK_DUPLICATE_RANGE, mData.size)
                        dataSize = data.size
                        val oldDataSize = mData.size
                        mPageDivider.add(oldDataSize + dataSize)
                        mEndPage++
                        mNextPage = nextPage
                        this.pages = pages.coerceAtLeast(mEndPage)
                        mNext = next
                        if (data.isEmpty()) {
                            // OK, that's all
                            if (mData.isEmpty()) {
                                // Ui change, show empty string
                                showEmptyString()
                            } else {
                                // Ui change, show content
                                showContent()
                                if (mCurrentTaskType == TYPE_NEXT_PAGE && binding.recyclerView.isAttachedToWindow) {
                                    // RecyclerView scroll
                                    scrollToPosition(oldDataSize)
                                }
                            }
                        } else {
                            mData.addAll(data)
                            onAddData(data)
                            notifyItemRangeInserted(oldDataSize, dataSize)

                            notifyDataSetChanged {
                                // Ui change, show content
                                showContent()
                                if (binding.recyclerView.isAttachedToWindow) {
                                    if (mCurrentTaskType == TYPE_NEXT_PAGE_KEEP_POS) {
                                        binding.recyclerView.stopScroll()
                                        binding.recyclerView.smoothScrollBy(0, mNextPageScrollSize)
                                    } else {
                                        scrollToPosition(oldDataSize)
                                    }
                                }
                            }
                        }
                    }

                    TYPE_SOMEWHERE -> {
                        mStartPage = mCurrentTaskPage
                        mEndPage = mCurrentTaskPage + 1
                        mNextPage = nextPage
                        this.pages = pages
                        mPrev = prev
                        mNext = next
                        mPageDivider.clear()
                        mPageDivider.add(data.size)
                        if (data.isEmpty()) {
                            mData.clear()
                            onClearData()
                            notifyDataSetChanged()

                            notifyDataSetChanged {
                                // Not found
                                // Ui change, show empty string
                                showEmptyString()
                            }
                        } else {
                            mData.clear()
                            onClearData()
                            mData.addAll(data)
                            onAddData(data)
                            notifyDataSetChanged()

                            notifyDataSetChanged {
                                // Ui change, show content
                                showContent()
                                if (binding.recyclerView.isAttachedToWindow) {
                                    // RecyclerView scroll
                                    scrollToPosition(0)
                                }
                            }
                        }
                    }

                    TYPE_REFRESH_PAGE -> {
                        if (mCurrentTaskPage < mStartPage || mCurrentTaskPage >= mEndPage) {
                            Log.e(
                                TAG,
                                "TYPE_REFRESH_PAGE, but mCurrentTaskPage = " + mCurrentTaskPage +
                                    ", mStartPage = " + mStartPage + ", mEndPage = " + mEndPage,
                            )
                            return
                        }
                        if (mCurrentTaskPage == mEndPage - 1) {
                            mNextPage = nextPage
                        }
                        this.pages = pages.coerceAtLeast(mEndPage)
                        val oldIndexStart =
                            if (mCurrentTaskPage == mStartPage) 0 else mPageDivider[mCurrentTaskPage - mStartPage - 1]
                        val oldIndexEnd = mPageDivider[mCurrentTaskPage - mStartPage]
                        val toRemove = mData.subList(oldIndexStart, oldIndexEnd)
                        onRemoveData(toRemove)
                        toRemove.clear()
                        removeDuplicateData(
                            data,
                            oldIndexStart - CHECK_DUPLICATE_RANGE,
                            oldIndexStart + CHECK_DUPLICATE_RANGE,
                        )
                        val newIndexEnd = oldIndexStart + data.size
                        mData.addAll(oldIndexStart, data)
                        onAddData(data)
                        notifyDataSetChanged()
                        notifyDataSetChanged {
                            var i = mCurrentTaskPage - mStartPage
                            val n = mPageDivider.size
                            while (i < n) {
                                mPageDivider[i] = mPageDivider[i] - oldIndexEnd + newIndexEnd
                                i++
                            }
                            if (mData.isEmpty()) {
                                // Ui change, show empty string
                                showEmptyString()
                            } else {
                                // Ui change, show content
                                showContent()

                                // RecyclerView scroll
                                if (newIndexEnd > oldIndexEnd && newIndexEnd > 0 && binding.recyclerView.isAttachedToWindow) {
                                    scrollToPosition(newIndexEnd - 1)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun onGetException(taskId: Int, e: Throwable) {
            if (mCurrentTaskId == taskId) {
                showProgress(PROGRESS_NONE)
                val readableError = run {
                    e.printStackTrace()
                    ExceptionUtils.getReadableString(e)
                }
                if (mViewTransition.shownViewIndex == 0) {
                    Toast.makeText(context, readableError, Toast.LENGTH_SHORT).show()
                } else {
                    showText(readableError)
                }
            }
        }

        private fun scrollToPosition(position: Int) {
            binding.recyclerView.stopScroll()
            LayoutManagerUtils.scrollToPositionWithOffset(
                binding.recyclerView.layoutManager,
                position,
                0,
            )
            onScrollToPosition(position)
        }

        private fun showProgress(type: Int) {
            when (type) {
                PROGRESS_NONE -> {
                    binding.refreshLayout.isRefreshing = false
                    binding.bottomProgress.hide()
                }

                PROGRESS_TOP -> {
                    binding.refreshLayout.isRefreshing = true
                    binding.bottomProgress.hide()
                }

                PROGRESS_BOTTOM -> {
                    binding.refreshLayout.isRefreshing = false
                    binding.bottomProgress.show()
                }
            }
        }

        private fun showContent() {
            showProgress(PROGRESS_NONE)
            mViewTransition.showView(0)
        }

        private val isContentShowing: Boolean
            get() = mViewTransition.shownViewIndex == 0

        @JvmOverloads
        fun showProgressBar(animation: Boolean = true) {
            mViewTransition.showView(1, animation)
        }

        private fun showText(text: CharSequence?) {
            binding.tip.text = text
            mViewTransition.showView(2)
        }

        private fun showEmptyString() {
            showProgress(PROGRESS_NONE)
            showText(mEmptyString)
        }

        private fun doRefresh() {
            mCurrentTaskId = mIdGenerator.nextId()
            mCurrentTaskType = TYPE_REFRESH
            mCurrentTaskPage = 0
            getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage, null, true)
        }

        /**
         * Like [.refresh], but no animation when show progress bar
         */
        fun firstRefresh() {
            showProgressBar(false)
            doRefresh()
        }

        /**
         * Show progress bar first, than do refresh
         */
        fun refresh() {
            showProgressBar()
            doRefresh()
        }

        private fun cancelCurrentTask() {
            mCurrentTaskId = mIdGenerator.nextId()
            showProgress(PROGRESS_NONE)
        }

        private fun getPageStart(page: Int): Int {
            return if (mStartPage == page) {
                0
            } else {
                mPageDivider[page - mStartPage - 1]
            }
        }

        private fun getPageForPosition(position: Int): Int {
            if (position < 0) {
                return -1
            }
            val pageDivider = mPageDivider
            var i = 0
            val n = pageDivider.size
            while (i < n) {
                if (position < pageDivider[i]) {
                    return i + mStartPage
                }
                i++
            }
            return -1
        }

        val pageForTop: Int
            get() = getPageForPosition(LayoutManagerUtils.getFirstVisibleItemPosition(binding.recyclerView.layoutManager))
        val pageForBottom: Int
            get() = getPageForPosition(LayoutManagerUtils.getLastVisibleItemPosition(binding.recyclerView.layoutManager))

        fun canGoTo(): Boolean {
            return isContentShowing
        }

        /**
         * Check range first!
         *
         * @param page the target page
         * @throws IndexOutOfBoundsException if page < 0 or page >= mPages
         */
        @Throws(IndexOutOfBoundsException::class)
        fun goTo(page: Int) {
            if (page < 0 || page >= pages) {
                throw IndexOutOfBoundsException("Page count is $pages, page is $page")
            } else if (page in mStartPage until mEndPage) {
                cancelCurrentTask()
                val position = getPageStart(page)
                scrollToPosition(position)
            } else if (page == mStartPage - 1) {
                showProgress(PROGRESS_TOP)
                mCurrentTaskId = mIdGenerator.nextId()
                mCurrentTaskType = TYPE_PRE_PAGE
                mCurrentTaskPage = page
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage, null, true)
            } else if (page == mEndPage) {
                showProgress(PROGRESS_BOTTOM)
                mCurrentTaskId = mIdGenerator.nextId()
                mCurrentTaskType = TYPE_NEXT_PAGE
                mCurrentTaskPage = page
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage, null, true)
            } else {
                showProgress(PROGRESS_TOP)
                mCurrentTaskId = mIdGenerator.nextId()
                mCurrentTaskType = TYPE_SOMEWHERE
                mCurrentTaskPage = page
                getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage, null, true)
            }
        }

        fun goTo(index: String?, isNext: Boolean) {
            showProgress(PROGRESS_TOP)
            mCurrentTaskId = mIdGenerator.nextId()
            mCurrentTaskType = TYPE_SOMEWHERE
            mCurrentTaskPage = 0
            getPageData(mCurrentTaskId, mCurrentTaskType, mCurrentTaskPage, index, isNext)
        }

        open fun saveInstanceState(superState: Parcelable?): Parcelable {
            if (mData.isNotEmpty()) cancelCurrentTask()
            val bundle = Bundle()
            bundle.putParcelable(KEY_SUPER, superState)
            val shownView = mViewTransition.shownViewIndex
            bundle.putInt(KEY_SHOWN_VIEW, shownView)
            bundle.putString(KEY_TIP, binding.tip.text.toString())
            synchronized(contentMap) {
                val size = contentMap.keys.takeIf { it.isNotEmpty() }?.max()?.inc() ?: 0
                contentMap[size] = mData
                bundle.putInt(KEY_DATA, size)
            }
            bundle.putInt(KEY_NEXT_ID, mIdGenerator.nextId())
            bundle.putParcelable(KEY_PAGE_DIVIDER, mPageDivider)
            bundle.putInt(KEY_START_PAGE, mStartPage)
            bundle.putInt(KEY_END_PAGE, mEndPage)
            bundle.putInt(KEY_PAGES, pages)
            bundle.putString(KEY_PREV, mPrev)
            bundle.putString(KEY_NEXT, mNext)
            return bundle
        }

        open fun restoreInstanceState(state: Parcelable): Parcelable? {
            return if (state is Bundle) {
                mViewTransition.showView(state.getInt(KEY_SHOWN_VIEW), false)
                binding.tip.text = state.getString(KEY_TIP)
                val data = state.getInt(KEY_DATA)
                synchronized(contentMap) {
                    mData = contentMap.remove(data) ?: ArrayList()
                }
                mIdGenerator.setNextId(state.getInt(KEY_NEXT_ID))
                mPageDivider = state.getParcelableCompat(KEY_PAGE_DIVIDER) ?: IntList()
                mStartPage = state.getInt(KEY_START_PAGE)
                mEndPage = state.getInt(KEY_END_PAGE)
                pages = state.getInt(KEY_PAGES)
                mPrev = state.getString(KEY_PREV)
                mNext = state.getString(KEY_NEXT)
                notifyDataSetChanged()
                notifyDataSetChanged {}
                if (mData.size == 0) {
                    mPageDivider.clear()
                    mStartPage = 0
                    mEndPage = 0
                    pages = 0
                    mPrev = null
                    mNext = null
                    firstRefresh()
                }
                state.getParcelableCompat(KEY_SUPER)
            } else {
                state
            }
        }

        companion object {
            const val TYPE_REFRESH = 0
            const val TYPE_PRE_PAGE = 1
            const val TYPE_PRE_PAGE_KEEP_POS = 2
            const val TYPE_NEXT_PAGE = 3
            const val TYPE_NEXT_PAGE_KEEP_POS = 4
            const val TYPE_SOMEWHERE = 5
            const val TYPE_REFRESH_PAGE = 6
            private val TAG = ContentHelper::class.java.simpleName
            private const val CHECK_DUPLICATE_RANGE = 50
            private const val PROGRESS_NONE = 0
            private const val PROGRESS_TOP = 1
            private const val PROGRESS_BOTTOM = 2
            private const val KEY_SUPER = "super"
            private const val KEY_SHOWN_VIEW = "shown_view"
            private const val KEY_TIP = "tip"
            private const val KEY_DATA = "data"
            private const val KEY_NEXT_ID = "next_id"
            private const val KEY_PAGE_DIVIDER = "page_divider"
            private const val KEY_START_PAGE = "start_page"
            private const val KEY_END_PAGE = "end_page"
            private const val KEY_PAGES = "pages"
            private const val KEY_PREV = "prev"
            private const val KEY_NEXT = "next"
        }
    }
}

// data "ArrayList<GalleryInfo>" is too large, we cannot put it as Parcelable otherwise TransactionTooLargeException would occur
// TODO: convert to paging3 source then keep it in a proper window
// @see PagingConfig.maxSize
private val contentMap = hashMapOf<Int, ArrayList<GalleryInfo>>()
