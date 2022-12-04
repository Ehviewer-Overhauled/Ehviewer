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

package com.hippo.ehviewer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.IntDef
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.hippo.app.BaseDialogBuilder
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.easyrecyclerview.SimpleHolder
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhConfig
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.widget.RadioGridGroup
import com.hippo.yorozuya.NumberUtils
import com.hippo.yorozuya.ViewUtils

@SuppressLint("InflateParams")
class SearchLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : EasyRecyclerView(context, attrs), CompoundButton.OnCheckedChangeListener, View.OnClickListener,
    ImageSearchLayout.Helper, OnTabSelectedListener {

    @SearchMode
    private var mSearchMode = SEARCH_MODE_NORMAL
    private var mEnableAdvance = false
    private var mCategoryGroup: ChipGroup
    private var mNormalView: View
    private var mNormalSearchMode: RadioGridGroup
    private var mNormalSearchModeHelp: ImageView
    private var mEnableAdvanceSwitch: CheckBox
    private var mAdvanceView: View
    private var mTableAdvanceSearch: AdvanceSearchTable
    private var mImageView: ImageSearchLayout
    private var mActionView: View
    private var mAction: TabLayout
    private var mAdapter: SearchAdapter
    private var mHelper: Helper? = null
    private val mSharePref: SharedPreferences
    private val mInflater: LayoutInflater

    init {
        mSharePref = PreferenceManager.getDefaultSharedPreferences(context)
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
            interval, paddingH, paddingV, paddingH, paddingV
        )
        addItemDecoration(decoration)
        decoration.applyPaddings(this)
        // Create normal view
        mNormalView = mInflater.inflate(R.layout.search_normal, null)
        val mCategoryStored = mSharePref.getInt(SEARCH_CATEGORY_PREF, EhConfig.ALL_CATEGORY)
        mCategoryGroup = mNormalView.findViewById(R.id.search_category_chipgroup)
        for (mPair in mCategoryTable) {
            val mChip = IdentifiedChip(context)
            mChip.isCheckable = true
            mChip.setText(mPair.second)
            mChip.idt = mPair.first
            mChip.isChecked = NumberUtils.int2boolean(mPair.first and mCategoryStored)
            mChip.setOnLongClickListener {
                mCategoryGroup.clearCheck()
                mChip.isChecked = true
                true
            }
            mCategoryGroup.addView(mChip)
        }
        mCategoryGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            var mCategory = 0
            for (index in checkedIds) {
                mCategory = mCategory or findViewById<IdentifiedChip>(index).idt
            }
            mSharePref.edit { putInt(SEARCH_CATEGORY_PREF, mCategory) }
        }
        mNormalSearchMode = mNormalView.findViewById(R.id.normal_search_mode)
        mNormalSearchModeHelp = mNormalView.findViewById(R.id.normal_search_mode_help)
        mEnableAdvanceSwitch = mNormalView.findViewById(R.id.search_enable_advance)
        mNormalSearchModeHelp.setOnClickListener(this)
        mEnableAdvanceSwitch.setOnCheckedChangeListener(this)
        // Create advance view
        mAdvanceView = mInflater.inflate(R.layout.search_advance, null)
        mTableAdvanceSearch = mAdvanceView.findViewById(R.id.search_advance_search_table)
        // Create image view
        mImageView = mInflater.inflate(R.layout.search_image, null) as ImageSearchLayout
        mImageView.setHelper(this)
        // Create action view
        mActionView = mInflater.inflate(R.layout.search_action, null)
        mActionView.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
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

    fun setImageUri(imageUri: Uri?) {
        mImageView.setImageUri(imageUri)
    }

    fun setNormalSearchMode(id: Int) {
        mNormalSearchMode.check(id)
    }

    override fun onSelectImage() {
        if (mHelper != null) {
            mHelper!!.onSelectImage()
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView === mEnableAdvanceSwitch) {
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
                when (mNormalSearchMode.checkedRadioButtonId) {
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
                urlBuilder.category = PreferenceManager.getDefaultSharedPreferences(context).getInt(
                    SEARCH_CATEGORY_PREF, 0
                )
                if (mEnableAdvance) {
                    urlBuilder.advanceSearch = mTableAdvanceSearch.advanceSearch
                    urlBuilder.minRating = mTableAdvanceSearch.minRating
                    urlBuilder.pageFrom = mTableAdvanceSearch.pageFrom
                    urlBuilder.pageTo = mTableAdvanceSearch.pageTo
                }
            }

            SEARCH_MODE_IMAGE -> {
                urlBuilder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                mImageView.formatListUrlBuilder(urlBuilder)
            }
        }
    }

    override fun onClick(v: View) {
        if (mNormalSearchModeHelp === v) {
            BaseDialogBuilder(context)
                .setMessage(R.string.search_tip)
                .show()
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        post { setSearchMode(tab.position) }
    }

    fun setSearchMode(@SearchMode mode: Int) {
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
        fun onSelectImage()
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
                val title = view.findViewById<TextView>(R.id.category_title)
                val content = view.findViewById<FrameLayout>(R.id.category_content)
                when (viewType) {
                    ITEM_TYPE_NORMAL -> {
                        title.setText(R.string.search_normal)
                        ViewUtils.removeFromParent(mNormalView)
                        content.addView(mNormalView)
                    }

                    ITEM_TYPE_NORMAL_ADVANCE -> {
                        title.setText(R.string.search_advance)
                        ViewUtils.removeFromParent(mAdvanceView)
                        content.addView(mAdvanceView)
                    }

                    ITEM_TYPE_IMAGE -> {
                        title.setText(R.string.search_image)
                        ViewUtils.removeFromParent(mImageView)
                        content.addView(mImageView)
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

    inner class IdentifiedChip @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
    ) : Chip(
        ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Widget_Material3_Chip_Filter
        ), attrs
    ) {
        var idt = 0
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
            3, 2
        )
        private val mCategoryTable: ArrayList<Pair<Int, Int>> = arrayListOf(
            Pair(EhConfig.DOUJINSHI, R.string.doujinshi),
            Pair(EhConfig.MANGA, R.string.manga),
            Pair(EhConfig.ARTIST_CG, R.string.artist_cg),
            Pair(EhConfig.GAME_CG, R.string.game_cg),
            Pair(EhConfig.WESTERN, R.string.western),
            Pair(EhConfig.NON_H, R.string.non_h),
            Pair(EhConfig.IMAGE_SET, R.string.image_set),
            Pair(EhConfig.COSPLAY, R.string.cosplay),
            Pair(EhConfig.ASIAN_PORN, R.string.asian_porn),
            Pair(EhConfig.MISC, R.string.misc)
        )
        private val SEARCH_ITEM_TYPE = arrayOf(
            intArrayOf(ITEM_TYPE_NORMAL, ITEM_TYPE_NORMAL_ADVANCE, ITEM_TYPE_ACTION), intArrayOf(
                ITEM_TYPE_IMAGE, ITEM_TYPE_ACTION
            )
        )
    }
}