/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.ehviewer.ui.legacy

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.IntDef
import androidx.core.content.edit
import androidx.core.view.forEach
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.databinding.SearchAdvanceBinding
import com.hippo.ehviewer.databinding.SearchCategoryBinding
import com.hippo.ehviewer.databinding.SearchNormalBinding
import com.hippo.ehviewer.yorozuya.NumberUtils
import com.hippo.ehviewer.yorozuya.ViewUtils

@SuppressLint("InflateParams")
class SearchLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : EasyRecyclerView(context, attrs),
    CompoundButton.OnCheckedChangeListener,
    View.OnClickListener,
    OnTabSelectedListener {

    @SearchMode
    private var mSearchMode = SEARCH_MODE_NORMAL
    private var mEnableAdvance = false
    private val binding: SearchNormalBinding
    private val advanceBinding: SearchAdvanceBinding
    private val mImageView: ImageSearchLayout
    private val mActionView: View
    private val mAction: TabLayout
    private val mAdapter: SearchAdapter
    private var mHelper: Helper? = null
    private val mSharePref: SharedPreferences = Settings.prefs
    private val mInflater: LayoutInflater

    init {
        mInflater = LayoutInflater.from(context)
        val resources = context.resources
        layoutManager = SearchLayoutManager(context)
        mAdapter = SearchAdapter()
        mAdapter.setHasStableIds(true)
        adapter = mAdapter
        setHasFixedSize(true)
        clipToPadding = false
        (itemAnimator as DefaultItemAnimator?)?.supportsChangeAnimations = false
        val interval = resources.getDimensionPixelOffset(R.dimen.search_layout_interval)
        val paddingH = resources.getDimensionPixelOffset(R.dimen.search_layout_margin_h)
        val paddingV = resources.getDimensionPixelOffset(R.dimen.search_layout_margin_v)
        val decoration = MarginItemDecoration(
            interval,
            paddingH,
            paddingV,
            paddingH,
            paddingV,
        )
        addItemDecoration(decoration)
        decoration.applyPaddings(this)
        // Create normal view
        binding = SearchNormalBinding.inflate(mInflater)
        val mCategoryStored = mSharePref.getInt(SEARCH_CATEGORY_PREF, EhUtils.ALL_CATEGORY)
        for (mPair in mCategoryTable) {
            val mChip = inflate(context, R.layout.filter_chip, null) as IdentifiedChip
            mChip.isCheckable = true
            mChip.setText(mPair.second)
            mChip.idt = mPair.first
            mChip.isChecked = NumberUtils.int2boolean(mPair.first and mCategoryStored)
            mChip.setOnLongClickListener {
                if (mChip.isChecked) {
                    binding.searchCategoryChipgroup.forEach {
                        (it as IdentifiedChip).isChecked = true
                    }
                    mChip.isChecked = false
                } else {
                    binding.searchCategoryChipgroup.clearCheck()
                    mChip.isChecked = true
                }
                true
            }
            binding.searchCategoryChipgroup.addView(mChip)
        }
        binding.searchCategoryChipgroup.setOnCheckedStateChangeListener { group, checkedIds ->
            var mCategory = 0
            for (index in checkedIds) {
                mCategory = mCategory or group.findViewById<IdentifiedChip>(index).idt
            }
            mSharePref.edit { putInt(SEARCH_CATEGORY_PREF, mCategory) }
        }
        binding.normalSearchModeHelp.setOnClickListener(this)
        binding.searchEnableAdvance.setOnCheckedChangeListener(this)
        // Create advance view
        advanceBinding = SearchAdvanceBinding.inflate(mInflater)
        // Create image view
        mImageView = mInflater.inflate(R.layout.search_image, null) as ImageSearchLayout
        // Create action view
        mActionView = mInflater.inflate(R.layout.search_action, null)
        mActionView.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        mAction = mActionView.findViewById(R.id.action)
        mAction.addOnTabSelectedListener(this)
    }

    fun setHelper(helper: Helper?) {
        mHelper = helper
    }

    fun scrollSearchContainerToTop() {
        (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
    }

    fun setNormalSearchMode(id: Int) {
        binding.normalSearchMode.check(id)
    }

    fun setCategory(category: Int) {
        binding.searchCategoryChipgroup.forEach {
            (it as IdentifiedChip).apply {
                isChecked = idt and category != 0
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView === binding.searchEnableAdvance) {
            post {
                mEnableAdvance = isChecked
                if (mSearchMode == SEARCH_MODE_NORMAL) {
                    if (mEnableAdvance) {
                        mAdapter.notifyItemInserted(1)
                    } else {
                        mAdapter.notifyItemRemoved(1)
                    }
                    if (mHelper != null) {
                        mHelper!!.onChangeSearchMode()
                    }
                }
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Throws(EhException::class)
    fun formatListUrlBuilder(urlBuilder: ListUrlBuilder, query: String?) {
        urlBuilder.reset()
        when (mSearchMode) {
            SEARCH_MODE_NORMAL -> {
                when (binding.normalSearchMode.checkedRadioButtonId) {
                    R.id.search_subscription_search -> {
                        urlBuilder.mode = ListUrlBuilder.MODE_SUBSCRIPTION
                    }

                    R.id.search_specify_uploader -> {
                        urlBuilder.mode = ListUrlBuilder.MODE_UPLOADER
                    }

                    R.id.search_specify_tag -> {
                        urlBuilder.mode = ListUrlBuilder.MODE_TAG
                    }

                    else -> {
                        urlBuilder.mode = ListUrlBuilder.MODE_NORMAL
                    }
                }
                urlBuilder.keyword = query
                urlBuilder.category = Settings.prefs.getInt(
                    SEARCH_CATEGORY_PREF,
                    0,
                )
                if (mEnableAdvance) {
                    urlBuilder.advanceSearch = advanceBinding.searchAdvanceSearchTable.advanceSearch
                    urlBuilder.minRating = advanceBinding.searchAdvanceSearchTable.minRating
                    val pageFrom = advanceBinding.searchAdvanceSearchTable.pageFrom
                    val pageTo = advanceBinding.searchAdvanceSearchTable.pageTo
                    if (pageTo != -1 && pageTo < 10) {
                        throw EhException(context.getString(R.string.search_sp_err1))
                    } else if (pageFrom != -1 && pageTo != -1 && pageTo - pageFrom < 20) {
                        throw EhException(context.getString(R.string.search_sp_err2))
                    }
                    urlBuilder.pageFrom = pageFrom
                    urlBuilder.pageTo = pageTo
                }
            }

            SEARCH_MODE_IMAGE -> {
                urlBuilder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                mImageView.formatListUrlBuilder(urlBuilder)
            }
        }
    }

    override fun onClick(v: View) {
        if (binding.normalSearchModeHelp === v) {
            BaseDialogBuilder(context)
                .setMessage(R.string.search_tip)
                .show()
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        post { setSearchMode(tab.position) }
    }

    private fun setSearchMode(@SearchMode mode: Int) {
        val oldItemCount = mAdapter.itemCount
        mSearchMode = mode
        val newItemCount = mAdapter.itemCount
        mAdapter.notifyItemRangeRemoved(0, oldItemCount - 1)
        mAdapter.notifyItemRangeInserted(0, newItemCount - 1)
        if (mHelper != null) {
            mHelper!!.onChangeSearchMode()
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}
    override fun onTabReselected(tab: TabLayout.Tab) {}

    @IntDef(SEARCH_MODE_NORMAL, SEARCH_MODE_IMAGE)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class SearchMode
    interface Helper {
        fun onChangeSearchMode()
    }

    internal class SearchLayoutManager(context: Context?) : LinearLayoutManager(context!!) {
        override fun onLayoutChildren(recycler: Recycler, state: State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()
            }
        }
    }

    private inner class SearchAdapter : Adapter<SimpleHolder>() {
        override fun getItemCount(): Int {
            var count = SEARCH_ITEM_COUNT_ARRAY[mSearchMode]
            if (mSearchMode == SEARCH_MODE_NORMAL && !mEnableAdvance) {
                count--
            }
            return count
        }

        override fun getItemViewType(position: Int): Int {
            var type = SEARCH_ITEM_TYPE[mSearchMode][position]
            if (mSearchMode == SEARCH_MODE_NORMAL && position == 1 && !mEnableAdvance) {
                type = ITEM_TYPE_ACTION
            }
            return type
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleHolder {
            val view: View?
            if (viewType == ITEM_TYPE_ACTION) {
                ViewUtils.removeFromParent(mActionView)
                view = mActionView
            } else {
                view = mInflater.inflate(R.layout.search_category, parent, false)
                SearchCategoryBinding.bind(view).run {
                    when (viewType) {
                        ITEM_TYPE_NORMAL -> {
                            categoryTitle.setText(R.string.search_normal)
                            ViewUtils.removeFromParent(binding.root)
                            categoryContent.addView(binding.root)
                        }

                        ITEM_TYPE_NORMAL_ADVANCE -> {
                            categoryTitle.setText(R.string.search_advance)
                            ViewUtils.removeFromParent(advanceBinding.root)
                            categoryContent.addView(advanceBinding.root)
                        }

                        ITEM_TYPE_IMAGE -> {
                            categoryTitle.setText(R.string.search_image)
                            ViewUtils.removeFromParent(mImageView)
                            categoryContent.addView(mImageView)
                        }
                    }
                }
            }
            return SimpleHolder(view)
        }

        override fun onBindViewHolder(holder: SimpleHolder, position: Int) {
            if (holder.itemViewType == ITEM_TYPE_ACTION) {
                mAction.selectTab(mAction.getTabAt(mSearchMode))
            }
        }

        override fun getItemId(position: Int): Long {
            var type = SEARCH_ITEM_TYPE[mSearchMode][position]
            if (mSearchMode == SEARCH_MODE_NORMAL && position == 1 && !mEnableAdvance) {
                type = ITEM_TYPE_ACTION
            }
            return type.toLong()
        }
    }

    companion object {
        const val SEARCH_MODE_NORMAL = 0
        const val SEARCH_MODE_IMAGE = 1
        const val SEARCH_CATEGORY_PREF = "search_pref"
        private const val ITEM_TYPE_NORMAL = 0
        private const val ITEM_TYPE_NORMAL_ADVANCE = 1
        private const val ITEM_TYPE_IMAGE = 2
        private const val ITEM_TYPE_ACTION = 3
        private val SEARCH_ITEM_COUNT_ARRAY = intArrayOf(
            3,
            2,
        )
        private val mCategoryTable: ArrayList<Pair<Int, Int>> = arrayListOf(
            Pair(EhUtils.DOUJINSHI, R.string.doujinshi),
            Pair(EhUtils.MANGA, R.string.manga),
            Pair(EhUtils.ARTIST_CG, R.string.artist_cg),
            Pair(EhUtils.GAME_CG, R.string.game_cg),
            Pair(EhUtils.WESTERN, R.string.western),
            Pair(EhUtils.NON_H, R.string.non_h),
            Pair(EhUtils.IMAGE_SET, R.string.image_set),
            Pair(EhUtils.COSPLAY, R.string.cosplay),
            Pair(EhUtils.ASIAN_PORN, R.string.asian_porn),
            Pair(EhUtils.MISC, R.string.misc),
        )
        private val SEARCH_ITEM_TYPE = arrayOf(
            intArrayOf(ITEM_TYPE_NORMAL, ITEM_TYPE_NORMAL_ADVANCE, ITEM_TYPE_ACTION),
            intArrayOf(
                ITEM_TYPE_IMAGE,
                ITEM_TYPE_ACTION,
            ),
        )
    }
}

class IdentifiedChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Chip(
    context,
    attrs,
) {
    var idt = 0
}
