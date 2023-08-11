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

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.compose.ui.platform.ComposeView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import arrow.core.partially1
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.Settings.detailSize
import com.hippo.ehviewer.Settings.thumbSizeDp
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.ui.legacy.AutoStaggeredGridLayoutManager
import com.hippo.ehviewer.ui.legacy.MarginItemDecoration
import com.hippo.ehviewer.ui.legacy.STRATEGY_MIN_SIZE
import com.hippo.ehviewer.ui.legacy.STRATEGY_SUITABLE_SIZE
import com.hippo.ehviewer.util.dp2px
import splitties.init.appCtx

private val diffCallback = object : DiffUtil.ItemCallback<BaseGalleryInfo>() {
    override fun areItemsTheSame(oldItem: BaseGalleryInfo, newItem: BaseGalleryInfo) = oldItem.gid == newItem.gid
    override fun areContentsTheSame(oldItem: BaseGalleryInfo, newItem: BaseGalleryInfo) = oldItem.gid == newItem.gid
}

class GalleryAdapter(
    private val recyclerView: RecyclerView,
    private val showFavorite: Boolean,
    private val onItemClick: (BaseGalleryInfo) -> Unit,
    private val onItemLongClick: (BaseGalleryInfo) -> Unit,
) : PagingDataAdapter<BaseGalleryInfo, GalleryHolder>(diffCallback) {
    private val resources = recyclerView.context.resources
    private val layoutManager: AutoStaggeredGridLayoutManager = AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
    private var mListDecoration: ItemDecoration? = null
    private var mGirdDecoration: MarginItemDecoration? = null
    private var mType = TYPE_INVALID
    var tracker: GallerySelectionTracker<BaseGalleryInfo>? = null

    var type: Int
        get() = mType
        set(type) {
            if (type == mType) return
            mType = type
            when (type) {
                TYPE_LIST -> {
                    val columnWidth = resources.getDimensionPixelOffset(
                        when (detailSize) {
                            0 -> R.dimen.gallery_list_column_width_long
                            1 -> R.dimen.gallery_list_column_width_short
                            else -> error("Unexpected value: $detailSize")
                        },
                    )
                    layoutManager.setColumnSize(columnWidth)
                    layoutManager.setStrategy(STRATEGY_MIN_SIZE)
                    if (null != mGirdDecoration) {
                        recyclerView.removeItemDecoration(mGirdDecoration!!)
                        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_h)
                        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_v)
                        recyclerView.setPadding(
                            recyclerView.paddingLeft - paddingH,
                            recyclerView.paddingTop - paddingV,
                            recyclerView.paddingRight - paddingH,
                            recyclerView.paddingBottom - paddingV,
                        )
                        mGirdDecoration = null
                    }
                    if (null == mListDecoration) {
                        val interval = resources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
                        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
                        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
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
                        recyclerView.addItemDecoration(mListDecoration!!)
                    }
                }

                TYPE_GRID -> {
                    val columnWidth = dp2px(appCtx, thumbSizeDp.toFloat())
                    layoutManager.setColumnSize(columnWidth)
                    layoutManager.setStrategy(STRATEGY_SUITABLE_SIZE)
                    if (null != mListDecoration) {
                        recyclerView.removeItemDecoration(mListDecoration!!)
                        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
                        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
                        recyclerView.setPadding(
                            recyclerView.paddingLeft - paddingH,
                            recyclerView.paddingTop - paddingV,
                            recyclerView.paddingRight - paddingH,
                            recyclerView.paddingBottom - paddingV,
                        )
                        mListDecoration = null
                    }
                    if (null == mGirdDecoration) {
                        val interval = resources.getDimensionPixelOffset(R.dimen.gallery_grid_interval)
                        val paddingH = resources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_h)
                        val paddingV = resources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_v)
                        mGirdDecoration = MarginItemDecoration(interval, 0, 0, 0, 0)
                        recyclerView.setPadding(
                            recyclerView.paddingLeft + paddingH,
                            recyclerView.paddingTop + paddingV,
                            recyclerView.paddingRight + paddingH,
                            recyclerView.paddingBottom + paddingV,
                        )
                        recyclerView.addItemDecoration(mGirdDecoration!!)
                    }
                }
            }
            refresh()
        }

    init {
        recyclerView.adapter = this
        recyclerView.layoutManager = layoutManager
        type = Settings.listMode
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_LIST -> ListGalleryHolder(ComposeView(parent.context), showFavorite)
        TYPE_GRID -> GridGalleryHolder(ComposeView(parent.context))
        else -> error("Unexpected value: $viewType")
    }

    override fun getItemViewType(position: Int) = mType

    override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
        val gi = getItem(position) ?: return
        val selected = tracker?.isSelected(gi.gid) ?: false
        holder.galleryId = gi.gid
        holder.bind(gi, selected, onItemClick.partially1(gi), onItemLongClick.partially1(gi))
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
