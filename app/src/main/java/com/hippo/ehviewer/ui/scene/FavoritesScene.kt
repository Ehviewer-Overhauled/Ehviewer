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

import android.content.DialogInterface
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
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.recyclerview.widget.DiffUtil
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
import com.hippo.ehviewer.databinding.SceneFavoritesBinding
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.legacy.AddDeleteDrawable
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.EasyRecyclerView
import com.hippo.ehviewer.ui.legacy.EasyRecyclerView.CustomChoiceListener
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.FabLayout.OnClickFabListener
import com.hippo.ehviewer.ui.legacy.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.yorozuya.SimpleHandler
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.logcat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import rikka.core.res.resolveColor
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class FavoritesScene : SearchBarScene(), OnClickFabListener, CustomChoiceListener {
    private var _binding: SceneFavoritesBinding? = null
    private val binding get() = _binding!!
    private val modifyList: MutableList<GalleryInfo> = ArrayList()
    private var mAdapter: PagingDataAdapter<GalleryInfo, GalleryHolder>? = null
    private var mAdapterDelegate: GalleryAdapter? = null
    private var urlBuilder = FavListUrlBuilder(favCat = Settings.recentFavCat)
    private val showNormalFabsRunnable = Runnable {
        updateJumpFab() // index: 0, 2
        binding.fabLayout.run {
            setSecondaryFabVisibilityAt(1, true)
            (3..6).forEach { setSecondaryFabVisibilityAt(it, false) }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            onRestore(savedInstanceState)
        }
    }

    private var initialKey: String? = null

    private val cloudDataFlow = Pager(PagingConfig(25)) {
        object : PagingSource<String, GalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, GalleryInfo>): String? = initialKey
            override suspend fun load(params: LoadParams<String>): LoadResult<String, GalleryInfo> {
                when (params) {
                    is LoadParams.Prepend -> {
                        urlBuilder.setIndex(params.key, isNext = false)
                    }

                    is LoadParams.Append -> {
                        urlBuilder.setIndex(params.key, isNext = true)
                    }

                    is LoadParams.Refresh -> {
                        val key = params.key
                        if (key.isNullOrBlank()) {
                            if (urlBuilder.jumpTo != null) {
                                urlBuilder.mNext ?: urlBuilder.setIndex("2", true)
                            }
                        } else {
                            urlBuilder.setIndex(key, false)
                        }
                    }
                }
                val r = EhEngine.getFavorites(urlBuilder.build())
                Settings.favCat = r.catArray
                Settings.favCount = r.countArray
                Settings.favCloudCount = r.countArray.sum()
                urlBuilder.jumpTo = null
                return LoadResult.Page(r.galleryInfoList, r.prev, r.next).also { logcat { it.toString() } }
            }
        }
    }.flow.cachedIn(lifecycleScope)

    private val localFavDataFlow = Pager(PagingConfig(20, enablePlaceholders = false, jumpThreshold = 40)) {
        val keyword = urlBuilder.keyword
        if (keyword.isNullOrBlank()) {
            EhDB.localFavLazyList
        } else {
            EhDB.searchLocalFav(keyword)
        }
    }.flow.cachedIn(lifecycleScope)

    fun onItemClick(position: Int): Boolean {
        if (isDrawerOpen(GravityCompat.END)) {
            // Skip if in search mode
            if (binding.recyclerView.isInCustomChoice) {
                return true
            }
            switchFav(position - 2)
            updateJumpFab()
            closeDrawer(GravityCompat.END)
        } else {
            if (binding.recyclerView.isInCustomChoice) {
                binding.recyclerView.toggleItemChecked(position)
            } else {
                val gi = mAdapter?.peek(position) ?: return true
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

    private var collectJob: Job? = null

    private fun switchFav(newCat: Int, keyword: String? = null) {
        _binding ?: return
        urlBuilder.keyword = keyword
        urlBuilder.favCat = newCat
        urlBuilder.jumpTo = null
        urlBuilder.setIndex(null, true)
        initialKey = null
        collectJob?.cancel()
        when (newCat) {
            -2 -> {
                collectJob = lifecycleScope.launchIO {
                    localFavDataFlow.collectLatest {
                        @Suppress("UNCHECKED_CAST")
                        mAdapter?.submitData(it as PagingData<GalleryInfo>)
                    }
                }
            }

            else -> {
                collectJob = lifecycleScope.launchIO {
                    cloudDataFlow.collectLatest {
                        mAdapter?.submitData(it)
                    }
                }
            }
        }
        mAdapter?.refresh()
        val favCatName: String = when (val favCat = urlBuilder.favCat) {
            in 0..9 -> Settings.favCat[favCat]
            FavListUrlBuilder.FAV_CAT_LOCAL -> getString(R.string.local_favorites)
            else -> getString(R.string.cloud_favorites)
        }
        if (keyword.isNullOrEmpty()) {
            setSearchBarHint(getString(R.string.favorites_title, favCatName))
        } else {
            setSearchBarHint(getString(R.string.favorites_title_2, favCatName, keyword))
        }
        setEditTextHint(getString(R.string.favorites_search_bar_hint, favCatName))
        Settings.recentFavCat = urlBuilder.favCat
    }

    override fun onResume() {
        super.onResume()
        mAdapterDelegate?.type = Settings.listMode
    }

    private fun onRestore(savedInstanceState: Bundle) {
        savedInstanceState.getParcelableCompat<FavListUrlBuilder>(KEY_URL_BUILDER)?.let {
            urlBuilder = it
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_URL_BUILDER, urlBuilder)
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneFavoritesBinding.inflate(inflater, container, false)
        setOnApplySearch {
            if (!binding.recyclerView.isInCustomChoice) {
                switchFav(urlBuilder.favCat, it)
            }
        }
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        binding.fastScroller.setHandlerDrawable(HandlerDrawable().apply { setColor(inflater.context.theme.resolveColor(androidx.appcompat.R.attr.colorPrimary)) })
        binding.fabLayout.run {
            addOnExpandListener { if (!it && binding.recyclerView.isInCustomChoice) binding.recyclerView.outOfCustomChoiceMode() }
            (parent as ViewGroup).removeView(this)
            container!!.addView(this)
            ViewCompat.setWindowInsetsAnimationCallback(binding.root, WindowInsetsAnimationHelper(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP, this))
            updateJumpFab()
            val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
            val addDelete = AddDeleteDrawable(context, colorID)
            primaryFab!!.setImageDrawable(addDelete)
            setExpanded(expanded = false, animation = false)
            setAutoCancel(true)
            setHidePrimaryFab(false)
            setOnClickFabListener(this@FavoritesScene)
            addOnExpandListener {
                if (it) {
                    addDelete.setDelete(ANIMATE_TIME)
                } else {
                    addDelete.setAdd(ANIMATE_TIME)
                }
            }
            addAboveSnackView(this)
        }
        binding.run {
            val paddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
            fastScroller.run {
                setPadding(
                    paddingLeft,
                    paddingTop + paddingTopSB,
                    paddingRight,
                    paddingBottom,
                )
                setOnDragHandlerListener(object : OnDragHandlerListener {
                    override fun onStartDragHandler() {
                        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
                    }

                    override fun onEndDragHandler() {
                        if (!binding.recyclerView.isInCustomChoice) {
                            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
                        }
                        showSearchBar()
                    }
                })
            }
        }
        binding.recyclerView.run {
            val delegateAdapter = object : GalleryAdapter(resources, this@run, Settings.listMode, false, { onItemClick(it) }, { onItemLongClick(it) }) {
                override fun getItemCount() = TODO()
                override fun getDataAt(position: Int) = mAdapter?.peek(position)
            }
            val diffCallback = object : DiffUtil.ItemCallback<GalleryInfo>() {
                override fun areItemsTheSame(oldItem: GalleryInfo, newItem: GalleryInfo) = oldItem.gid == newItem.gid
                override fun areContentsTheSame(oldItem: GalleryInfo, newItem: GalleryInfo) = oldItem.gid == newItem.gid
            }
            mAdapter = object : PagingDataAdapter<GalleryInfo, GalleryHolder>(diffCallback) {
                override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
                    getItem(position)
                    delegateAdapter.onBindViewHolder(holder, position)
                }
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = delegateAdapter.onCreateViewHolder(parent, delegateAdapter.type)
            }.also { adapter ->
                binding.recyclerView.adapter = adapter
                switchFav(Settings.recentFavCat)
            }
            mAdapterDelegate = delegateAdapter
            setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM)
            setCustomCheckedListener(this@FavoritesScene)
        }
        setAllowEmptySearch(false)
        return binding.root
    }

    // Hide jump fab on local fav cat
    private fun updateJumpFab() {
        binding.fabLayout.setSecondaryFabVisibilityAt(
            0,
            urlBuilder.favCat != FavListUrlBuilder.FAV_CAT_LOCAL,
        )
        binding.fabLayout.setSecondaryFabVisibilityAt(
            2,
            urlBuilder.favCat != FavListUrlBuilder.FAV_CAT_LOCAL,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.stopScroll()
        (binding.fabLayout.parent as ViewGroup).removeView(binding.fabLayout)
        removeAboveSnackView(binding.fabLayout)
        mAdapter = null
        mAdapterDelegate = null
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
                                trailingContent = {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                modifier = Modifier.clickable { onItemClick(index) },
                            )
                        }
                    }
                }
            }
        }
    }

    fun onItemLongClick(position: Int): Boolean {
        // Can not into
        if (!binding.recyclerView.isInCustomChoice) {
            binding.recyclerView.intoCustomChoiceMode()
        }
        binding.recyclerView.toggleItemChecked(position)
        return true
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (binding.recyclerView.isInCustomChoice) {
            binding.recyclerView.outOfCustomChoiceMode()
        } else {
            binding.fabLayout.toggle()
        }
    }

    private fun showGoToDialog() {
        context ?: return
        val local = LocalDateTime.of(2007, 3, 21, 0, 0)
        val fromDate = local.atZone(ZoneId.ofOffset("UTC", ZoneOffset.UTC)).toInstant().toEpochMilli()
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
        datePicker.addOnPositiveButtonClickListener { time: Long ->
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
            val jumpTo = formatter.format(Instant.ofEpochMilli(time))
            urlBuilder.jumpTo = jumpTo
            mAdapter?.refresh()
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        val context = context ?: return
        if (!binding.recyclerView.isInCustomChoice) {
            when (position) {
                0 -> {
                    showGoToDialog()
                }
                1 -> mAdapter?.refresh()
                2 -> {
                    initialKey = "1-0"
                    mAdapter?.refresh()
                }
            }
            view.isExpanded = false
            return
        }
        modifyList.clear()
        val stateArray = binding.recyclerView.checkedItemPositions!!
        for (i in 0 until stateArray.size()) {
            if (stateArray.valueAt(i)) {
                val gi = mAdapter?.peek(stateArray.keyAt(i))
                if (gi != null) {
                    modifyList.add(gi)
                }
            }
        }
        when (position) {
            // Check all
            3 -> binding.recyclerView.checkAll()

            // Download
            4 -> {
                CommonOperations.startDownload(mainActivity!!, modifyList, false)
                modifyList.clear()
                binding.recyclerView.outOfCustomChoiceMode()
            }

            // Delete
            5 -> {
                val helper = DeleteDialogHelper()
                BaseDialogBuilder(context)
                    .setTitle(R.string.delete_favorites_dialog_title)
                    .setMessage(getString(R.string.delete_favorites_dialog_message, modifyList.size))
                    .setPositiveButton(android.R.string.ok, helper)
                    .setOnCancelListener(helper)
                    .show()
            }

            // Move
            6 -> {
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
        binding.refreshLayout.isEnabled = false
        // Lock drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onOutOfCustomChoice(view: EasyRecyclerView) {
        showNormalFabs()
        binding.fabLayout.setAutoCancel(true)
        binding.fabLayout.isExpanded = false
        binding.refreshLayout.isEnabled = true
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

    private inner class DeleteDialogHelper : DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            binding.recyclerView.outOfCustomChoiceMode()
            lifecycleScope.launchIO {
                if (urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                    val gidArray = modifyList.map { it.gid }.toLongArray()
                    EhDB.removeLocalFavorites(gidArray)
                } else {
                    val delArray = modifyList.map { it.gid to it.token!! }.toTypedArray()
                    EhEngine.addFavoritesRange(delArray, -1)
                }
                modifyList.clear()
                mAdapter?.refresh()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            modifyList.clear()
        }
    }

    private inner class MoveDialogHelper : DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val srcCat = urlBuilder.favCat
            val dstCat = if (which == 0) { FavListUrlBuilder.FAV_CAT_LOCAL } else { which - 1 }
            if (srcCat == dstCat) return
            binding.recyclerView.outOfCustomChoiceMode()
            lifecycleScope.launchIO {
                if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                    // Move from local to cloud
                    val gidArray = modifyList.map { it.gid }.toLongArray()
                    EhDB.removeLocalFavorites(gidArray)
                }
                if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                    // Move from cloud to local
                    EhDB.putLocalFavorites(modifyList)
                } else {
                    // Move from cloud/local to cloud
                    val gidArray = modifyList.map { it.gid }.toLongArray()
                    val url = ehUrl { addPathSegments(EhUrl.FAV_PATH) }.toString()
                    EhEngine.modifyFavorites(url, gidArray, dstCat)
                }
                modifyList.clear()
                mAdapter?.refresh()
            }
        }

        override fun onCancel(dialog: DialogInterface) {
            modifyList.clear()
        }
    }

    companion object {
        private const val ANIMATE_TIME = 300L
        private const val KEY_URL_BUILDER = "url_builder"
    }
}
