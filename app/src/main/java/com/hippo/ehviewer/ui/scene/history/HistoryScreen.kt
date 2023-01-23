package com.hippo.ehviewer.ui.scene.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import eu.kanade.tachiyomi.util.lang.launchIO

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
            Modifier.padding(paddingValues)
        ) {
            items(
                items = historyData,
                key = { item -> item.gid }
            ) {
                it?.let {
                    Text(text = it.title!!)
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
