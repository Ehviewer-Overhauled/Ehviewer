package com.hippo.ehviewer.ui.scene.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.AsyncImage
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.scene.HistoryComposeScreenFragmentBridge
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.pxToDp
import java.util.Locale

val downloadManager = EhApplication.downloadManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(hostFragment: HistoryComposeScreenFragmentBridge) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val historyData = remember {
        Pager(
            PagingConfig(20)
        ) {
            EhDB.getHistoryLazyList()
        }.flow.cachedIn(coroutineScope)
    }.collectAsLazyPagingItems()

    var dialogStatus by remember { mutableStateOf(HistoryScreenDialogStatus.NONE) }
    var selectedGalleryInfo by remember { mutableStateOf<GalleryInfo?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.history))
                },
                navigationIcon = {
                    IconButton(onClick = { hostFragment.toggleNavigationDrawer() }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "")
                    }
                },
                actions = {
                    IconButton(onClick = { dialogStatus = HistoryScreenDialogStatus.CLEAR_ALL }) {
                        Icon(
                            painter = painterResource(id = R.drawable.v_clear_all_dark_x24),
                            contentDescription = ""
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues
        ) {
            items(
                items = historyData,
                key = { item -> item.gid }
            ) {
                it?.let {
                    InfoCard(
                        { hostFragment.navToDetail(it) },
                        {
                            selectedGalleryInfo = it
                            dialogStatus = HistoryScreenDialogStatus.SELECT_ITEM
                        },
                        info = it,
                    )
                }
            }
        }

        if (historyData.itemCount == 0) {
            NoHistory()
        }
    }

    if (dialogStatus == HistoryScreenDialogStatus.CLEAR_ALL) {
        AlertDialog(
            onDismissRequest = { dialogStatus = HistoryScreenDialogStatus.NONE },
            title = {
                Text(text = stringResource(id = R.string.clear_all))
            },
            text = {
                Text(text = stringResource(id = R.string.clear_all_history))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launchIO {
                            EhDB.clearHistoryInfo()
                            dialogStatus = HistoryScreenDialogStatus.NONE
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogStatus = HistoryScreenDialogStatus.NONE
                    }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    if (dialogStatus == HistoryScreenDialogStatus.SELECT_ITEM) {
        selectedGalleryInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { dialogStatus = HistoryScreenDialogStatus.NONE },
                title = {
                    Text(
                        text = EhUtils.getSuitableTitle(info),
                        maxLines = 2,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hostFragment.navToDetail(info) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.v_book_open_x24),
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.read),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
private fun NoHistory() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.big_history),
            contentDescription = null,
            Modifier.padding(16.dp),
        )
        Text(
            text = stringResource(id = R.string.no_history),
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    info: GalleryInfo,
) {
    OutlinedCard(
        modifier = Modifier.padding(6.dp),
        border = remember { BorderStroke(1.dp, Color.Transparent) }
    ) {
        val listCardSize = remember { Settings.getListThumbSize().pxToDp }
        Row(
            modifier = Modifier
                .height((listCardSize * 3).dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
        ) {
            Card {
                AsyncImage(
                    model = info.thumb,
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width((listCardSize * 2).dp)
                        .height((listCardSize * 3).dp)
                )
            }
            Column(Modifier.padding(8.dp, 4.dp)) {
                Text(
                    text = EhUtils.getSuitableTitle(info),
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(
                            text = info.uploader ?: "(DISOWNED)",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                        ComposeSimpleRatingView(rating = info.rating)
                        Text(
                            text = EhUtils.getCategory(info.category).uppercase(Locale.ROOT),
                            modifier = Modifier
                                .background(
                                    Color(
                                        EhUtils.getCategoryColor(
                                            info.category
                                        )
                                    )
                                )
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (remember { downloadManager.containDownloadInfo(info.gid) }) {
                                Icon(
                                    painterResource(id = R.drawable.v_download_x16),
                                    contentDescription = null
                                )
                            }
                            if (info.favoriteSlot != -2) {
                                Icon(
                                    painterResource(id = R.drawable.v_heart_x16),
                                    contentDescription = null
                                )
                            }
                            Text(
                                text = info.simpleLanguage.orEmpty(),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text(
                            text = info.posted.orEmpty(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComposeSimpleRatingView(rating: Float) {
    val r = (rating * 2).toInt().coerceAtLeast(0).coerceAtMost(10)
    val fullStar = r.floorDiv(2)
    val halfStar = r % 2
    val outlineStar = 5 - fullStar - halfStar
    val colorYellow800 = Color(0xfff9a825)
    Row {
        repeat(fullStar) {
            Icon(
                painter = painterResource(id = R.drawable.v_star_x16),
                contentDescription = null,
                tint = colorYellow800
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.rating_interval)))
        }
        repeat(halfStar) {
            Icon(
                painter = painterResource(id = R.drawable.v_star_half_x16),
                contentDescription = null,
                tint = colorYellow800
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.rating_interval)))
        }
        repeat(outlineStar) {
            Icon(
                painter = painterResource(id = R.drawable.v_star_outline_x16),
                contentDescription = null,
                tint = colorYellow800
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.rating_interval)))
        }
    }
}

enum class HistoryScreenDialogStatus {
    NONE,
    CLEAR_ALL,
    SELECT_ITEM
}
