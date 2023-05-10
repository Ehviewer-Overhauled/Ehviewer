package com.hippo.widget

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hippo.widget.pullrefresh.PullRefreshIndicator
import com.hippo.widget.pullrefresh.pullRefresh
import com.hippo.widget.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching

@Composable
inline fun <reified T : Parcelable> ContentLayout(
    state: ContentState<T>,
    noinline getData: suspend (Int) -> List<T>,
    modifier: Modifier = Modifier,
    content: @Composable (Array<T>) -> Unit,
) {
    var refreshing by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshing = true
        if (state.startPage == 0) {
            coroutineScope.launch {
                val result = runSuspendCatching { getData(0) }.getOrNull()
                result?.let {
                    state.data.clear()
                    state.data.addAll(it)
                }
            }.invokeOnCompletion { refreshing = false }
        } else {
            TODO()
        }
    })
    Box(modifier = Modifier.pullRefresh(refreshState)) {
        content(state.data.toTypedArray())
        PullRefreshIndicator(refreshing = refreshing, state = refreshState, modifier = modifier.align(Alignment.TopCenter))
    }
}

class ContentState<T>(
    val data: SnapshotStateList<T>,
    _startPage: MutableState<Int>,
    _endPage: MutableState<Int>,
) {
    var startPage by _startPage
    var endPage by _endPage
}

@Composable
fun <T> rememberContentState(
    initialData: List<T>,
): ContentState<T> {
    val snapshotStateListSaver = remember {
        listSaver(
            save = { it.toList() },
            restore = { mutableStateListOf<T>().apply { addAll(it) } },
        )
    }
    val data = rememberSaveable(saver = snapshotStateListSaver) { mutableStateListOf<T>().apply { addAll(initialData) } }
    val startPage = rememberSaveable { mutableStateOf(0) }
    val endPage = rememberSaveable { mutableStateOf(0) }
    return remember { ContentState(data, startPage, endPage) }
}
