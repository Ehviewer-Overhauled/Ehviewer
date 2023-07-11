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
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.util.valueIterator
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
import androidx.paging.PagingData
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
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.getValue
import com.hippo.ehviewer.util.lazyMut
import com.hippo.ehviewer.util.setValue
import com.hippo.ehviewer.yorozuya.SimpleHandler
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
    var initialKey: String? = null
    private val cloudDataFlow = Pager(PagingConfig(25)) {
        object : PagingSource<String, GalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, GalleryInfo>): String? = initialKey
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
}

class FavoritesScene : SearchBarScene() {
    private val vm: VMStorage by viewModels()
    private var urlBuilder by lazyMut { vm::urlBuilder }
    private var initialKey by lazyMut { vm::initialKey }
    private var _binding: SceneFavoritesBinding? = null
    private val binding get() = _binding!!
    private var mAdapter: GalleryAdapter? = null
    private val showNormalFabsRunnable = Runnable {
        updateJumpFab() // index: 0, 2
        binding.fabLayout.run {
            setSecondaryFabVisibilityAt(1, true)
            (3..6).forEach { setSecondaryFabVisibilityAt(it, false) }
        }
    }

    private fun onItemClick(position: Int) {
        if (isDrawerOpen(GravityCompat.END)) {
            // Skip if in search mode
            if (!binding.recyclerView.isInCustomChoice) {
                switchFav(position - 2)
                updateJumpFab()
                closeDrawer(GravityCompat.END)
            }
        }
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
        collectJob = viewLifecycleOwner.lifecycleScope.launchIO {
            vm.dataflow().collectLatest {
                @Suppress("UNCHECKED_CAST")
                mAdapter?.submitData(it as PagingData<GalleryInfo>)
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
        mAdapter?.type = Settings.listMode
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
            setOnClickFabListener(object : OnClickFabListener {
                override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
                    if (binding.recyclerView.isInCustomChoice) {
                        binding.recyclerView.outOfCustomChoiceMode()
                    } else {
                        binding.fabLayout.toggle()
                    }
                }

                override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
                    if (!binding.recyclerView.isInCustomChoice) {
                        when (position) {
                            0 -> showGoToDialog()
                            1 -> switchFav(urlBuilder.favCat)
                            2 -> {
                                initialKey = "1-0"
                                mAdapter?.refresh()
                            }
                        }
                        view.isExpanded = false
                        return
                    }
                    when (position) {
                        // Check all
                        3 -> binding.recyclerView.checkAll()
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
                            val array = arrayOfNulls<String>(11)
                            array[0] = getString(R.string.local_favorites)
                            System.arraycopy(Settings.favCat, 0, array, 1, 10)
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
            mAdapter = GalleryAdapter(
                this@run,
                false,
                { info, pos ->
                    if (binding.recyclerView.isInCustomChoice) {
                        binding.recyclerView.toggleItemChecked(pos)
                    } else {
                        navAnimated(
                            R.id.galleryDetailScene,
                            bundleOf(
                                GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GALLERY_INFO,
                                GalleryDetailScene.KEY_GALLERY_INFO to info,
                            ),
                        )
                    }
                },
                { _, pos ->
                    if (!binding.recyclerView.isInCustomChoice) {
                        binding.recyclerView.intoCustomChoiceMode()
                    }
                    binding.recyclerView.toggleItemChecked(pos)
                },
            ).also { adapter ->
                val drawable = ContextCompat.getDrawable(context, R.drawable.big_sad_pandroid)!!
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                binding.tip.setCompoundDrawables(null, drawable, null, null)
                binding.tip.setOnClickListener { mAdapter?.refresh() }
                val transition = ViewTransition(binding.recyclerView, binding.progress, binding.tip)
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
            }
            switchFav(Settings.recentFavCat)
            setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM)
            setCustomCheckedListener(object : CustomChoiceListener {
                override fun onIntoCustomChoice(view: EasyRecyclerView) {
                    showSelectionFab()
                    binding.fabLayout.setAutoCancel(false)
                    // Delay expanding action to make layout work fine
                    SimpleHandler.post { binding.fabLayout.isExpanded = true }
                    binding.refreshLayout.isEnabled = false
                    // Lock drawer
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
                }

                override fun onOutOfCustomChoice(view: EasyRecyclerView) {
                    showNormalFab()
                    binding.fabLayout.setAutoCancel(true)
                    binding.fabLayout.isExpanded = false
                    binding.refreshLayout.isEnabled = true
                    // Unlock drawer
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
                    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
                }

                override fun onItemCheckedStateChanged(view: EasyRecyclerView, position: Int, id: Long, checked: Boolean) {
                    if (view.checkedItemCount == 0) {
                        view.outOfCustomChoiceMode()
                    }
                }
            })
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
        _binding = null
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(inflater.context).apply {
        setMD3Content {
            ElevatedCard {
                TopAppBar(title = { Text(text = stringResource(id = R.string.collections)) })
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

    private fun takeCheckedInfo() = run {
        val stateArray = binding.recyclerView.checkedItemPositions!!
        stateArray.valueIterator().asSequence().withIndex().filter { it.value }
            .mapNotNull { mAdapter?.peek(stateArray.keyAt(it.index)) }.toList()
    }.also { binding.recyclerView.outOfCustomChoiceMode() }

    private fun checkedSize() = binding.recyclerView.checkedItemCount

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
                    val gidArray = info.map { it.gid }.toLongArray()
                    EhDB.removeLocalFavorites(gidArray)
                } else {
                    val delArray = info.map { it.gid to it.token!! }.toTypedArray()
                    EhEngine.addFavoritesRange(delArray, -1)
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
                    val gidArray = info.map { it.gid }.toLongArray()
                    EhDB.removeLocalFavorites(gidArray)
                }
                if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
                    // Move from cloud to local
                    EhDB.putLocalFavorites(info)
                } else {
                    // Move from cloud/local to cloud
                    val gidArray = info.map { it.gid }.toLongArray()
                    val url = ehUrl { addPathSegments(EhUrl.FAV_PATH) }.toString()
                    EhEngine.modifyFavorites(url, gidArray, dstCat)
                }
                mAdapter?.refresh()
            }
        }
    }
}

private const val ANIMATE_TIME = 300L
