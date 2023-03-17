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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import arrow.core.partially1
import coil.Coil.imageLoader
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.hippo.app.BaseDialogBuilder
import com.hippo.app.CheckBoxDialogBuilder
import com.hippo.app.EditTextDialogBuilder
import com.hippo.ehviewer.EhApplication.Companion.galleryDetailCache
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
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
import com.hippo.ehviewer.download.DownloadManager.DownloadInfoListener
import com.hippo.ehviewer.spider.SpiderDen
import com.hippo.ehviewer.spider.SpiderQueen
import com.hippo.ehviewer.spider.SpiderQueen.Companion.MODE_READ
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.GalleryInfoBottomSheet
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.scene.GalleryListScene.Companion.toStartArgs
import com.hippo.ehviewer.ui.widget.CrystalCard
import com.hippo.ehviewer.ui.widget.EhAsyncPreview
import com.hippo.ehviewer.ui.widget.GalleryDetailHeaderCard
import com.hippo.ehviewer.ui.widget.GalleryDetailRating
import com.hippo.ehviewer.widget.GalleryRatingBar
import com.hippo.ehviewer.widget.GalleryRatingBar.OnUserRateListener
import com.hippo.text.URLImageGetter
import com.hippo.util.AppHelper
import com.hippo.util.ExceptionUtils
import com.hippo.util.ReadableTime
import com.hippo.util.addTextToClipboard
import com.hippo.util.getParcelableCompat
import com.hippo.widget.ObservedTextView
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.ViewUtils
import com.hippo.yorozuya.collect.IntList
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.HttpUrl.Companion.toHttpUrl
import kotlin.math.roundToInt
import com.hippo.ehviewer.download.DownloadManager as downloadManager

class GalleryDetailScene : BaseScene(), DownloadInfoListener {
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
    private var favourite by mutableStateOf(false)
    private var favButtonText by mutableStateOf("")
    private var getDetailError by mutableStateOf("")

    private var mTorrentList: List<TorrentParser.Result>? = null
    private var requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result && composeBindingGD != null) {
            val helper = TorrentListDialogHelper()
            val dialog: Dialog = BaseDialogBuilder(requireActivity())
                .setTitle(R.string.torrents)
                .setView(R.layout.dialog_torrent_list)
                .setOnDismissListener(helper)
                .show()
            helper.setDialog(dialog, composeBindingGD!!.torrentUrl)
        }
    }
    private var mArchiveFormParamOr: String? = null
    private var mArchiveList: List<ArchiveParser.Archive>? = null
    private var mCurrentFunds: HomeParser.Funds? = null
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
        val url = galleryDetailUrl
        val activity: Activity? = mainActivity
        if (null != url && null != activity) {
            UrlOpener.openUrl(activity, url, false)
        }
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
        savedInstanceState: Bundle?
    ): View {
        // Get download state
        val gid = gid
        mDownloadState = if (gid != -1L) {
            downloadManager.getDownloadState(gid)
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
        downloadManager.addDownloadInfoListener(this)
        (requireActivity() as MainActivity).mShareUrl = galleryDetailUrl
        return ComposeView(requireContext()).apply {
            setContent {
                Mdc3Theme {
                    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                    Scaffold(
                        topBar = {
                            LargeTopAppBar(
                                title = {
                                    composeBindingGI?.let {
                                        Text(
                                            text = EhUtils.getSuitableTitle(it),
                                            maxLines = 2
                                        )
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        findNavController().popBackStack()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = null
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                                actions = {
                                    var dropdown by remember { mutableStateOf(false) }
                                    IconButton(onClick = { dropdown = !dropdown }) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = null
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = dropdown,
                                        onDismissRequest = { dropdown = false }) {
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(id = R.string.action_add_tag)) },
                                            onClick = {
                                                dropdown = false
                                                actionAddTag()
                                            })
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(id = R.string.refresh)) },
                                            onClick = {
                                                dropdown = false
                                                actionRefresh()
                                            })
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(id = R.string.open_in_other_app)) },
                                            onClick = {
                                                dropdown = false
                                                actionOpenInOtherApp()
                                            })
                                    }
                                }
                            )
                        }
                    ) {
                        Surface {
                            if (getDetailError.isNotBlank()) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.big_sad_pandroid),
                                        contentDescription = null,
                                        modifier = Modifier.clickable(onClick = ::actionRefresh)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(text = getDetailError)
                                }
                            } else {
                                val gi = composeBindingGI
                                if (gi != null) {
                                    GalleryDetailContent(
                                        galleryInfo = gi,
                                        galleryDetail = composeBindingGD,
                                        contentPadding = it,
                                        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
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
        modifier: Modifier
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Settings.thumbSizeDp),
            contentPadding = contentPadding,
            modifier = modifier.padding(horizontal = dimensionResource(id = R.dimen.keyline_margin))
        ) {
            item(span = { GridItemSpan(maxCurrentLineSpan) }) {
                Column {
                    GalleryDetailHeaderCard(
                        galleryInfo = galleryInfo,
                        galleryDetail = galleryDetail,
                        onInfoCardClick = ::onGalleryInfoCardClick,
                        onCategoryChipClick = ::onCategoryChipClick,
                        onUploaderChipClick = ::onUploaderChipClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.keyline_margin))
                    )
                    Row {
                        FilledTonalButton(
                            onClick = ::onDownloadButtonClick,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .weight(1F)
                        ) {
                            Text(text = downloadButtonText)
                        }
                        Button(
                            onClick = ::onReadButtonClick,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .weight(1F)
                        ) {
                            Text(text = readButtonText)
                        }
                    }
                    if (galleryDetail != null) {
                        BelowHeader(galleryDetail)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
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

    private fun LazyGridScope.galleryDetailPreview(gd: GalleryDetail) {
        val previewList = gd.previewList
        item {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        onClick = ::navigateToPreview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.6666667F)
                    ) {}
                    Text(stringResource(R.string.more_previews))
                }
                Text("")
            }
        }
        items(previewList) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        onClick = {
                            mainActivity?.startReaderActivity(gd, it.position)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.6666667F)
                    ) {
                        EhAsyncPreview(
                            model = it,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Text((it.position + 1).toString())
            }
        }
        item {
            val footerText = if (gd.previewPages <= 0 || previewList.isEmpty()) {
                stringResource(R.string.no_previews)
            } else if (gd.previewPages == 1) {
                stringResource(R.string.no_more_previews)
            } else {
                stringResource(R.string.more_previews)
            }
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        onClick = ::navigateToPreview.partially1(true),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.6666667F)
                    ) {}
                    Text(footerText)
                }
                Text("")
            }
        }
    }

    @Composable
    private fun BelowHeader(galleryDetail: GalleryDetail) {
        @Composable
        fun EhIconButton(
            icon: ImageVector,
            text: String,
            onClick: () -> Unit,
        ) {
            OutlinedButton(
                onClick = onClick,
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = text)
            }
        }

        @Composable
        fun EhAccentIconButton(
            icon: ImageVector,
            text: String,
            onClick: () -> Unit,
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSecondaryContainer),
                contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = text)
            }
        }
        if (galleryDetail.newerVersions.isNotEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                CrystalCard(
                    onClick = ::showNewerVersionDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                ) {}
                Text(text = stringResource(id = R.string.newer_version_avaliable))
            }
            Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        }
        FlowRow {
            if (favourite) {
                EhAccentIconButton(
                    icon = Icons.Default.Favorite,
                    text = favButtonText,
                    onClick = ::modifyFavourite
                )
            } else {
                EhIconButton(
                    icon = Icons.Default.FavoriteBorder,
                    text = stringResource(id = R.string.not_favorited),
                    onClick = ::modifyFavourite
                )
            }
            EhIconButton(
                icon = Icons.Default.Difference,
                text = stringResource(id = R.string.similar_gallery),
                onClick = ::showSimilarGalleryList
            )
            EhIconButton(
                icon = Icons.Default.ImageSearch,
                text = stringResource(id = R.string.search_cover),
                onClick = ::showCoverGalleryList
            )
            EhIconButton(
                icon = Icons.Default.Share,
                text = stringResource(id = R.string.share),
                onClick = ::doShareGallery
            )
            EhIconButton(
                icon = Icons.Default.SwapVerticalCircle,
                text = torrentText,
                onClick = ::showTorrentDialog
            )
            EhIconButton(
                icon = Icons.Default.CloudDone,
                text = stringResource(id = R.string.archive),
                onClick = ::showArchiveDialog
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GalleryDetailRating(rating = galleryDetail.rating)
                Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
                Text(text = ratingText)
            }
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        val tags = galleryDetail.tags
        if (tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = R.string.no_tags))
            }
        } else {
            GalleryDetailTags(tags)
        }
        Spacer(modifier = Modifier.size(dimensionResource(id = R.dimen.keyline_margin)))
        if (Settings.showComments) {
            GalleryDetailComment(galleryDetail.comments?.comments)
        }
    }

    private fun onGalleryInfoCardClick() {
        val gd = composeBindingGD ?: return
        val galleryInfoBottomSheet = GalleryInfoBottomSheet(gd)
        galleryInfoBottomSheet.show(
            requireActivity().supportFragmentManager,
            GalleryInfoBottomSheet.TAG
        )
    }

    private fun onCategoryChipClick() {
        val category = category
        if (category == EhUtils.NONE || category == EhUtils.PRIVATE || category == EhUtils.UNKNOWN) {
            return
        }
        val lub = ListUrlBuilder()
        lub.category = category
        navigate(R.id.galleryListScene, lub.toStartArgs(), true)
    }

    private fun onUploaderChipClick() {
        if (uploader.isNullOrEmpty() || disowned) {
            return
        }
        val lub = ListUrlBuilder()
        lub.mode = ListUrlBuilder.MODE_UPLOADER
        lub.keyword = uploader
        navigate(R.id.galleryListScene, lub.toStartArgs(), true)
    }

    private fun onDownloadButtonClick() {
        val galleryDetail = composeBindingGD ?: return
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
    }

    private fun onReadButtonClick() {
        val galleryDetail = composeBindingGD ?: return
        val intent = Intent(activity, ReaderActivity::class.java)
        intent.action = ReaderActivity.ACTION_EH
        intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, galleryDetail)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        downloadManager.removeDownloadInfoListener(this)
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
            }.onSuccess {
                galleryDetailCache.put(it.gid, it)
                EhDB.putHistoryInfo(it)
                composeBindingGD = it
                composeBindingGI = it
                updateDownloadState()
                bindViewSecond()
            }.onFailure {
                it.printStackTrace()
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
            val containLocalFav = EhDB.containLocalFavorites(gd.gid)
            if (gd.isFavorited || containLocalFav) {
                favourite = true
                favButtonText = gd.favoriteName ?: getString(R.string.local_favorites)
            } else {
                favourite = false
            }
        }
    }

    private fun modifyFavourite() {
        val galleryDetail = composeBindingGD ?: return
        lifecycleScope.launchIO {
            if (!mModifyingFavorites) {
                var remove = false
                val containLocalFavorites =
                    EhDB.containLocalFavorites(galleryDetail.gid)
                if (containLocalFavorites || galleryDetail.isFavorited) {
                    mModifyingFavorites = true
                    CommonOperations.removeFromFavorites(
                        activity, galleryDetail,
                        ModifyFavoritesListener(requireContext(), true)
                    )
                    remove = true
                }
                withUIContext {
                    if (!remove) {
                        mModifyingFavorites = true
                        CommonOperations.addToFavorites(
                            requireActivity(), galleryDetail,
                            ModifyFavoritesListener(requireContext(), false)
                        )
                    }
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

    private fun showArchiveDialog() {
        val galleryDetail = composeBindingGD ?: return
        if (galleryDetail.apiUid < 0) {
            showTip(R.string.sign_in_first, LENGTH_LONG)
            return
        }
        val helper = ArchiveListDialogHelper()
        val dialog: Dialog = BaseDialogBuilder(requireContext())
            .setTitle(R.string.settings_download)
            .setView(R.layout.dialog_archive_list)
            .setOnDismissListener(helper)
            .show()
        helper.setDialog(dialog, galleryDetail.archiveUrl)
    }

    private fun bindViewSecond() {
        val gd = composeBindingGD ?: return
        if (mPage != 0) {
            Snackbar.make(
                requireActivity().findViewById(R.id.snackbar),
                getString(R.string.read_from, mPage),
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.read) {
                    val intent = Intent(requireContext(), ReaderActivity::class.java)
                    intent.action = ReaderActivity.ACTION_EH
                    intent.putExtra(ReaderActivity.KEY_GALLERY_INFO, composeBindingGD)
                    intent.putExtra(ReaderActivity.KEY_PAGE, mPage)
                    startActivity(intent)
                }
                .show()
        }
        composeBindingGI = gd
        composeBindingGD = gd
        updateDownloadText()
        updateFavoriteDrawable()
        ratingText = getAllRatingText(gd.rating, gd.ratingCount)
        torrentText = resources.getString(R.string.torrent_count, gd.torrentCount)
    }

    @Composable
    private fun GalleryDetailTags(tagGroups: Array<GalleryTagGroup>) {
        @Composable
        fun baseRoundText(
            text: String,
            weak: Boolean = false,
            isGroup: Boolean = false,
            modifier: Modifier = Modifier
        ) {
            val bgColor = if (isGroup) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
            Surface(
                modifier = Modifier.padding(4.dp),
                color = bgColor,
                shape = RoundedCornerShape(64.dp)
            ) {
                Text(
                    text = text,
                    modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.let { if (weak) it.copy(0.5F) else it },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        val canTranslate = Settings.showTagTranslations && isTranslatable(requireContext())
        val ehTags = EhTagDatabase.takeIf { canTranslate }
        fun String.translate(): String {
            return ehTags?.takeIf { it.isInitialized() }?.getTranslation(tag = this) ?: this
        }

        fun String.translate(prefix: String?): String {
            return ehTags?.takeIf { it.isInitialized() }
                ?.getTranslation(prefix = prefix, tag = this) ?: this
        }

        tagGroups.forEach {
            Row {
                it.groupName?.run {
                    baseRoundText(
                        text = translate(),
                        isGroup = true
                    )
                    val prefix = namespaceToPrefix(this)
                    FlowRow {
                        it.forEach {
                            val weak = it.startsWith('_')
                            val real = it.removePrefix("_")
                            val translated = real.translate(prefix)
                            fun onClick() {
                                val lub = ListUrlBuilder()
                                lub.mode = ListUrlBuilder.MODE_TAG
                                lub.keyword = real
                                navigate(R.id.galleryListScene, lub.toStartArgs(), true)
                            }

                            fun onLongClick() {
                                showTagDialog(translated, real)
                            }
                            baseRoundText(
                                text = translated,
                                weak = weak,
                                modifier = Modifier.combinedClickable(
                                    onClick = ::onClick,
                                    onLongClick = ::onLongClick
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onNavigateToCommentScene() {
        val galleryDetail = composeBindingGD ?: return
        val args = Bundle()
        args.putLong(GalleryCommentsScene.KEY_API_UID, galleryDetail.apiUid)
        args.putString(
            GalleryCommentsScene.KEY_API_KEY,
            galleryDetail.apiKey
        )
        args.putLong(GalleryCommentsScene.KEY_GID, galleryDetail.gid)
        args.putString(GalleryCommentsScene.KEY_TOKEN, galleryDetail.token)
        args.putParcelable(
            GalleryCommentsScene.KEY_COMMENT_LIST,
            galleryDetail.comments
        )
        args.putParcelable(
            GalleryCommentsScene.KEY_GALLERY_DETAIL,
            galleryDetail
        )
        navigate(R.id.galleryCommentsScene, args)
    }

    @SuppressLint("InflateParams")
    @Composable
    private fun GalleryDetailComment(commentsList: Array<GalleryComment>?) {
        val maxShowCount = 2
        val commentText = if (commentsList.isNullOrEmpty()) stringResource(R.string.no_comments)
        else if (commentsList.size <= maxShowCount) stringResource(R.string.no_more_comments)
        else stringResource(R.string.more_comment)
        CrystalCard(onClick = ::onNavigateToCommentScene) {
            if (commentsList != null) {
                val length = maxShowCount.coerceAtMost(commentsList.size)
                for (i in 0 until length) {
                    val comment = commentsList[i]
                    AndroidView(factory = {
                        LayoutInflater.from(it).inflate(R.layout.item_gallery_comment, null, false)
                            .apply {
                                val user = findViewById<TextView>(R.id.user)
                                user.text = comment.user
                                user.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                val time = findViewById<TextView>(R.id.time)
                                time.text = ReadableTime.getTimeAgo(comment.time)
                                val c = findViewById<ObservedTextView>(R.id.comment)
                                c.maxLines = 5
                                c.text = Html.fromHtml(
                                    comment.comment, Html.FROM_HTML_MODE_LEGACY,
                                    URLImageGetter(c), null
                                )
                            }
                    })
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
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
            ratingCount
        )
    }

    private fun showSimilarGalleryList() {
        val gd = composeBindingGD ?: return
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
            val key = getPreviewThumbKey(composeBindingGI!!.thumb!!)
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
        composeBindingGD?.let {
            val args = Bundle()
            args.putParcelable(GalleryPreviewsScene.KEY_GALLERY_DETAIL, it)
            args.putBoolean(GalleryPreviewsScene.KEY_NEXT_PAGE, nextPage)
            navigate(R.id.galleryPreviewsScene, args)
        }
    }

    private fun showTorrentDialog() {
        val galleryDetail = composeBindingGD ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            val helper = TorrentListDialogHelper()
            val dialog: Dialog = BaseDialogBuilder(requireContext())
                .setTitle(R.string.torrents)
                .setView(R.layout.dialog_torrent_list)
                .setOnDismissListener(helper)
                .show()
            helper.setDialog(dialog, galleryDetail.torrentUrl)
        }
    }

    private fun showRateDialog() {
        val galleryDetail = composeBindingGD ?: return
        if (galleryDetail.apiUid < 0) {
            showTip(R.string.sign_in_first, LENGTH_LONG)
            return
        }
        val helper = RateDialogHelper()
        val dialog: Dialog = BaseDialogBuilder(requireContext())
            .setTitle(R.string.rate)
            .setView(R.layout.dialog_rate)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, helper)
            .show()
        helper.setDialog(dialog, galleryDetail.rating)
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
                        UrlOpener.openUrl(context, EhUrl.getTagDefinitionUrl(temp), false)
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
        val context = context
        val activity = mainActivity
        if (null == context || null == activity) {
            return
        }
        val request = EhRequest()
            .setMethod(EhClient.METHOD_VOTE_TAG)
            .setArgs(
                composeBindingGD!!.apiUid,
                composeBindingGD!!.apiKey!!,
                composeBindingGD!!.gid,
                composeBindingGD!!.token!!,
                tag,
                vote
            )
            .setCallback(VoteTagListener(context))
        request.enqueue(this)
    }

    private fun downloadLongClick() {
        val galleryInfo = composeBindingGI
        if (galleryInfo != null) {
            CommonOperations.startDownload(activity as MainActivity, galleryInfo, true)
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
                val filter = Filter()
                filter.mode = EhFilter.MODE_UPLOADER
                filter.text = uploader
                EhFilter.addFilter(filter)
                showTip(R.string.filter_added, LENGTH_SHORT)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun modifyFavouriteLongClick() {
        lifecycleScope.launchIO {
            if (composeBindingGD != null && !mModifyingFavorites) {
                var remove = false
                if (EhDB.containLocalFavorites(composeBindingGD!!.gid) || composeBindingGD!!.isFavorited) {
                    mModifyingFavorites = true
                    CommonOperations.removeFromFavorites(
                        activity, composeBindingGD!!,
                        ModifyFavoritesListener(requireActivity(), true)
                    )
                    remove = true
                }
                withUIContext {
                    if (!remove) {
                        mModifyingFavorites = true
                        CommonOperations.addToFavorites(
                            requireActivity(), composeBindingGD!!,
                            ModifyFavoritesListener(requireActivity(), false), true
                        )
                    }
                    // Update UI
                    updateFavoriteDrawable()
                }
            }
        }
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

    private fun onRateGallerySuccess(result: RateGalleryParser.Result) {
        composeBindingGD?.apply {
            rating = result.rating
            ratingCount = result.ratingCount
        }
        composeBindingGD = composeBindingGD
        ratingText = getAllRatingText(result.rating, result.ratingCount)
    }

    private fun onModifyFavoritesSuccess(addOrRemove: Boolean) {
        mModifyingFavorites = false
        if (composeBindingGD != null) {
            composeBindingGD!!.isFavorited = !addOrRemove && composeBindingGD!!.favoriteName != null
            updateFavoriteDrawable()
        }
    }

    private fun onModifyFavoritesFailure() {
        mModifyingFavorites = false
    }

    private fun onModifyFavoritesCancel() {
        mModifyingFavorites = false
    }

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
                    composeBindingGD!!.gid,
                    composeBindingGD!!.token!!,
                    mArchiveFormParamOr!!,
                    res,
                    isHAtH
                )
                request.setCallback(DownloadArchiveListener(context, composeBindingGD))
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
            if (null == context || null == activity || which != DialogInterface.BUTTON_POSITIVE || null == composeBindingGD || null == mRatingBar) {
                return
            }
            val request = EhRequest()
                .setMethod(EhClient.METHOD_GET_RATE_GALLERY)
                .setArgs(
                    composeBindingGD!!.apiUid, composeBindingGD!!.apiKey!!,
                    composeBindingGD!!.gid, composeBindingGD!!.token!!, mRatingBar!!.rating
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