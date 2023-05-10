package com.hippo.widget

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.PagingSource
import androidx.paging.compose.LazyPagingItems
import com.hippo.widget.pullrefresh.PullRefreshIndicator
import com.hippo.widget.pullrefresh.pullRefresh
import com.hippo.widget.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
inline fun <reified T : Parcelable> ContentLayout(
    state: ContentState<T>,
    modifier: Modifier = Modifier,
    content: @Composable (LazyPagingItems<T>) -> Unit,
) {
    var refreshing by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshing = true
        if (state.startPage == 0) {
            coroutineScope.launch {
            }.invokeOnCompletion { refreshing = false }
        } else {
            TODO()
        }
    })
    Box(modifier = Modifier.pullRefresh(refreshState)) {
        PullRefreshIndicator(refreshing = refreshing, state = refreshState, modifier = modifier.align(Alignment.TopCenter))
    }
}

class ContentState<T : Any>(
    val data: PagingSource<Int, T>,
    _startPage: MutableState<Int>,
    _endPage: MutableState<Int>,
) {
    var startPage by _startPage
    var endPage by _endPage
}

@Composable
fun <T : Any> rememberContentState(
    source: PagingSource<Int, T>,
): ContentState<T> {
    val startPage = rememberSaveable { mutableStateOf(0) }
    val endPage = rememberSaveable { mutableStateOf(0) }
    return remember { ContentState(source, startPage, endPage) }
}
