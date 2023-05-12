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

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager
import com.hippo.widget.recyclerview.STRATEGY_MIN_SIZE
import com.hippo.widget.recyclerview.STRATEGY_SUITABLE_SIZE

abstract class GalleryAdapter(
    private val mResources: Resources,
    private val mRecyclerView: RecyclerView,
    type: Int,
    showFavourited: Boolean,
) : RecyclerView.Adapter<GalleryHolder>() {
    private val mLayoutManager: AutoStaggeredGridLayoutManager =
        AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
    private val mPaddingTopSB: Int =
        mResources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
    private val mShowFavourited: Boolean
    private var mListDecoration: ItemDecoration? = null
    private var mGirdDecoration: MarginItemDecoration? = null
    private var mType = TYPE_INVALID

    var type: Int
        get() = mType
        set(type) {
            if (type == mType) {
                return
            }
            mType = type
            val recyclerView = mRecyclerView
            @SuppressLint("NotifyDataSetChanged")
            when (type) {
                TYPE_LIST -> {
                    val columnWidth = mResources.getDimensionPixelOffset(Settings.detailSizeResId)
                    mLayoutManager.setColumnSize(columnWidth)
                    mLayoutManager.setStrategy(STRATEGY_MIN_SIZE)
                    if (null != mGirdDecoration) {
                        recyclerView.removeItemDecoration(mGirdDecoration!!)
                    }
                    if (null == mListDecoration) {
                        val interval = mResources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
                        val paddingH = mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
                        val paddingV = mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
                        mListDecoration = object : ItemDecoration() {
                            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                                outRect.set(0, interval / 2, 0, interval / 2)
                            }
                        }
                        recyclerView.setPadding(
                            recyclerView.paddingLeft + paddingH,
                            recyclerView.paddingTop + paddingV,
                            recyclerView.paddingRight + paddingH,
                            recyclerView.paddingBottom + paddingV,
                        )
                    }
                    recyclerView.addItemDecoration(mListDecoration!!)
                    notifyDataSetChanged()
                }

                TYPE_GRID -> {
                    val columnWidth = Settings.thumbSize
                    mLayoutManager.setColumnSize(columnWidth)
                    mLayoutManager.setStrategy(STRATEGY_SUITABLE_SIZE)
                    if (null != mListDecoration) {
                        recyclerView.removeItemDecoration(mListDecoration!!)
                    }
                    if (null == mGirdDecoration) {
                        val interval = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_interval)
                        val paddingH = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_h)
                        val paddingV = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_v)
                        mGirdDecoration = MarginItemDecoration(interval, 0, 0, 0, 0)
                        recyclerView.setPadding(
                            recyclerView.paddingLeft + paddingH,
                            recyclerView.paddingTop + paddingV,
                            recyclerView.paddingRight + paddingH,
                            recyclerView.paddingBottom + paddingV,
                        )
                    }
                    recyclerView.addItemDecoration(mGirdDecoration!!)
                    notifyDataSetChanged()
                }
            }
        }

    init {
        mShowFavourited = showFavourited
        mRecyclerView.adapter = this
        mRecyclerView.layoutManager = mLayoutManager
        @SuppressLint("InflateParams")
        this.type = type
        adjustPaddings()
    }

    private fun adjustPaddings() {
        val recyclerView = mRecyclerView
        recyclerView.setPadding(
            recyclerView.paddingLeft,
            recyclerView.paddingTop + mPaddingTopSB,
            recyclerView.paddingRight,
            recyclerView.paddingBottom,
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder {
        val holder = when (viewType) {
            TYPE_LIST -> ListGalleryHolder(
                ComposeView(parent.context),
                mShowFavourited,
            )
            TYPE_GRID -> GridGalleryHolder(ComposeView(parent.context))
            else -> throw IllegalStateException("Unexpected value: $viewType")
        }
        return holder
    }

    abstract fun onItemClick(position: Int)
    abstract fun onItemLongClick(position: Int): Boolean
    override fun getItemViewType(position: Int): Int {
        return mType
    }

    abstract fun getDataAt(position: Int): GalleryInfo?

    override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
        val gi = getDataAt(position) ?: return
        holder.bind(
            gi,
            { onItemClick(position) },
            { onItemLongClick(position) },
        )
    }

    @IntDef(TYPE_LIST, TYPE_GRID)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type
    companion object {
        const val TYPE_INVALID = -1
        const val TYPE_LIST = 0
        const val TYPE_GRID = 1
    }
}
