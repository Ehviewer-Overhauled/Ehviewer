package com.hippo.ehviewer.ui.scene.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDismissState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.scene.HistoryComposeScreenFragmentBridge
import com.hippo.ehviewer.ui.widget.GalleryListLongClickDialog
import com.hippo.ehviewer.ui.widget.ListInfoCard
import eu.kanade.tachiyomi.util.lang.launchIO

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
            ) { info ->
                info?.let {
                    val dismissState = rememberDismissState(
                        confirmValueChange = {
                            if (it == DismissValue.DismissedToStart)
                                coroutineScope.launchIO {
                                    EhDB.deleteHistoryInfo(info)
                                }
                            true
                        }
                    )

                    SwipeToDismiss(
                        state = dismissState,
                        background = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    DismissValue.Default -> Color.Transparent
                                    DismissValue.DismissedToEnd -> Color.Green
                                    DismissValue.DismissedToStart -> Color.Red
                                }
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(color)
                            )
                        },
                        dismissContent = {
                            ListInfoCard(
                                { hostFragment.navToDetail(it) },
                                {
                                    selectedGalleryInfo = it
                                    dialogStatus = HistoryScreenDialogStatus.SELECT_ITEM
                                },
                                info = it,
                                modifier = Modifier.animateItemPlacement()
                            )
                        },
                        directions = setOf(DismissDirection.EndToStart)
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
            GalleryListLongClickDialog(info = info) {
                dialogStatus = HistoryScreenDialogStatus.NONE
            }
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

enum class HistoryScreenDialogStatus {
    NONE,
    CLEAR_ALL,
    SELECT_ITEM
}
