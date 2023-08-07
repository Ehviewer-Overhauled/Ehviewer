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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
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
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.client.ehUrl
import com.hippo.ehviewer.databinding.SceneFavoritesBinding
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.legacy.AddDeleteDrawable
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.FabLayout.OnClickFabListener
import com.hippo.ehviewer.ui.legacy.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.SimpleHandler
import com.hippo.ehviewer.util.getValue
import com.hippo.ehviewer.util.lazyMut
import com.hippo.ehviewer.util.setValue
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Note that we do not really follow mvvm structure, just use it as ... storage
class VMStorage : ViewModel() {
    var urlBuilder = FavListUrlBuilder(favCat = Settings.recentFavCat)
    private val cloudDataFlow = Pager(PagingConfig(25)) {
        object : PagingSource<String, BaseGalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, BaseGalleryInfo>): String? = null
            override suspend fun load(params: LoadParams<String>) = withIOContext {
                when (params) {
                    is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                    is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
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
                val r = runSuspendCatching {
                    EhEngine.getFavorites(urlBuilder.build())
                }.onFailure {
                    return@withIOContext LoadResult.Error(it)
                }.getOrThrow()
                Settings.favCat = r.catArray
                Settings.favCount = r.countArray
                Settings.favCloudCount = r.countArray.sum()
                urlBuilder.jumpTo = null
                LoadResult.Page(r.galleryInfoList, r.prev, r.next)
            }
        }
    }.flow.cachedIn(viewModelScope)

    private val localFavDataFlow = Pager(PagingConfig(20, enablePlaceholders = false, jumpThreshold = 40)) {
        val keyword = urlBuilder.keyword
        if (keyword.isNullOrBlank()) {
            EhDB.localFavLazyList
        } else {
            EhDB.searchLocalFav(keyword)
        }
    }.flow.cachedIn(viewModelScope)
    fun dataflow() = if (urlBuilder.favCat == -2) localFavDataFlow else cloudDataFlow
    val localFavCount = EhDB.localFavCount
}

class FavoritesScene : SearchBarScene() {
    private val vm: VMStorage by viewModels()
    private var urlBuilder by lazyMut { vm::urlBuilder }
    private var _binding: SceneFavoritesBinding? = null
    private val binding get() = _binding!!
    private var mAdapter: GalleryAdapter? = null
    private val tracker get() = mAdapter!!.tracker!!
    private val showNormalFabsRunnable = Runnable {
        updateJumpFab() // index: 0, 2
        binding.fabLayout.run {
            setSecondaryFabVisibilityAt(1, true)
            (3..6).forEach { setSecondaryFabVisibilityAt(it, false) }
        }
    }

    private fun onItemClick(position: Int) {
        // Skip if in search mode
        if (!tracker.isInCustomChoice) {
            switchFav(position - 2)
            updateJumpFab()
            closeSideSheet()
        }
    }

    private var collectJob: Job? = null

    private fun switchFav(newCat: Int, keyword: String? = null) {
        _binding ?: return
        urlBuilder.keyword = keyword
        urlBuilder.favCat = newCat
        urlBuilder.jumpTo = null
        urlBuilder.setIndex(null, true)
        collectJob?.cancel()
        collectJob = viewLifecycleOwner.lifecycleScope.launchIO {
            vm.dataflow().collectLatest {
                mAdapter?.submitData(it)
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

    override fun getMenuResId() = R.menu.scene_favorites

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mAdapter?.let { tracker.saveSelection(outState) }
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.type = Settings.listMode
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneFavoritesBinding.inflate(inflater, container!!)
        setOnApplySearch {
            if (!tracker.isInCustomChoice) {
                switchFav(urlBuilder.favCat, it)
            }
        }
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        binding.fastScroller.setHandlerDrawable(HandlerDrawable().apply { setColor(inflater.context.theme.resolveColor(androidx.appcompat.R.attr.colorPrimary)) })
        binding.fabLayout.run {
            ViewCompat.setWindowInsetsAnimationCallback(binding.root, WindowInsetsAnimationHelper(WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP, this))
            updateJumpFab()
            val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
            val addDelete = AddDeleteDrawable(context, colorID)
            primaryFab!!.setImageDrawable(addDelete)
            setExpanded(expanded = false, animation = false)
            setAutoCancel(true)
            setHidePrimaryFab(false)
            setOnClickFabListener(object : OnClickFabListener {
                override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
                    if (tracker.isInCustomChoice) {
                        tracker.clearSelection()
                    } else {
                        binding.fabLayout.toggle()
                    }
                }

                override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
                    if (!tracker.isInCustomChoice) {
                        when (position) {
                            0 -> showGoToDialog()
                            1 -> switchFav(urlBuilder.favCat)
                            2 -> {
                                urlBuilder.setIndex("1-0", false)
                                mAdapter?.refresh()
                            }
                        }
                        view.isExpanded = false
                        return
                    }
                    when (position) {
                        // Check all
                        3 -> tracker.selectAll()
                        // Download
                        4 -> CommonOperations.startDownload(mainActivity!!, takeCheckedInfo(), false)
                        // Delete
                        5 -> {
                            val helper = DeleteDialogHelper()
                            BaseDialogBuilder(context)
                                .setTitle(R.string.delete_favorites_dialog_title)
                                .setMessage(getString(R.string.delete_favorites_dialog_message, checkedSize()))
                                .setPositiveButton(android.R.string.ok, helper)
                                .show()
                        }
                        // Move
                        6 -> {
                            val helper = MoveDialogHelper()
                            // First is local favorite, the other 10 is cloud favorite
                            val localFav = getString(R.string.local_favorites)
                            val array = if (EhCookieStore.hasSignedIn()) {
                                arrayOf(localFav, *Settings.favCat)
                            } else {
                                arrayOf(localFav)
                            }
                            BaseDialogBuilder(context).setTitle(R.string.move_favorites_dialog_title).setItems(array, helper).show()
                        }
                    }
                }
            })
            addOnExpandListener {
                if (it) {
                    addDelete.setDelete(ANIMATE_TIME)
                } else {
                    addDelete.setAdd(ANIMATE_TIME)
                    if (tracker.isInCustomChoice) tracker.clearSelection()
                }
            }
            addAboveSnackView(this)
        }
        binding.fastScroller.setOnDragHandlerListener(object : OnDragHandlerListener {
            override fun onStartDragHandler() {}
            override fun onEndDragHandler() {
                showSearchBar()
            }
        })
        binding.recyclerView.run {
            mAdapter = GalleryAdapter(
                this@run,
                false,
                { info ->
                    navAnimated(
                        R.id.galleryDetailScene,
                        bundleOf(
                            GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GALLERY_INFO,
                            GalleryDetailScene.KEY_GALLERY_INFO to info,
                        ),
                    )
                },
                {},
            ).also { adapter ->
                val drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid)!!
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                binding.tip.setCompoundDrawables(null, drawable, null, null)
                binding.tip.setOnClickListener { mAdapter?.refresh() }
                binding.refreshLayout.setOnRefreshListener { mAdapter?.refresh() }
                val transition = ViewTransition(binding.refreshLayout, binding.progress, binding.tip)
                val empty = getString(R.string.gallery_list_empty_hit)
                adapter.addLoadStateListener {
                    lifecycleScope.launchUI {
                        when (val state = it.refresh) {
                            is LoadState.Loading -> {
                                showSearchBar()
                                transition.showView(1)
                            }
                            is LoadState.Error -> {
                                binding.tip.text = ExceptionUtils.getReadableString(state.error)
                                transition.showView(2)
                            }
                            is LoadState.NotLoading -> {
                                delay(500)
                                if (mAdapter?.itemCount == 0) {
                                    binding.tip.text = empty
                                    transition.showView(2)
                                } else {
                                    transition.showView(0)
                                }
                            }
                        }
                    }
                }
                adapter.tracker = GallerySelectionTracker(
                    "favorite-selection",
                    this,
                    { adapter.snapshot().items },
                    { (this as GalleryHolder).galleryId },
                ).apply {
                    addCustomChoiceListener({
                        showSelectionFab()
                        binding.fabLayout.setAutoCancel(false)
                        // Delay expanding action to make layout work fine
                        SimpleHandler.post { binding.fabLayout.isExpanded = true }
                        binding.refreshLayout.isEnabled = false
                        // Lock drawer
                        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
                    }) {
                        showNormalFab()
                        binding.fabLayout.setAutoCancel(true)
                        binding.fabLayout.isExpanded = false
                        binding.refreshLayout.isEnabled = true
                        // Unlock drawer
                        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
                    }
                    restoreSelection(savedInstanceState)
                }
            }
            switchFav(Settings.recentFavCat)
        }
        allowEmptySearch = false
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
        removeAboveSnackView(binding.fabLayout)
        mAdapter = null
        _binding = null
    }

    override fun onCreateDrawerView(inflater: LayoutInflater) = ComposeView(inflater.context).apply {
        setMD3Content {
            val localFavCount by vm.localFavCount.collectAsState(0)
            ElevatedCard {
                TopAppBar(title = { Text(text = stringResource(id = R.string.collections)) })
                val scope = currentRecomposeScope
                LaunchedEffect(Unit) {
                    Settings.favChangesFlow.collect {
                        scope.invalidate()
                    }
                }
                val localFav = stringResource(id = R.string.local_favorites) to localFavCount
                val faves = if (EhCookieStore.hasSignedIn()) {
                    arrayOf(
                        localFav,
                        stringResource(id = R.string.cloud_favorites) to Settings.favCloudCount,
                        *Settings.favCat.zip(Settings.favCount.toTypedArray()).toTypedArray(),
                    )
                } else {
                    arrayOf(localFav)
                }
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

    private fun takeCheckedInfo() = tracker.getAndClearSelection()

    private fun checkedSize() = tracker.selectionSize

    private fun showNormalFab() {
        // Delay showing normal fab to avoid mutation
        SimpleHandler.removeCallbacks(showNormalFabsRunnable)
        SimpleHandler.postDelayed(showNormalFabsRunnable, 300)
    }

    private fun showSelectionFab() {
        SimpleHandler.removeCallbacks(showNormalFabsRunnable)
        binding.fabLayout.run {
            (0..2).forEach { setSecondaryFabVisibilityAt(it, false) }
            (3..6).forEach { setSecondaryFabVisibilityAt(it, true) }
        }
    }

    private inner class DeleteDialogHelper : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val info = takeCheckedInfo()
            lifecycleScope.launchIO {
                if (urlBuilder.favCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                    EhDB.removeLocalFavorites(info)
                } else {
                    val delList = info.map { it.gid to it.token!! }
                    EhEngine.modifyFavoritesRange(delList, -1)
                }
                mAdapter?.refresh()
            }
        }
    }

    private inner class MoveDialogHelper : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            val srcCat = urlBuilder.favCat
            val dstCat = if (which == 0) { FavListUrlBuilder.FAV_CAT_LOCAL } else { which - 1 }
            if (srcCat == dstCat) return
            val info = takeCheckedInfo()
            lifecycleScope.launchIO {
                if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                    // Move from local to cloud
                    EhDB.removeLocalFavorites(info)
                    val galleryList = info.map { it.gid to it.token!! }
                    runCatching {
                        EhEngine.modifyFavoritesRange(galleryList, dstCat)
                    }
                } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                    // Move from cloud to local
                    EhDB.putLocalFavorites(info)
                } else {
                    // Move from cloud to cloud
                    val gidArray = info.map { it.gid }.toLongArray()
                    val url = ehUrl { addPathSegments(EhUrl.FAV_PATH) }.toString()
                    runCatching {
                        EhEngine.modifyFavorites(url, gidArray, dstCat)
                    }
                }
                mAdapter?.refresh()
            }
        }
    }
}

private const val ANIMATE_TIME = 300L
