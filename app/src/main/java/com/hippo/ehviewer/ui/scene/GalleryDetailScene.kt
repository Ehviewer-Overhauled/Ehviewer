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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.DownloadManager
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.text.parseAsHtml
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import arrow.core.partially1
import coil.imageLoader
import com.google.android.material.snackbar.Snackbar
import com.hippo.ehviewer.EhApplication.Companion.galleryDetailCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.FavouriteStatusRouter
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhFilter.remember
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryComment
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED
import com.hippo.ehviewer.client.data.GalleryTagGroup
import com.hippo.ehviewer.client.data.ListUrlBuilder
import com.hippo.ehviewer.client.exception.NoHAtHClientException
import com.hippo.ehviewer.client.parser.ArchiveParser
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.client.parser.ParserUtils
import com.hippo.ehviewer.client.parser.TorrentResult
import com.hippo.ehviewer.coil.imageRequest
import com.hippo.ehviewer.coil.justDownload
import com.hippo.ehviewer.dao.DownloadInfo
import com.hippo.ehviewer.dao.Filter
import com.hippo.ehviewer.dao.FilterMode
import com.hippo.ehviewer.databinding.DialogArchiveListBinding
import com.hippo.ehviewer.databinding.DialogRateBinding
import com.hippo.ehviewer.databinding.DialogTorrentListBinding
import com.hippo.ehviewer.databinding.ItemGalleryCommentBinding
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.MODE_READ
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.addToFavorites
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.legacy.CheckBoxDialogBuilder
import com.hippo.ehviewer.ui.legacy.EditTextDialogBuilder
import com.hippo.ehviewer.ui.legacy.GalleryRatingBar.OnUserRateListener
import com.hippo.ehviewer.ui.legacy.URLImageGetter
import com.hippo.ehviewer.ui.legacy.calculateSuitableSpanCount
import com.hippo.ehviewer.ui.main.EhPreviewItem
import com.hippo.ehviewer.ui.main.GalleryDetailErrorTip
import com.hippo.ehviewer.ui.main.GalleryDetailHeaderCard
import com.hippo.ehviewer.ui.main.GalleryTags
import com.hippo.ehviewer.ui.navToReader
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.setMD3Content
import com.hippo.ehviewer.ui.tools.CrystalCard
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconButton
import com.hippo.ehviewer.ui.tools.FilledTertiaryIconToggleButton
import com.hippo.ehviewer.ui.tools.GalleryDetailRating
import com.hippo.ehviewer.util.AppHelper
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.util.addTextToClipboard
import com.hippo.ehviewer.util.getParcelableCompat
import com.hippo.ehviewer.yorozuya.FileUtils
import com.hippo.ehviewer.yorozuya.collect.IntList
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.HttpUrl.Companion.toHttpUrl
import splitties.systemservices.downloadManager
import kotlin.math.roundToInt
import com.hippo.ehviewer.download.DownloadManager as EhDownloadManager

class GalleryDetailScene : BaseScene() {
    private var mDownloadState = 0
    private var mAction: String? = null
    private var mGid: Long = 0
    private var mToken: String? = null
    private var mPage = 0

    private var composeBindingGI by mutableStateOf<GalleryInfo?>(null)
    private var composeBindingGD by mutableStateOf<GalleryDetail?>(null)
    private var readButtonText by mutableStateOf("")
    private var downloadButtonText by mutableStateOf("")
    private var ratingText by mutableStateOf("")
    private var torrentText by mutableStateOf("")
    private var favButtonText by mutableStateOf("")
    private var getDetailError by mutableStateOf("")

    private var mTorrentList: TorrentResult? = null
    private var requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result: Boolean ->
        if (result && composeBindingGD != null) {
            val helper = TorrentListDialogHelper()
            val binding = DialogTorrentListBinding.inflate(layoutInflater)
            val dialog: Dialog = BaseDialogBuilder(requireActivity())
                .setTitle(R.string.torrents)
                .setView(binding.root)
                .setOnDismissListener(helper)
                .show()
            helper.setDialog(dialog, binding, composeBindingGD!!.torrentUrl)
        }
    }
    private var mArchiveFormParamOr: String? = null
    private var mArchiveList: List<ArchiveParser.Archive>? = null
    private var mCurrentFunds: HomeParser.Funds? = null
    private var favoritesLock = Mutex()

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
            composeBindingGI = args.getParcelableCompat(KEY_GALLERY_INFO)
            composeBindingGI?.let { lifecycleScope.launchIO { EhDB.putHistoryInfo(it) } }
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
            if (composeBindingGD != null) {
                gid = composeBindingGD!!.gid
                token = composeBindingGD!!.token
            } else if (composeBindingGI != null) {
                gid = composeBindingGI!!.gid
                token = composeBindingGI!!.token
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
        get() = composeBindingGD?.gid ?: composeBindingGI?.gid
            ?: mGid.takeIf { mAction == ACTION_GID_TOKEN } ?: -1

    private val uploader: String?
        get() = composeBindingGD?.uploader ?: composeBindingGI?.uploader

    // Judging by the uploader to exclude the cooldown period
    private val disowned: Boolean
        get() = uploader == "(Disowned)"

    // -1 for error
    private val category: Int
        get() = composeBindingGD?.category ?: composeBindingGI?.category ?: -1

    private val dialogState = DialogState()

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
        composeBindingGI?.let {
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
        composeBindingGI = savedInstanceState.getParcelableCompat(KEY_GALLERY_INFO)
        mGid = savedInstanceState.getLong(KEY_GID)
        mToken = savedInstanceState.getString(KEY_TOKEN)
        composeBindingGD = savedInstanceState.getParcelableCompat(KEY_GALLERY_DETAIL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mAction != null) {
            outState.putString(KEY_ACTION, mAction)
        }
        if (composeBindingGI != null) {
            outState.putParcelable(KEY_GALLERY_INFO, composeBindingGI)
        }
        outState.putLong(KEY_GID, mGid)
        if (mToken != null) {
            outState.putString(KEY_TOKEN, mAction)
        }
        if (composeBindingGD != null) {
            outState.putParcelable(KEY_GALLERY_DETAIL, composeBindingGD)
        }
    }

    private fun actionOpenInOtherApp() {
        val url = galleryDetailUrl ?: return
        val activity: Activity = mainActivity ?: return
        activity.openBrowser(url)
    }

    private fun actionRefresh() {
        if (composeBindingGD == null && getDetailError == "") return
        getDetailError = ""
        composeBindingGD = null
        request()
    }

    private fun actionAddTag() {
        composeBindingGD ?: return
        if (composeBindingGD!!.apiUid < 0) {
            showTip(R.string.sign_in_first, LENGTH_LONG)
            return
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Get download state
        val gid = gid
        mDownloadState = if (gid != -1L) {
            EhDownloadManager.getDownloadState(gid)
        } else {
            DownloadInfo.STATE_INVALID
        }
        readButtonText = getString(R.string.read)
        composeBindingGI?.let { gi ->
            if (mAction == ACTION_GALLERY_INFO) {
                composeBindingGI = gi
                updateDownloadText()
            }
        }
        if (prepareData()) {
            if (composeBindingGD != null) {
                bindViewSecond()
            }
        } else {
            getDetailError = getString(R.string.error_cannot_find_gallery)
        }
        (requireActivity() as MainActivity).mShareUrl = galleryDetailUrl
        return ComposeView(requireContext()).apply {
            setMD3Content {
                LaunchedEffect(gid) {
                    EhDownloadManager.stateFlow(gid).collect {
                        updateDownloadState()
                    }
                }
                dialogState.Handler()
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                Scaffold(
                    topBar = {
                        LargeTopAppBar(
                            title = {
                                composeBindingGI?.let {
                                    Text(
                                        text = EhUtils.getSuitableTitle(it),
                                        maxLines = 2,
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    findNavController().popBackStack()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = null,
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                var dropdown by remember { mutableStateOf(false) }
                                IconButton(onClick = { dropdown = !dropdown }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = null,
                                    )
                                }
                                DropdownMenu(
                                    expanded = dropdown,
                                    onDismissRequest = { dropdown = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(id = R.string.action_add_tag)) },
                                        onClick = {
                                            dropdown = false
                                            actionAddTag()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(id = R.string.refresh)) },
                                        onClick = {
                                            dropdown = false
                                            actionRefresh()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(id = R.string.open_in_other_app)) },
                                        onClick = {
                                            dropdown = false
                                            actionOpenInOtherApp()
                                        },
                                    )
                                }
                            },
                        )
                    },
                ) {
                    Surface {
                        val gi = composeBindingGI
                        if (gi != null) {
                            GalleryDetailContent(
                                galleryInfo = gi,
                                galleryDetail = composeBindingGD,
                                contentPadding = it,
                                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun GalleryDetailContent(
        galleryInfo: GalleryInfo,
        galleryDetail: GalleryDetail?,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        var showSheet by rememberSaveable { mutableStateOf(false) }
        fun onGalleryInfoCardClick() {
            showSheet = true
        }

        val navController = remember { findNavController() }
        CompositionLocalProvider(LocalNavController provides navController) {
            composeBindingGD?.let {
                if (showSheet) {
                    GalleryInfoBottomSheet(galleryDetail = it) { showSheet = false }
                }
            }
        }
        val windowSizeClass = calculateWindowSizeClass(requireActivity())
        val columnCount = calculateSuitableSpanCount()
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Medium, WindowWidthSizeClass.Compact -> LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
            ) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        GalleryDetailHeaderCard(
                            info = galleryDetail ?: galleryInfo,
                            onInfoCardClick = ::onGalleryInfoCardClick,
                            onCategoryChipClick = ::onCategoryChipClick,
                            onUploaderChipClick = ::onUploaderChipClick,
                            onBlockUploaderIconClick = ::showFilterUploaderDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = dimensionResource(id = R.dimen.keyline_margin)),
                        )
                        Row {
                            FilledTonalButton(
                                onClick = ::onDownloadButtonClick,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .weight(1F),
                            ) {
                                Text(text = downloadButtonText)
                            }
                            Button(
                                onClick = ::onReadButtonClick,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .weight(1F),
                            ) {
                                Text(text = readButtonText)
                            }
                        }
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = ::actionRefresh)
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                if (galleryDetail != null) {
                    galleryDetailPreview(galleryDetail)
                }
            }

            WindowWidthSizeClass.Expanded -> LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                contentPadding = contentPadding,
                modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.strip_item_padding_v)),
            ) {
                item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                    LocalPinnableContainer.current!!.run { remember { pin() } }
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            GalleryDetailHeaderCard(
                                info = galleryDetail ?: galleryInfo,
                                onInfoCardClick = ::onGalleryInfoCardClick,
                                onCategoryChipClick = ::onCategoryChipClick,
                                onUploaderChipClick = ::onUploaderChipClick,
                                onBlockUploaderIconClick = ::showFilterUploaderDialog,
                                modifier = Modifier
                                    .width(dimensionResource(id = R.dimen.gallery_detail_card_landscape_width))
                                    .padding(vertical = dimensionResource(id = R.dimen.keyline_margin)),
                            )
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(modifier = modifier.height(16.dp))
                                Button(
                                    onClick = ::onReadButtonClick,
                                    modifier = Modifier
                                        .height(56.dp)
                                        .padding(horizontal = 16.dp)
                                        .width(192.dp),
                                ) {
                                    Text(text = readButtonText)
                                }
                                Spacer(modifier = modifier.height(24.dp))
                                FilledTonalButton(
                                    onClick = ::onDownloadButtonClick,
                                    modifier = Modifier
                                        .height(56.dp)
                                        .padding(horizontal = 16.dp)
                                        .width(192.dp),
                                ) {
                                    Text(text = downloadButtonText)
                                }
                            }
                        }
                        if (getDetailError.isNotBlank()) {
                            GalleryDetailErrorTip(error = getDetailError, onClick = ::actionRefresh)
                        } else if (galleryDetail != null) {
                            BelowHeader(galleryDetail)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                if (galleryDetail != null) {
                    galleryDetailPreview(galleryDetail)
                }
            }
        }
    }

    private fun LazyGridScope.galleryDetailPreview(gd: GalleryDetail) {
        val previewList = gd.previewList
        items(previewList) {
            EhPreviewItem(
                galleryPreview = it,
                onClick = { context?.navToReader(gd, it.position) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            val footerText = if (gd.previewPages <= 0 || previewList.isEmpty()) {
                stringResource(R.string.no_previews)
            } else if (gd.previewPages == 1) {
                stringResource(R.string.no_more_previews)
            } else {
                stringResource(R.string.more_previews)
            }
            TextButton(onClick = ::navigateToPreview.partially1(true)) {
                Text(footerText)
            }
        }
    }

    @Composable
    private fun BelowHeader(galleryDetail: GalleryDetail) {
        @Composable
        fun EhIconButton(
            icon: ImageVector,
            onClick: () -> Unit,
        ) = FilledTertiaryIconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        if (galleryDetail.newerVersions.isNotEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                CrystalCard(
                    onClick = ::showNewerVersionDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                ) {}
                Text(text = stringResource(id = R.string.newer_version_available))
            }
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            val favored by produceState(initialValue = false) {
                value = galleryDetail.favoriteSlot != NOT_FAVORITED
                FavouriteStatusRouter.stateFlow(galleryDetail.gid).collect { value = it != NOT_FAVORITED }
            }
            FilledTertiaryIconToggleButton(
                checked = favored,
                onCheckedChange = { modifyFavourite() },
            ) {
                Icon(
                    imageVector = if (favored) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                )
            }
            EhIconButton(
                icon = Icons.Default.Difference,
                onClick = ::showSimilarGalleryList,
            )
            EhIconButton(
                icon = Icons.Default.ImageSearch,
                onClick = ::showCoverGalleryList,
            )
            EhIconButton(
                icon = Icons.Default.Share,
                onClick = ::doShareGallery,
            )
            EhIconButton(
                icon = Icons.Default.SwapVerticalCircle,
                onClick = ::showTorrentDialog,
            )
            EhIconButton(
                icon = Icons.Default.CloudDone,
                onClick = ::showArchiveDialog,
            )
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        CrystalCard(
            onClick = ::showRateDialog,
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GalleryDetailRating(rating = galleryDetail.rating)
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                Text(text = ratingText)
            }
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        val tags = galleryDetail.tags
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(id = R.string.no_tags))
            }
        } else {
            GalleryTags(
                tagGroups = tags,
                onTagClick = {
                    val lub = ListUrlBuilder()
                    lub.mode = ListUrlBuilder.MODE_TAG
                    lub.keyword = it
                    navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
                },
                onTagLongClick = { translation, realTag ->
                    showTagDialog(translation, realTag)
                },
            )
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        if (Settings.showComments) {
            GalleryDetailComment(galleryDetail.comments.comments)
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.strip_item_padding_v)))
        }
    }

    private fun onCategoryChipClick() {
        val category = category
        if (category == EhUtils.NONE || category == EhUtils.PRIVATE || category == EhUtils.UNKNOWN) {
            return
        }
        val lub = ListUrlBuilder()
        lub.category = category
        navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
    }

    private fun onUploaderChipClick() {
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        val lub = ListUrlBuilder()
        lub.mode = ListUrlBuilder.MODE_UPLOADER
        lub.keyword = uploader
        navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
    }

    private fun onDownloadButtonClick() {
        val galleryDetail = composeBindingGD ?: return
        if (EhDownloadManager.getDownloadState(galleryDetail.gid) == DownloadInfo.STATE_INVALID) {
            CommonOperations.startDownload(
                activity as MainActivity,
                galleryDetail,
                false,
            )
        } else {
            val builder = CheckBoxDialogBuilder(
                requireContext(),
                getString(
                    R.string.download_remove_dialog_message,
                    galleryDetail.title,
                ),
                getString(R.string.download_remove_dialog_check_text),
                Settings.removeImageFiles,
            )
            val helper = DeleteDialogHelper(
                galleryDetail,
                builder,
            )
            builder.setTitle(R.string.download_remove_dialog_title)
                .setPositiveButton(android.R.string.ok, helper)
                .show()
        }
    }

    private fun onReadButtonClick() = composeBindingGI?.let { context?.navToReader(it) }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as MainActivity).mShareUrl = null
    }

    private fun prepareData(): Boolean {
        composeBindingGD?.let { return true }
        val gid = gid
        if (gid == -1L) {
            return false
        }

        // Get from cache
        composeBindingGD = galleryDetailCache[gid]
        composeBindingGD?.let { return true }
        request()
        return true
    }

    private fun request() {
        val url = galleryDetailUrl ?: return
        viewLifecycleOwner.lifecycleScope.launchIO {
            runSuspendCatching {
                EhEngine.getGalleryDetail(url)
            }.onSuccess { galleryDetail ->
                galleryDetailCache.put(galleryDetail.gid, galleryDetail)
                EhDB.putHistoryInfo(galleryDetail)
                if (Settings.preloadThumbAggressively) {
                    lifecycleScope.launchIO {
                        galleryDetail.previewList.forEach {
                            context?.run { imageLoader.enqueue(imageRequest(it) { justDownload() }) }
                        }
                    }
                }
                composeBindingGD = galleryDetail
                composeBindingGI = galleryDetail
                updateDownloadState()
                bindViewSecond()
            }.onFailure {
                getDetailError = ExceptionUtils.getReadableString(it)
            }
        }
    }

    private fun doShareGallery() {
        galleryDetailUrl?.let { AppHelper.share(requireActivity(), it) }
    }

    private fun updateFavoriteDrawable() {
        val gd = composeBindingGD ?: return
        lifecycleScope.launchIO {
            favButtonText = gd.favoriteName ?: getString(R.string.local_favorites)
        }
    }

    private fun modifyFavourite() {
        val galleryDetail = composeBindingGD ?: return
        lifecycleScope.launchIO {
            favoritesLock.withLock {
                var remove = false
                runCatching {
                    remove = !dialogState.addToFavorites(galleryDetail)
                    if (remove) {
                        showTip(R.string.remove_from_favorite_success, LENGTH_SHORT)
                    } else {
                        showTip(R.string.add_to_favorite_success, LENGTH_SHORT)
                    }
                    withUIContext {
                        onModifyFavoritesSuccess()
                    }
                }.onFailure {
                    if (it !is CancellationException) {
                        if (remove) {
                            showTip(R.string.remove_from_favorite_failure, LENGTH_LONG)
                        } else {
                            showTip(R.string.add_to_favorite_failure, LENGTH_LONG)
                        }
                    }
                }
                withUIContext {
                    // Update UI
                    updateFavoriteDrawable()
                }
            }
        }
    }

    private fun showNewerVersionDialog() {
        val galleryDetail = composeBindingGD ?: return
        val titles = ArrayList<CharSequence>()
        for (newerVersion in galleryDetail.newerVersions) {
            titles.add(
                getString(
                    R.string.newer_version_title,
                    newerVersion.title,
                    newerVersion.posted,
                ),
            )
        }
        BaseDialogBuilder(requireContext())
            .setItems(titles.toTypedArray()) { _: DialogInterface?, which: Int ->
                val newerVersion = galleryDetail.newerVersions[which]
                val args = Bundle()
                args.putString(KEY_ACTION, ACTION_GID_TOKEN)
                args.putLong(KEY_GID, newerVersion.gid)
                args.putString(KEY_TOKEN, newerVersion.token)
                navAnimated(R.id.galleryDetailScene, args)
            }
            .show()
    }

    private fun showArchiveDialog() {
        val galleryDetail = composeBindingGD ?: return
        if (galleryDetail.apiUid < 0) {
            showTip(R.string.sign_in_first, LENGTH_LONG)
            return
        }
        val helper = ArchiveListDialogHelper()
        val binding = DialogArchiveListBinding.inflate(layoutInflater)
        val dialog: Dialog = BaseDialogBuilder(requireContext())
            .setTitle(R.string.settings_download)
            .setView(binding.root)
            .setOnDismissListener(helper)
            .show()
        helper.setDialog(dialog, binding, galleryDetail.archiveUrl)
    }

    private fun bindViewSecond() {
        val gd = composeBindingGD ?: return
        context ?: return
        if (mPage != 0) {
            Snackbar.make(
                requireActivity().findViewById(R.id.snackbar),
                getString(R.string.read_from, mPage),
                Snackbar.LENGTH_LONG,
            ).setAction(R.string.read) { context?.navToReader(gd, mPage) }.show()
        }
        composeBindingGI = gd
        composeBindingGD = gd
        updateDownloadText()
        updateFavoriteDrawable()
        ratingText = getAllRatingText(gd.rating, gd.ratingCount)
        torrentText = resources.getString(R.string.torrent_count, gd.torrentCount)
    }

    private fun onNavigateToCommentScene() {
        val galleryDetail = composeBindingGD ?: return
        val args = Bundle()
        args.putLong(GalleryCommentsScene.KEY_API_UID, galleryDetail.apiUid)
        args.putString(
            GalleryCommentsScene.KEY_API_KEY,
            galleryDetail.apiKey,
        )
        args.putLong(GalleryCommentsScene.KEY_GID, galleryDetail.gid)
        args.putString(GalleryCommentsScene.KEY_TOKEN, galleryDetail.token)
        args.putParcelable(
            GalleryCommentsScene.KEY_COMMENT_LIST,
            galleryDetail.comments,
        )
        args.putParcelable(
            GalleryCommentsScene.KEY_GALLERY_DETAIL,
            galleryDetail,
        )
        navAnimated(R.id.galleryCommentsScene, args)
    }

    @SuppressLint("InflateParams")
    @Composable
    private fun GalleryDetailComment(commentsList: Array<GalleryComment>?) {
        val maxShowCount = 2
        val commentText = if (commentsList.isNullOrEmpty()) {
            stringResource(R.string.no_comments)
        } else if (commentsList.size <= maxShowCount) {
            stringResource(R.string.no_more_comments)
        } else {
            stringResource(R.string.more_comment)
        }
        CrystalCard(onClick = ::onNavigateToCommentScene) {
            if (commentsList != null) {
                val length = maxShowCount.coerceAtMost(commentsList.size)
                for (i in 0 until length) {
                    val comment = commentsList[i]
                    AndroidView(factory = {
                        ItemGalleryCommentBinding.inflate(LayoutInflater.from(it), null, false)
                            .apply {
                                user.text = comment.user
                                user.setBackgroundColor(Color.TRANSPARENT)
                                time.text = ReadableTime.getTimeAgo(comment.time)
                                this.comment.maxLines = 5
                                this.comment.text = comment.comment.orEmpty().parseAsHtml(imageGetter = URLImageGetter(this.comment))
                            }.root
                    })
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = R.dimen.strip_item_padding_v)),
                contentAlignment = Alignment.Center,
            ) {
                Text(commentText)
            }
        }
    }

    private fun getAllRatingText(rating: Float, ratingCount: Int): String {
        return getString(
            R.string.rating_text,
            getString(getRatingText(rating)),
            rating,
            ratingCount,
        )
    }

    private fun showSimilarGalleryList() {
        val gd = composeBindingGD ?: return
        val keyword = EhUtils.extractTitle(gd.title)
        if (null != keyword) {
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_NORMAL
            lub.keyword = "\"" + keyword + "\""
            navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
            return
        }
        val artist = getArtist(gd.tags)
        if (null != artist) {
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_TAG
            lub.keyword = "artist:$artist"
            navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
            return
        }
        if (null != gd.uploader) {
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_UPLOADER
            lub.keyword = gd.uploader
            navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
        }
    }

    private fun showCoverGalleryList() {
        context ?: return
        val gid = gid
        if (-1L == gid) {
            return
        }
        try {
            val key = composeBindingGI!!.thumbKey!!
            val lub = ListUrlBuilder()
            lub.mode = ListUrlBuilder.MODE_NORMAL
            lub.hash = key.substringAfterLast('/').substringBefore('-')
            navAnimated(R.id.galleryListScene, lub.toStartArgs(), true)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun navigateToPreview(nextPage: Boolean = false) {
        composeBindingGD?.let {
            val args = Bundle()
            args.putParcelable(KEY_GALLERY_DETAIL, it)
            args.putBoolean(KEY_NEXT_PAGE, nextPage)
            navAnimated(R.id.galleryPreviewsScene, args)
        }
    }

    private fun showTorrentDialog() {
        val galleryDetail = composeBindingGD ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            val helper = TorrentListDialogHelper()
            val binding = DialogTorrentListBinding.inflate(layoutInflater)
            val dialog: Dialog = BaseDialogBuilder(requireContext())
                .setTitle(R.string.torrents)
                .setView(binding.root)
                .setOnDismissListener(helper)
                .show()
            helper.setDialog(dialog, binding, galleryDetail.torrentUrl)
        }
    }

    private fun showRateDialog() {
        val galleryDetail = composeBindingGD ?: return
        if (galleryDetail.apiUid < 0) {
            showTip(R.string.sign_in_first, LENGTH_LONG)
            return
        }
        val binding = DialogRateBinding.inflate(layoutInflater)
        val helper = RateDialogHelper(binding, galleryDetail.rating)
        BaseDialogBuilder(requireContext())
            .setTitle(R.string.rate)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, helper)
            .show()
    }

    private fun showFilterTagDialog(tag: String) {
        val context = context ?: return
        BaseDialogBuilder(context)
            .setMessage(getString(R.string.filter_the_tag, tag))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                Filter(FilterMode.TAG, tag).remember()
                showTip(R.string.filter_added, LENGTH_SHORT)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTagDialog(translated: String, tag: String) {
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
        if (temp != translated) {
            menu.add(resources.getString(R.string.copy_trans))
            menuId.add(R.id.copy_trans)
        }
        menu.add(resources.getString(R.string.show_definition))
        menuId.add(R.id.show_definition)
        menu.add(resources.getString(R.string.add_filter))
        menuId.add(R.id.add_filter)
        if (composeBindingGD != null && composeBindingGD!!.apiUid >= 0) {
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
                        context.openBrowser(EhUrl.getTagDefinitionUrl(temp))
                    }

                    R.id.add_filter -> {
                        showFilterTagDialog(tag)
                    }

                    R.id.copy -> {
                        requireActivity().addTextToClipboard(tag, false)
                    }

                    R.id.copy_trans -> {
                        requireActivity().addTextToClipboard(translated, false)
                    }
                }
            }.show()
    }

    private fun voteTag(tag: String, vote: Int) {
        composeBindingGD?.run {
            lifecycleScope.launchIO {
                runSuspendCatching {
                    EhEngine.voteTag(apiUid, apiKey, gid, token, tag, vote)
                }.onSuccess { result ->
                    if (result.isNotEmpty()) {
                        showTip(result, LENGTH_SHORT)
                    } else {
                        showTip(R.string.tag_vote_successfully, LENGTH_SHORT)
                    }
                }.onFailure {
                    showTip(R.string.vote_failed, LENGTH_LONG)
                }
            }
        }
    }

    private fun showFilterUploaderDialog() {
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        val context = context
        val uploader = uploader
        if (context == null || uploader == null) {
            return
        }
        BaseDialogBuilder(context)
            .setMessage(getString(R.string.filter_the_uploader, uploader))
            .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                Filter(FilterMode.UPLOADER, uploader).remember()
                showTip(R.string.filter_added, LENGTH_SHORT)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateDownloadText() {
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
        val downloadState = EhDownloadManager.getDownloadState(gid)
        if (downloadState == mDownloadState) {
            return
        }
        mDownloadState = downloadState
        updateDownloadText()
    }

    private fun onModifyFavoritesSuccess() {
        if (composeBindingGD != null) {
            updateFavoriteDrawable()
        }
    }

    private inner class DeleteDialogHelper(
        private val mGalleryInfo: GalleryInfo,
        private val mBuilder: CheckBoxDialogBuilder,
    ) : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return
            }

            // Delete
            EhDownloadManager.deleteDownload(mGalleryInfo.gid)

            // Delete image files
            val checked = mBuilder.isChecked
            Settings.removeImageFiles = checked
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

    private inner class ArchiveListDialogHelper :
        AdapterView.OnItemClickListener,
        DialogInterface.OnDismissListener {
        private var _binding: DialogArchiveListBinding? = null
        private val binding get() = _binding!!
        private var mJob: Job? = null
        private var mDialog: Dialog? = null
        fun setDialog(dialog: Dialog?, dialogBinding: DialogArchiveListBinding, url: String?) {
            mDialog = dialog
            _binding = dialogBinding
            binding.listView.onItemClickListener = this
            val context = context
            if (context != null) {
                if (mArchiveList == null) {
                    binding.text.visibility = View.GONE
                    binding.listView.visibility = View.GONE
                    mJob = lifecycleScope.launchIO {
                        runSuspendCatching {
                            EhEngine.getArchiveList(url!!, mGid, mToken)
                        }.onSuccess { result ->
                            mArchiveFormParamOr = result.paramOr
                            mArchiveList = result.archiveList
                            mCurrentFunds = result.funds
                            withUIContext {
                                bind(result.archiveList, result.funds)
                            }
                        }.onFailure {
                            withUIContext {
                                binding.progress.visibility = View.GONE
                                binding.text.visibility = View.VISIBLE
                                binding.listView.visibility = View.GONE
                                binding.text.text = ExceptionUtils.getReadableString(it)
                            }
                        }
                        mJob = null
                    }
                } else {
                    bind(mArchiveList, mCurrentFunds)
                }
            }
        }

        private fun bind(data: List<ArchiveParser.Archive>?, funds: HomeParser.Funds?) {
            mDialog ?: return
            if (data.isNullOrEmpty()) {
                binding.progress.visibility = View.GONE
                binding.text.visibility = View.VISIBLE
                binding.listView.visibility = View.GONE
                binding.text.setText(R.string.no_archives)
            } else {
                val nameArray = data.map {
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
                }.toTypedArray()
                binding.progress.visibility = View.GONE
                binding.text.visibility = View.GONE
                binding.listView.visibility = View.VISIBLE
                binding.listView.adapter =
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
                        mArchiveList!![position].cost.removeSuffix("GP").removeSuffix("Credits"),
                        0,
                    )
                    if (cost > maxOf(mCurrentFunds!!.fundsGP, mCurrentFunds!!.fundsC)) {
                        showTip(R.string.insufficient_funds, LENGTH_SHORT)
                        return
                    }
                }
                val res = mArchiveList!![position].res
                val isHAtH = mArchiveList!![position].isHAtH
                composeBindingGD?.run {
                    lifecycleScope.launchIO {
                        runSuspendCatching {
                            EhEngine.downloadArchive(gid, token, mArchiveFormParamOr, res, isHAtH)
                        }.onSuccess { result ->
                            val r = DownloadManager.Request(Uri.parse(result))
                            val name = "$gid-" + EhUtils.getSuitableTitle(this@run) + ".zip"
                            r.setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                FileUtils.sanitizeFilename(name),
                            )
                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            runCatching {
                                downloadManager.enqueue(r)
                            }.onFailure {
                                it.printStackTrace()
                            }
                            showTip(R.string.download_archive_started, LENGTH_SHORT)
                        }.onFailure {
                            if (it is NoHAtHClientException) {
                                showTip(R.string.download_archive_failure_no_hath, LENGTH_LONG)
                            } else {
                                showTip(R.string.download_archive_failure, LENGTH_LONG)
                            }
                        }
                    }
                }
            }
            if (mDialog != null) {
                mDialog!!.dismiss()
                mDialog = null
            }
        }

        override fun onDismiss(dialog: DialogInterface) {
            mJob?.cancel()
            mJob = null
            mDialog = null
            _binding = null
        }
    }

    private inner class TorrentListDialogHelper :
        AdapterView.OnItemClickListener,
        DialogInterface.OnDismissListener {
        private var _binding: DialogTorrentListBinding? = null
        private val binding get() = _binding!!
        private var mJob: Job? = null
        private var mDialog: Dialog? = null
        fun setDialog(dialog: Dialog?, dialogBinding: DialogTorrentListBinding, url: String?) {
            mDialog = dialog
            _binding = dialogBinding
            binding.listView.onItemClickListener = this
            val context = context
            if (context != null) {
                if (mTorrentList == null) {
                    binding.text.visibility = View.GONE
                    binding.listView.visibility = View.GONE
                    mJob = lifecycleScope.launchIO {
                        runSuspendCatching {
                            EhEngine.getTorrentList(url!!, mGid, mToken)
                        }.onSuccess {
                            mTorrentList = it
                            withUIContext {
                                bind(it)
                            }
                        }.onFailure {
                            withUIContext {
                                binding.progress.visibility = View.GONE
                                binding.text.visibility = View.VISIBLE
                                binding.listView.visibility = View.GONE
                                binding.text.text = ExceptionUtils.getReadableString(it)
                            }
                        }
                        mJob = null
                    }
                } else {
                    bind(mTorrentList)
                }
            }
        }

        private fun bind(data: TorrentResult?) {
            mDialog ?: return
            if (data.isNullOrEmpty()) {
                binding.progress.visibility = View.GONE
                binding.text.visibility = View.VISIBLE
                binding.listView.visibility = View.GONE
                binding.text.setText(R.string.no_torrents)
            } else {
                val nameArray = data.map { it.format() }.toTypedArray()
                binding.progress.visibility = View.GONE
                binding.text.visibility = View.GONE
                binding.listView.visibility = View.VISIBLE
                binding.listView.adapter =
                    ArrayAdapter(mDialog!!.context, R.layout.item_select_dialog, nameArray)
            }
        }

        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            val context = context
            if (null != context && null != mTorrentList && position < mTorrentList!!.size) {
                val url = mTorrentList!![position].url
                val name = mTorrentList!![position].name
                // TODO: Don't use buggy system download service
                val r =
                    DownloadManager.Request(Uri.parse(url.replace("exhentai.org", "ehtracker.org")))
                r.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    FileUtils.sanitizeFilename("$name.torrent"),
                )
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                r.addRequestHeader("Cookie", EhCookieStore.getCookieHeader(url.toHttpUrl()))
                try {
                    downloadManager.enqueue(r)
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
            mJob?.cancel()
            mJob = null
            mDialog = null
            _binding = null
        }
    }

    private inner class RateDialogHelper(private var binding: DialogRateBinding, rating: Float) :
        OnUserRateListener,
        DialogInterface.OnClickListener {
        init {
            binding.ratingText.setText(getRatingText(rating))
            binding.ratingView.rating = rating
            binding.ratingView.setOnUserRateListener(this)
        }

        override fun onUserRate(rating: Float) {
            binding.ratingText.setText(getRatingText(rating))
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            if (which != DialogInterface.BUTTON_POSITIVE) return
            val gd = composeBindingGD ?: return
            val r = binding.ratingView.rating
            lifecycleScope.launchIO {
                runSuspendCatching {
                    EhEngine.rateGallery(gd.apiUid, gd.apiKey, gd.gid, gd.token, r)
                }.onSuccess { result ->
                    showTip(R.string.rate_successfully, LENGTH_SHORT)
                    composeBindingGD = composeBindingGD?.apply {
                        rating = result.rating
                        ratingCount = result.ratingCount
                    }
                    ratingText = getAllRatingText(result.rating, result.ratingCount)
                }.onFailure {
                    it.printStackTrace()
                    showTip(R.string.rate_failed, LENGTH_LONG)
                }
            }
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
        private const val KEY_GALLERY_DETAIL = "gallery_detail"
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
