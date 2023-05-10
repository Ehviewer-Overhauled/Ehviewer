package com.hippo.widget

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hippo.widget.pullrefresh.PullRefreshIndicator
import com.hippo.widget.pullrefresh.pullRefresh
import com.hippo.widget.pullrefresh.rememberPullRefreshState

@Composable
inline fun <reified T : Parcelable> ContentLayout(
    content: @Composable (Array<T>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var refreshing by rememberSaveable { mutableStateOf(false) }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = {
        refreshing = true
    })
    val data = rememberSaveable { mutableStateListOf<T>() }
    Box(modifier = modifier.pullRefresh(refreshState)) {
        content(data.toTypedArray())
        PullRefreshIndicator(refreshing = refreshing, state = refreshState)
    }
}
