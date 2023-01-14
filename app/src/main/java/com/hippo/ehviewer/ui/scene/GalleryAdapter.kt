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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.hippo.drawable.TriangleDrawable
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.EhApplication.Companion.downloadManager
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCacheKeyFactory
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.widget.TileThumb
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
    private val mDownloadManager: DownloadManager
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
                    val columnWidth =
                        mResources.getDimensionPixelOffset(Settings.getDetailSizeResId())
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
                    adjustPaddings()
                    notifyDataSetChanged()
                }

                TYPE_GRID -> {
                    val columnWidth = Settings.getThumbSize()
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
                    adjustPaddings()
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
        mDownloadManager = downloadManager
    }

    private fun adjustPaddings() {
        val recyclerView = mRecyclerView
        recyclerView.setPadding(
            recyclerView.paddingLeft, recyclerView.paddingTop + mPaddingTopSB,
            recyclerView.paddingRight, recyclerView.paddingBottom
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder {
        val layoutId = when (viewType) {
            TYPE_LIST -> R.layout.item_gallery_list
            TYPE_GRID -> R.layout.item_gallery_grid
            else -> throw IllegalStateException("Unexpected value: $viewType")
        }
        val holder = GalleryHolder(mInflater.inflate(layoutId, parent, false))
        if (viewType == TYPE_LIST) {
            val lp = holder.thumb.layoutParams
            lp.width = mListThumbWidth
            lp.height = mListThumbHeight
            holder.thumb.layoutParams = lp
        }
        holder.card.setOnClickListener {
            onItemClick(
                holder.itemView,
                holder.bindingAdapterPosition
            )
        }
        holder.card.setOnLongClickListener {
            onItemLongClick(
                holder.itemView,
                holder.bindingAdapterPosition
            )
        }
        return holder
    }

    abstract fun onItemClick(view: View, position: Int)
    abstract fun onItemLongClick(view: View, position: Int): Boolean
    override fun getItemViewType(position: Int): Int {
        return mType
    }

    abstract fun getDataAt(position: Int): GalleryInfo?

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
        val gi = getDataAt(position) ?: return
        when (mType) {
            TYPE_LIST -> {
                holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb!!)
                holder.title!!.text = EhUtils.getSuitableTitle(gi)
                holder.uploader!!.text = gi.uploader
                holder.uploader.alpha = if (gi.disowned) .5f else 1f
                holder.rating!!.rating = gi.rating
                val category = holder.category
                val newCategoryText = EhUtils.getCategory(gi.category)
                if (newCategoryText != category.text.toString()) {
                    category.text = newCategoryText
                    category.setBackgroundColor(EhUtils.getCategoryColor(gi.category))
                }
                holder.posted!!.text = gi.posted
                if (gi.pages == 0 || !Settings.getShowGalleryPages()) {
                    holder.pages?.text = null
                    holder.pages!!.visibility = View.GONE
                } else {
                    holder.pages!!.text = gi.pages.toString() + "P"
                    holder.pages.visibility = View.VISIBLE
                }
                if (TextUtils.isEmpty(gi.simpleLanguage)) {
                    holder.simpleLanguage.text = null
                    holder.simpleLanguage.visibility = View.GONE
                } else {
                    holder.simpleLanguage.text = gi.simpleLanguage
                    holder.simpleLanguage.visibility = View.VISIBLE
                }
                holder.favourited!!.visibility =
                    if (mShowFavourited && gi.favoriteSlot >= -1 && gi.favoriteSlot <= 10) View.VISIBLE else View.GONE
                holder.downloaded!!.visibility =
                    if (mDownloadManager.containDownloadInfo(gi.gid)) View.VISIBLE else View.GONE
            }

            TYPE_GRID -> {
                (holder.thumb as TileThumb).setThumbSize(gi.thumbWidth, gi.thumbHeight)
                holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb!!)
                val category: View = holder.category
                var drawable = category.background
                val color = EhUtils.getCategoryColor(gi.category)
                if (drawable !is TriangleDrawable) {
                    drawable = TriangleDrawable(color)
                    category.background = drawable
                } else {
                    drawable.setColor(color)
                }
                holder.simpleLanguage.text = gi.simpleLanguage
            }
        }
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