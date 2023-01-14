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
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.app.BaseDialogBuilder
import com.hippo.drawable.AddDeleteDrawable
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.EasyRecyclerView.CustomChoiceListener
import com.hippo.easyrecyclerview.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.WindowInsetsAnimationHelper
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.parser.FavoritesParser
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.widget.GalleryInfoContentHelper
import com.hippo.widget.ContentLayout
import com.hippo.widget.FabLayout
import com.hippo.widget.FabLayout.OnClickFabListener
import com.hippo.widget.FabLayout.OnExpandListener
import com.hippo.yorozuya.AssertUtils
import com.hippo.yorozuya.ObjectUtils
import com.hippo.yorozuya.SimpleHandler
import com.hippo.yorozuya.ViewUtils
import rikka.core.res.resolveColor
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

// TODO Get favorite, modify favorite, add favorite, what a mess!
@SuppressLint("NotifyDataSetChanged", "RtlHardcoded")
class FavoritesScene : SearchBarScene(), OnDragHandlerListener, OnClickFabListener,
    OnExpandListener, CustomChoiceListener {
    // For modify action
    private val mModifyGiList: MutableList<GalleryInfo> = ArrayList()
    var current // -1 for error
            = 0
    var limit // -1 for error
            = 0

    private var mRecyclerView: EasyRecyclerView? = null

    private var mFabLayout: FabLayout? = null

    private var mAdapter: FavoritesAdapter? = null

    private var mHelper: FavoritesHelper? = null
    private var mActionFabDrawable: AddDeleteDrawable? = null
    private var mDrawerLayout: DrawerLayout? = null

    private var mDrawerAdapter: FavDrawerAdapter? = null

    private var mClient: EhClient? = null

    private var mFavCatArray: Array<String>? = Settings.getFavCat()

    private var mFavCountArray: IntArray? = Settings.getFavCount()

    private var mUrlBuilder: FavListUrlBuilder? = null
    private val showNormalFabsRunnable = Runnable {
        if (mFabLayout != null) {
            updateJumpFab() // index: 0, 2
            mFabLayout!!.setSecondaryFabVisibilityAt(1, true)
            mFabLayout!!.setSecondaryFabVisibilityAt(3, false)
            mFabLayout!!.setSecondaryFabVisibilityAt(4, false)
            mFabLayout!!.setSecondaryFabVisibilityAt(5, false)
            mFabLayout!!.setSecondaryFabVisibilityAt(6, false)
        }
    }
    private var mFavLocalCount = 0
    private var mFavCountSum = 0
    private var mHasFirstRefresh = false

    // Avoid unnecessary search bar update
    private var mOldFavCat: String? = null

    // Avoid unnecessary search bar update
    private var mOldKeyword: String? = null

    // For modify action
    private var mEnableModify = false

    // For modify action
    private var mModifyFavCat = 0

    // For modify action
    private var mModifyAdd = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = context
        AssertUtils.assertNotNull(context)
        mClient = EhClient
        mFavLocalCount = Settings.getFavLocalCount()
        mFavCountSum = Settings.getFavCloudCount()
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun onInit() {
        mUrlBuilder = FavListUrlBuilder()
        mUrlBuilder!!.favCat = Settings.getRecentFavCat()
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mUrlBuilder = savedInstanceState.getParcelable(KEY_URL_BUILDER)
        if (mUrlBuilder == null) {
            mUrlBuilder = FavListUrlBuilder()
        }
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH)
        mFavCountArray = savedInstanceState.getIntArray(KEY_FAV_COUNT_ARRAY)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val hasFirstRefresh: Boolean = if (mHelper != null && 1 == mHelper!!.shownViewIndex) {
            false
        } else {
            mHasFirstRefresh
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh)
        outState.putParcelable(KEY_URL_BUILDER, mUrlBuilder)
        outState.putIntArray(KEY_FAV_COUNT_ARRAY, mFavCountArray)
    }

    override fun onDestroy() {
        super.onDestroy()
        mClient = null
        mFavCatArray = null
        mFavCountArray = null
        mFavCountSum = 0
        mUrlBuilder = null
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.scene_favorites, container, false)
        val mContentLayout = view.findViewById<ContentLayout>(R.id.content_layout)
        val activity = mainActivity!!
        AssertUtils.assertNotNull(activity)
        setOnApplySearch { query: String? ->
            onApplySearch(query)
        }
        mDrawerLayout = ViewUtils.`$$`(activity, R.id.draw_view) as DrawerLayout
        mRecyclerView = mContentLayout.recyclerView
        val fastScroller = mContentLayout.fastScroller
        mFabLayout = ViewUtils.`$$`(view, R.id.fab_layout) as FabLayout
        mFabLayout!!.addOnExpandListener(FabLayoutListener())
        (mFabLayout!!.parent as ViewGroup).removeView(mFabLayout)
        AssertUtils.assertNotNull(container)
        container!!.addView(mFabLayout)
        ViewCompat.setWindowInsetsAnimationCallback(
            view, WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                mFabLayout
            )
        )
        val context = context
        AssertUtils.assertNotNull(context)
        val resources = context!!.resources
        val paddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
        mHelper = FavoritesHelper()
        mHelper!!.setEmptyString(resources.getString(R.string.gallery_list_empty_hit))
        mContentLayout.setHelper(mHelper)
        mContentLayout.fastScroller.setOnDragHandlerListener(this)
        mContentLayout.setFitPaddingTop(paddingTopSB)
        mAdapter = FavoritesAdapter(inflater, resources, mRecyclerView!!, Settings.getListMode())
        mRecyclerView!!.clipToPadding = false
        mRecyclerView!!.clipChildren = false
        mRecyclerView!!.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM)
        mRecyclerView!!.setCustomCheckedListener(this)
        fastScroller.setPadding(
            fastScroller.paddingLeft, fastScroller.paddingTop + paddingTopSB,
            fastScroller.paddingRight, fastScroller.paddingBottom
        )
        setAllowEmptySearch(false)
        updateSearchBar()
        updateJumpFab()
        val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
        mActionFabDrawable = AddDeleteDrawable(context, colorID)
        mFabLayout!!.primaryFab!!.setImageDrawable(mActionFabDrawable)
        mFabLayout!!.setExpanded(expanded = false, animation = false)
        mFabLayout!!.setAutoCancel(true)
        mFabLayout!!.setHidePrimaryFab(false)
        mFabLayout!!.setOnClickFabListener(this)
        mFabLayout!!.addOnExpandListener(this)
        addAboveSnackView(mFabLayout)

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true
            mHelper!!.firstRefresh()
        }
        return view
    }

    // keyword of mUrlBuilder, fav cat of mUrlBuilder, mFavCatArray.
    // They changed, call it
    private fun updateSearchBar() {
        val context = context
        if (null == context || null == mUrlBuilder || null == mFavCatArray) {
            return
        }

        // Update title
        val favCatName: String = when (val favCat = mUrlBuilder!!.favCat) {
            in 0..9 -> {
                mFavCatArray!![favCat]
            }

            FavListUrlBuilder.FAV_CAT_LOCAL -> {
                getString(R.string.local_favorites)
            }

            else -> {
                getString(R.string.cloud_favorites)
            }
        }
        val keyword = mUrlBuilder!!.keyword
        if (TextUtils.isEmpty(keyword)) {
            if (!ObjectUtils.equal(favCatName, mOldFavCat)) {
                setSearchBarHint(getString(R.string.favorites_title, favCatName))
            }
        } else {
            if (!ObjectUtils.equal(favCatName, mOldFavCat) || !ObjectUtils.equal(
                    keyword,
                    mOldKeyword
                )
            ) {
                setSearchBarHint(getString(R.string.favorites_title_2, favCatName, keyword))
            }
        }

        // Update hint
        if (!ObjectUtils.equal(favCatName, mOldFavCat)) {
            setEditTextHint(getString(R.string.favorites_search_bar_hint, favCatName))
        }
        mOldFavCat = favCatName
        mOldKeyword = keyword

        // Save recent fav cat
        Settings.putRecentFavCat(mUrlBuilder!!.favCat)
    }

    // Hide jump fab on local fav cat
    private fun updateJumpFab() {
        if (mFabLayout != null && mUrlBuilder != null) {
            mFabLayout!!.setSecondaryFabVisibilityAt(
                0,
                mUrlBuilder!!.favCat != FavListUrlBuilder.FAV_CAT_LOCAL
            )
            mFabLayout!!.setSecondaryFabVisibilityAt(
                2,
                mUrlBuilder!!.favCat != FavListUrlBuilder.FAV_CAT_LOCAL
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (null != mHelper) {
            mHelper!!.destroy()
            if (1 == mHelper!!.shownViewIndex) {
                mHasFirstRefresh = false
            }
        }
        if (null != mRecyclerView) {
            mRecyclerView!!.stopScroll()
            mRecyclerView = null
        }
        if (null != mFabLayout) {
            (mFabLayout!!.parent as ViewGroup).removeView(mFabLayout)
            removeAboveSnackView(mFabLayout)
            mFabLayout = null
        }
        mAdapter = null
        mOldFavCat = null
        mOldKeyword = null
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.drawer_list_rv, container, false)
        val context = context
        val toolbar = ViewUtils.`$$`(view, R.id.toolbar) as Toolbar
        AssertUtils.assertNotNull(context)
        toolbar.setTitle(R.string.collections)
        toolbar.inflateMenu(R.menu.drawer_favorites)
        toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.action_default_favorites_slot) {
                val items = arrayOfNulls<String>(12)
                items[0] = getString(R.string.let_me_select)
                items[1] = getString(R.string.local_favorites)
                val favCat = Settings.getFavCat()
                System.arraycopy(favCat, 0, items, 2, 10)
                BaseDialogBuilder(context!!)
                    .setTitle(R.string.default_favorites_collection)
                    .setItems(items) { _: DialogInterface?, which: Int ->
                        Settings.putDefaultFavSlot(
                            which - 2
                        )
                    }
                    .show()
                return@setOnMenuItemClickListener true
            }
            false
        }
        val recyclerView = view.findViewById<EasyRecyclerView>(R.id.recycler_view_drawer)
        recyclerView.layoutManager = LinearLayoutManager(context)
        mDrawerAdapter = FavDrawerAdapter(inflater)
        recyclerView.adapter = mDrawerAdapter
        return view
    }

    override fun onDestroyDrawerView() {
        super.onDestroyDrawerView()
        mDrawerAdapter = null
    }

    override fun onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onEndDragHandler() {
        // Restore right drawer
        if (null != mRecyclerView && !mRecyclerView!!.isInCustomChoice) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        }
        showSearchBar()
    }

    fun onItemClick(position: Int): Boolean {
        if (mDrawerLayout != null && mDrawerLayout!!.isDrawerOpen(GravityCompat.END)) {
            // Skip if in search mode
            if (mRecyclerView != null && mRecyclerView!!.isInCustomChoice) {
                return true
            }
            if (mUrlBuilder == null || mHelper == null) {
                return true
            }

            // Local favorite position is 0, All favorite position is 1, so position - 2 is OK
            val newFavCat = position - 2

            // Check is the same
            if (mUrlBuilder!!.favCat == newFavCat) {
                return true
            }
            mUrlBuilder!!.keyword = null
            mUrlBuilder!!.favCat = newFavCat
            updateSearchBar()
            updateJumpFab()
            mHelper!!.refresh()
            closeDrawer(GravityCompat.END)
        } else {
            if (mRecyclerView != null && mRecyclerView!!.isInCustomChoice) {
                mRecyclerView!!.toggleItemChecked(position)
            } else if (mHelper != null) {
                val gi = mHelper!!.getDataAtEx(position) ?: return true
                val args = Bundle()
                args.putString(
                    GalleryDetailScene.KEY_ACTION,
                    GalleryDetailScene.ACTION_GALLERY_INFO
                )
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
                navigate(R.id.galleryDetailScene, args)
            }
        }
        return true
    }

    fun onItemLongClick(position: Int): Boolean {
        // Can not into
        if (mRecyclerView != null) {
            if (!mRecyclerView!!.isInCustomChoice) {
                mRecyclerView!!.intoCustomChoiceMode()
            }
            mRecyclerView!!.toggleItemChecked(position)
        }
        return true
    }

    private fun onApplySearch(query: String?) {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView!!.isInCustomChoice) {
            return
        }
        if (mUrlBuilder == null || mHelper == null) {
            return
        }
        mUrlBuilder!!.keyword = query
        updateSearchBar()
        mHelper!!.refresh()
    }

    override fun onExpand(expanded: Boolean) {
        if (expanded) {
            mActionFabDrawable!!.setDelete(ANIMATE_TIME)
        } else {
            mActionFabDrawable!!.setAdd(ANIMATE_TIME)
        }
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (mRecyclerView != null && mFabLayout != null) {
            if (mRecyclerView!!.isInCustomChoice) {
                mRecyclerView!!.outOfCustomChoiceMode()
            } else {
                mFabLayout!!.toggle()
            }
        }
    }

    private fun showGoToDialog() {
        val context = context
        if (null == context || null == mHelper) {
            return
        }
        val local = LocalDateTime.of(2007, 3, 21, 0, 0)
        val fromDate =
            local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()
        val toDate = MaterialDatePicker.todayInUtcMilliseconds()
        val listValidators = ArrayList<DateValidator>()
        listValidators.add(DateValidatorPointForward.from(fromDate))
        listValidators.add(DateValidatorPointBackward.before(toDate))
        val constraintsBuilder = CalendarConstraints.Builder()
            .setStart(fromDate)
            .setEnd(toDate)
            .setValidator(CompositeDateValidator.allOf(listValidators))
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setCalendarConstraints(constraintsBuilder.build())
            .setTitleText(R.string.go_to)
            .setSelection(toDate)
            .build()
        datePicker.show(requireActivity().supportFragmentManager, "date-picker")
        datePicker.addOnPositiveButtonClickListener { v: Long? ->
            mHelper!!.goTo(
                v!!, true
            )
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        val context = context
        if (null == context || null == mRecyclerView || null == mHelper) {
            return
        }
        if (!mRecyclerView!!.isInCustomChoice) {
            when (position) {
                0 -> {
                    if (mHelper!!.canGoTo()) showGoToDialog()
                }

                1 -> mHelper!!.refresh()
                2 -> mHelper!!.goTo("1-0", false)
            }
            view.isExpanded = false
            return
        }
        mModifyGiList.clear()
        val stateArray = mRecyclerView!!.checkedItemPositions
        var i = 0
        val n = stateArray.size()
        while (i < n) {
            if (stateArray.valueAt(i)) {
                val gi = mHelper!!.getDataAtEx(stateArray.keyAt(i))
                if (gi != null) {
                    mModifyGiList.add(gi)
                }
            }
            i++
        }
        when (position) {
            3 ->  // Check all
                mRecyclerView!!.checkAll()

            4 -> { // Download
                val activity: Activity? = mainActivity
                if (activity != null) {
                    CommonOperations.startDownload(mainActivity, mModifyGiList, false)
                }
                mModifyGiList.clear()
                if (mRecyclerView != null && mRecyclerView!!.isInCustomChoice) {
                    mRecyclerView!!.outOfCustomChoiceMode()
                }
            }

            5 -> { // Delete
                val helper = DeleteDialogHelper()
                BaseDialogBuilder(context)
                    .setTitle(R.string.delete_favorites_dialog_title)
                    .setMessage(
                        getString(
                            R.string.delete_favorites_dialog_message,
                            mModifyGiList.size
                        )
                    )
                    .setPositiveButton(android.R.string.ok, helper)
                    .setOnCancelListener(helper)
                    .show()
            }

            6 -> { // Move
                val helper = MoveDialogHelper()
                // First is local favorite, the other 10 is cloud favorite
                val array = arrayOfNulls<String>(11)
                array[0] = getString(R.string.local_favorites)
                System.arraycopy(Settings.getFavCat(), 0, array, 1, 10)
                BaseDialogBuilder(context)
                    .setTitle(R.string.move_favorites_dialog_title)
                    .setItems(array, helper)
                    .setOnCancelListener(helper)
                    .show()
            }
        }
    }

    private fun showNormalFabs() {
        // Delay showing normal fabs to avoid mutation
        SimpleHandler.getInstance().removeCallbacks(showNormalFabsRunnable)
        SimpleHandler.getInstance().postDelayed(showNormalFabsRunnable, 300)
    }

    private fun showSelectionFabs() {
        SimpleHandler.getInstance().removeCallbacks(showNormalFabsRunnable)
        if (mFabLayout != null) {
            mFabLayout!!.setSecondaryFabVisibilityAt(0, false)
            mFabLayout!!.setSecondaryFabVisibilityAt(1, false)
            mFabLayout!!.setSecondaryFabVisibilityAt(2, false)
            mFabLayout!!.setSecondaryFabVisibilityAt(3, true)
            mFabLayout!!.setSecondaryFabVisibilityAt(4, true)
            mFabLayout!!.setSecondaryFabVisibilityAt(5, true)
            mFabLayout!!.setSecondaryFabVisibilityAt(6, true)
        }
    }

    override fun onIntoCustomChoice(view: EasyRecyclerView) {
        if (mFabLayout != null) {
            showSelectionFabs()
            mFabLayout!!.setAutoCancel(false)
            // Delay expanding action to make layout work fine
            SimpleHandler.getInstance().post { mFabLayout!!.isExpanded = true }
        }
        if (mHelper != null) {
            mHelper!!.setRefreshLayoutEnable(false)
        }
        // Lock drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onOutOfCustomChoice(view: EasyRecyclerView) {
        if (mFabLayout != null) {
            showNormalFabs()
            mFabLayout!!.setAutoCancel(true)
            mFabLayout!!.isExpanded = false
        }
        if (mHelper != null) {
            mHelper!!.setRefreshLayoutEnable(true)
        }
        // Unlock drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
    }

    override fun onItemCheckedStateChanged(
        view: EasyRecyclerView,
        position: Int,
        id: Long,
        checked: Boolean
    ) {
        if (view.checkedItemCount == 0) {
            view.outOfCustomChoiceMode()
        }
    }

    private fun onGetFavoritesSuccess(result: FavoritesParser.Result, taskId: Int) {
        if (mHelper != null && mHelper!!.isCurrentTask(taskId)) {
            if (mFavCatArray != null) {
                System.arraycopy(result.catArray, 0, mFavCatArray, 0, 10)
            }
            mFavCountArray = result.countArray
            if (mFavCountArray != null) {
                mFavCountSum = 0
                for (i in 0..9) {
                    mFavCountSum += mFavCountArray!![i]
                }
                Settings.putFavCloudCount(mFavCountSum)
            }
            updateSearchBar()
            mHelper!!.onGetPageData(taskId, 0, 0, result.prev, result.next, result.galleryInfoList)
            if (mDrawerAdapter != null) {
                mDrawerAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun onGetFavoritesFailure(e: Exception, taskId: Int) {
        if (mHelper != null && mHelper!!.isCurrentTask(taskId)) {
            mHelper!!.onGetException(taskId, e)
        }
    }

    private fun onGetFavoritesLocal(keyword: String?, taskId: Int) {
        if (mHelper != null && mHelper!!.isCurrentTask(taskId)) {
            val list: List<GalleryInfo> = if (TextUtils.isEmpty(keyword)) {
                EhDB.getAllLocalFavorites()
            } else {
                EhDB.searchLocalFavorites(keyword)
            }
            if (list.isEmpty()) {
                mHelper!!.onGetPageData(taskId, 0, 0, null, null, list)
            } else {
                mHelper!!.onGetPageData(taskId, 1, 0, null, null, list)
            }
            if (TextUtils.isEmpty(keyword)) {
                mFavLocalCount = list.size
                Settings.putFavLocalCount(mFavLocalCount)
                if (mDrawerAdapter != null) {
                    mDrawerAdapter!!.notifyDataSetChanged()
                }
            }
        }
    }

    private class FavDrawerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val key: TextView
        val value: TextView

        init {
            key = ViewUtils.`$$`(itemView, R.id.key) as TextView
            value = ViewUtils.`$$`(itemView, R.id.value) as TextView
        }
    }

    private inner class AddFavoritesListener(
        context: Context,
        private val mTaskId: Int,
        private val mKeyword: String?,
        private val mBackup: List<GalleryInfo>
    ) : EhCallback<FavoritesScene?, Void?>(context) {
        override fun onSuccess(result: Void?) {
            val scene = this@FavoritesScene
            scene.onGetFavoritesLocal(mKeyword, mTaskId)
        }

        override fun onFailure(e: Exception) {
            // TODO It's a failure, add all of backup back to db.
            // But how to known which one is failed?
            EhDB.putLocalFavorites(mBackup)
            val scene = this@FavoritesScene
            scene.onGetFavoritesLocal(mKeyword, mTaskId)
        }

        override fun onCancel() {}
    }

    private inner class GetFavoritesListener(
        context: Context,
        private val mTaskId: Int, // Local fav is shown now, but operation need be done for cloud fav
        private val mLocal: Boolean,
        private val mKeyword: String?
    ) : EhCallback<FavoritesScene?, FavoritesParser.Result>(context) {
        override fun onSuccess(result: FavoritesParser.Result) {
            // Put fav cat
            Settings.putFavCat(result.catArray)
            Settings.putFavCount(result.countArray)
            val scene = this@FavoritesScene
            if (mLocal) {
                scene.onGetFavoritesLocal(mKeyword, mTaskId)
            } else {
                scene.onGetFavoritesSuccess(result, mTaskId)
            }
        }

        override fun onFailure(e: Exception) {
            val scene = this@FavoritesScene
            if (mLocal) {
                e.printStackTrace()
                scene.onGetFavoritesLocal(mKeyword, mTaskId)
            } else {
                scene.onGetFavoritesFailure(e, mTaskId)
            }
        }

        override fun onCancel() {}
    }

    private inner class FavDrawerAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<FavDrawerHolder>() {
        override fun getItemViewType(position: Int): Int {
            return position
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavDrawerHolder {
            return FavDrawerHolder(mInflater.inflate(R.layout.item_drawer_favorites, parent, false))
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: FavDrawerHolder, position: Int) {
            when (position) {
                0 -> {
                    holder.key.setText(R.string.local_favorites)
                    holder.value.text = mFavLocalCount.toString()
                    holder.itemView.isEnabled = true
                }

                1 -> {
                    holder.key.setText(R.string.cloud_favorites)
                    holder.value.text = mFavCountSum.toString()
                    holder.itemView.isEnabled = true
                }

                else -> {
                    if (null == mFavCatArray || null == mFavCountArray || mFavCatArray!!.size < position - 1 || mFavCountArray!!.size < position - 1) {
                        return
                    }
                    holder.key.text = mFavCatArray!![position - 2]
                    holder.value.text = mFavCountArray!![position - 2].toString()
                    holder.itemView.isEnabled = true
                }
            }
            holder.itemView.setOnClickListener { onItemClick(position) }
        }

        override fun getItemCount(): Int {
            return if (null == mFavCatArray) {
                2
            } else 12
        }
    }

    private inner class DeleteDialogHelper : DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }
            if (mRecyclerView == null || mHelper == null || mUrlBuilder == null) {
                return
            }
            mRecyclerView!!.outOfCustomChoiceMode()
            if (mUrlBuilder!!.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                val gidArray = LongArray(mModifyGiList.size)
                var i = 0
                val n = mModifyGiList.size
                while (i < n) {
                    gidArray[i] = mModifyGiList[i].gid
                    i++
                }
                EhDB.removeLocalFavorites(gidArray)
                mModifyGiList.clear()
                mHelper!!.refresh()
            } else { // Delete cloud fav
                mEnableModify = true
                mModifyFavCat = -1
                mModifyAdd = false
                mHelper!!.refresh()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            mModifyGiList.clear()
        }
    }

    private inner class MoveDialogHelper : DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (mRecyclerView == null || mHelper == null || mUrlBuilder == null) {
                return
            }
            val srcCat = mUrlBuilder!!.favCat
            val dstCat: Int = if (which == 0) {
                FavListUrlBuilder.FAV_CAT_LOCAL
            } else {
                which - 1
            }
            if (srcCat == dstCat) {
                return
            }
            mRecyclerView!!.outOfCustomChoiceMode()
            if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from local to cloud
                val gidArray = LongArray(mModifyGiList.size)
                var i = 0
                val n = mModifyGiList.size
                while (i < n) {
                    gidArray[i] = mModifyGiList[i].gid
                    i++
                }
                EhDB.removeLocalFavorites(gidArray)
                mEnableModify = true
                mModifyFavCat = dstCat
                mModifyAdd = true
                mHelper!!.refresh()
            } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from cloud to local
                EhDB.putLocalFavorites(mModifyGiList)
                mEnableModify = true
                mModifyFavCat = -1
                mModifyAdd = false
                mHelper!!.refresh()
            } else {
                mEnableModify = true
                mModifyFavCat = dstCat
                mModifyAdd = false
                mHelper!!.refresh()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            mModifyGiList.clear()
        }
    }

    private inner class FavoritesAdapter(
        inflater: LayoutInflater, resources: Resources,
        recyclerView: RecyclerView, type: Int
    ) : GalleryAdapter(inflater, resources, recyclerView, type, false) {
        override fun getItemCount(): Int {
            return if (null != mHelper) mHelper!!.size() else 0
        }

        override fun onItemClick(view: View, position: Int) {
            this@FavoritesScene.onItemClick(position)
        }

        override fun onItemLongClick(view: View, position: Int): Boolean {
            return this@FavoritesScene.onItemLongClick(position)
        }

        override fun getDataAt(position: Int): GalleryInfo? {
            return if (null != mHelper) mHelper!!.getDataAtEx(position) else null
        }
    }

    private inner class FabLayoutListener : OnExpandListener {
        override fun onExpand(expanded: Boolean) {
            if (!expanded && mRecyclerView != null && mRecyclerView!!.isInCustomChoice) mRecyclerView!!.outOfCustomChoiceMode()
        }
    }

    private inner class FavoritesHelper : GalleryInfoContentHelper() {
        override fun getPageData(
            taskId: Int,
            type: Int,
            page: Int,
            index: String?,
            isNext: Boolean
        ) {
            val activity = mainActivity
            if (null == activity || null == mUrlBuilder || null == mClient) {
                return
            }
            if (mEnableModify) {
                mEnableModify = false
                val local = mUrlBuilder!!.favCat == FavListUrlBuilder.FAV_CAT_LOCAL
                val gidArray = LongArray(mModifyGiList.size)
                if (mModifyAdd) {
                    val tokenArray = arrayOfNulls<String>(mModifyGiList.size)
                    var i = 0
                    val n = mModifyGiList.size
                    while (i < n) {
                        val gi = mModifyGiList[i]
                        gidArray[i] = gi.gid
                        tokenArray[i] = gi.token
                        i++
                    }
                    val modifyGiListBackup: List<GalleryInfo> = ArrayList(mModifyGiList)
                    mModifyGiList.clear()
                    val request = EhRequest()
                    request.setMethod(EhClient.METHOD_ADD_FAVORITES_RANGE)
                    request.setCallback(
                        AddFavoritesListener(
                            context,
                            taskId,
                            mUrlBuilder!!.keyword,
                            modifyGiListBackup
                        )
                    )
                    request.setArgs(gidArray, tokenArray, mModifyFavCat)
                    request.enqueue(this@FavoritesScene)
                } else {
                    var i = 0
                    val n = mModifyGiList.size
                    while (i < n) {
                        gidArray[i] = mModifyGiList[i].gid
                        i++
                    }
                    mModifyGiList.clear()
                    val url: String = if (local) {
                        // Local fav is shown now, but operation need be done for cloud fav
                        EhUrl.getFavoritesUrl()
                    } else {
                        mUrlBuilder!!.build()
                    }
                    mUrlBuilder!!.setIndex(index, true)
                    val request = EhRequest()
                    request.setMethod(EhClient.METHOD_MODIFY_FAVORITES)
                    request.setCallback(
                        GetFavoritesListener(
                            context,
                            taskId, local, mUrlBuilder!!.keyword
                        )
                    )
                    request.setArgs(url, gidArray, mModifyFavCat)
                    request.enqueue(this@FavoritesScene)
                }
            } else if (mUrlBuilder!!.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                val keyword = mUrlBuilder!!.keyword
                SimpleHandler.getInstance().post { onGetFavoritesLocal(keyword, taskId) }
            } else {
                mUrlBuilder!!.setIndex(index, isNext)
                mUrlBuilder!!.jumpTo = jumpTo
                val url = mUrlBuilder!!.build()
                val request = EhRequest()
                request.setMethod(EhClient.METHOD_GET_FAVORITES)
                request.setCallback(
                    GetFavoritesListener(
                        context,
                        taskId, false, mUrlBuilder!!.keyword
                    )
                )
                request.setArgs(url)
                request.enqueue(this@FavoritesScene)
            }
        }

        override fun getContext(): Context {
            return this@FavoritesScene.requireContext()
        }

        override fun notifyDataSetChanged() {
            // Ensure outOfCustomChoiceMode to avoid error
            if (mRecyclerView != null) {
                mRecyclerView!!.outOfCustomChoiceMode()
            }
            if (mAdapter != null) {
                mAdapter!!.notifyDataSetChanged()
            }
        }

        override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (mAdapter != null) {
                mAdapter!!.notifyItemRangeInserted(positionStart, itemCount)
            }
        }

        override fun onShowView(hiddenView: View, shownView: View) {
            showSearchBar()
        }

        override fun isDuplicate(d1: GalleryInfo?, d2: GalleryInfo?): Boolean {
            return d1?.gid == d2?.gid && d1 != null && d2 != null
        }

        override fun onScrollToPosition(position: Int) {
            if (0 == position) {
                showSearchBar()
            }
        }
    }

    companion object {
        private const val ANIMATE_TIME = 300L
        private const val KEY_URL_BUILDER = "url_builder"
        private const val KEY_HAS_FIRST_REFRESH = "has_first_refresh"
        private const val KEY_FAV_COUNT_ARRAY = "fav_count_array"
    }
}