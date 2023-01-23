package com.hippo.ehviewer.ui.scene.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import coil.compose.AsyncImage
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.widget.SimpleRatingView
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.system.pxToDp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(toggleDrawer: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val historyData = remember {
        Pager(
            PagingConfig(20)
        ) {
            EhDB.getHistoryLazyList()
        }.flow.cachedIn(coroutineScope)
    }.collectAsLazyPagingItems()

    var clearAllDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.history))
                },
                navigationIcon = {
                    IconButton(onClick = { toggleDrawer() }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "")
                    }
                },
                actions = {
                    IconButton(onClick = { clearAllDialog = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.v_clear_all_dark_x24),
                            contentDescription = ""
                        )
                    }
                }
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
                    InfoCard(info = it)
                }
            }
        }

        if (historyData.itemCount == 0) {
            NoHistory()
        }
    }

    if (clearAllDialog) {
        AlertDialog(
            onDismissRequest = { clearAllDialog = false },
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
                            clearAllDialog = false
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.clear_all))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        clearAllDialog = false
                    }
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        )
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

@Composable
private fun InfoCard(info: GalleryInfo) {
    OutlinedCard(
        modifier = Modifier.padding(6.dp),
        border = remember { BorderStroke(1.dp, Color.Transparent) }
    ) {
        val listCardSize = remember { Settings.getListThumbSize().pxToDp }
        Row(
            modifier = Modifier.height((listCardSize * 3).dp)
        ) {
            Card {
                AsyncImage(
                    model = info.thumb,
                    contentDescription = "",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .width((listCardSize * 2).dp)
                        .height((listCardSize * 3).dp)
                )
            }
            Column(Modifier.padding(8.dp, 4.dp)) {
                Text(
                    text = EhUtils.getSuitableTitle(info)!!,
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
                        AndroidView(factory = { context ->
                            SimpleRatingView(context).apply {
                                rating = info.rating
                            }
                        })
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
                            if (false) {
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
