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
package com.hippo.ehviewer.ui.scene

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.hippo.app.BaseDialogBuilder
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryPreview
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.databinding.DialogGoToBinding
import com.hippo.ehviewer.databinding.SceneGalleryPreviewsBinding
import com.hippo.util.getParcelableCompat
import com.hippo.widget.ContentLayout.ContentHelper
import com.hippo.widget.recyclerview.AutoGridLayoutManager
import com.hippo.widget.recyclerview.STRATEGY_SUITABLE_SIZE
import com.hippo.yorozuya.LayoutUtils
import java.util.Locale

class GalleryPreviewsScene : BaseToolbarScene() {
    private var _binding: SceneGalleryPreviewsBinding? = null
    private val binding
        get() = _binding!!
    private var mGalleryDetail: GalleryDetail? = null
    private var mAdapter: GalleryPreviewsAdapter? = null
    private var mHelper: GalleryPreviewHelper? = null
    private var mHasFirstRefresh = false
    private var mNextPage: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun onInit() {
        val args = arguments ?: return
        mGalleryDetail = args.getParcelableCompat(KEY_GALLERY_DETAIL)
        mNextPage = args.getBoolean(KEY_NEXT_PAGE)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mGalleryDetail = savedInstanceState.getParcelableCompat(KEY_GALLERY_DETAIL)
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val hasFirstRefresh: Boolean = if (mHelper != null && 1 == mHelper!!.shownViewIndex) {
            false
        } else {
            mHasFirstRefresh
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh)
        outState.putParcelable(KEY_GALLERY_DETAIL, mGalleryDetail)
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SceneGalleryPreviewsBinding.inflate(inflater, container, false)
        binding.contentLayout.hideFastScroll()
        setLiftOnScrollTargetView(binding.contentLayout.recyclerView)
        mAdapter = GalleryPreviewsAdapter {
            mainActivity!!.startReaderActivity(mGalleryDetail!!, it.position)
        }
        binding.contentLayout.recyclerView.adapter = mAdapter
        val columnWidth = Settings.thumbSize
        val layoutManager =
            AutoGridLayoutManager(context, columnWidth, LayoutUtils.dp2pix(context, 16f))
        layoutManager.setStrategy(STRATEGY_SUITABLE_SIZE)
        binding.contentLayout.recyclerView.layoutManager = layoutManager
        binding.contentLayout.recyclerView.clipToPadding = false
        val padding = LayoutUtils.dp2pix(context, 4f)
        val decoration = MarginItemDecoration(padding, padding, padding, padding, padding)
        binding.contentLayout.recyclerView.addItemDecoration(decoration)
        mHelper = GalleryPreviewHelper()
        binding.contentLayout.setHelper(mHelper!!)

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true
            mHelper!!.onGetPageData(
                0,
                mGalleryDetail!!.previewPages,
                0,
                null,
                null,
                mGalleryDetail!!.previewList
            )
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (null != mHelper) {
            if (1 == mHelper!!.shownViewIndex) {
                mHasFirstRefresh = false
            }
        }
        binding.contentLayout.recyclerView.stopScroll()
        _binding = null
        mAdapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.gallery_previews)
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24)
        if (mGalleryDetail != null) {
            if (mGalleryDetail!!.previewPages > 2) showMenu(R.menu.scene_gallery_previews)
        }
        runCatching {
            if (mNextPage && mHelper!!.pages > 1) mHelper!!.goTo(1)
        }.onFailure {
            it.printStackTrace()
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val context = context ?: return false
        val id = item.itemId
        if (id == R.id.action_go_to) {
            if (mHelper == null) {
                return true
            }
            val pages = mHelper!!.pages
            if (pages > 1 && mHelper!!.canGoTo()) {
                showGoToDialog(context, pages, mHelper!!.pageForBottom)
            }
            return true
        }
        return false
    }

    private fun onGetPreviewListSuccess(result: Pair<List<GalleryPreview>, Int>, taskId: Int) {
        if (null != mHelper && mHelper!!.isCurrentTask(taskId) && null != mGalleryDetail) {
            mHelper!!.onGetPageData(taskId, result.second, 0, null, null, result.first)
        }
    }

    private fun onGetPreviewListFailure(e: Exception, taskId: Int) {
        if (mHelper != null && mHelper!!.isCurrentTask(taskId)) {
            mHelper!!.onGetException(taskId, e)
        }
    }

    private inner class GetPreviewListListener(
        context: Context,
        private val mTaskId: Int
    ) : EhCallback<GalleryPreviewsScene, Pair<List<GalleryPreview>, Int>>(context) {
        override fun onSuccess(result: Pair<List<GalleryPreview>, Int>) {
            val scene = this@GalleryPreviewsScene
            scene.onGetPreviewListSuccess(result, mTaskId)
        }

        override fun onFailure(e: Exception) {
            val scene = this@GalleryPreviewsScene
            scene.onGetPreviewListFailure(e, mTaskId)
        }

        override fun onCancel() {}
    }

    private inner class GalleryPreviewHelper : ContentHelper<GalleryPreview>() {
        override fun getPageData(
            taskId: Int,
            type: Int,
            page: Int,
            index: String?,
            isNext: Boolean
        ) {
            val activity = mainActivity
            if (null == activity || null == mGalleryDetail) {
                onGetException(taskId, EhException(getString(R.string.error_cannot_find_gallery)))
                return
            }
            val url =
                EhUrl.getGalleryDetailUrl(mGalleryDetail!!.gid, mGalleryDetail!!.token, page, false)
            val request = EhRequest()
            request.setMethod(EhClient.METHOD_GET_PREVIEW_LIST)
            request.setCallback(GetPreviewListListener(context, taskId))
            request.setArgs(url)
            request.enqueue(this@GalleryPreviewsScene)
        }

        override val context
            get() = this@GalleryPreviewsScene.requireContext()

        override fun notifyDataSetChanged(callback: () -> Unit) {
            mAdapter?.submitList(data.toMutableList(), callback)
        }

        override fun notifyDataSetChanged() {}

        override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {}

        override fun isDuplicate(d1: GalleryPreview, d2: GalleryPreview): Boolean {
            return false
        }
    }

    private fun showGoToDialog(context: Context, pages: Int, currentPage: Int) {
        val dialogBinding = DialogGoToBinding.inflate(layoutInflater)
        dialogBinding.start.text = String.format(Locale.US, "%d", 1)
        dialogBinding.end.text = String.format(Locale.US, "%d", pages)
        dialogBinding.slider.valueTo = pages.toFloat()
        dialogBinding.slider.value = (currentPage + 1).toFloat()
        val dialog = BaseDialogBuilder(context)
            .setTitle(R.string.go_to)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val page = (dialogBinding.slider.value - 1).toInt()
                mHelper!!.goTo(page)
            }
            .create()
        dialog.show()
    }

    companion object {
        const val KEY_GALLERY_DETAIL = "gallery_detail"
        const val KEY_NEXT_PAGE = "next_page"
        private const val KEY_HAS_FIRST_REFRESH = "has_first_refresh"
    }
}