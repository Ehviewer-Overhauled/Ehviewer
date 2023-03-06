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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.databinding.ItemGalleryGridBinding
import com.hippo.ehviewer.databinding.ItemGalleryListBinding
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager
import com.hippo.yorozuya.ViewUtils

internal abstract class GalleryAdapter(
    private val mInflater: LayoutInflater, private val mResources: Resources,
    private val mRecyclerView: RecyclerView, type: Int, showFavourited: Boolean
) : RecyclerView.Adapter<GalleryHolder>() {
    private val mLayoutManager: AutoStaggeredGridLayoutManager =
        AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL)
    private val mPaddingTopSB: Int =
        mResources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
    private val mListThumbWidth: Int
    private val mListThumbHeight: Int
    private val mShowFavourited: Boolean
    private var mListDecoration: MarginItemDecoration? = null
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
            when (type) {
                TYPE_LIST -> {
                    val columnWidth = mResources.getDimensionPixelOffset(Settings.detailSizeResId)
                    mLayoutManager.setColumnSize(columnWidth)
                    mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE)
                    if (null != mGirdDecoration) {
                        recyclerView.removeItemDecoration(mGirdDecoration!!)
                    }
                    if (null == mListDecoration) {
                        val interval =
                            mResources.getDimensionPixelOffset(R.dimen.gallery_list_interval)
                        val paddingH =
                            mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h)
                        val paddingV =
                            mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v)
                        mListDecoration =
                            MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV)
                    }
                    recyclerView.addItemDecoration(mListDecoration!!)
                    notifyDataSetChanged()
                }

                TYPE_GRID -> {
                    val columnWidth = Settings.thumbSize
                    mLayoutManager.setColumnSize(columnWidth)
                    mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_SUITABLE_SIZE)
                    if (null != mListDecoration) {
                        recyclerView.removeItemDecoration(mListDecoration!!)
                    }
                    if (null == mGirdDecoration) {
                        val interval =
                            mResources.getDimensionPixelOffset(R.dimen.gallery_grid_interval)
                        val paddingH =
                            mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_h)
                        val paddingV =
                            mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_v)
                        mGirdDecoration =
                            MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV)
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
        @SuppressLint("InflateParams") val calculator =
            mInflater.inflate(R.layout.item_gallery_list_thumb_height, null)
        ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT)
        mListThumbHeight = calculator.measuredHeight
        mListThumbWidth = mListThumbHeight * 2 / 3
        this.type = type
        adjustPaddings()
    }

    private fun adjustPaddings() {
        val recyclerView = mRecyclerView
        recyclerView.setPadding(
            recyclerView.paddingLeft, recyclerView.paddingTop + mPaddingTopSB,
            recyclerView.paddingRight, recyclerView.paddingBottom
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder {
        val holder = when (viewType) {
            TYPE_LIST -> ListGalleryHolder(
                ItemGalleryListBinding.inflate(mInflater, parent, false),
                mShowFavourited,
                mListThumbWidth,
                mListThumbHeight
            )

            TYPE_GRID -> GridGalleryHolder(ItemGalleryGridBinding.inflate(mInflater, parent, false))
            else -> throw IllegalStateException("Unexpected value: $viewType")
        }
        holder.itemView.setOnClickListener {
            onItemClick(holder.itemView, holder.bindingAdapterPosition)
        }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(holder.itemView, holder.bindingAdapterPosition)
        }
        return holder
    }

    abstract fun onItemClick(view: View, position: Int)
    abstract fun onItemLongClick(view: View, position: Int): Boolean
    override fun getItemViewType(position: Int): Int {
        return mType
    }

    abstract fun getDataAt(position: Int): GalleryInfo?

    override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
        val gi = getDataAt(position) ?: return
        holder.bind(gi)
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