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
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
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
import com.hippo.app.BaseDialogBuilder
import com.hippo.app.EditTextDialogBuilder
import com.hippo.drawable.AddDeleteDrawable
import com.hippo.easyrecyclerview.EasyRecyclerView
import com.hippo.easyrecyclerview.FastScroller.OnDragHandlerListener
import com.hippo.ehviewer.EhApplication.Companion.downloadManager
import com.hippo.ehviewer.EhApplication.Companion.favouriteStatusRouter
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.FavouriteStatusRouter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.WindowInsetsAnimationHelper
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_SUBSCRIPTION
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_TOPLIST
import com.hippo.ehviewer.client.data.ListUrlBuilder.Companion.MODE_WHATS_HOT
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.parser.GalleryDetailUrlParser
import com.hippo.ehviewer.client.parser.GalleryListParser
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.databinding.DrawerListRvBinding
import com.hippo.ehviewer.databinding.SceneGalleryListBinding
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.dialog.SelectItemWithIconAdapter
import com.hippo.ehviewer.widget.GalleryInfoContentHelper
import com.hippo.ehviewer.widget.SearchLayout
import com.hippo.scene.Announcer
import com.hippo.view.BringOutTransition
import com.hippo.view.ViewTransition
import com.hippo.widget.FabLayout
import com.hippo.widget.FabLayout.OnClickFabListener
import com.hippo.widget.FabLayout.OnExpandListener
import com.hippo.yorozuya.AnimationUtils
import com.hippo.yorozuya.MathUtils
import com.hippo.yorozuya.SimpleAnimatorListener
import com.hippo.yorozuya.StringUtils
import com.hippo.yorozuya.ViewUtils
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import rikka.core.res.resolveColor
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class GalleryListScene : SearchBarScene(), OnDragHandlerListener, SearchLayout.Helper,
    OnClickFabListener, OnExpandListener {
    private val mCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (mState) {
                STATE_NORMAL -> throw IllegalStateException("SearchStateOnBackPressedCallback should not be enabled on STATE_NORMAL")
                STATE_SIMPLE_SEARCH, STATE_SEARCH -> setState(STATE_NORMAL)
                STATE_SEARCH_SHOW_LIST -> setState(STATE_SEARCH)
            }
        }
    }
    private lateinit var mUrlBuilder: ListUrlBuilder
    private var _binding: SceneGalleryListBinding? = null
    private val binding get() = _binding!!
    private val selectImageLauncher = registerForActivityResult<PickVisualMediaRequest, Uri>(
        ActivityResultContracts.PickVisualMedia()
    ) { result: Uri? -> binding.searchLayout.setImageUri(result) }
    private var mSearchFab: View? = null
    private val mSearchFabAnimatorListener: Animator.AnimatorListener =
        object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                mSearchFab?.visibility = View.INVISIBLE
            }
        }
    private val mActionFabAnimatorListener: Animator.AnimatorListener =
        object : SimpleAnimatorListener() {
            override fun onAnimationEnd(animation: Animator) {
                binding.fabLayout.primaryFab?.visibility = View.INVISIBLE
            }
        }
    private var fabAnimator: ViewPropertyAnimator? = null
    private var mViewTransition: ViewTransition? = null
    private var mAdapter: GalleryListAdapter? = null
    private var mHelper: GalleryListHelper? = null
    private var mActionFabDrawable: AddDeleteDrawable? = null
    lateinit var mQuickSearchList: MutableList<QuickSearch>
    private var mHideActionFabSlop = 0
    private var mShowActionFab = true

    @State
    private var mState = STATE_NORMAL
    private val mOnScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {}
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy >= mHideActionFabSlop) {
                    hideActionFab()
                } else if (dy <= -mHideActionFabSlop / 2) {
                    showActionFab()
                }
            }
        }
    private var mHasFirstRefresh = false
    private var mNavCheckedId = 0
    private val mDownloadManager: DownloadManager = downloadManager

    @SuppressLint("NotifyDataSetChanged")
    private val mDownloadInfoListener: DownloadInfoListener = object : DownloadInfoListener {
        override fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
            mAdapter?.notifyDataSetChanged()
        }

        override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {}
        override fun onUpdateAll() {}
        override fun onReload() {
            mAdapter?.notifyDataSetChanged()
        }

        override fun onChange() {
            mAdapter?.notifyDataSetChanged()
        }

        override fun onRenameLabel(from: String, to: String) {}
        override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
            mAdapter?.notifyDataSetChanged()
        }

        override fun onUpdateLabels() {}
    }

    private val mFavouriteStatusRouter: FavouriteStatusRouter = favouriteStatusRouter

    @SuppressLint("NotifyDataSetChanged")
    private val mFavouriteStatusRouterListener: FavouriteStatusRouter.Listener =
        FavouriteStatusRouter.Listener { _: Long, _: Int ->
            mAdapter?.notifyDataSetChanged()
        }

    private var mIsTopList = false
    override fun getMenuResId(): Int {
        return R.menu.scene_gallery_list_searchbar_menu
    }

    override fun getNavCheckedItem(): Int {
        return mNavCheckedId
    }

    private fun handleArgs(args: Bundle?) {
        val action = args?.getString(KEY_ACTION) ?: Settings.getLaunchPageGalleryListSceneAction()
        mUrlBuilder = when (action) {
            ACTION_HOMEPAGE -> ListUrlBuilder()
            ACTION_SUBSCRIPTION -> ListUrlBuilder(MODE_SUBSCRIPTION)
            ACTION_WHATS_HOT -> ListUrlBuilder(MODE_WHATS_HOT)
            ACTION_TOP_LIST -> ListUrlBuilder(MODE_TOPLIST, mKeyword = "11")
            ACTION_LIST_URL_BUILDER -> args?.getParcelable<ListUrlBuilder>(KEY_LIST_URL_BUILDER)
                ?.copy() ?: ListUrlBuilder()

            else -> throw IllegalStateException("Wrong KEY_ACTION:${args?.getString(KEY_ACTION)} when handle args!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDownloadManager.addDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter.addListener(mFavouriteStatusRouterListener)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun onInit() {
        handleArgs(arguments)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH)
        mUrlBuilder = savedInstanceState.getParcelable(KEY_LIST_URL_BUILDER)!!
        mState = savedInstanceState.getInt(KEY_STATE)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val hasFirstRefresh: Boolean = if (mHelper != null && 1 == mHelper!!.shownViewIndex) {
            false
        } else {
            mHasFirstRefresh
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh)
        outState.putParcelable(KEY_LIST_URL_BUILDER, mUrlBuilder)
        outState.putInt(KEY_STATE, mState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDownloadManager.removeDownloadInfoListener(mDownloadInfoListener)
        mFavouriteStatusRouter.removeListener(mFavouriteStatusRouterListener)
    }

    private fun setSearchBarHint() {
        setEditTextHint(getString(if (EhUrl.SITE_EX == Settings.getGallerySite()) R.string.gallery_list_search_bar_hint_exhentai else R.string.gallery_list_search_bar_hint_e_hentai))
    }

    private fun setSearchBarSuggestionProvider() {
        setSuggestionProvider(object : SuggestionProvider {
            override fun providerSuggestions(text: String): List<Suggestion>? {
                val result1 = GalleryDetailUrlParser.parse(text, false)
                if (result1 != null) {
                    return listOf<Suggestion>(
                        GalleryDetailUrlSuggestion(
                            result1.gid,
                            result1.token
                        )
                    )
                }
                val result2 = GalleryPageUrlParser.parse(text, false)
                if (result2 != null) {
                    return listOf<Suggestion>(
                        GalleryPageUrlSuggestion(
                            result2.gid,
                            result2.pToken,
                            result2.page
                        )
                    )
                }
                return null
            }
        })
    }

    private fun wrapTagKeyword(keyword: String?): String {
        var keyword = keyword
        keyword = keyword!!.trim { it <= ' ' }
        val index1 = keyword.indexOf(':')
        if (index1 == -1 || index1 >= keyword.length - 1) {
            // Can't find :, or : is the last char
            return keyword
        }
        if (keyword[index1 + 1] == '"') {
            // The char after : is ", the word must be quoted
            return keyword
        }
        val index2 = keyword.indexOf(' ')
        return if (index2 <= index1) {
            // Can't find space, or space is before :
            keyword
        } else keyword.substring(0, index1 + 1) + "\"" + keyword.substring(index1 + 1) + "$\""
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

        // Update normal search mode
        binding.searchLayout.setNormalSearchMode(if (mode == MODE_SUBSCRIPTION) R.id.search_subscription_search else R.id.search_normal_search)

        // Update search edit text
        if (!mIsTopList) {
            if (mode == ListUrlBuilder.MODE_TAG) {
                keyword = wrapTagKeyword(keyword)
            }
            setSearchBarText(keyword)
        }

        // Update title
        var title = getSuitableTitleForUrlBuilder(resources, mUrlBuilder, true)
        if (null == title) {
            title = resources.getString(R.string.search)
        }
        setSearchBarHint(title)

        // Update nav checked item
        val checkedItemId: Int = when (mode) {
            ListUrlBuilder.MODE_NORMAL -> if (EhUtils.NONE == category && TextUtils.isEmpty(keyword)) R.id.nav_homepage else 0
            MODE_SUBSCRIPTION -> R.id.nav_subscription
            MODE_WHATS_HOT -> R.id.nav_whats_hot
            MODE_TOPLIST -> R.id.nav_toplist
            ListUrlBuilder.MODE_TAG, ListUrlBuilder.MODE_UPLOADER, ListUrlBuilder.MODE_IMAGE_SEARCH -> 0
            else -> throw IllegalStateException("Unexpected value: $mode")
        }
        navCheckedItem = checkedItemId
        mNavCheckedId = checkedItemId
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SceneGalleryListBinding.inflate(inflater, container, false)
        requireActivity().onBackPressedDispatcher.addCallback(mCallback)
        mHideActionFabSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        mShowActionFab = true
        mSearchFab = ViewUtils.`$$`(container, R.id.search_fab)
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root, WindowInsetsAnimationHelper(
                WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP,
                binding.fabLayout,
                mSearchFab!!.parent as View
            )
        )
        (binding.fabLayout.parent as ViewGroup).removeView(binding.fabLayout)
        container!!.addView(binding.fabLayout)
        val paddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar)
        val paddingBottomFab = resources.getDimensionPixelOffset(R.dimen.gallery_padding_bottom_fab)
        mViewTransition = BringOutTransition(binding.contentLayout, binding.searchLayout)
        mHelper = GalleryListHelper()
        binding.contentLayout.setHelper(mHelper)
        binding.contentLayout.fastScroller.setOnDragHandlerListener(this)
        binding.contentLayout.setFitPaddingTop(paddingTopSB)
        mAdapter = GalleryListAdapter(
            inflater, resources,
            binding.contentLayout.recyclerView, Settings.getListMode()
        )
        binding.contentLayout.recyclerView.clipToPadding = false
        binding.contentLayout.recyclerView.clipChildren = false
        binding.contentLayout.recyclerView.addOnScrollListener(mOnScrollListener)
        binding.contentLayout.fastScroller.run {
            setPadding(paddingLeft, paddingTopSB + paddingTop, paddingRight, paddingBottom)
        }
        setOnApplySearch { query: String? ->
            onApplySearch(query)
        }
        setSearchBarHint()
        setSearchBarSuggestionProvider()
        binding.searchLayout.setHelper(this)
        binding.searchLayout.run {
            setPadding(
                paddingLeft,
                paddingTop + paddingTopSB,
                paddingRight,
                paddingBottom + paddingBottomFab
            )
        }
        binding.fabLayout.setAutoCancel(true)
        binding.fabLayout.isExpanded = false
        binding.fabLayout.setHidePrimaryFab(false)
        binding.fabLayout.setOnClickFabListener(this)
        binding.fabLayout.addOnExpandListener(this)
        addAboveSnackView(binding.fabLayout)
        val colorID = theme.resolveColor(com.google.android.material.R.attr.colorOnSurface)
        mActionFabDrawable = AddDeleteDrawable(context, colorID)
        binding.fabLayout.primaryFab!!.setImageDrawable(mActionFabDrawable)
        mSearchFab!!.setOnClickListener {
            if (STATE_NORMAL != mState) {
                onApplySearch()
                hideSoftInput()
            }
        }

        // Update list url builder
        onUpdateUrlBuilder()

        // Restore state
        val newState = mState
        mState = STATE_NORMAL
        setState(newState, false)

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true
            mHelper!!.firstRefresh()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mCallback.remove()
        if (null != mHelper) {
            mHelper!!.destroy()
            if (1 == mHelper!!.shownViewIndex) {
                mHasFirstRefresh = false
            }
        }
        binding.contentLayout.recyclerView.stopScroll()
        (binding.fabLayout.parent as ViewGroup).removeView(binding.fabLayout)
        removeAboveSnackView(binding.fabLayout)
        _binding = null
        mAdapter = null
        mViewTransition = null
        mActionFabDrawable = null
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
        recyclerView: EasyRecyclerView, tip: TextView
    ) {
        val context = context
        if (null == context || null == mHelper) {
            return
        }

        // Can't add image search as quick search
        if (ListUrlBuilder.MODE_IMAGE_SEARCH == mUrlBuilder.mode) {
            showTip(R.string.image_search_not_quick_search, LENGTH_LONG)
            return
        }
        val gi = mHelper!!.firstVisibleItem
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
            getSuitableTitleForUrlBuilder(context.resources, mUrlBuilder, false),
            getString(R.string.quick_search)
        )
        builder.setTitle(R.string.add_quick_search_dialog_title)
        builder.setPositiveButton(android.R.string.ok, null)
        // TODO: It's ugly
        val checked = booleanArrayOf(Settings.getQSSaveProgress())
        val hint = arrayOf(getString(R.string.save_progress))
        builder.setMultiChoiceItems(
            hint,
            checked
        ) { _: DialogInterface?, which: Int, isChecked: Boolean -> checked[which] = isChecked }
        val dialog = builder.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            lifecycleScope.launchIO {
                var text = builder.text.trim { it <= ' ' }

                // Check name empty
                if (TextUtils.isEmpty(text)) {
                    withUIContext {
                        builder.setError(getString(R.string.name_is_empty))
                    }
                    return@launchIO
                }
                if (checked[0] && next != null) {
                    text += next
                    Settings.putQSSaveProgress(true)
                } else {
                    Settings.putQSSaveProgress(false)
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val drawerBinding = DrawerListRvBinding.inflate(inflater, container, false)
        drawerBinding.recyclerViewDrawer.layoutManager = LinearLayoutManager(context)
        val qsDrawerAdapter = QsDrawerAdapter(inflater)
        qsDrawerAdapter.setHasStableIds(true)
        drawerBinding.recyclerViewDrawer.adapter = qsDrawerAdapter
        val itemTouchHelper = ItemTouchHelper(GalleryListQSItemTouchHelperCallback(qsDrawerAdapter))
        itemTouchHelper.attachToRecyclerView(drawerBinding.recyclerViewDrawer)
        if (!mIsTopList) {
            drawerBinding.tip.visibility = View.VISIBLE
            drawerBinding.recyclerViewDrawer.visibility = View.GONE
        }
        lifecycleScope.launchIO {
            mQuickSearchList = EhDB.getAllQuickSearch()
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
                    drawerBinding.tip
                )
            } else if (id == R.id.action_help) {
                showQuickSearchTipDialog()
            }
            true
        }
        return drawerBinding.root
    }

    fun onItemClick(position: Int) {
        _binding ?: return
        mHelper ?: return
        val gi = mHelper!!.getDataAtEx(position) ?: return
        val args = Bundle()
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO)
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi)
        navigate(R.id.galleryDetailScene, args)
    }

    override fun onClickPrimaryFab(view: FabLayout, fab: FloatingActionButton) {
        if (STATE_NORMAL == mState) {
            view.toggle()
        }
    }

    private fun showGoToDialog() {
        val context = context
        if (null == context || null == mHelper) {
            return
        }
        if (mIsTopList) {
            val page = mHelper!!.pageForTop + 1
            val pages = mHelper!!.pages
            val hint = getString(R.string.go_to_hint, page, pages)
            val builder = EditTextDialogBuilder(context, null, hint)
            builder.editText.inputType =
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val dialog = builder.setTitle(R.string.go_to)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                if (null == mHelper) {
                    dialog.dismiss()
                    return@setOnClickListener
                }
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
                mHelper!!.goTo(goTo)
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
            datePicker.addOnPositiveButtonClickListener { v: Long? ->
                mHelper!!.goTo(
                    v!!, true
                )
            }
        }
    }

    override fun onClickSecondaryFab(view: FabLayout, fab: FloatingActionButton, position: Int) {
        if (null == mHelper) {
            return
        }
        when (position) {
            0 -> {
                if (mHelper!!.canGoTo()) showGoToDialog()
            }

            1 -> mHelper!!.refresh()
            2 -> {
                if (mIsTopList) {
                    mHelper!!.goTo(mHelper!!.pages - 1)
                } else {
                    mHelper!!.goTo("1", false)
                }
            }
        }
        view.isExpanded = false
    }

    override fun onExpand(expanded: Boolean) {
        if (null == mActionFabDrawable) {
            return
        }
        if (expanded) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
            mActionFabDrawable!!.setDelete(ANIMATE_TIME)
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
            mActionFabDrawable!!.setAdd(ANIMATE_TIME)
        }
    }

    override fun onSearchViewExpanded() {
        super.onSearchViewExpanded()
        selectSearchFab(true)
    }

    override fun onSearchViewHidden() {
        super.onSearchViewHidden()
        if (mState == STATE_NORMAL) selectActionFab(true)
    }

    fun onItemLongClick(position: Int): Boolean {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity || null == mHelper) {
            return false
        }
        val gi = mHelper!!.getDataAtEx(position) ?: return true
        val downloaded = mDownloadManager.getDownloadState(gi.gid) != DownloadInfo.STATE_INVALID
        val favourited = gi.favoriteSlot != -2
        val items = if (downloaded) arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.delete_downloads),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites),
            context.getString(R.string.download_move_dialog_title)
        ) else arrayOf<CharSequence>(
            context.getString(R.string.read),
            context.getString(R.string.download),
            context.getString(if (favourited) R.string.remove_from_favourites else R.string.add_to_favourites)
        )
        val icons = if (downloaded) intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_delete_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24,
            R.drawable.v_folder_move_x24
        ) else intArrayOf(
            R.drawable.v_book_open_x24,
            R.drawable.v_download_x24,
            if (favourited) R.drawable.v_heart_broken_x24 else R.drawable.v_heart_x24
        )
        BaseDialogBuilder(context)
            .setTitle(EhUtils.getSuitableTitle(gi))
            .setAdapter(
                SelectItemWithIconAdapter(
                    context,
                    items,
                    icons
                )
            ) { _: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        val intent = Intent(activity, ReaderActivity::class.java)
                        intent.action = ReaderActivity.ACTION_EH
                        intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, gi)
                        startActivity(intent)
                    }

                    1 -> if (downloaded) {
                        BaseDialogBuilder(context)
                            .setTitle(R.string.download_remove_dialog_title)
                            .setMessage(
                                getString(
                                    R.string.download_remove_dialog_message,
                                    gi.title
                                )
                            )
                            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                                mDownloadManager.deleteDownload(
                                    gi.gid
                                )
                            }
                            .show()
                    } else {
                        CommonOperations.startDownload(activity, gi, false)
                    }

                    2 -> if (favourited) {
                        CommonOperations.removeFromFavorites(
                            activity,
                            gi,
                            RemoveFromFavoriteListener(context)
                        )
                    } else {
                        CommonOperations.addToFavorites(
                            activity,
                            gi,
                            AddToFavoriteListener(context)
                        )
                    }

                    3 -> {
                        val labelRawList = downloadManager.labelList
                        val labelList: MutableList<String> = ArrayList(labelRawList.size + 1)
                        labelList.add(getString(R.string.default_download_label_name))
                        var i = 0
                        val n = labelRawList.size
                        while (i < n) {
                            labelRawList[i].label?.let { labelList.add(it) }
                            i++
                        }
                        val labels = labelList.toTypedArray()
                        val helper = MoveDialogHelper(labels, gi)
                        BaseDialogBuilder(context)
                            .setTitle(R.string.download_move_dialog_title)
                            .setItems(labels, helper)
                            .show()
                    }
                }
            }.show()
        return true
    }

    private fun showActionFab() {
        _binding ?: return
        if (STATE_NORMAL == mState && !mShowActionFab) {
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
        if (STATE_NORMAL == mState && mShowActionFab) {
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
        if (null == mSearchFab) {
            return
        }
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
            mSearchFab!!.visibility = View.VISIBLE
            mSearchFab!!.rotation = -45.0f
            mSearchFab!!.animate().scaleX(1.0f).scaleY(1.0f).rotation(0.0f).setListener(null)
                .setDuration(ANIMATE_TIME).setStartDelay(delay)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR).start()
        } else {
            binding.fabLayout.setExpanded(expanded = false, animation = false)
            val fab: View? = binding.fabLayout.primaryFab
            fab!!.visibility = View.INVISIBLE
            fab.scaleX = 0.0f
            fab.scaleY = 0.0f
            mSearchFab!!.visibility = View.VISIBLE
            mSearchFab!!.scaleX = 1.0f
            mSearchFab!!.scaleY = 1.0f
        }
    }

    private fun selectActionFab(animation: Boolean) {
        _binding ?: return
        if (null == mSearchFab) {
            return
        }
        mShowActionFab = true
        if (animation) {
            val delay: Long
            if (View.INVISIBLE == mSearchFab!!.visibility) {
                delay = 0L
            } else {
                delay = ANIMATE_TIME
                mSearchFab!!.animate().scaleX(0.0f).scaleY(0.0f)
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
            mSearchFab!!.visibility = View.INVISIBLE
            mSearchFab!!.scaleX = 0.0f
            mSearchFab!!.scaleY = 0.0f
        }
    }

    private fun setState(@State state: Int) {
        setState(state, true)
    }

    private fun setState(@State state: Int, animation: Boolean) {
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
                STATE_NORMAL -> when (state) {
                    STATE_SIMPLE_SEARCH -> {
                        selectSearchFab(animation)
                    }

                    STATE_SEARCH -> {
                        mViewTransition!!.showView(1, animation)
                        binding.searchLayout.scrollSearchContainerToTop()
                        selectSearchFab(animation)
                    }

                    STATE_SEARCH_SHOW_LIST -> {
                        mViewTransition!!.showView(1, animation)
                        binding.searchLayout.scrollSearchContainerToTop()
                        selectSearchFab(animation)
                    }
                }

                STATE_SIMPLE_SEARCH -> when (state) {
                    STATE_NORMAL -> {
                        selectActionFab(animation)
                    }

                    STATE_SEARCH -> {
                        mViewTransition!!.showView(1, animation)
                        binding.searchLayout.scrollSearchContainerToTop()
                    }

                    STATE_SEARCH_SHOW_LIST -> {
                        mViewTransition!!.showView(1, animation)
                        binding.searchLayout.scrollSearchContainerToTop()
                    }
                }

                STATE_SEARCH, STATE_SEARCH_SHOW_LIST -> if (state == STATE_NORMAL) {
                    mViewTransition!!.showView(0, animation)
                    selectActionFab(animation)
                } else if (state == STATE_SIMPLE_SEARCH) {
                    mViewTransition!!.showView(0, animation)
                }
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        if (mState == STATE_NORMAL) {
            setState(STATE_SEARCH)
        } else {
            setState(STATE_NORMAL)
        }
        return true
    }

    private fun onApplySearch(query: String?) {
        _binding ?: return
        if (null == mHelper) {
            return
        }
        if (mState == STATE_SEARCH || mState == STATE_SEARCH_SHOW_LIST) {
            try {
                binding.searchLayout.formatListUrlBuilder(mUrlBuilder, query)
            } catch (e: EhException) {
                showTip(e.message, LENGTH_LONG)
                return
            }
        } else {
            val oldMode = mUrlBuilder.mode
            // If it's MODE_SUBSCRIPTION, keep it
            val newMode =
                if (oldMode == MODE_SUBSCRIPTION) MODE_SUBSCRIPTION else ListUrlBuilder.MODE_NORMAL
            mUrlBuilder.reset()
            mUrlBuilder.mode = newMode
            mUrlBuilder.keyword = query
        }
        onUpdateUrlBuilder()
        mHelper!!.refresh()
        setState(STATE_NORMAL)
    }

    override fun onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
    }

    override fun onEndDragHandler() {
        // Restore right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        showSearchBar()
    }

    private fun onStateChange(newState: Int) {
        mCallback.isEnabled = newState != STATE_NORMAL
        if (newState == STATE_NORMAL || newState == STATE_SIMPLE_SEARCH) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        } else {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
        }
    }

    override fun onChangeSearchMode() {
        showSearchBar()
    }

    override fun onSelectImage() {
        val builder = PickVisualMediaRequest.Builder()
        builder.setMediaType(ImageOnly)
        selectImageLauncher.launch(builder.build())
    }

    private fun onGetGalleryListSuccess(result: GalleryListParser.Result, taskId: Int) {
        if (mHelper != null && mHelper!!.isCurrentTask(taskId)) {
            val emptyString =
                getString(if (mUrlBuilder.mode == MODE_SUBSCRIPTION && result.noWatchedTags) R.string.gallery_list_empty_hit_subscription else R.string.gallery_list_empty_hit)
            mHelper!!.setEmptyString(emptyString)
            if (mIsTopList) {
                mHelper!!.onGetPageData(
                    taskId,
                    result.pages,
                    result.nextPage,
                    null,
                    null,
                    result.galleryInfoList
                )
            } else {
                mHelper!!.onGetPageData(
                    taskId,
                    0,
                    0,
                    result.prev,
                    result.next,
                    result.galleryInfoList
                )
            }
        }
    }

    private fun onGetGalleryListFailure(e: Exception, taskId: Int) {
        if (mHelper != null && mHelper!!.isCurrentTask(taskId)) {
            mHelper!!.onGetException(taskId, e)
        }
    }

    @IntDef(STATE_NORMAL, STATE_SIMPLE_SEARCH, STATE_SEARCH, STATE_SEARCH_SHOW_LIST)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class State
    private inner class GetGalleryListListener(
        context: Context,
        private val mTaskId: Int
    ) : EhCallback<GalleryListScene, GalleryListParser.Result>(context) {
        override fun onSuccess(result: GalleryListParser.Result) {
            val scene = this@GalleryListScene
            scene.onGetGalleryListSuccess(result, mTaskId)
        }

        override fun onFailure(e: Exception) {
            val scene = this@GalleryListScene
            scene.onGetGalleryListFailure(e, mTaskId)
        }

        override fun onCancel() {}
    }

    private class AddToFavoriteListener(context: Context) :
        EhCallback<GalleryListScene, Void>(context) {
        override fun onSuccess(result: Void) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.add_to_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
    }

    private class RemoveFromFavoriteListener(context: Context) :
        EhCallback<GalleryListScene, Void>(context) {
        override fun onSuccess(result: Void) {
            showTip(R.string.remove_from_favorite_success, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.remove_from_favorite_failure, LENGTH_LONG)
        }

        override fun onCancel() {}
    }

    private class QsDrawerHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val key: TextView
        val option: ImageView

        init {
            key = ViewUtils.`$$`(itemView, R.id.tv_key) as TextView
            option = ViewUtils.`$$`(itemView, R.id.iv_option) as ImageView
        }
    }

    private inner class MoveDialogHelper(
        private val mLabels: Array<String>,
        private val mGi: GalleryInfo
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            // Cancel check mode
            _binding ?: return
            binding.contentLayout.recyclerView.outOfCustomChoiceMode()
            val downloadManager = downloadManager
            val downloadInfo = downloadManager.getDownloadInfo(mGi.gid) ?: return
            val label = if (which == 0) null else mLabels[which]
            downloadManager.changeLabel(listOf(downloadInfo), label)
        }
    }

    private inner class QsDrawerAdapter(private val mInflater: LayoutInflater) :
        RecyclerView.Adapter<QsDrawerHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QsDrawerHolder {
            return QsDrawerHolder(mInflater.inflate(R.layout.item_drawer_list, parent, false))
        }

        override fun onBindViewHolder(holder: QsDrawerHolder, position: Int) {
            if (!mIsTopList) {
                holder.key.text = mQuickSearchList[position].name
                holder.itemView.setOnClickListener {
                    if (null == mHelper) {
                        return@setOnClickListener
                    }
                    val q = mQuickSearchList[position]
                    mUrlBuilder.set(q)
                    onUpdateUrlBuilder()
                    val i = q.name!!.lastIndexOf("@")
                    mHelper!!.goTo(if (i != -1) q.name!!.substring(i + 1) else null, true)
                    setState(STATE_NORMAL)
                    closeDrawer(GravityCompat.END)
                }
            } else {
                val keywords = intArrayOf(11, 12, 13, 15)
                val toplists = intArrayOf(
                    R.string.toplist_alltime,
                    R.string.toplist_pastyear,
                    R.string.toplist_pastmonth,
                    R.string.toplist_yesterday
                )
                holder.key.text = getString(toplists[position])
                holder.option.visibility = View.GONE
                holder.itemView.setOnClickListener {
                    if (null == mHelper) {
                        return@setOnClickListener
                    }
                    mUrlBuilder.keyword = keywords[position].toString()
                    onUpdateUrlBuilder()
                    mHelper!!.refresh()
                    setState(STATE_NORMAL)
                    closeDrawer(GravityCompat.END)
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
            navigate(getDestination(), getArgs())
            if (mState == STATE_SIMPLE_SEARCH) {
                setState(STATE_NORMAL)
            } else if (mState == STATE_SEARCH_SHOW_LIST) {
                setState(STATE_SEARCH)
            }
        }
        abstract fun getDestination(): Int

        abstract fun getArgs(): Bundle
    }

    private inner class GalleryDetailUrlSuggestion(
        private val mGid: Long,
        private val mToken: String
    ) : UrlSuggestion() {
        override fun getDestination(): Int {
            return R.id.galleryDetailScene
        }

        override fun getArgs(): Bundle {
            val args = Bundle()
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN)
            args.putLong(GalleryDetailScene.KEY_GID, mGid)
            args.putString(GalleryDetailScene.KEY_TOKEN, mToken)
            return args
        }
    }

    private inner class GalleryPageUrlSuggestion(
        private val mGid: Long,
        private val mPToken: String,
        private val mPage: Int
    ) : UrlSuggestion() {
        override fun getDestination(): Int {
            return R.id.progressScene
        }

        override fun getArgs(): Bundle {
            val args = Bundle()
            args.putString(ProgressScene.KEY_ACTION, ProgressScene.ACTION_GALLERY_TOKEN)
            args.putLong(ProgressScene.KEY_GID, mGid)
            args.putString(ProgressScene.KEY_PTOKEN, mPToken)
            args.putInt(ProgressScene.KEY_PAGE, mPage)
            return args
        }
    }

    private inner class GalleryListAdapter(
        inflater: LayoutInflater,
        resources: Resources, recyclerView: RecyclerView, type: Int
    ) : GalleryAdapter(inflater, resources, recyclerView, type, true) {
        override fun getItemCount(): Int {
            return if (null != mHelper) mHelper!!.size() else 0
        }

        public override fun onItemClick(view: View, position: Int) {
            this@GalleryListScene.onItemClick(position)
        }

        public override fun onItemLongClick(view: View, position: Int): Boolean {
            return this@GalleryListScene.onItemLongClick(position)
        }

        override fun getDataAt(position: Int): GalleryInfo? {
            return if (null != mHelper) mHelper!!.getDataAtEx(position) else null
        }
    }

    private inner class GalleryListHelper : GalleryInfoContentHelper() {
        override fun getPageData(
            taskId: Int,
            type: Int,
            page: Int,
            index: String?,
            isNext: Boolean
        ) {
            val activity = mainActivity
            if (null == activity || null == mHelper) {
                return
            }
            if (mIsTopList) {
                mUrlBuilder.setJumpTo(page.toString())
            } else {
                mUrlBuilder.setIndex(index, isNext)
                mUrlBuilder.setJumpTo(jumpTo)
            }
            if (ListUrlBuilder.MODE_IMAGE_SEARCH == mUrlBuilder.mode) {
                val request = EhRequest()
                request.setMethod(EhClient.METHOD_IMAGE_SEARCH)
                request.setCallback(
                    GetGalleryListListener(
                        context,
                        activity.stageId
                    )
                )
                request.setArgs(
                    File(StringUtils.avoidNull(mUrlBuilder.imagePath)),
                    mUrlBuilder.isUseSimilarityScan,
                    mUrlBuilder.isOnlySearchCovers, mUrlBuilder.isShowExpunged
                )
                request.enqueue(this@GalleryListScene)
            } else {
                val url = mUrlBuilder.build()
                val request = EhRequest()
                request.setMethod(EhClient.METHOD_GET_GALLERY_LIST)
                request.setCallback(
                    GetGalleryListListener(
                        context,
                        activity.stageId
                    )
                )
                request.setArgs(url)
                request.enqueue(this@GalleryListScene)
            }
        }

        override fun getContext(): Context {
            return requireContext()
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun notifyDataSetChanged() {
            if (null != mAdapter) {
                mAdapter!!.notifyDataSetChanged()
            }
        }

        override fun notifyItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (null != mAdapter) {
                mAdapter!!.notifyItemRangeInserted(positionStart, itemCount)
            }
        }

        override fun onShowView(hiddenView: View, shownView: View) {
            showSearchBar()
            showActionFab()
        }

        override fun isDuplicate(d1: GalleryInfo?, d2: GalleryInfo?): Boolean {
            return d1?.gid == d2?.gid && d1 != null && d2 != null
        }

        override fun onScrollToPosition(position: Int) {
            if (0 == position) {
                showSearchBar()
                showActionFab()
            }
        }
    }

    private inner class GalleryListQSItemTouchHelperCallback(private val mAdapter: QsDrawerAdapter) :
        ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeMovementFlags(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT
            )
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
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
            lifecycleScope.launchIO {
                EhDB.deleteQuickSearch(quickSearch)
                mQuickSearchList.removeAt(position)
                withUIContext {
                    mAdapter.notifyItemRemoved(position)
                }
            }
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_HOMEPAGE = "action_homepage"
        const val ACTION_SUBSCRIPTION = "action_subscription"
        const val ACTION_WHATS_HOT = "action_whats_hot"
        const val ACTION_TOP_LIST = "action_top_list"
        const val ACTION_LIST_URL_BUILDER = "action_list_url_builder"
        const val KEY_LIST_URL_BUILDER = "list_url_builder"
        const val KEY_HAS_FIRST_REFRESH = "has_first_refresh"
        const val KEY_STATE = "state"
        private const val STATE_NORMAL = 0
        private const val STATE_SIMPLE_SEARCH = 1
        private const val STATE_SEARCH = 2
        private const val STATE_SEARCH_SHOW_LIST = 3
        private const val ANIMATE_TIME = 300L
        private fun getSuitableTitleForUrlBuilder(
            resources: Resources, urlBuilder: ListUrlBuilder, appName: Boolean
        ): String? {
            val keyword = urlBuilder.keyword
            val category = urlBuilder.category
            return if (ListUrlBuilder.MODE_NORMAL == urlBuilder.mode && EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword) && urlBuilder.advanceSearch == -1 && urlBuilder.minRating == -1 && urlBuilder.pageFrom == -1 && urlBuilder.pageTo == -1
            ) {
                resources.getString(if (appName) R.string.app_name else R.string.homepage)
            } else if (MODE_SUBSCRIPTION == urlBuilder.mode && EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword) && urlBuilder.advanceSearch == -1 && urlBuilder.minRating == -1 && urlBuilder.pageFrom == -1 && urlBuilder.pageTo == -1
            ) {
                resources.getString(R.string.subscription)
            } else if (MODE_WHATS_HOT == urlBuilder.mode) {
                resources.getString(R.string.whats_hot)
            } else if (MODE_TOPLIST == urlBuilder.mode) {
                when (urlBuilder.keyword) {
                    "11" -> resources.getString(R.string.toplist_alltime)
                    "12" -> resources.getString(R.string.toplist_pastyear)
                    "13" -> resources.getString(R.string.toplist_pastmonth)
                    "15" -> resources.getString(R.string.toplist_yesterday)
                    else -> null
                }
            } else if (!TextUtils.isEmpty(keyword)) {
                keyword
            } else if (MathUtils.hammingWeight(category) == 1) {
                EhUtils.getCategory(category)
            } else {
                null
            }
        }

        fun getStartAnnouncer(lub: ListUrlBuilder?): Announcer {
            val args = Bundle()
            args.putString(KEY_ACTION, ACTION_LIST_URL_BUILDER)
            args.putParcelable(KEY_LIST_URL_BUILDER, lub)
            return Announcer(GalleryListScene::class.java).setArgs(args)
        }

        @JvmStatic
        fun getStartArgs(lub: ListUrlBuilder): Bundle {
            val args = Bundle()
            args.putString(KEY_ACTION, ACTION_LIST_URL_BUILDER)
            args.putParcelable(KEY_LIST_URL_BUILDER, lub)
            return args
        }
    }
}