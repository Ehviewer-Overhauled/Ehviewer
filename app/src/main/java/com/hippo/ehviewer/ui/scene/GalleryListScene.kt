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

import android.animation.Animator
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_NORMAL
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TAG
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.databinding.DrawerListRvBinding
import com.hippo.ehviewer.databinding.ItemDrawerListBinding
import com.hippo.ehviewer.databinding.SceneGalleryListBinding
import com.hippo.ehviewer.ui.doGalleryInfoAction
import com.hippo.ehviewer.ui.legacy.AddDeleteDrawable
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.BringOutTransition
import com.hippo.ehviewer.ui.legacy.EasyRecyclerView
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.FabLayout
import com.hippo.ehviewer.ui.legacy.FabLayout.OnClickFabListener
import com.hippo.ehviewer.ui.legacy.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.ui.legacy.HandlerDrawable
import com.hippo.ehviewer.ui.legacy.LayoutManagerUtils.firstVisibleItemPosition
import com.hippo.ehviewer.ui.legacy.ViewTransition
import com.hippo.ehviewer.ui.legacy.WindowInsetsAnimationHelper
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.ui.settings.showNewVersion
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.util.AnimationUtils
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.SimpleAnimatorListener
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.util.getValue
import com.hippo.ehviewer.util.lazyMut
import com.hippo.ehviewer.util.setValue
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.launchUI
import eu.kanade.tachiyomi.util.lang.withIOContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import moe.tarsin.coroutines.runSuspendCatching
import rikka.core.res.resolveColor
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class VMStorage1 : ViewModel() {
    var urlBuilder = ListUrlBuilder()
    val dataFlow = Pager(PagingConfig(25)) {
        object : PagingSource<String, GalleryInfo>() {
            override fun getRefreshKey(state: PagingState<String, GalleryInfo>): String? = null
            override suspend fun load(params: LoadParams<String>) = withIOContext {
                if (urlBuilder.mode == MODE_TOPLIST) {
                    // TODO: Since we know total pages, let pager support jump
                    val key = (params.key ?: urlBuilder.mJumpTo ?: "0").toInt()
                    val prev = if (key != 0) key - 1 else null
                    val next = if (key != 199) key + 1 else null
                    runSuspendCatching {
                        urlBuilder.setJumpTo(key.toString())
                        EhEngine.getGalleryList(urlBuilder.build())
                    }.onFailure {
                        return@withIOContext LoadResult.Error(it)
                    }.onSuccess {
                        return@withIOContext LoadResult.Page(it.galleryInfoList, prev?.toString(), next?.toString())
                    }
                }
                when (params) {
                    is LoadParams.Prepend -> urlBuilder.setIndex(params.key, isNext = false)
                    is LoadParams.Append -> urlBuilder.setIndex(params.key, isNext = true)
                    is LoadParams.Refresh -> {
                        val key = params.key
                        if (key.isNullOrBlank()) {
                            if (urlBuilder.mJumpTo != null) {
                                urlBuilder.mNext ?: urlBuilder.setIndex("2", true)
                            }
                        } else {
                            urlBuilder.setIndex(key, false)
                        }
                    }
                }
                val r = runSuspendCatching {
                    if (ListUrlBuilder.MODE_IMAGE_SEARCH == urlBuilder.mode) {
                        EhEngine.imageSearch(
                            File(urlBuilder.imagePath!!),
                            urlBuilder.isUseSimilarityScan,
                            urlBuilder.isOnlySearchCovers,
                        )
                    } else {
                        val url = urlBuilder.build()
                        EhEngine.getGalleryList(url)
                    }
                }.onFailure {
                    return@withIOContext LoadResult.Error(it)
                }.getOrThrow()
                urlBuilder.mJumpTo = null
                LoadResult.Page(r.galleryInfoList, r.prev, r.next)
            }
        }
    }.flow.cachedIn(viewModelScope)
}

class GalleryListScene : SearchBarScene() {
    private val vm: VMStorage1 by viewModels()
    private var mUrlBuilder by lazyMut { vm::urlBuilder }
    private var mIsTopList = false

    private val stateBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (mState) {
                State.NORMAL -> error("SearchStateOnBackPressedCallback should not be enabled on $mState")
                State.SIMPLE_SEARCH, State.SEARCH -> setState(State.NORMAL)
                State.SEARCH_SHOW_LIST -> setState(State.SEARCH)
            }
        }
    }
    private var _binding: SceneGalleryListBinding? = null
    private val binding get() = _binding!!
    private val mSearchFabAnimatorListener = object : SimpleAnimatorListener() {
        override fun onAnimationEnd(animation: Animator) {
            mSearchFab.visibility = View.INVISIBLE
        }
    }
    private val mActionFabAnimatorListener = object : SimpleAnimatorListener() {
        override fun onAnimationEnd(animation: Animator) {
            binding.fabLayout.primaryFab?.visibility = View.INVISIBLE
        }
    }
    private var fabAnimator: ViewPropertyAnimator? = null
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: GalleryAdapter? = null
    lateinit var mQuickSearchList: MutableList<QuickSearch>
    private var mHideActionFabSlop = 0
    private var mShowActionFab = true
    private var mState = State.NORMAL
    override fun getMenuResId(): Int {
        return R.menu.scene_gallery_list_searchbar_menu
    }

    private fun handleArgs(args: Bundle?) {
        val action = args?.getString(KEY_ACTION) ?: ACTION_HOMEPAGE
        mUrlBuilder = when (action) {
            ACTION_HOMEPAGE -> ListUrlBuilder()
            ACTION_SUBSCRIPTION -> ListUrlBuilder(MODE_SUBSCRIPTION)
            ACTION_WHATS_HOT -> ListUrlBuilder(MODE_WHATS_HOT)
            ACTION_TOP_LIST -> ListUrlBuilder(MODE_TOPLIST, mKeyword = "11")
            ACTION_LIST_URL_BUILDER -> args?.getParcelableCompat<ListUrlBuilder>(KEY_LIST_URL_BUILDER)?.copy() ?: ListUrlBuilder()
            else -> throw IllegalStateException("Wrong KEY_ACTION:${args?.getString(KEY_ACTION)} when handle args!")
        }
        args?.getString(KEY_GOTO)?.let { mUrlBuilder.mNext = it }
    }

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
        handleArgs(arguments)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mUrlBuilder = savedInstanceState.getParcelableCompat(KEY_LIST_URL_BUILDER)!!
        mState = savedInstanceState.getParcelableCompat(KEY_STATE)!!
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_LIST_URL_BUILDER, mUrlBuilder)
        outState.putParcelable(KEY_STATE, mState)
    }

    private fun setSearchBarHint() {
        setEditTextHint(getString(if (EhUtils.isExHentai) R.string.gallery_list_search_bar_hint_exhentai else R.string.gallery_list_search_bar_hint_e_hentai))
    }

    private fun setSearchBarSuggestionProvider() {
        setSuggestionProvider(object : SuggestionProvider {
            override fun providerSuggestions(text: String): List<Suggestion>? {
                val result1 = GalleryDetailUrlParser.parse(text, false)
                if (result1 != null) {
                    return listOf<Suggestion>(
                        GalleryDetailUrlSuggestion(
                            result1.gid,
                            result1.token,
                        ),
                    )
                }
                val result2 = GalleryPageUrlParser.parse(text, false)
                if (result2 != null) {
                    return listOf<Suggestion>(
                        GalleryPageUrlSuggestion(
                            result2.gid,
                            result2.pToken,
                            result2.page,
                        ),
                    )
                }
                return null
            }
        })
    }

    // Update search bar title, drawer checked item
    private fun onUpdateUrlBuilder() {
        _binding ?: return
        var keyword = mUrlBuilder.keyword
        val category = mUrlBuilder.category
        val mode = mUrlBuilder.mode
        val isPopular = mode == MODE_WHATS_HOT
        val isTopList = mode == MODE_TOPLIST
        if (isTopList != mIsTopList) {
            mIsTopList = isTopList
            recreateDrawerView()
        }

        // Update fab visibility
        binding.fabLayout.setSecondaryFabVisibilityAt(0, !isPopular)
        binding.fabLayout.setSecondaryFabVisibilityAt(2, !isPopular)

        // Update normal search mode and category
        binding.searchLayout.setSearchMyTags(mode == MODE_SUBSCRIPTION)
        if (category != EhUtils.NONE) {
            binding.searchLayout.setCategory(category)
        }

        // Update search edit text
        if (!mIsTopList) {
            if (mode == MODE_TAG) {
                keyword = wrapTagKeyword(keyword!!)
            }
            setSearchBarText(keyword)
        }

        // Update title
        var title = requireContext().getSuitableTitleForUrlBuilder(mUrlBuilder, true)
        if (null == title) {
            title = resources.getString(R.string.search)
        }
        setSearchBarHint(title)
    }

    private val dialogState = DialogState()

    // TODO: Move this out
    private fun checkForUpdates() {
        lifecycleScope.launchIO {
            runSuspendCatching {
                AppUpdater.checkForUpdate()?.let {
                    dialogState.showNewVersion(requireContext(), it)
                }
            }
        }
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SceneGalleryListBinding.inflate(inflater, container!!)
        container.addView(ComposeView(inflater.context).apply { setMD3Content { dialogState.Handler() } })
        checkForUpdates()
        requireActivity().onBackPressedDispatcher.addCallback(stateBackPressedCallback)
        mHideActionFabSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        mShowActionFab = true
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                binding.fabLayout,
                mSearchFab.parent as View,
            ),
        )
        mViewTransition = BringOutTransition(binding.contentLayout, binding.searchLayout)
        binding.fastScroller.setOnDragHandlerListener(object : OnDragHandlerListener {
            override fun onStartDragHandler() {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
            }
            override fun onEndDragHandler() {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
                showSearchBar()
            }
        })
        mAdapter = GalleryAdapter(
            binding.recyclerView,
            true,
            { info, _ ->
                navAnimated(
                    R.id.galleryDetailScene,
                    bundleOf(
                        GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GALLERY_INFO,
                        GalleryDetailScene.KEY_GALLERY_INFO to info,
                    ),
                )
            },
            { info, _ ->
                lifecycleScope.launchIO {
                    dialogState.doGalleryInfoAction(info, requireContext())
                }
            },
        ).also { adapter ->
            viewLifecycleOwner.lifecycleScope.launch {
                val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_sad_pandroid)!!
                drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                binding.tip.setCompoundDrawables(null, drawable, null, null)
                binding.tip.setOnClickListener { mAdapter?.refresh() }
                val transition = ViewTransition(binding.recyclerView, binding.progress, binding.tip)
                val empty = getString(R.string.gallery_list_empty_hit)
                val noWatch = getString(R.string.gallery_list_empty_hit_subscription)
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
                                    if (mUrlBuilder.mode == MODE_SUBSCRIPTION) {
                                        binding.tip.text = noWatch
                                    } else {
                                        binding.tip.text = empty
                                    }
                                    transition.showView(2)
                                } else {
                                    transition.showView(0)
                                }
                            }
                        }
                    }
                }
                vm.dataFlow.collectLatest {
                    adapter.submitData(it)
                }
            }
        }
        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy >= mHideActionFabSlop) {
                        hideActionFab()
                    } else if (dy <= -mHideActionFabSlop / 2) {
                        showActionFab()
                    }
                }
            },
        )
        binding.fastScroller.attachToRecyclerView(binding.recyclerView)
        binding.fastScroller.setHandlerDrawable(HandlerDrawable().apply { setColor(inflater.context.theme.resolveColor(androidx.appcompat.R.attr.colorPrimary)) })
        setOnApplySearch { query: String? ->
            onApplySearch(query)
        }
        setSearchBarHint()
        setSearchBarSuggestionProvider()
        binding.fabLayout.setAutoCancel(true)
        binding.fabLayout.isExpanded = false
        binding.fabLayout.setHidePrimaryFab(false)
        binding.fabLayout.setOnClickFabListener(object : OnClickFabListener {
            override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
                if (State.NORMAL == mState) {
                    view.toggle()
                }
            }
            override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
                when (position) {
                    0 -> showGoToDialog()
                    1 -> {
                        mUrlBuilder.setIndex(null, true)
                        mAdapter?.refresh()
                    }
                    2 -> {
                        if (mIsTopList) {
                            mAdapter?.refresh()
                        } else {
                            mUrlBuilder.setIndex("1", false)
                            mAdapter?.refresh()
                        }
                    }
                }
                view.isExpanded = false
            }
        })
        val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
        val actionFabDrawable = AddDeleteDrawable(requireContext(), colorID)
        binding.fabLayout.addOnExpandListener {
            if (it) {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
                setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
                actionFabDrawable.setDelete(ANIMATE_TIME)
            } else {
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
                setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
                actionFabDrawable.setAdd(ANIMATE_TIME)
            }
        }
        binding.fabLayout.primaryFab!!.setImageDrawable(actionFabDrawable)
        addAboveSnackView(binding.fabLayout)
        mSearchFab.setOnClickListener { onApplySearch() }

        // Update list url builder
        onUpdateUrlBuilder()

        // Restore state
        val newState = mState
        mState = State.NORMAL
        setState(newState, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stateBackPressedCallback.remove()
        binding.recyclerView.stopScroll()
        removeAboveSnackView(binding.fabLayout)
        _binding = null
        fabAnimator = null
        mAdapter = null
        mViewTransition = null
    }

    private fun showQuickSearchTipDialog() {
        val context = context ?: return
        val builder = BaseDialogBuilder(context)
        builder.setMessage(R.string.add_quick_search_tip)
        builder.setTitle(R.string.readme)
        builder.show()
    }

    private fun showAddQuickSearchDialog(
        adapter: QsDrawerAdapter,
        recyclerView: EasyRecyclerView,
        tip: TextView,
    ) {
        val context = context ?: return

        // Can't add image search as quick search
        if (ListUrlBuilder.MODE_IMAGE_SEARCH == mUrlBuilder.mode) {
            showTip(R.string.image_search_not_quick_search, LENGTH_LONG)
            return
        }
        val gi = mAdapter?.peek(binding.recyclerView.layoutManager!!.firstVisibleItemPosition)
        val next = if (gi != null) "@" + (gi.gid + 1) else null

        // Check duplicate
        for (q in mQuickSearchList) {
            if (mUrlBuilder.equalsQuickSearch(q)) {
                val i = q.name!!.lastIndexOf("@")
                if (i != -1 && q.name!!.substring(i) == next) {
                    showTip(getString(R.string.duplicate_quick_search, q.name), LENGTH_LONG)
                    return
                }
            }
        }
        val builder = EditTextDialogBuilder(
            context,
            context.getSuitableTitleForUrlBuilder(mUrlBuilder, false),
            getString(R.string.quick_search),
        )
        builder.setTitle(R.string.add_quick_search_dialog_title)
        builder.setPositiveButton(android.R.string.ok, null)
        // TODO: It's ugly
        val checked = booleanArrayOf(Settings.qSSaveProgress)
        val hint = arrayOf(getString(R.string.save_progress))
        builder.setMultiChoiceItems(hint, checked) { _, which, isChecked -> checked[which] = isChecked }
        val dialog = builder.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            lifecycleScope.launchIO {
                var text = builder.text.trim { it <= ' ' }

                // Check name empty
                if (text.isEmpty()) {
                    withUIContext {
                        builder.setError(getString(R.string.name_is_empty))
                    }
                    return@launchIO
                }
                if (checked[0] && next != null) {
                    text += next
                    Settings.qSSaveProgress = true
                } else {
                    Settings.qSSaveProgress = false
                }

                // Check name duplicate
                for ((_, name) in mQuickSearchList) {
                    if (text == name) {
                        withUIContext {
                            builder.setError(getString(R.string.duplicate_name))
                        }
                        return@launchIO
                    }
                }
                builder.setError(null)
                dialog.dismiss()
                val quickSearch = mUrlBuilder.toQuickSearch()
                quickSearch.name = text
                EhDB.insertQuickSearch(quickSearch)
                mQuickSearchList.add(quickSearch)
                withUIContext {
                    adapter.notifyItemInserted(mQuickSearchList.size - 1)
                    if (0 == mQuickSearchList.size) {
                        tip.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tip.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onCreateDrawerView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val drawerBinding = DrawerListRvBinding.inflate(inflater, container, false)
        drawerBinding.recyclerViewDrawer.layoutManager = LinearLayoutManager(context)
        val qsDrawerAdapter = QsDrawerAdapter(inflater)
        qsDrawerAdapter.setHasStableIds(true)
        drawerBinding.recyclerViewDrawer.adapter = qsDrawerAdapter
        if (!mIsTopList) {
            val itemTouchHelper =
                ItemTouchHelper(GalleryListQSItemTouchHelperCallback(qsDrawerAdapter))
            itemTouchHelper.attachToRecyclerView(drawerBinding.recyclerViewDrawer)
            drawerBinding.tip.visibility = View.VISIBLE
            drawerBinding.recyclerViewDrawer.visibility = View.GONE
        }
        lifecycleScope.launchIO {
            mQuickSearchList = EhDB.getAllQuickSearch() as MutableList<QuickSearch>
            if (mQuickSearchList.isNotEmpty()) {
                withUIContext {
                    drawerBinding.tip.visibility = View.GONE
                    drawerBinding.recyclerViewDrawer.visibility = View.VISIBLE
                }
            }
        }
        drawerBinding.tip.setText(R.string.quick_search_tip)
        if (mIsTopList) {
            drawerBinding.toolbar.setTitle(R.string.toplist)
        } else {
            drawerBinding.toolbar.setTitle(R.string.quick_search)
        }
        if (!mIsTopList) drawerBinding.toolbar.inflateMenu(R.menu.drawer_gallery_list)
        drawerBinding.toolbar.setOnMenuItemClickListener { item: MenuItem ->
            val id = item.itemId
            if (id == R.id.action_add) {
                showAddQuickSearchDialog(
                    qsDrawerAdapter,
                    drawerBinding.recyclerViewDrawer,
                    drawerBinding.tip,
                )
            } else if (id == R.id.action_help) {
                showQuickSearchTipDialog()
            }
            true
        }
        return drawerBinding.root
    }

    private fun showGoToDialog() {
        val context = context ?: return
        mAdapter ?: return
        if (mIsTopList) {
            val page = 1
            val pages = 200
            val hint = getString(R.string.go_to_hint, page, pages)
            val builder = EditTextDialogBuilder(context, null, hint)
            builder.editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val dialog = builder.setTitle(R.string.go_to).setPositiveButton(android.R.string.ok, null).show()
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val text = builder.text.trim { it <= ' ' }
                val goTo: Int = try {
                    text.toInt() - 1
                } catch (e: NumberFormatException) {
                    builder.setError(getString(R.string.error_invalid_number))
                    return@setOnClickListener
                }
                if (goTo < 0 || goTo >= pages) {
                    builder.setError(getString(R.string.error_out_of_range))
                    return@setOnClickListener
                }
                builder.setError(null)
                mUrlBuilder.setJumpTo(goTo.toString())
                mAdapter?.refresh()
                dialog.dismiss()
            }
        } else {
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
            datePicker.addOnPositiveButtonClickListener { time ->
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
                val jumpTo = formatter.format(Instant.ofEpochMilli(time))
                mUrlBuilder.mJumpTo = jumpTo
                mAdapter?.refresh()
            }
        }
    }

    override fun onSearchViewExpanded() {
        super.onSearchViewExpanded()
        if (mState == State.NORMAL) selectSearchFab(true)
    }

    override fun onSearchViewHidden() {
        super.onSearchViewHidden()
        if (mState == State.NORMAL) selectActionFab(true)
    }

    private fun showActionFab() {
        _binding ?: return
        if (State.NORMAL == mState && !mShowActionFab) {
            mShowActionFab = true
            val fab: View? = binding.fabLayout.primaryFab
            if (fabAnimator != null) {
                fabAnimator!!.cancel()
            }
            fab!!.visibility = View.VISIBLE
            fab.rotation = -45.0f
            fabAnimator = fab.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(0L)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR)
            fabAnimator!!.start()
        }
    }

    private fun hideActionFab() {
        _binding ?: return
        if (State.NORMAL == mState && mShowActionFab) {
            mShowActionFab = false
            val fab: View? = binding.fabLayout.primaryFab
            if (fabAnimator != null) {
                fabAnimator!!.cancel()
            }
            fabAnimator =
                fab!!.animate().scaleX(0.0f).scaleY(0.0f).setListener(mActionFabAnimatorListener)
                    .setDuration(ANIMATE_TIME).setStartDelay(0L)
                    .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR)
            fabAnimator!!.start()
        }
    }

    private fun selectSearchFab(animation: Boolean) {
        _binding ?: return
        mShowActionFab = false
        if (animation) {
            val fab: View? = binding.fabLayout.primaryFab
            val delay: Long
            if (View.INVISIBLE == fab!!.visibility) {
                delay = 0L
            } else {
                delay = ANIMATE_TIME
                binding.fabLayout.setExpanded(expanded = false, animation = true)
                fab.animate().scaleX(0.0f).scaleY(0.0f).setListener(mActionFabAnimatorListener)
                    .setDuration(ANIMATE_TIME).setStartDelay(0L)
                    .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR).start()
            }
            mSearchFab.visibility = View.VISIBLE
            mSearchFab.rotation = -45.0f
            mSearchFab.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(delay)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR).start()
        } else {
            binding.fabLayout.setExpanded(expanded = false, animation = false)
            val fab: View? = binding.fabLayout.primaryFab
            fab!!.visibility = View.INVISIBLE
            fab.scaleX = 0.0f
            fab.scaleY = 0.0f
            mSearchFab.visibility = View.VISIBLE
            mSearchFab.scaleX = 1.0f
            mSearchFab.scaleY = 1.0f
        }
    }

    private fun selectActionFab(animation: Boolean) {
        _binding ?: return
        mShowActionFab = true
        if (animation) {
            val delay: Long
            if (View.INVISIBLE == mSearchFab.visibility) {
                delay = 0L
            } else {
                delay = ANIMATE_TIME
                mSearchFab.animate().scaleX(0.0f).scaleY(0.0f)
                    .setListener(mSearchFabAnimatorListener)
                    .setDuration(ANIMATE_TIME).setStartDelay(0L)
                    .setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR).start()
            }
            val fab: View? = binding.fabLayout.primaryFab
            fab!!.visibility = View.VISIBLE
            fab.rotation = -45.0f
            fab.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(delay)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR).start()
        } else {
            binding.fabLayout.setExpanded(expanded = false, animation = false)
            val fab: View? = binding.fabLayout.primaryFab
            fab!!.visibility = View.VISIBLE
            fab.scaleX = 1.0f
            fab.scaleY = 1.0f
            mSearchFab.visibility = View.INVISIBLE
            mSearchFab.scaleX = 0.0f
            mSearchFab.scaleY = 0.0f
        }
    }

    private fun setState(state: State) {
        setState(state, true)
    }

    private fun setState(state: State, animation: Boolean) {
        _binding ?: return
        if (null == mViewTransition) {
            return
        }
        if (mState != state) {
            val oldState = mState
            mState = state
            showSearchBar()
            onStateChange(state)
            when (oldState) {
                State.NORMAL -> when (state) {
                    State.SIMPLE_SEARCH -> {
                        selectSearchFab(animation)
                    }
                    State.SEARCH -> {
                        mViewTransition!!.showView(1, animation)
                        selectSearchFab(animation)
                    }
                    State.SEARCH_SHOW_LIST -> {
                        mViewTransition!!.showView(1, animation)
                        selectSearchFab(animation)
                    }
                    else -> error("Unreachable!!!")
                }
                State.SIMPLE_SEARCH -> when (state) {
                    State.NORMAL -> selectActionFab(animation)
                    State.SEARCH -> mViewTransition!!.showView(1, animation)
                    State.SEARCH_SHOW_LIST -> mViewTransition!!.showView(1, animation)
                    else -> error("Unreachable!!!")
                }
                State.SEARCH, State.SEARCH_SHOW_LIST -> if (state == State.NORMAL) {
                    mViewTransition!!.showView(0, animation)
                    selectActionFab(animation)
                } else if (state == State.SIMPLE_SEARCH) {
                    mViewTransition!!.showView(0, animation)
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (mState == State.NORMAL) {
            setState(State.SEARCH)
        } else {
            setState(State.NORMAL)
        }
        return true
    }

    private fun onApplySearch(query: String?) {
        _binding ?: return
        lifecycleScope.launchIO {
            if (mState == State.SEARCH || mState == State.SEARCH_SHOW_LIST) {
                try {
                    binding.searchLayout.formatListUrlBuilder(mUrlBuilder, query)
                } catch (e: EhException) {
                    showTip(e.message, LENGTH_LONG)
                    return@launchIO
                }
            } else {
                val oldMode = mUrlBuilder.mode
                // If it's MODE_SUBSCRIPTION, keep it
                val newMode = if (oldMode == MODE_SUBSCRIPTION) MODE_SUBSCRIPTION else MODE_NORMAL
                mUrlBuilder.reset()
                mUrlBuilder.mode = newMode
                mUrlBuilder.keyword = query
            }
            withUIContext {
                onUpdateUrlBuilder()
                mAdapter?.refresh()
                setState(State.NORMAL)
            }
        }
    }

    private fun onStateChange(newState: State) {
        stateBackPressedCallback.isEnabled = newState != State.NORMAL
        if (newState == State.NORMAL || newState == State.SIMPLE_SEARCH) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
        }
    }

    private class QsDrawerHolder(val binding: ItemDrawerListBinding) : RecyclerView.ViewHolder(binding.root)

    private inner class QsDrawerAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<QsDrawerHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QsDrawerHolder {
            val holder = QsDrawerHolder(ItemDrawerListBinding.inflate(mInflater, parent, false))
            if (!mIsTopList) {
                holder.itemView.setOnClickListener {
                    val q = mQuickSearchList[holder.bindingAdapterPosition]
                    val i = q.name!!.lastIndexOf("@")
                    val goto = if (i != -1) q.name!!.substring(i + 1) else null
                    val args = ListUrlBuilder().apply { set(q) }.toStartArgs().apply { putString(KEY_GOTO, goto) }
                    navAnimated(R.id.galleryListScene, args, true)
                    setState(State.NORMAL)
                    closeDrawer(GravityCompat.END)
                }
            } else {
                val keywords = intArrayOf(11, 12, 13, 15)
                holder.itemView.setOnClickListener {
                    mUrlBuilder.keyword = keywords[holder.bindingAdapterPosition].toString()
                    onUpdateUrlBuilder()
                    mAdapter?.refresh()
                    setState(State.NORMAL)
                    closeDrawer(GravityCompat.END)
                }
            }
            return holder
        }

        override fun onBindViewHolder(holder: QsDrawerHolder, position: Int) {
            holder.binding.run {
                if (!mIsTopList) {
                    text.text = mQuickSearchList[position].name
                } else {
                    val toplists = intArrayOf(
                        R.string.toplist_alltime,
                        R.string.toplist_pastyear,
                        R.string.toplist_pastmonth,
                        R.string.toplist_yesterday,
                    )
                    text.text = getString(toplists[position])
                    option.visibility = View.GONE
                }
            }
        }

        override fun getItemId(position: Int): Long {
            if (mIsTopList) {
                return position.toLong()
            }
            return mQuickSearchList[position].id!!
        }

        override fun getItemCount(): Int {
            return if (!mIsTopList) mQuickSearchList.size else 4
        }
    }

    private abstract inner class UrlSuggestion : Suggestion() {
        override fun getText(textView: TextView): CharSequence? {
            return if (textView.id == android.R.id.text1) {
                val bookImage =
                    ContextCompat.getDrawable(textView.context, R.drawable.v_book_open_x24)
                val ssb = SpannableStringBuilder("    ")
                ssb.append(getString(R.string.gallery_list_search_bar_open_gallery))
                val imageSize = (textView.textSize * 1.25).toInt()
                if (bookImage != null) {
                    bookImage.setBounds(0, 0, imageSize, imageSize)
                    ssb.setSpan(ImageSpan(bookImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                ssb
            } else {
                null
            }
        }

        override fun onClick() {
            navAnimated(getDestination(), getArgs())
            if (mState == State.SIMPLE_SEARCH) {
                setState(State.NORMAL)
            } else if (mState == State.SEARCH_SHOW_LIST) {
                setState(State.SEARCH)
            }
        }

        abstract fun getDestination(): Int

        abstract fun getArgs(): Bundle
    }

    private inner class GalleryDetailUrlSuggestion(
        private val mGid: Long,
        private val mToken: String,
    ) : UrlSuggestion() {
        override fun getDestination(): Int {
            return R.id.galleryDetailScene
        }

        override fun getArgs() = bundleOf(
            GalleryDetailScene.KEY_ACTION to GalleryDetailScene.ACTION_GID_TOKEN,
            GalleryDetailScene.KEY_GID to mGid,
            GalleryDetailScene.KEY_TOKEN to mToken,
        )
    }

    private inner class GalleryPageUrlSuggestion(
        private val mGid: Long,
        private val mPToken: String,
        private val mPage: Int,
    ) : UrlSuggestion() {
        override fun getDestination(): Int {
            return R.id.progressScene
        }

        override fun getArgs() = bundleOf(
            ProgressScene.KEY_ACTION to ProgressScene.ACTION_GALLERY_TOKEN,
            ProgressScene.KEY_GID to mGid,
            ProgressScene.KEY_PTOKEN to mPToken,
            ProgressScene.KEY_PAGE to mPage,
        )
    }

    private inner class GalleryListQSItemTouchHelperCallback(private val mAdapter: QsDrawerAdapter) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
        ) = makeMovementFlags(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT,
        )

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder,
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            if (fromPosition == toPosition) {
                return false
            }
            lifecycleScope.launchIO {
                EhDB.moveQuickSearch(fromPosition, toPosition)
                val item = mQuickSearchList.removeAt(fromPosition)
                mQuickSearchList.add(toPosition, item)
                withUIContext {
                    mAdapter.notifyItemMoved(fromPosition, toPosition)
                }
            }
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            val quickSearch = mQuickSearchList[position]
            BaseDialogBuilder(context!!)
                .setMessage(getString(R.string.delete_quick_search, quickSearch.name))
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launchIO {
                        EhDB.deleteQuickSearch(quickSearch)
                        mQuickSearchList.removeAt(position)
                        withUIContext {
                            mAdapter.notifyItemRemoved(position)
                        }
                    }
                }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }.setOnCancelListener {
                    mAdapter.notifyItemChanged(position)
                }.show()
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val KEY_GOTO = "goto"
        const val ACTION_HOMEPAGE = "action_homepage"
        const val ACTION_SUBSCRIPTION = "action_subscription"
        const val ACTION_WHATS_HOT = "action_whats_hot"
        const val ACTION_TOP_LIST = "action_top_list"
        const val ACTION_LIST_URL_BUILDER = "action_list_url_builder"
        const val KEY_LIST_URL_BUILDER = "list_url_builder"
        const val KEY_STATE = "state"
        private const val ANIMATE_TIME = 300L
        fun ListUrlBuilder.toStartArgs() = bundleOf(
            KEY_ACTION to ACTION_LIST_URL_BUILDER,
            KEY_LIST_URL_BUILDER to this,
        )
    }
}

@Parcelize
enum class State : Parcelable {
    NORMAL, SIMPLE_SEARCH, SEARCH, SEARCH_SHOW_LIST
}

private fun Context.getSuitableTitleForUrlBuilder(
    urlBuilder: ListUrlBuilder,
    appName: Boolean,
): String? {
    val keyword = urlBuilder.keyword
    val category = urlBuilder.category
    val mode = urlBuilder.mode
    return if (mode == MODE_WHATS_HOT) {
        getString(R.string.whats_hot)
    } else if (!keyword.isNullOrEmpty()) {
        when (mode) {
            MODE_TOPLIST -> {
                when (keyword) {
                    "11" -> getString(R.string.toplist_alltime)
                    "12" -> getString(R.string.toplist_pastyear)
                    "13" -> getString(R.string.toplist_pastmonth)
                    "15" -> getString(R.string.toplist_yesterday)
                    else -> null
                }
            }

            MODE_TAG -> {
                val canTranslate = Settings.showTagTranslations && EhTagDatabase.isTranslatable(this) && EhTagDatabase.initialized
                wrapTagKeyword(keyword, canTranslate)
            }
            else -> keyword
        }
    } else if (category == EhUtils.NONE && urlBuilder.advanceSearch == -1) {
        when (mode) {
            MODE_NORMAL -> getString(if (appName) R.string.app_name else R.string.homepage)
            MODE_SUBSCRIPTION -> getString(R.string.subscription)
            else -> null
        }
    } else if (category.countOneBits() == 1) {
        EhUtils.getCategory(category)
    } else {
        null
    }
}
