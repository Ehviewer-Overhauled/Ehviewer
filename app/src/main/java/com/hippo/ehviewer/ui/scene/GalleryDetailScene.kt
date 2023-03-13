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

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import coil.Coil.imageLoader
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.hippo.app.BaseDialogBuilder
import com.hippo.app.CheckBoxDialogBuilder
import com.hippo.app.EditTextDialogBuilder
import com.hippo.easyrecyclerview.MarginItemDecoration
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhApplication.Companion.galleryDetailCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhFilter
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.EhTagDatabase.isTranslatable
import com.hippo.ehviewer.client.EhTagDatabase.namespaceToPrefix
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import com.hippo.ehviewer.client.getPreviewThumbKey
import com.hippo.ehviewer.client.parser.ArchiveParser
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.client.parser.ParserUtils
import com.hippo.ehviewer.client.parser.RateGalleryParser
import com.hippo.ehviewer.client.parser.TorrentParser
import com.hippo.ehviewer.client.parser.VoteTagParser
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.databinding.SceneGalleryDetailBinding
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.MODE_READ
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.widget.EhAsyncThumb
import com.hippo.ehviewer.widget.GalleryRatingBar
import com.hippo.ehviewer.widget.GalleryRatingBar.OnUserRateListener
import com.hippo.text.URLImageGetter
import com.hippo.util.AppHelper
import com.hippo.util.ExceptionUtils
import com.hippo.util.ReadableTime
import com.hippo.util.addTextToClipboard
import com.hippo.util.getParcelableCompat
import com.hippo.view.ViewTransition
import com.hippo.widget.AutoWrapLayout
import com.hippo.widget.ObservedTextView
import com.hippo.widget.SimpleGridAutoSpanLayout
import com.hippo.widget.recyclerview.AutoGridLayoutManager
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IntIdGenerator
import com.hippo.yorozuya.LayoutUtils
import com.hippo.yorozuya.ViewUtils
import com.hippo.yorozuya.collect.IntList
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import rikka.core.res.resolveColor
import kotlin.math.roundToInt
import com.hippo.ehviewer.download.DownloadManager as downloadManager

class GalleryDetailScene : CollapsingToolbarScene(), View.OnClickListener, DownloadInfoListener,
    OnLongClickListener {
    private var _binding: SceneGalleryDetailBinding? = null
    private val binding
        get() = _binding!!
    private var mViewTransition: ViewTransition? = null
    private var mViewTransition2: ViewTransition? = null

    private var mDownloadState = 0
    private var mAction: String? = null
    private var mGalleryInfo: GalleryInfo? = null
    private var mGid: Long = 0
    private var mToken: String? = null
    private var mPage = 0
    private var mGalleryDetail by mutableStateOf<GalleryDetail?>(null)
    private var mRequestId = IntIdGenerator.INVALID_ID
    private var mTorrentList: List<TorrentParser.Result>? = null
    private var requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result && mGalleryDetail != null) {
            val helper = TorrentListDialogHelper()
            val dialog: Dialog = BaseDialogBuilder(requireActivity())
                .setTitle(R.string.torrents)
                .setView(R.layout.dialog_torrent_list)
                .setOnDismissListener(helper)
                .show()
            helper.setDialog(dialog, mGalleryDetail!!.torrentUrl)
        }
    }
    private var mArchiveFormParamOr: String? = null
    private var mArchiveList: List<ArchiveParser.Archive>? = null
    private var mCurrentFunds: HomeParser.Funds? = null
    private var previewsAdapter: GalleryPreviewsAdapter? = null
    private var footerAdapter: HintAdapter? = null
    private var headerAdapter: HintAdapter? = null

    @State
    private var mState = STATE_INIT
    private var mModifyingFavorites = false

    @StringRes
    private fun getRatingText(rating: Float): Int {
        return when ((rating * 2).roundToInt()) {
            0 -> R.string.rating0
            1 -> R.string.rating1
            2 -> R.string.rating2
            3 -> R.string.rating3
            4 -> R.string.rating4
            5 -> R.string.rating5
            6 -> R.string.rating6
            7 -> R.string.rating7
            8 -> R.string.rating8
            9 -> R.string.rating9
            10 -> R.string.rating10
            else -> R.string.rating_none
        }
    }

    private fun handleArgs(args: Bundle?) {
        val action = args?.getString(KEY_ACTION) ?: return
        mAction = action
        if (ACTION_GALLERY_INFO == action) {
            mGalleryInfo = args.getParcelableCompat(KEY_GALLERY_INFO)
            // Add history
            mGalleryInfo?.let { lifecycleScope.launchIO { EhDB.putHistoryInfo(it) } }
        } else if (ACTION_GID_TOKEN == action) {
            mGid = args.getLong(KEY_GID)
            mToken = args.getString(KEY_TOKEN)
            mPage = args.getInt(KEY_PAGE)
        }
    }

    private val galleryDetailUrl: String?
        get() {
            val gid: Long
            val token: String?
            if (mGalleryDetail != null) {
                gid = mGalleryDetail!!.gid
                token = mGalleryDetail!!.token
            } else if (mGalleryInfo != null) {
                gid = mGalleryInfo!!.gid
                token = mGalleryInfo!!.token
            } else if (ACTION_GID_TOKEN == mAction) {
                gid = mGid
                token = mToken
            } else {
                return null
            }
            return EhUrl.getGalleryDetailUrl(gid, token, 0, false)
        }

    // -1 for error
    private val gid: Long
        get() = if (mGalleryDetail != null) {
            mGalleryDetail!!.gid
        } else if (mGalleryInfo != null) {
            mGalleryInfo!!.gid
        } else if (ACTION_GID_TOKEN == mAction) {
            mGid
        } else {
            -1
        }
    private val uploader: String?
        get() = if (mGalleryDetail != null) {
            mGalleryDetail!!.uploader
        } else if (mGalleryInfo != null) {
            mGalleryInfo!!.uploader
        } else {
            null
        }

    // Judging by the uploader to exclude the cooldown period
    private val disowned: Boolean
        get() = uploader == "(Disowned)"

    // -1 for error
    private val category: Int
        get() = if (mGalleryDetail != null) {
            mGalleryDetail!!.category
        } else if (mGalleryInfo != null) {
            mGalleryInfo!!.category
        } else {
            -1
        }
    private val galleryInfo: GalleryInfo?
        get() = if (null != mGalleryDetail) {
            mGalleryDetail
        } else if (null != mGalleryInfo) {
            mGalleryInfo
        } else {
            null
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
        _binding ?: return
        mGalleryInfo?.let {
            viewLifecycleOwner.lifecycleScope.launchIO {
                runCatching {
                    val queen = SpiderQueen.obtainSpiderQueen(it, MODE_READ)
                    val startPage = queen.awaitStartPage()
                    SpiderQueen.releaseSpiderQueen(queen, MODE_READ)
                    readButtonText = if (startPage == 0) {
                        getString(R.string.read)
                    } else {
                        getString(R.string.read_from, startPage + 1)
                    }
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }

    private fun onInit() {
        handleArgs(arguments)
    }

    private fun onRestore(savedInstanceState: Bundle) {
        mAction = savedInstanceState.getString(KEY_ACTION)
        mGalleryInfo = savedInstanceState.getParcelableCompat(KEY_GALLERY_INFO)
        mGid = savedInstanceState.getLong(KEY_GID)
        mToken = savedInstanceState.getString(KEY_TOKEN)
        mGalleryDetail = savedInstanceState.getParcelableCompat(KEY_GALLERY_DETAIL)
        mRequestId = savedInstanceState.getInt(KEY_REQUEST_ID)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mAction != null) {
            outState.putString(KEY_ACTION, mAction)
        }
        if (mGalleryInfo != null) {
            outState.putParcelable(KEY_GALLERY_INFO, mGalleryInfo)
        }
        outState.putLong(KEY_GID, mGid)
        if (mToken != null) {
            outState.putString(KEY_TOKEN, mAction)
        }
        if (mGalleryDetail != null) {
            outState.putParcelable(KEY_GALLERY_DETAIL, mGalleryDetail)
        }
        outState.putInt(KEY_REQUEST_ID, mRequestId)
    }

    override fun getMenuResId(): Int {
        return R.menu.scene_gallery_detail
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setNavigationIcon(R.drawable.v_arrow_left_dark_x24)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.action_open_in_other_app) {
            val url = galleryDetailUrl
            val activity: Activity? = mainActivity
            if (null != url && null != activity) {
                UrlOpener.openUrl(activity, url, false)
            }
        } else if (itemId == R.id.action_refresh) {
            if (mState != STATE_REFRESH && mState != STATE_REFRESH_HEADER) {
                adjustViewVisibility(STATE_REFRESH)
                request()
            }
        } else if (itemId == R.id.action_add_tag) {
            if (mGalleryDetail == null) {
                return false
            }
            if (mGalleryDetail!!.apiUid < 0) {
                showTip(R.string.sign_in_first, LENGTH_LONG)
                return false
            }
            val builder =
                EditTextDialogBuilder(requireContext(), "", getString(R.string.action_add_tag_tip))
            builder.setPositiveButton(android.R.string.ok, null)
            val dialog = builder.setTitle(R.string.action_add_tag)
                .show()
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener {
                    voteTag(builder.text.trim { it <= ' ' }, 1)
                    dialog.dismiss()
                }
        }
        return true
    }

    override fun onCreateViewWithToolbar(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Get download state
        val gid = gid
        mDownloadState = if (gid != -1L) {
            downloadManager.getDownloadState(gid)
        } else {
            DownloadInfo.STATE_INVALID
        }
        _binding = SceneGalleryDetailBinding.inflate(inflater, container, false)
        setLiftOnScrollTargetView(binding.scrollView)
        mViewTransition = ViewTransition(binding.scrollView, binding.progressView, binding.tip)
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.big_sad_pandroid)!!
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        binding.tip.setCompoundDrawables(null, drawable, null, null)
        binding.tip.setOnClickListener(this)
        binding.content.actions.let {
            it.newerVersion.setOnClickListener(this)
            it.heart.setOnClickListener(this)
            it.heart.setOnLongClickListener(this)
            it.heartOutline.setOnClickListener(this)
            it.heartOutline.setOnLongClickListener(this)
            it.torrent.setOnClickListener(this)
            it.archive.setOnClickListener(this)
            it.share.setOnClickListener(this)
            it.rate.setOnClickListener(this)
            it.similar.setOnClickListener(this)
            it.searchCover.setOnClickListener(this)
        }
        binding.content.actions.newerVersion.setOnClickListener(this)
        if (Settings.showComments) {
            binding.content.comments.comments.setOnClickListener(this)
        } else {
            binding.content.comments.comments.visibility = View.GONE
        }
        binding.content.recyclerView.run {
            previewsAdapter = GalleryPreviewsAdapter {
                mainActivity!!.startReaderActivity(mGalleryDetail!!, it.position)
            }
            footerAdapter = HintAdapter { navigateToPreview(true) }
            headerAdapter = HintAdapter { navigateToPreview() }
            adapter = ConcatAdapter(headerAdapter, previewsAdapter, footerAdapter)
            val columnWidth = Settings.thumbSize
            layoutManager = AutoGridLayoutManager(context, columnWidth).apply {
                setStrategy(SimpleGridAutoSpanLayout.STRATEGY_SUITABLE_SIZE)
            }
            val padding = LayoutUtils.dp2pix(context, 8f)
            val decoration = MarginItemDecoration(padding, padding, padding, padding, padding)
            addItemDecoration(decoration)
            val lp = layoutParams
            lp.height = resources.displayMetrics.heightPixels * 8 / 7
            layoutParams = lp
        }
        bindComposeView()
        mViewTransition2 = ViewTransition(binding.content.belowHeader, binding.content.progress)
        if (prepareData()) {
            if (mGalleryDetail != null) {
                bindViewSecond()
                adjustViewVisibility(STATE_NORMAL)
            } else if (mGalleryInfo != null) {
                adjustViewVisibility(STATE_REFRESH_HEADER)
            } else {
                adjustViewVisibility(STATE_REFRESH)
            }
        } else {
            binding.tip.setText(R.string.error_cannot_find_gallery)
            adjustViewVisibility(STATE_FAILED)
        }
        downloadManager.addDownloadInfoListener(this)
        (requireActivity() as MainActivity).mShareUrl = galleryDetailUrl
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        downloadManager.removeDownloadInfoListener(this)
        (requireActivity() as MainActivity).mShareUrl = null
        mViewTransition = null
        mViewTransition2 = null
        previewsAdapter = null
        footerAdapter = null
        headerAdapter = null
        _binding = null
    }

    private fun prepareData(): Boolean {
        if (mGalleryDetail != null) {
            return true
        }
        val gid = gid
        if (gid == -1L) {
            return false
        }

        // Get from cache
        mGalleryDetail = galleryDetailCache[gid]
        if (mGalleryDetail != null) {
            return true
        }
        val application = requireContext().applicationContext as EhApplication
        return if (application.containGlobalStuff(mRequestId)) {
            // request exist
            true
        } else request()

        // Do request
    }

    private fun request(): Boolean {
        val context = context
        val activity = mainActivity
        val url = galleryDetailUrl
        if (null == context || null == activity || null == url) {
            return false
        }
        val callback: EhClient.Callback<*> = GetGalleryDetailListener(context)
        mRequestId = (context.applicationContext as EhApplication).putGlobalStuff(callback)
        val request = EhRequest()
            .setMethod(EhClient.METHOD_GET_GALLERY_DETAIL)
            .setArgs(url)
            .setCallback(callback)
        request.enqueue(this)
        return true
    }

    private fun adjustViewVisibility(state: Int) {
        if (state == mState) {
            return
        }
        if (mViewTransition == null || mViewTransition2 == null) {
            return
        }
        mState = state
        val animation = !TRANSITION_ANIMATION_DISABLED
        when (state) {
            STATE_NORMAL -> {
                // Show mMainView
                mViewTransition!!.showView(0, animation)
                // Show mBelowHeader
                mViewTransition2!!.showView(0, animation)
            }

            STATE_REFRESH ->  // Show mProgressView
                mViewTransition!!.showView(1, animation)

            STATE_REFRESH_HEADER -> {
                // Show mMainView
                mViewTransition!!.showView(0, animation)
                // Show mProgress
                mViewTransition2!!.showView(1, animation)
            }

            STATE_INIT, STATE_FAILED ->  // Show mFailedView
                mViewTransition!!.showView(2, animation)
        }
    }

    private var readButtonText by mutableStateOf("")
    private var downloadButtonText by mutableStateOf("")
    private var thumbUrl by mutableStateOf("")
    private var uploaderText by mutableStateOf("")
    private var categoryText by mutableStateOf("")

    private fun bindComposeView() {
        _binding ?: return
        readButtonText = getString(R.string.read)
        binding.content.header.setContent {
            Mdc3Theme {
                Column(
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin))
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.keyline_margin))
                    ) {
                        Row {
                            Card {
                                EhAsyncThumb(
                                    model = galleryInfo?.thumb,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .height(dimensionResource(id = R.dimen.gallery_detail_thumb_height))
                                        .width(dimensionResource(id = R.dimen.gallery_detail_thumb_width))
                                )
                            }
                            Spacer(modifier = Modifier.weight(1F))
                            Column(
                                modifier = Modifier.height(dimensionResource(id = R.dimen.gallery_detail_thumb_height)),
                                horizontalAlignment = Alignment.End
                            ) {
                                mGalleryDetail?.run {
                                    Card(
                                        onClick = {
                                            val galleryInfoBottomSheet =
                                                GalleryInfoBottomSheet(this)
                                            galleryInfoBottomSheet.show(
                                                requireActivity().supportFragmentManager,
                                                GalleryInfoBottomSheet.TAG
                                            )
                                        },
                                        modifier = Modifier
                                            .width(dimensionResource(id = R.dimen.gallery_detail_thumb_width))
                                            .padding(
                                                top = dimensionResource(id = R.dimen.keyline_margin),
                                                end = dimensionResource(id = R.dimen.keyline_margin)
                                            )
                                    ) {
                                        Row {
                                            Text(
                                                text = language.orEmpty(),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Spacer(modifier = Modifier.weight(1F))
                                            Text(
                                                text = size.orEmpty(),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Row {
                                            Text(
                                                text = stringResource(
                                                    id = R.string.favored_times,
                                                    favoriteCount
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            Spacer(modifier = Modifier.weight(1F))
                                            Text(
                                                text = pluralStringResource(
                                                    id = R.plurals.page_count,
                                                    pages,
                                                    pages
                                                ),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        Text(
                                            text = posted.orEmpty(),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1F))
                                AssistChip(onClick = {
                                    val category = category
                                    if (category == EhUtils.NONE || category == EhUtils.PRIVATE || category == EhUtils.UNKNOWN) {
                                        return@AssistChip
                                    }
                                    val lub = ListUrlBuilder()
                                    lub.category = category
                                    navigate(R.id.galleryListScene, lub.toStartArgs(), true)
                                }, label = {
                                    Text(text = categoryText)
                                }, modifier = Modifier.padding(
                                    horizontal = dimensionResource(id = R.dimen.keyline_margin)
                                ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_baseline_label_24),
                                            contentDescription = null
                                        )
                                    })
                                AssistChip(onClick = {
                                    if (uploader.isNullOrEmpty() || disowned) {
                                        return@AssistChip
                                    }
                                    val lub = ListUrlBuilder()
                                    lub.mode = ListUrlBuilder.MODE_UPLOADER
                                    lub.keyword = uploader
                                    navigate(R.id.galleryListScene, lub.toStartArgs(), true)
                                },
                                    label = { Text(text = uploaderText) },
                                    modifier = Modifier.padding(
                                        horizontal = dimensionResource(id = R.dimen.keyline_margin)
                                    ),
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.file_upload_black_24dp),
                                            contentDescription = null
                                        )
                                    })
                            }
                        }
                    }
                    Row {
                        FilledTonalButton(
                            onClick = {
                                val galleryDetail = mGalleryDetail ?: return@FilledTonalButton
                                if (downloadManager.getDownloadState(galleryDetail.gid) == DownloadInfo.STATE_INVALID) {
                                    CommonOperations.startDownload(
                                        activity as MainActivity,
                                        galleryDetail,
                                        false
                                    )
                                } else {
                                    val builder = CheckBoxDialogBuilder(
                                        requireContext(),
                                        getString(
                                            R.string.download_remove_dialog_message,
                                            galleryDetail.title
                                        ),
                                        getString(R.string.download_remove_dialog_check_text),
                                        Settings.removeImageFiles
                                    )
                                    val helper = DeleteDialogHelper(
                                        downloadManager, galleryDetail, builder
                                    )
                                    builder.setTitle(R.string.download_remove_dialog_title)
                                        .setPositiveButton(android.R.string.ok, helper)
                                        .show()
                                }
                            }, modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .weight(1F)
                        ) {
                            Text(text = downloadButtonText)
                        }
                        Button(
                            onClick = {
                                val galleryDetail = mGalleryDetail ?: return@Button
                                val intent = Intent(activity, ReaderActivity::class.java)
                                intent.action = ReaderActivity.ACTION_EH
                                intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, galleryDetail)
                                startActivity(intent)
                            }, modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .weight(1F)
                        ) {
                            Text(text = readButtonText)
                        }
                    }
                }
            }
        }
        mGalleryInfo?.let { gi ->
            if (mAction == ACTION_GALLERY_INFO) {
                binding.content.header.run {
                    thumbUrl = gi.thumb!!
                    setTitle(EhUtils.getSuitableTitle(gi))
                    uploaderText = gi.uploader.orEmpty()
                    categoryText = EhUtils.getCategory(gi.category).toUpperCase(Locale.current)
                    updateDownloadText()
                }
            }
        }
    }

    private fun updateFavoriteDrawable() {
        val gd = mGalleryDetail ?: return
        _binding ?: return
        binding.content.actions.run {
            lifecycleScope.launchIO {
                val containLocalFav = EhDB.containLocalFavorites(gd.gid)
                withUIContext {
                    if (gd.isFavorited || containLocalFav) {
                        heart.visibility = View.VISIBLE
                        if (gd.favoriteName == null) {
                            heart.setText(R.string.local_favorites)
                        } else {
                            heart.text = gd.favoriteName
                        }
                        heartOutline.visibility = View.GONE
                    } else {
                        heart.visibility = View.GONE
                        heartOutline.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun bindViewSecond() {
        val gd = mGalleryDetail ?: return
        if (mPage != 0) {
            Snackbar.make(
                requireActivity().findViewById(R.id.snackbar),
                getString(R.string.read_from, mPage),
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.read) {
                    val intent = Intent(requireContext(), ReaderActivity::class.java)
                    intent.action = ReaderActivity.ACTION_EH
                    intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, mGalleryDetail)
                    intent.putExtra(ReaderActivity.KEY_PAGE, mPage)
                    startActivity(intent)
                }
                .show()
        }
        _binding ?: return
        val resources = resources
        thumbUrl = gd.thumb!!
        setTitle(EhUtils.getSuitableTitle(gd))
        uploaderText = gd.uploader.orEmpty()
        categoryText = EhUtils.getCategory(gd.category).toUpperCase(Locale.current)
        updateDownloadText()
        /*
        binding.content.header.run {
            language.text = gd.language
            pages.text = resources.getQuantityString(R.plurals.page_count, gd.pages, gd.pages)
            size.text = gd.size
            posted.text = gd.posted
            favoredTimes.text = resources.getString(R.string.favored_times, gd.favoriteCount)
        }
         */
        if (gd.newerVersions.size != 0) {
            binding.content.actions.newerVersion.visibility = View.VISIBLE
        }
        binding.content.actions.run {
            ratingText.text = getAllRatingText(gd.rating, gd.ratingCount)
            rating.rating = gd.rating
            updateFavoriteDrawable()
            torrent.text = resources.getString(R.string.torrent_count, gd.torrentCount)
        }
        bindTags(gd.tags)
        bindComments(gd.comments!!.comments)
        bindPreviews(gd)
    }

    private fun bindTags(tagGroups: Array<GalleryTagGroup>?) {
        context ?: return
        val inflater = layoutInflater
        _binding ?: return
        binding.content.tags.run {
            tags.removeViews(1, tags.childCount - 1)
            if (tagGroups.isNullOrEmpty()) {
                noTags.visibility = View.VISIBLE
                return
            } else {
                noTags.visibility = View.GONE
            }
        }
        tagGroups ?: return
        val ehTags =
            if (Settings.showTagTranslations && isTranslatable(requireContext())) EhTagDatabase else null
        val colorTag = theme.resolveColor(R.attr.tagBackgroundColor)
        val colorName = theme.resolveColor(R.attr.tagGroupBackgroundColor)
        for (tg in tagGroups) {
            val ll = inflater.inflate(
                R.layout.gallery_tag_group,
                binding.content.tags.tags,
                false
            ) as LinearLayout
            ll.orientation = LinearLayout.HORIZONTAL
            binding.content.tags.tags.addView(ll)
            var readableTagName: String? = null
            if (ehTags != null && ehTags.isInitialized()) {
                readableTagName = ehTags.getTranslation(tag = tg.groupName)
            }
            val tgName = inflater.inflate(R.layout.item_gallery_tag, ll, false) as TextView
            ll.addView(tgName)
            tgName.text = readableTagName ?: tg.groupName
            tgName.backgroundTintList = ColorStateList.valueOf(colorName)
            val prefix = namespaceToPrefix(tg.groupName!!)
            val awl = AutoWrapLayout(context)
            ll.addView(
                awl,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tg.forEach {
                val tag = inflater.inflate(R.layout.item_gallery_tag, awl, false) as TextView
                awl.addView(tag)
                val tagStr = if (it.startsWith('_')) {
                    val alpha = ColorUtils.setAlphaComponent(tag.currentTextColor, 128)
                    tag.setTextColor(alpha)
                    it.substring(1)
                } else {
                    it
                }
                var readableTag: String? = null
                if (ehTags != null && ehTags.isInitialized()) {
                    readableTag = ehTags.getTranslation(prefix, tagStr)
                }
                tag.text = readableTag ?: tagStr
                tag.backgroundTintList = ColorStateList.valueOf(colorTag)
                tag.setTag(R.id.tag, tg.groupName + ":" + tagStr)
                tag.setOnClickListener(this)
                tag.setOnLongClickListener(this)
            }
        }
    }

    private fun bindComments(commentsList: Array<GalleryComment>?) {
        context ?: return
        val inflater = layoutInflater
        _binding ?: return
        binding.content.comments.run {
            comments.removeViews(0, comments.childCount - 1)
            val maxShowCount = 2
            if (commentsList.isNullOrEmpty()) {
                commentsText.setText(R.string.no_comments)
                return
            } else if (commentsList.size <= maxShowCount) {
                commentsText.setText(R.string.no_more_comments)
            } else {
                commentsText.setText(R.string.more_comment)
            }
            val length = maxShowCount.coerceAtMost(commentsList.size)
            for (i in 0 until length) {
                val comment = commentsList[i]
                val v = inflater.inflate(R.layout.item_gallery_comment, comments, false)
                comments.addView(v, i)
                val user = v.findViewById<TextView>(R.id.user)
                user.text = comment.user
                user.setBackgroundColor(Color.TRANSPARENT)
                val time = v.findViewById<TextView>(R.id.time)
                time.text = ReadableTime.getTimeAgo(comment.time)
                val c = v.findViewById<ObservedTextView>(R.id.comment)
                c.maxLines = 5
                c.text = Html.fromHtml(
                    comment.comment, Html.FROM_HTML_MODE_LEGACY,
                    URLImageGetter(c), null
                )
                v.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun bindPreviews(gd: GalleryDetail) {
        _binding ?: return
        val previewList = gd.previewList
        headerAdapter?.text = getString(R.string.more_previews)
        if (gd.previewPages <= 0 || previewList.isEmpty()) {
            footerAdapter?.text = getString(R.string.no_previews)
            return
        } else if (gd.previewPages == 1) {
            footerAdapter?.text = getString(R.string.no_more_previews)
        } else {
            footerAdapter?.text = getString(R.string.more_previews)
        }
        previewsAdapter?.submitList(previewList)
    }

    private fun getAllRatingText(rating: Float, ratingCount: Int): String {
        return getString(
            R.string.rating_text,
            getString(getRatingText(rating)),
            rating,
            ratingCount
        )
    }

    private fun showSimilarGalleryList() {
        val gd = mGalleryDetail ?: return
        val keyword = EhUtils.extractTitle(gd.title)
        if (null != keyword) {
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_NORMAL
            lub.keyword = "\"" + keyword + "\""
            navigate(R.id.galleryListScene, lub.toStartArgs(), true)
            return
        }
        val artist = getArtist(gd.tags)
        if (null != artist) {
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_TAG
            lub.keyword = "artist:$artist"
            navigate(R.id.galleryListScene, lub.toStartArgs(), true)
            return
        }
        if (null != gd.uploader) {
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_UPLOADER
            lub.keyword = gd.uploader
            navigate(R.id.galleryListScene, lub.toStartArgs(), true)
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun showCoverGalleryList() {
        val context = context ?: return
        val gid = gid
        if (-1L == gid) {
            return
        }
        try {
            val key = getPreviewThumbKey(mGalleryInfo!!.thumb!!)
            val path = imageLoader(context).diskCache!![key]!!.use { it.data }
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_IMAGE_SEARCH
            lub.imagePath = path.toString()
            lub.isUseSimilarityScan = true
            navigate(R.id.galleryListScene, lub.toStartArgs(), true)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun navigateToPreview(nextPage: Boolean = false) {
        mGalleryDetail?.let {
            val args = Bundle()
            args.putParcelable(GalleryPreviewsScene.KEY_GALLERY_DETAIL, it)
            args.putBoolean(GalleryPreviewsScene.KEY_NEXT_PAGE, nextPage)
            navigate(R.id.galleryPreviewsScene, args)
        }
    }

    override fun onClick(v: View) {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity) {
            return
        }
        if (binding.tip === v) {
            if (request()) {
                adjustViewVisibility(STATE_REFRESH)
            }
            return
        }

        val galleryDetail = mGalleryDetail ?: return
        when (v) {
            binding.content.actions.newerVersion -> {
                val titles = ArrayList<CharSequence>()
                for (newerVersion in galleryDetail.newerVersions) {
                    titles.add(
                        getString(
                            R.string.newer_version_title,
                            newerVersion.title,
                            newerVersion.posted
                        )
                    )
                }
                BaseDialogBuilder(requireContext())
                    .setItems(titles.toTypedArray()) { _: DialogInterface?, which: Int ->
                        val newerVersion = galleryDetail.newerVersions[which]
                        val args = Bundle()
                        args.putString(KEY_ACTION, ACTION_GID_TOKEN)
                        args.putLong(KEY_GID, newerVersion.gid)
                        args.putString(KEY_TOKEN, newerVersion.token)
                        navigate(R.id.galleryDetailScene, args)
                    }
                    .show()
            }

            binding.content.actions.heart,
            binding.content.actions.heartOutline -> {
                lifecycleScope.launchIO {
                    if (!mModifyingFavorites) {
                        var remove = false
                        val containLocalFavorites = EhDB.containLocalFavorites(galleryDetail.gid)
                        if (containLocalFavorites || galleryDetail.isFavorited) {
                            mModifyingFavorites = true
                            CommonOperations.removeFromFavorites(
                                activity, galleryDetail,
                                ModifyFavoritesListener(context, true)
                            )
                            remove = true
                        }
                        withUIContext {
                            if (!remove) {
                                mModifyingFavorites = true
                                CommonOperations.addToFavorites(
                                    activity, galleryDetail,
                                    ModifyFavoritesListener(context, false)
                                )
                            }
                            // Update UI
                            updateFavoriteDrawable()
                        }
                    }
                }
            }

            binding.content.actions.share -> {
                galleryDetailUrl?.let {
                    AppHelper.share(activity, it)
                }
            }

            binding.content.actions.torrent -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    val helper = TorrentListDialogHelper()
                    val dialog: Dialog = BaseDialogBuilder(context)
                        .setTitle(R.string.torrents)
                        .setView(R.layout.dialog_torrent_list)
                        .setOnDismissListener(helper)
                        .show()
                    helper.setDialog(dialog, galleryDetail.torrentUrl)
                }
            }

            binding.content.actions.archive -> {
                if (galleryDetail.apiUid < 0) {
                    showTip(R.string.sign_in_first, LENGTH_LONG)
                    return
                }
                val helper = ArchiveListDialogHelper()
                val dialog: Dialog = BaseDialogBuilder(context)
                    .setTitle(R.string.settings_download)
                    .setView(R.layout.dialog_archive_list)
                    .setOnDismissListener(helper)
                    .show()
                helper.setDialog(dialog, galleryDetail.archiveUrl)
            }

            binding.content.actions.rate -> {
                if (galleryDetail.apiUid < 0) {
                    showTip(R.string.sign_in_first, LENGTH_LONG)
                    return
                }
                val helper = RateDialogHelper()
                val dialog: Dialog = BaseDialogBuilder(context)
                    .setTitle(R.string.rate)
                    .setView(R.layout.dialog_rate)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, helper)
                    .show()
                helper.setDialog(dialog, galleryDetail.rating)
            }

            binding.content.actions.similar -> {
                showSimilarGalleryList()
            }

            binding.content.actions.searchCover -> {
                showCoverGalleryList()
            }

            binding.content.comments.comments -> {
                val args = Bundle()
                args.putLong(GalleryCommentsScene.KEY_API_UID, galleryDetail.apiUid)
                args.putString(GalleryCommentsScene.KEY_API_KEY, galleryDetail.apiKey)
                args.putLong(GalleryCommentsScene.KEY_GID, galleryDetail.gid)
                args.putString(GalleryCommentsScene.KEY_TOKEN, galleryDetail.token)
                args.putParcelable(GalleryCommentsScene.KEY_COMMENT_LIST, galleryDetail.comments)
                args.putParcelable(GalleryCommentsScene.KEY_GALLERY_DETAIL, galleryDetail)
                navigate(R.id.galleryCommentsScene, args)
            }

            else -> {
                val o = v.getTag(R.id.tag)
                if (o is String) {
                    val lub = ListUrlBuilder()
                    lub.mode = ListUrlBuilder.MODE_TAG
                    lub.keyword = o
                    navigate(R.id.galleryListScene, lub.toStartArgs(), true)
                    return
                }
            }
        }
    }

    private fun showFilterTagDialog(tag: String) {
        val context = context ?: return
        BaseDialogBuilder(context)
            .setMessage(getString(R.string.filter_the_tag, tag))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                val filter = Filter()
                filter.mode = EhFilter.MODE_TAG
                filter.text = tag
                EhFilter.addFilter(filter)
                showTip(R.string.filter_added, LENGTH_SHORT)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTagDialog(tv: TextView, tag: String) {
        val context = context ?: return
        val temp: String
        val index = tag.indexOf(':')
        temp = if (index >= 0) {
            tag.substring(index + 1)
        } else {
            tag
        }
        val menu: MutableList<String> = ArrayList()
        val menuId = IntList()
        val resources = context.resources
        menu.add(resources.getString(android.R.string.copy))
        menuId.add(R.id.copy)
        if (temp != tv.text.toString()) {
            menu.add(resources.getString(R.string.copy_trans))
            menuId.add(R.id.copy_trans)
        }
        menu.add(resources.getString(R.string.show_definition))
        menuId.add(R.id.show_definition)
        menu.add(resources.getString(R.string.add_filter))
        menuId.add(R.id.add_filter)
        if (mGalleryDetail != null && mGalleryDetail!!.apiUid >= 0) {
            menu.add(resources.getString(R.string.tag_vote_up))
            menuId.add(R.id.vote_up)
            menu.add(resources.getString(R.string.tag_vote_down))
            menuId.add(R.id.vote_down)
        }
        BaseDialogBuilder(context)
            .setTitle(tag)
            .setItems(menu.toTypedArray()) { _: DialogInterface?, which: Int ->
                if (which < 0 || which >= menuId.size) {
                    return@setItems
                }
                when (menuId[which]) {
                    R.id.vote_up -> {
                        voteTag(tag, 1)
                    }

                    R.id.vote_down -> {
                        voteTag(tag, -1)
                    }

                    R.id.show_definition -> {
                        UrlOpener.openUrl(context, EhUrl.getTagDefinitionUrl(temp), false)
                    }

                    R.id.add_filter -> {
                        showFilterTagDialog(tag)
                    }

                    R.id.copy -> {
                        requireActivity().addTextToClipboard(tag, false)
                    }

                    R.id.copy_trans -> {
                        requireActivity().addTextToClipboard(tv.text.toString(), false)
                    }
                }
            }.show()
    }

    private fun voteTag(tag: String, vote: Int) {
        val context = context
        val activity = mainActivity
        if (null == context || null == activity) {
            return
        }
        val request = EhRequest()
            .setMethod(EhClient.METHOD_VOTE_TAG)
            .setArgs(
                mGalleryDetail!!.apiUid,
                mGalleryDetail!!.apiKey!!,
                mGalleryDetail!!.gid,
                mGalleryDetail!!.token!!,
                tag,
                vote
            )
            .setCallback(VoteTagListener(context))
        request.enqueue(this)
    }

    override fun onLongClick(v: View): Boolean {
        val activity = mainActivity ?: return false
        if (binding.content.actions.heart === v || binding.content.actions.heartOutline === v) {
            lifecycleScope.launchIO {
                if (mGalleryDetail != null && !mModifyingFavorites) {
                    var remove = false
                    if (EhDB.containLocalFavorites(mGalleryDetail!!.gid) || mGalleryDetail!!.isFavorited) {
                        mModifyingFavorites = true
                        CommonOperations.removeFromFavorites(
                            activity, mGalleryDetail!!,
                            ModifyFavoritesListener(activity, true)
                        )
                        remove = true
                    }
                    withUIContext {
                        if (!remove) {
                            mModifyingFavorites = true
                            CommonOperations.addToFavorites(
                                activity, mGalleryDetail!!,
                                ModifyFavoritesListener(activity, false), true
                            )
                        }
                        // Update UI
                        updateFavoriteDrawable()
                    }
                }
            }
        } else {
            val tag = v.getTag(R.id.tag) as? String
            if (null != tag) {
                showTagDialog(v as TextView, tag)
                return true
            }
        }
        return false
    }

    private fun updateDownloadText() {
        _binding ?: return
        downloadButtonText = when (mDownloadState) {
            DownloadInfo.STATE_INVALID -> getString(R.string.download)
            DownloadInfo.STATE_NONE -> getString(R.string.download_state_none)
            DownloadInfo.STATE_WAIT -> getString(R.string.download_state_wait)
            DownloadInfo.STATE_DOWNLOAD -> getString(R.string.download_state_downloading)
            DownloadInfo.STATE_FINISH -> getString(R.string.download_state_downloaded)
            DownloadInfo.STATE_FAILED -> getString(R.string.download_state_failed)
            else -> throw IllegalArgumentException()
        }
    }

    private fun updateDownloadState() {
        val context = context
        val gid = gid
        if (null == context || -1L == gid) {
            return
        }
        val downloadState = com.hippo.ehviewer.download.DownloadManager.getDownloadState(gid)
        if (downloadState == mDownloadState) {
            return
        }
        mDownloadState = downloadState
        updateDownloadText()
    }

    override fun onAdd(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
        updateDownloadState()
    }

    override fun onUpdate(info: DownloadInfo, list: List<DownloadInfo>) {
        updateDownloadState()
    }

    override fun onUpdateAll() {
        updateDownloadState()
    }

    override fun onReload() {
        updateDownloadState()
    }

    override fun onChange() {
        updateDownloadState()
    }

    override fun onRemove(info: DownloadInfo, list: List<DownloadInfo>, position: Int) {
        updateDownloadState()
    }

    override fun onRenameLabel(from: String, to: String) {}
    override fun onUpdateLabels() {}
    private fun onGetGalleryDetailSuccess(result: GalleryDetail) {
        mGalleryDetail = result
        updateDownloadState()
        adjustViewVisibility(STATE_NORMAL)
        bindViewSecond()
    }

    private fun onGetGalleryDetailFailure(e: Exception) {
        e.printStackTrace()
        _binding ?: return
        val context = context
        if (null != context) {
            val error = ExceptionUtils.getReadableString(e)
            binding.tip.text = error
            adjustViewVisibility(STATE_FAILED)
        }
    }

    private fun onRateGallerySuccess(result: RateGalleryParser.Result) {
        if (mGalleryDetail != null) {
            mGalleryDetail!!.rating = result.rating
            mGalleryDetail!!.ratingCount = result.ratingCount
        }

        // Update UI
        _binding ?: return
        binding.content.actions.run {
            ratingText.text = getAllRatingText(result.rating, result.ratingCount)
            rating.rating = result.rating
        }
    }

    private fun onModifyFavoritesSuccess(addOrRemove: Boolean) {
        mModifyingFavorites = false
        if (mGalleryDetail != null) {
            mGalleryDetail!!.isFavorited = !addOrRemove && mGalleryDetail!!.favoriteName != null
            updateFavoriteDrawable()
        }
    }

    private fun onModifyFavoritesFailure() {
        mModifyingFavorites = false
    }

    private fun onModifyFavoritesCancel() {
        mModifyingFavorites = false
    }

    @IntDef(STATE_INIT, STATE_NORMAL, STATE_REFRESH, STATE_REFRESH_HEADER, STATE_FAILED)
    @Retention(AnnotationRetention.SOURCE)
    private annotation class State
    private class VoteTagListener(context: Context) :
        EhCallback<GalleryDetailScene?, VoteTagParser.Result>(context) {
        override fun onSuccess(result: VoteTagParser.Result) {
            if (!result.error.isNullOrEmpty()) {
                showTip(result.error, LENGTH_SHORT)
            } else {
                showTip(R.string.tag_vote_successfully, LENGTH_SHORT)
            }
        }

        override fun onFailure(e: Exception) {
            showTip(R.string.vote_failed, LENGTH_LONG)
        }

        override fun onCancel() {}
    }

    private class DownloadArchiveListener(
        context: Context,
        private val mGalleryInfo: GalleryInfo?
    ) : EhCallback<GalleryDetailScene?, String?>(context) {
        override fun onSuccess(result: String?) {
            // TODO: Don't use buggy system download service
            val r = DownloadManager.Request(Uri.parse(result))
            val name =
                mGalleryInfo!!.gid.toString() + "-" + EhUtils.getSuitableTitle(mGalleryInfo) + ".zip"
            r.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                FileUtils.sanitizeFilename(name)
            )
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val dm = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            try {
                dm.enqueue(r)
            } catch (e: Throwable) {
                e.printStackTrace()
                ExceptionUtils.throwIfFatal(e)
            }
            showTip(R.string.download_archive_started, LENGTH_SHORT)
        }

        override fun onFailure(e: Exception) {
            if (e is NoHAtHClientException) {
                showTip(R.string.download_archive_failure_no_hath, LENGTH_LONG)
            } else {
                showTip(R.string.download_archive_failure, LENGTH_LONG)
            }
        }

        override fun onCancel() {}
    }

    private inner class DeleteDialogHelper(
        private val mDownloadManager: com.hippo.ehviewer.download.DownloadManager?,
        private val mGalleryInfo: GalleryInfo, private val mBuilder: CheckBoxDialogBuilder
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }

            // Delete
            mDownloadManager?.deleteDownload(mGalleryInfo.gid)

            // Delete image files
            val checked = mBuilder.isChecked
            Settings.putRemoveImageFiles(checked)
            if (checked) {
                val file = SpiderDen.getGalleryDownloadDir(mGalleryInfo.gid)
                EhDB.removeDownloadDirname(mGalleryInfo.gid)
                lifecycleScope.launchIO {
                    runCatching {
                        file?.delete()
                    }
                }
            }
        }
    }

    private inner class GetGalleryDetailListener(context: Context) :
        EhCallback<GalleryDetailScene?, GalleryDetail>(context) {
        override fun onSuccess(result: GalleryDetail) {
            application.removeGlobalStuff(this)

            // Put gallery detail to cache
            galleryDetailCache.put(result.gid, result)

            // Add history
            lifecycleScope.launchIO { EhDB.putHistoryInfo(result) }

            // Notify success
            val scene = this@GalleryDetailScene
            scene.onGetGalleryDetailSuccess(result)
        }

        override fun onFailure(e: Exception) {
            application.removeGlobalStuff(this)
            val scene = this@GalleryDetailScene
            scene.onGetGalleryDetailFailure(e)
        }

        override fun onCancel() {
            application.removeGlobalStuff(this)
        }
    }

    private inner class RateGalleryListener(
        context: Context
    ) : EhCallback<GalleryDetailScene?, RateGalleryParser.Result>(context) {
        override fun onSuccess(result: RateGalleryParser.Result) {
            showTip(R.string.rate_successfully, LENGTH_SHORT)
            val scene = this@GalleryDetailScene
            scene.onRateGallerySuccess(result)
        }

        override fun onFailure(e: Exception) {
            e.printStackTrace()
            showTip(R.string.rate_failed, LENGTH_LONG)
        }

        override fun onCancel() {}
    }

    private inner class ModifyFavoritesListener(
        context: Context,
        private val mAddOrRemove: Boolean
    ) :
        EhCallback<GalleryDetailScene?, Unit>(context) {
        override fun onSuccess(result: Unit) {
            showTip(
                if (mAddOrRemove) R.string.remove_from_favorite_success else R.string.add_to_favorite_success,
                LENGTH_SHORT
            )
            val scene = this@GalleryDetailScene
            scene.onModifyFavoritesSuccess(mAddOrRemove)
        }

        override fun onFailure(e: Exception) {
            showTip(
                if (mAddOrRemove) R.string.remove_from_favorite_failure else R.string.add_to_favorite_failure,
                LENGTH_LONG
            )
            val scene = this@GalleryDetailScene
            scene.onModifyFavoritesFailure()
        }

        override fun onCancel() {
            val scene = this@GalleryDetailScene
            scene.onModifyFavoritesCancel()
        }
    }

    private inner class ArchiveListDialogHelper : AdapterView.OnItemClickListener,
        DialogInterface.OnDismissListener, EhClient.Callback<ArchiveParser.Result> {
        private var mProgressView: CircularProgressIndicator? = null
        private var mErrorText: TextView? = null
        private var mListView: ListView? = null
        private var mRequest: EhRequest? = null
        private var mDialog: Dialog? = null
        fun setDialog(dialog: Dialog?, url: String?) {
            mDialog = dialog
            mProgressView = ViewUtils.`$$`(dialog, R.id.progress) as CircularProgressIndicator
            mErrorText = ViewUtils.`$$`(dialog, R.id.text) as TextView
            mListView = ViewUtils.`$$`(dialog, R.id.list_view) as ListView
            mListView!!.onItemClickListener = this
            val context = context
            if (context != null) {
                if (mArchiveList == null) {
                    mErrorText!!.visibility = View.GONE
                    mListView!!.visibility = View.GONE
                    mRequest = EhRequest().setMethod(EhClient.METHOD_ARCHIVE_LIST)
                        .setArgs(url!!, mGid, mToken)
                        .setCallback(this)
                    mRequest!!.enqueue(this@GalleryDetailScene)
                } else {
                    bind(mArchiveList, mCurrentFunds)
                }
            }
        }

        private fun bind(data: List<ArchiveParser.Archive>?, funds: HomeParser.Funds?) {
            if (null == mDialog || null == mProgressView || null == mErrorText || null == mListView) {
                return
            }
            if (data.isNullOrEmpty()) {
                mProgressView!!.visibility = View.GONE
                mErrorText!!.visibility = View.VISIBLE
                mListView!!.visibility = View.GONE
                mErrorText!!.setText(R.string.no_archives)
            } else {
                val nameArray = data.stream().map {
                    it.run {
                        if (isHAtH) {
                            val costStr =
                                if (cost == "Free") resources.getString(R.string.archive_free) else cost
                            "[H@H] $name [$size] [$costStr]"
                        } else {
                            val nameStr =
                                resources.getString(if (res == "org") R.string.archive_original else R.string.archive_resample)
                            val costStr =
                                if (cost == "Free!") resources.getString(R.string.archive_free) else cost
                            "$nameStr [$size] [$costStr]"
                        }
                    }
                }.toArray()
                mProgressView!!.visibility = View.GONE
                mErrorText!!.visibility = View.GONE
                mListView!!.visibility = View.VISIBLE
                mListView!!.adapter =
                    ArrayAdapter(mDialog!!.context, R.layout.item_select_dialog, nameArray)
                if (funds != null) {
                    var fundsGP = funds.fundsGP.toString()
                    // Ex GP numbers are rounded down to the nearest thousand
                    if (EhUtils.isExHentai) {
                        fundsGP += "+"
                    }
                    mDialog!!.setTitle(getString(R.string.current_funds, fundsGP, funds.fundsC))
                }
            }
        }

        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            val context = context
            val activity = mainActivity
            if (null != context && null != activity && null != mArchiveList && position < mArchiveList!!.size) {
                if (mArchiveList!![position].name == "Insufficient Funds") {
                    showTip(R.string.insufficient_funds, LENGTH_SHORT)
                    return
                } else if (mCurrentFunds != null) {
                    val cost = ParserUtils.parseInt(
                        mArchiveList!![position].cost.removeSuffix("GP").removeSuffix("Credits"), 0
                    )
                    if (cost > maxOf(mCurrentFunds!!.fundsGP, mCurrentFunds!!.fundsC)) {
                        showTip(R.string.insufficient_funds, LENGTH_SHORT)
                        return
                    }
                }
                val res = mArchiveList!![position].res
                val isHAtH = mArchiveList!![position].isHAtH
                val request = EhRequest()
                request.setMethod(EhClient.METHOD_DOWNLOAD_ARCHIVE)
                request.setArgs(
                    mGalleryDetail!!.gid,
                    mGalleryDetail!!.token!!,
                    mArchiveFormParamOr!!,
                    res,
                    isHAtH
                )
                request.setCallback(DownloadArchiveListener(context, mGalleryDetail))
                request.enqueue(this@GalleryDetailScene)
            }
            if (mDialog != null) {
                mDialog!!.dismiss()
                mDialog = null
            }
        }

        override fun onDismiss(dialog: DialogInterface) {
            if (mRequest != null) {
                mRequest!!.cancel()
                mRequest = null
            }
            mDialog = null
            mProgressView = null
            mErrorText = null
            mListView = null
        }

        override fun onSuccess(result: ArchiveParser.Result) {
            if (mRequest != null) {
                mRequest = null
                mArchiveFormParamOr = result.paramOr
                mArchiveList = result.archiveList
                mCurrentFunds = result.funds
                bind(result.archiveList, result.funds)
            }
        }

        override fun onFailure(e: Exception) {
            mRequest = null
            val context = context
            if (null != context && null != mProgressView && null != mErrorText && null != mListView) {
                mProgressView!!.visibility = View.GONE
                mErrorText!!.visibility = View.VISIBLE
                mListView!!.visibility = View.GONE
                mErrorText!!.text = ExceptionUtils.getReadableString(e)
            }
        }

        override fun onCancel() {
            mRequest = null
        }
    }

    private inner class TorrentListDialogHelper : AdapterView.OnItemClickListener,
        DialogInterface.OnDismissListener, EhClient.Callback<List<TorrentParser.Result>> {
        private var mProgressView: CircularProgressIndicator? = null
        private var mErrorText: TextView? = null
        private var mListView: ListView? = null
        private var mRequest: EhRequest? = null
        private var mDialog: Dialog? = null
        fun setDialog(dialog: Dialog?, url: String?) {
            mDialog = dialog
            mProgressView = ViewUtils.`$$`(dialog, R.id.progress) as CircularProgressIndicator
            mErrorText = ViewUtils.`$$`(dialog, R.id.text) as TextView
            mListView = ViewUtils.`$$`(dialog, R.id.list_view) as ListView
            mListView!!.onItemClickListener = this
            val context = context
            if (context != null) {
                if (mTorrentList == null) {
                    mErrorText!!.visibility = View.GONE
                    mListView!!.visibility = View.GONE
                    mRequest = EhRequest().setMethod(EhClient.METHOD_GET_TORRENT_LIST)
                        .setArgs(url!!, mGid, mToken)
                        .setCallback(this)
                    mRequest!!.enqueue(this@GalleryDetailScene)
                } else {
                    bind(mTorrentList)
                }
            }
        }

        private fun bind(data: List<TorrentParser.Result>?) {
            if (null == mDialog || null == mProgressView || null == mErrorText || null == mListView) {
                return
            }
            if (data.isNullOrEmpty()) {
                mProgressView!!.visibility = View.GONE
                mErrorText!!.visibility = View.VISIBLE
                mListView!!.visibility = View.GONE
                mErrorText!!.setText(R.string.no_torrents)
            } else {
                val nameArray = data.stream().map { torrent: TorrentParser.Result ->
                    torrent.format { id: Int ->
                        resources.getString(id)
                    }
                }.toArray()
                mProgressView!!.visibility = View.GONE
                mErrorText!!.visibility = View.GONE
                mListView!!.visibility = View.VISIBLE
                mListView!!.adapter =
                    ArrayAdapter(mDialog!!.context, R.layout.item_select_dialog, nameArray)
            }
        }

        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            val context = context
            if (null != context && null != mTorrentList && position < mTorrentList!!.size) {
                val url = mTorrentList!![position].url()
                val name = mTorrentList!![position].name()
                // TODO: Don't use buggy system download service
                val r =
                    DownloadManager.Request(Uri.parse(url.replace("exhentai.org", "ehtracker.org")))
                r.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    FileUtils.sanitizeFilename("$name.torrent")
                )
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                r.addRequestHeader("Cookie", EhCookieStore.getCookieHeader(url.toHttpUrl()))
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                try {
                    dm.enqueue(r)
                    showTip(R.string.download_torrent_started, LENGTH_SHORT)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    ExceptionUtils.throwIfFatal(e)
                    showTip(R.string.download_torrent_failure, LENGTH_SHORT)
                }
            }
            if (mDialog != null) {
                mDialog!!.dismiss()
                mDialog = null
            }
        }

        override fun onDismiss(dialog: DialogInterface) {
            if (mRequest != null) {
                mRequest!!.cancel()
                mRequest = null
            }
            mDialog = null
            mProgressView = null
            mErrorText = null
            mListView = null
        }

        override fun onSuccess(result: List<TorrentParser.Result>) {
            if (mRequest != null) {
                mRequest = null
                mTorrentList = result
                bind(result)
            }
        }

        override fun onFailure(e: Exception) {
            mRequest = null
            val context = context
            if (null != context && null != mProgressView && null != mErrorText && null != mListView) {
                mProgressView!!.visibility = View.GONE
                mErrorText!!.visibility = View.VISIBLE
                mListView!!.visibility = View.GONE
                mErrorText!!.text = ExceptionUtils.getReadableString(e)
            }
        }

        override fun onCancel() {
            mRequest = null
        }
    }

    private inner class RateDialogHelper : OnUserRateListener, DialogInterface.OnClickListener {
        private var mRatingBar: GalleryRatingBar? = null
        private var mRatingText: TextView? = null
        fun setDialog(dialog: Dialog?, rating: Float) {
            mRatingText = ViewUtils.`$$`(dialog, R.id.rating_text) as TextView
            mRatingBar = ViewUtils.`$$`(dialog, R.id.rating_view) as GalleryRatingBar
            mRatingText!!.setText(getRatingText(rating))
            mRatingBar!!.rating = rating
            mRatingBar!!.setOnUserRateListener(this)
        }

        override fun onUserRate(rating: Float) {
            if (null != mRatingText) {
                mRatingText!!.setText(getRatingText(rating))
            }
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            val context = context
            val activity = mainActivity
            if (null == context || null == activity || which != DialogInterface.BUTTON_POSITIVE || null == mGalleryDetail || null == mRatingBar) {
                return
            }
            val request = EhRequest()
                .setMethod(EhClient.METHOD_GET_RATE_GALLERY)
                .setArgs(
                    mGalleryDetail!!.apiUid, mGalleryDetail!!.apiKey!!,
                    mGalleryDetail!!.gid, mGalleryDetail!!.token!!, mRatingBar!!.rating
                )
                .setCallback(
                    RateGalleryListener(
                        context
                    )
                )
            request.enqueue(this@GalleryDetailScene)
        }
    }

    companion object {
        const val KEY_ACTION = "action"
        const val ACTION_GALLERY_INFO = "action_gallery_info"
        const val ACTION_GID_TOKEN = "action_gid_token"
        const val KEY_GALLERY_INFO = "gallery_info"
        const val KEY_GID = "gid"
        const val KEY_TOKEN = "token"
        const val KEY_PAGE = "page"
        private const val STATE_INIT = -1
        private const val STATE_NORMAL = 0
        private const val STATE_REFRESH = 1
        private const val STATE_REFRESH_HEADER = 2
        private const val STATE_FAILED = 3
        private const val KEY_GALLERY_DETAIL = "gallery_detail"
        private const val KEY_REQUEST_ID = "request_id"
        private const val TRANSITION_ANIMATION_DISABLED = true
        private fun getArtist(tagGroups: Array<GalleryTagGroup>?): String? {
            if (null == tagGroups) {
                return null
            }
            for (tagGroup in tagGroups) {
                if ("artist" == tagGroup.groupName && tagGroup.size > 0) {
                    return tagGroup[0].removePrefix("_")
                }
            }
            return null
        }
    }
}