/*
 * Copyright (C) 2022 tarsin norbin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.widget

import com.hippo.easyrecyclerview.EasyRecyclerView
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import android.view.LayoutInflater
import com.google.android.material.chip.ChipGroup
import com.hippo.widget.RadioGridGroup
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.LinearLayoutManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.DefaultItemAnimator
import com.hippo.ehviewer.R
import com.hippo.easyrecyclerview.MarginItemDecoration
import android.view.ViewGroup
import android.util.SparseArray
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.widget.*
import kotlin.Throws
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.annotation.IntDef
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.android.material.chip.Chip
import com.hippo.easyrecyclerview.SimpleHolder
import com.hippo.ehviewer.client.EhConfig
import com.hippo.yorozuya.NumberUtils
import com.hippo.yorozuya.ViewUtils
import java.lang.IndexOutOfBoundsException

class SearchLayout : EasyRecyclerView, CompoundButton.OnCheckedChangeListener, View.OnClickListener,
    ImageSearchLayout.Helper, OnTabSelectedListener {

    private val mInflater : LayoutInflater
    private val mSharePref : SharedPreferences

    @SearchMode
    private var mSearchMode = SEARCH_MODE_NORMAL
    private var mEnableAdvance = false
    private var mNormalView: View? = null
    private var mNormalSearchMode: RadioGridGroup? = null
    private var mNormalSearchModeHelp: ImageView? = null
    private var mEnableAdvanceSwitch: Switch? = null
    private var mAdvanceView: View? = null
    private var mTableAdvanceSearch: AdvanceSearchTable? = null
    private var mImageView: ImageSearchLayout? = null
    private var mActionView: View? = null
    private var mAction: TabLayout? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private var mAdapter: SearchAdapter? = null
    private var mHelper: Helper? = null

    private val mCategoryTable : ArrayList<Pair<Int, Int>> = arrayListOf(
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

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mSharePref = PreferenceManager.getDefaultSharedPreferences(context)
        mInflater = LayoutInflater.from(context)
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        mSharePref = PreferenceManager.getDefaultSharedPreferences(context)
        mInflater = LayoutInflater.from(context)
        init(context)
    }

    @SuppressLint("InflateParams")
    private fun init(context: Context) {
        val resources = context.resources
        mLayoutManager = SearchLayoutManager(context)
        mAdapter = SearchAdapter()
        mAdapter!!.setHasStableIds(true)
        layoutManager = mLayoutManager
        adapter = mAdapter
        setHasFixedSize(true)
        clipToPadding = false
        (itemAnimator as DefaultItemAnimator?)!!.supportsChangeAnimations = false
        val interval = resources.getDimensionPixelOffset(R.dimen.search_layout_interval)
        val paddingH = resources.getDimensionPixelOffset(R.dimen.search_layout_margin_h)
        val paddingV = resources.getDimensionPixelOffset(R.dimen.search_layout_margin_v)
        val decoration = MarginItemDecoration(
            interval, paddingH, paddingV, paddingH, paddingV
        )
        addItemDecoration(decoration)
        decoration.applyPaddings(this)

        // Create normal view
        val normalView = mInflater.inflate(R.layout.search_normal, null)
        mNormalView = normalView
        val mCategoryStored = mSharePref.getInt(SEARCH_CATEGORY_PREF, EhConfig.ALL_CATEGORY)

        val mCategoryGroup = normalView.findViewById(R.id.search_category_chipgroup) as ChipGroup
        for (mPair in mCategoryTable) {
            val mChip = IdentifiedChip(context)
            mChip.isCheckable = true
            mChip.setText(mPair.second)
            mChip.idt = mPair.first
            mChip.isChecked = NumberUtils.int2boolean(mPair.first and mCategoryStored)
            mCategoryGroup.addView(mChip)
        }
        mCategoryGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            var mCategory = 0
            for (index in checkedIds) {
                mCategory = mCategory or findViewById<IdentifiedChip>(index).idt
            }
            mSharePref.edit { putInt(SEARCH_CATEGORY_PREF, mCategory) }
        }

        mNormalSearchMode = normalView.findViewById(R.id.normal_search_mode)
        mNormalSearchModeHelp = normalView.findViewById(R.id.normal_search_mode_help)
        mEnableAdvanceSwitch = normalView.findViewById(R.id.search_enable_advance)
        mNormalSearchModeHelp!!.setOnClickListener(this)
        mEnableAdvanceSwitch!!.setOnCheckedChangeListener(this)
        mEnableAdvanceSwitch!!.switchPadding = resources.getDimensionPixelSize(R.dimen.switch_padding)

        // Create advance view
        mAdvanceView = mInflater.inflate(R.layout.search_advance, null)
        mTableAdvanceSearch = mAdvanceView!!.findViewById(R.id.search_advance_search_table)

        // Create image view
        mImageView = mInflater.inflate(R.layout.search_image, null) as ImageSearchLayout
        mImageView!!.setHelper(this)

        // Create action view
        mActionView = mInflater.inflate(R.layout.search_action, null)
        mActionView!!.layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        mAction = mActionView!!.findViewById(R.id.action)
        mAction!!.addOnTabSelectedListener(this)
    }

    fun setHelper(helper: Helper?) {
        mHelper = helper
    }

    fun scrollSearchContainerToTop() {
        mLayoutManager!!.scrollToPositionWithOffset(0, 0)
    }

    fun setImageUri(imageUri: Uri?) {
        mImageView!!.setImageUri(imageUri)
    }

    fun setNormalSearchMode(id: Int) {
        mNormalSearchMode!!.check(id)
    }

    override fun onSelectImage() {
        if (mHelper != null) {
            mHelper!!.onSelectImage()
        }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchSaveInstanceState(container)
        mNormalView!!.saveHierarchyState(container)
        mAdvanceView!!.saveHierarchyState(container)
        mImageView!!.saveHierarchyState(container)
        mActionView!!.saveHierarchyState(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>) {
        super.dispatchRestoreInstanceState(container)
        mNormalView!!.restoreHierarchyState(container)
        mAdvanceView!!.restoreHierarchyState(container)
        mImageView!!.restoreHierarchyState(container)
        mActionView!!.restoreHierarchyState(container)
    }

    override fun onSaveInstanceState(): Parcelable {
        val state = Bundle()
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState())
        state.putInt(STATE_KEY_SEARCH_MODE, mSearchMode)
        state.putBoolean(STATE_KEY_ENABLE_ADVANCE, mEnableAdvance)
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable(STATE_KEY_SUPER))
            mSearchMode = state.getInt(STATE_KEY_SEARCH_MODE)
            mEnableAdvance = state.getBoolean(STATE_KEY_ENABLE_ADVANCE)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (buttonView === mEnableAdvanceSwitch) {
            post {
                mEnableAdvance = isChecked
                if (mSearchMode == SEARCH_MODE_NORMAL) {
                    if (mEnableAdvance) {
                        mAdapter!!.notifyItemInserted(1)
                    } else {
                        mAdapter!!.notifyItemRemoved(1)
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
                when (mNormalSearchMode!!.checkedRadioButtonId) {
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
                    SEARCH_CATEGORY_PREF, 0)
                if (mEnableAdvance) {
                    urlBuilder.advanceSearch = mTableAdvanceSearch!!.advanceSearch
                    urlBuilder.minRating = mTableAdvanceSearch!!.minRating
                    urlBuilder.pageFrom = mTableAdvanceSearch!!.pageFrom
                    urlBuilder.pageTo = mTableAdvanceSearch!!.pageTo
                }
            }
            SEARCH_MODE_IMAGE -> {
                urlBuilder.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
                mImageView!!.formatListUrlBuilder(urlBuilder)
            }
        }
    }

    override fun onClick(v: View) {
        if (mNormalSearchModeHelp === v) {
            MaterialAlertDialogBuilder(context)
                .setMessage(R.string.search_tip)
                .show()
        }
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        post { setSearchMode(tab.position) }
    }

    fun setSearchMode(@SearchMode mode: Int) {
        val oldItemCount = mAdapter!!.itemCount
        mSearchMode = mode
        val newItemCount = mAdapter!!.itemCount
        mAdapter!!.notifyItemRangeRemoved(0, oldItemCount - 1)
        mAdapter!!.notifyItemRangeInserted(0, newItemCount - 1)
        if (mHelper != null) {
            mHelper!!.onChangeSearchMode()
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {}
    override fun onTabReselected(tab: TabLayout.Tab) {}

    @IntDef(SEARCH_MODE_NORMAL, SEARCH_MODE_IMAGE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    private annotation class SearchMode
    interface Helper {
        fun onChangeSearchMode()
        fun onSelectImage()
    }

    internal class SearchLayoutManager(context: Context?) : LinearLayoutManager(context) {
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
                mAction!!.selectTab(mAction!!.getTabAt(mSearchMode))
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
    ) : Chip(context, attrs) {
        var idt  = 0
    }

    companion object {
        const val SEARCH_MODE_NORMAL = 0
        const val SEARCH_MODE_IMAGE = 1
        private const val STATE_KEY_SUPER = "super"
        private const val STATE_KEY_SEARCH_MODE = "search_mode"
        private const val STATE_KEY_ENABLE_ADVANCE = "enable_advance"
        const val SEARCH_CATEGORY_PREF = "search_pref"
        private const val ITEM_TYPE_NORMAL = 0
        private const val ITEM_TYPE_NORMAL_ADVANCE = 1
        private const val ITEM_TYPE_IMAGE = 2
        private const val ITEM_TYPE_ACTION = 3
        private val SEARCH_ITEM_COUNT_ARRAY = intArrayOf(
            3, 2
        )
        private val SEARCH_ITEM_TYPE = arrayOf(
            intArrayOf(ITEM_TYPE_NORMAL, ITEM_TYPE_NORMAL_ADVANCE, ITEM_TYPE_ACTION), intArrayOf(
                ITEM_TYPE_IMAGE, ITEM_TYPE_ACTION
            )
        )
    }
}