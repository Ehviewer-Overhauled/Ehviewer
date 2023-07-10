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
import android.content.DialogInterface
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.ehUrl
import com.hippo.ehviewer.client.parser.FavoritesParser
import com.hippo.ehviewer.databinding.SceneFavoritesBinding
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.legacy.AddDeleteDrawable
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.EasyRecyclerView
import com.hippo.ehviewer.ui.legacy.EasyRecyclerView.CustomChoiceListener
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.FabLayout.OnClickFabListener
import com.hippo.ehviewer.ui.legacy.FabLayout.OnExpandListener
import com.hippo.ehviewer.ui.legacy.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.ui.legacy.GalleryInfoContentHelper
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.yorozuya.SimpleHandler
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

// TODO Get favorite, modify favorite, add favorite, what a mess!
@SuppressLint("NotifyDataSetChanged", "RtlHardcoded")
class FavoritesScene :
    SearchBarScene(),
    OnDragHandlerListener,
    OnClickFabListener,
    OnExpandListener,
    CustomChoiceListener {
    private var _binding: SceneFavoritesBinding? = null
    private val binding get() = _binding!!
    private val mModifyGiList: MutableList<GalleryInfo> = ArrayList()
    private var mAdapter: FavoritesAdapter? = null
    private lateinit var mHelper: FavoritesHelper
    private lateinit var mActionFabDrawable: AddDeleteDrawable
    private var mUrlBuilder: FavListUrlBuilder? = null
    private val showNormalFabsRunnable = Runnable {
        updateJumpFab() // index: 0, 2
        binding.fabLayout.run {
            setSecondaryFabVisibilityAt(1, true)
            (3..6).forEach { setSecondaryFabVisibilityAt(it, false) }
        }
    }
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
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.type = Settings.listMode
    }

    private fun onInit() {
        mUrlBuilder = FavListUrlBuilder()
        mUrlBuilder!!.favCat = Settings.recentFavCat
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mUrlBuilder = savedInstanceState.getParcelableCompat(KEY_URL_BUILDER)
        if (mUrlBuilder == null) {
            mUrlBuilder = FavListUrlBuilder()
        }
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val hasFirstRefresh: Boolean = if (1 == mHelper.shownViewIndex) {
            false
        } else {
            mHasFirstRefresh
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh)
        outState.putParcelable(KEY_URL_BUILDER, mUrlBuilder)
    }

    override fun onDestroy() {
        super.onDestroy()
        mUrlBuilder = null
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneFavoritesBinding.inflate(inflater, container, false)
        setOnApplySearch { query: String? ->
            onApplySearch(query)
        }
        binding.fabLayout.run {
            addOnExpandListener(FabLayoutListener())
            (parent as ViewGroup).removeView(this)
            container!!.addView(this)
            ViewCompat.setWindowInsetsAnimationCallback(
                binding.root,
                WindowInsetsAnimationHelper(
                    WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                    this,
                ),
            )
            updateJumpFab()
            val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
            mActionFabDrawable = AddDeleteDrawable(context, colorID)
            primaryFab!!.setImageDrawable(mActionFabDrawable)
            setExpanded(expanded = false, animation = false)
            setAutoCancel(true)
            setHidePrimaryFab(false)
            setOnClickFabListener(this@FavoritesScene)
            addOnExpandListener(this@FavoritesScene)
            addAboveSnackView(this)
        }
        binding.contentLayout.run {
            mHelper = FavoritesHelper()
            mHelper.setEmptyString(getString(R.string.gallery_list_empty_hit))
            setHelper(mHelper)
            val paddingTopSB =
                resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
            setFitPaddingTop(paddingTopSB)
            fastScroller.run {
                setPadding(
                    paddingLeft,
                    paddingTop + paddingTopSB,
                    paddingRight,
                    paddingBottom,
                )
                setOnDragHandlerListener(this@FavoritesScene)
            }
        }
        binding.contentLayout.recyclerView.run {
            mAdapter = FavoritesAdapter(resources, this, Settings.listMode)
            clipToPadding = false
            clipChildren = false
            setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM)
            setCustomCheckedListener(this@FavoritesScene)
        }
        setAllowEmptySearch(false)
        updateSearchBar()

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true
            mHelper.firstRefresh()
        }
        return binding.root
    }

    // keyword of mUrlBuilder, fav cat of mUrlBuilder, mFavCatArray.
    // They changed, call it
    private fun updateSearchBar() {
        _binding ?: return
        mUrlBuilder ?: return

        // Update title
        val favCatName: String = when (val favCat = mUrlBuilder!!.favCat) {
            in 0..9 -> {
                Settings.favCat[favCat]
            }

            FavListUrlBuilder.FAV_CAT_LOCAL -> {
                getString(R.string.local_favorites)
            }

            else -> {
                getString(R.string.cloud_favorites)
            }
        }
        val keyword = mUrlBuilder!!.keyword
        if (keyword.isNullOrEmpty()) {
            if (favCatName != mOldFavCat) {
                setSearchBarHint(getString(R.string.favorites_title, favCatName))
            }
        } else {
            if (favCatName != mOldFavCat || keyword != mOldKeyword) {
                setSearchBarHint(getString(R.string.favorites_title_2, favCatName, keyword))
            }
        }

        // Update hint
        if (favCatName != mOldFavCat) {
            setEditTextHint(getString(R.string.favorites_search_bar_hint, favCatName))
        }
        mOldFavCat = favCatName
        mOldKeyword = keyword

        // Save recent fav cat
        Settings.recentFavCat = mUrlBuilder!!.favCat
    }

    // Hide jump fab on local fav cat
    private fun updateJumpFab() {
        if (mUrlBuilder != null) {
            binding.fabLayout.setSecondaryFabVisibilityAt(
                0,
                mUrlBuilder!!.favCat != FavListUrlBuilder.FAV_CAT_LOCAL,
            )
            binding.fabLayout.setSecondaryFabVisibilityAt(
                2,
                mUrlBuilder!!.favCat != FavListUrlBuilder.FAV_CAT_LOCAL,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mHelper.destroy()
        if (1 == mHelper.shownViewIndex) {
            mHasFirstRefresh = false
        }
        binding.contentLayout.recyclerView.stopScroll()
        (binding.fabLayout.parent as ViewGroup).removeView(binding.fabLayout)
        removeAboveSnackView(binding.fabLayout)
        mAdapter = null
        mOldFavCat = null
        mOldKeyword = null
        _binding = null
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(inflater.context).apply {
            setMD3Content {
                ElevatedCard {
                    val scope = currentRecomposeScope
                    LaunchedEffect(Unit) {
                        Settings.favChangesFlow.collect {
                            scope.invalidate()
                        }
                    }
                    val faves = arrayOf(
                        stringResource(id = R.string.local_favorites) to Settings.favLocalCount,
                        stringResource(id = R.string.cloud_favorites) to Settings.favCloudCount,
                        *Settings.favCat.zip(Settings.favCount.toTypedArray()).toTypedArray(),
                    )
                    TopAppBar(title = { Text(text = stringResource(id = R.string.collections)) })
                    LazyColumn {
                        itemsIndexed(faves) { index, (name, count) ->
                            ListItem(
                                headlineContent = { Text(text = name) },
                                trailingContent = { Text(text = count.toString(), style = MaterialTheme.typography.bodyLarge) },
                                modifier = Modifier.clickable { onItemClick(index) },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onEndDragHandler() {
        // Restore right drawer
        if (!binding.contentLayout.recyclerView.isInCustomChoice) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        }
        showSearchBar()
    }

    fun onItemClick(position: Int): Boolean {
        if (isDrawerOpen(GravityCompat.END)) {
            // Skip if in search mode
            if (binding.contentLayout.recyclerView.isInCustomChoice) {
                return true
            }
            if (mUrlBuilder == null) {
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
            mHelper.refresh()
            closeDrawer(GravityCompat.END)
        } else {
            if (binding.contentLayout.recyclerView.isInCustomChoice) {
                binding.contentLayout.recyclerView.toggleItemChecked(position)
            } else {
                val gi = mHelper.getDataAtEx(position) ?: return true
                val args = Bundle()
                args.putString(
                    GalleryDetailScene.KEY_ACTION,
                    GalleryDetailScene.ACTION_GALLERY_INFO,
                )
                args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
                navAnimated(R.id.galleryDetailScene, args)
            }
        }
        return true
    }

    fun onItemLongClick(position: Int): Boolean {
        // Can not into
        if (!binding.contentLayout.recyclerView.isInCustomChoice) {
            binding.contentLayout.recyclerView.intoCustomChoiceMode()
        }
        binding.contentLayout.recyclerView.toggleItemChecked(position)
        return true
    }

    private fun onApplySearch(query: String?) {
        // Skip if in search mode
        if (binding.contentLayout.recyclerView.isInCustomChoice) {
            return
        }
        if (mUrlBuilder == null) {
            return
        }
        mUrlBuilder!!.keyword = query
        updateSearchBar()
        mHelper.refresh()
    }

    override fun onExpand(expanded: Boolean) {
        if (expanded) {
            mActionFabDrawable.setDelete(ANIMATE_TIME)
        } else {
            mActionFabDrawable.setAdd(ANIMATE_TIME)
        }
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (binding.contentLayout.recyclerView.isInCustomChoice) {
            binding.contentLayout.recyclerView.outOfCustomChoiceMode()
        } else {
            binding.fabLayout.toggle()
        }
    }

    private fun showGoToDialog() {
        context ?: return
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
            mHelper.goTo(
                v!!,
                true,
            )
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        val context = context ?: return
        if (!binding.contentLayout.recyclerView.isInCustomChoice) {
            when (position) {
                0 -> {
                    if (mHelper.canGoTo()) showGoToDialog()
                }

                1 -> mHelper.refresh()
                2 -> mHelper.goTo("1-0", false)
            }
            view.isExpanded = false
            return
        }
        mModifyGiList.clear()
        val stateArray = binding.contentLayout.recyclerView.checkedItemPositions!!
        for (i in 0 until stateArray.size()) {
            if (stateArray.valueAt(i)) {
                val gi = mHelper.getDataAtEx(stateArray.keyAt(i))
                if (gi != null) {
                    mModifyGiList.add(gi)
                }
            }
        }
        when (position) {
            3 -> // Check all
                binding.contentLayout.recyclerView.checkAll()

            4 -> { // Download
                val activity: Activity? = mainActivity
                if (activity != null) {
                    CommonOperations.startDownload(mainActivity!!, mModifyGiList, false)
                }
                mModifyGiList.clear()
                if (binding.contentLayout.recyclerView.isInCustomChoice) {
                    binding.contentLayout.recyclerView.outOfCustomChoiceMode()
                }
            }

            5 -> { // Delete
                val helper = DeleteDialogHelper()
                BaseDialogBuilder(context)
                    .setTitle(R.string.delete_favorites_dialog_title)
                    .setMessage(
                        getString(
                            R.string.delete_favorites_dialog_message,
                            mModifyGiList.size,
                        ),
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
                System.arraycopy(Settings.favCat, 0, array, 1, 10)
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
        SimpleHandler.removeCallbacks(showNormalFabsRunnable)
        SimpleHandler.postDelayed(showNormalFabsRunnable, 300)
    }

    private fun showSelectionFabs() {
        SimpleHandler.removeCallbacks(showNormalFabsRunnable)
        binding.fabLayout.run {
            (0..2).forEach { setSecondaryFabVisibilityAt(it, false) }
            (3..6).forEach { setSecondaryFabVisibilityAt(it, true) }
        }
    }

    override fun onIntoCustomChoice(view: EasyRecyclerView) {
        showSelectionFabs()
        binding.fabLayout.setAutoCancel(false)
        // Delay expanding action to make layout work fine
        SimpleHandler.post { binding.fabLayout.isExpanded = true }
        mHelper.setRefreshLayoutEnable(false)
        // Lock drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onOutOfCustomChoice(view: EasyRecyclerView) {
        showNormalFabs()
        binding.fabLayout.setAutoCancel(true)
        binding.fabLayout.isExpanded = false
        mHelper.setRefreshLayoutEnable(true)
        // Unlock drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
    }

    override fun onItemCheckedStateChanged(
        view: EasyRecyclerView,
        position: Int,
        id: Long,
        checked: Boolean,
    ) {
        if (view.checkedItemCount == 0) {
            view.outOfCustomChoiceMode()
        }
    }

    private fun onGetFavoritesSuccess(result: FavoritesParser.Result, taskId: Int) {
        if (mHelper.isCurrentTask(taskId)) {
            Settings.favCat = result.catArray
            Settings.favCount = result.countArray
            Settings.favCloudCount = result.countArray.sum()
            _binding ?: return
            updateSearchBar()
            mHelper.onGetPageData(taskId, 0, 0, result.prev, result.next, result.galleryInfoList)
        }
    }

    private fun onGetFavoritesFailure(e: Throwable, taskId: Int) {
        if (mHelper.isCurrentTask(taskId)) {
            mHelper.onGetException(taskId, e)
        }
    }

    private fun onGetFavoritesLocal(keyword: String?, taskId: Int) {
        if (mHelper.isCurrentTask(taskId)) {
            val list: List<GalleryInfo> = if (keyword.isNullOrEmpty()) {
                EhDB.allLocalFavorites
            } else {
                EhDB.searchLocalFavorites(keyword)
            }
            if (list.isEmpty()) {
                mHelper.onGetPageData(taskId, 0, 0, null, null, list)
            } else {
                mHelper.onGetPageData(taskId, 1, 0, null, null, list)
            }
            if (keyword.isNullOrEmpty()) {
                Settings.favLocalCount = list.size
            }
        }
    }

    private inner class DeleteDialogHelper :
        DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }
            if (mUrlBuilder == null) {
                return
            }
            binding.contentLayout.recyclerView.outOfCustomChoiceMode()
            if (mUrlBuilder!!.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                val gidArray = mModifyGiList.map { it.gid }.toLongArray()
                EhDB.removeLocalFavorites(gidArray)
                mModifyGiList.clear()
                mHelper.refresh()
            } else { // Delete cloud fav
                mEnableModify = true
                mModifyFavCat = -1
                mModifyAdd = false
                mHelper.refresh()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            mModifyGiList.clear()
        }
    }

    private inner class MoveDialogHelper :
        DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (mUrlBuilder == null) {
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
            binding.contentLayout.recyclerView.outOfCustomChoiceMode()
            if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from local to cloud
                val gidArray = mModifyGiList.map { it.gid }.toLongArray()
                EhDB.removeLocalFavorites(gidArray)
                mEnableModify = true
                mModifyFavCat = dstCat
                mModifyAdd = true
                mHelper.refresh()
            } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from cloud to local
                EhDB.putLocalFavorites(mModifyGiList)
                mEnableModify = true
                mModifyFavCat = -1
                mModifyAdd = false
                mHelper.refresh()
            } else {
                mEnableModify = true
                mModifyFavCat = dstCat
                mModifyAdd = false
                mHelper.refresh()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            mModifyGiList.clear()
        }
    }

    private inner class FavoritesAdapter(
        resources: Resources,
        recyclerView: RecyclerView,
        type: Int,
    ) : GalleryAdapter(
        resources,
        recyclerView,
        type,
        false,
        { onItemClick(it) },
        { onItemLongClick(it) },
    ) {
        override fun getItemCount(): Int {
            return mHelper.size()
        }

        override fun getDataAt(position: Int): GalleryInfo? {
            return mHelper.getDataAtEx(position)
        }
    }

    private inner class FabLayoutListener : OnExpandListener {
        override fun onExpand(expanded: Boolean) {
            if (!expanded && binding.contentLayout.recyclerView.isInCustomChoice) binding.contentLayout.recyclerView.outOfCustomChoiceMode()
        }
    }

    private inner class FavoritesHelper : GalleryInfoContentHelper() {
        override fun getPageData(
            taskId: Int,
            type: Int,
            page: Int,
            index: String?,
            isNext: Boolean,
        ) {
            val activity = mainActivity
            if (null == activity || null == mUrlBuilder) {
                return
            }
            if (mEnableModify) {
                mEnableModify = false
                val local = mUrlBuilder!!.favCat == FavListUrlBuilder.FAV_CAT_LOCAL
                if (mModifyAdd) {
                    val gidTokenArray = mModifyGiList.map { Pair(it.gid, it.token!!) }.toTypedArray()
                    val modifyGiListBackup: List<GalleryInfo> = ArrayList(mModifyGiList)
                    mModifyGiList.clear()
                    lifecycleScope.launchIO {
                        runSuspendCatching {
                            EhEngine.addFavoritesRange(gidTokenArray, mModifyFavCat)
                        }.onSuccess {
                            withUIContext {
                                onGetFavoritesLocal(mUrlBuilder?.keyword, taskId)
                            }
                        }.onFailure {
                            // TODO It's a failure, add all of backup back to db.
                            // But how to known which one is failed?
                            EhDB.putLocalFavorites(modifyGiListBackup)
                            withUIContext {
                                onGetFavoritesLocal(mUrlBuilder?.keyword, taskId)
                            }
                        }
                    }
                } else {
                    val gidArray = mModifyGiList.map { it.gid }.toLongArray()
                    mModifyGiList.clear()
                    val url: String = if (local) {
                        // Local fav is shown now, but operation need be done for cloud fav
                        ehUrl { addPathSegments(EhUrl.FAV_PATH) }.toString()
                    } else {
                        mUrlBuilder!!.build()
                    }
                    mUrlBuilder!!.setIndex(index, true)
                    lifecycleScope.launchIO {
                        runSuspendCatching {
                            EhEngine.modifyFavorites(url, gidArray, mModifyFavCat)
                        }.onSuccess { result ->
                            // Put fav cat
                            Settings.favCat = result.catArray
                            Settings.favCount = result.countArray
                            withUIContext {
                                if (local) {
                                    onGetFavoritesLocal(mUrlBuilder?.keyword, taskId)
                                } else {
                                    onGetFavoritesSuccess(result, taskId)
                                }
                            }
                        }.onFailure {
                            it.printStackTrace()
                            withUIContext {
                                if (local) {
                                    onGetFavoritesLocal(mUrlBuilder?.keyword, taskId)
                                } else {
                                    onGetFavoritesFailure(it, taskId)
                                }
                            }
                        }
                    }
                }
            } else if (mUrlBuilder!!.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                val keyword = mUrlBuilder!!.keyword
                SimpleHandler.post { onGetFavoritesLocal(keyword, taskId) }
            } else {
                mUrlBuilder!!.setIndex(index, isNext)
                mUrlBuilder!!.jumpTo = jumpTo
                val url = mUrlBuilder!!.build()
                lifecycleScope.launchIO {
                    runSuspendCatching {
                        EhEngine.getFavorites(url)
                    }.onSuccess { result ->
                        Settings.favCat = result.catArray
                        Settings.favCount = result.countArray
                        withUIContext {
                            onGetFavoritesSuccess(result, taskId)
                        }
                    }.onFailure {
                        withUIContext {
                            onGetFavoritesFailure(it, taskId)
                        }
                    }
                }
            }
        }

        override val context
            get() = this@FavoritesScene.requireContext()

        override fun notifyDataSetChanged() {
            // Ensure outOfCustomChoiceMode to avoid error
            binding.contentLayout.recyclerView.outOfCustomChoiceMode()
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

        override fun isDuplicate(d1: GalleryInfo, d2: GalleryInfo): Boolean {
            return d1.gid == d2.gid
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
    }
}
