package com.hippo.ehviewer.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import my.nanihadesuka.compose.InternalLazyColumnScrollbar

@Composable
fun LazyColumnWithScrollBar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical,
    content: LazyListScope.() -> Unit,
) = Box(
    modifier = modifier,
) {
    LazyColumn(
        modifier = Modifier,
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement,
        content = content,
    )
    Box(modifier = Modifier.padding(contentPadding)) {
        InternalLazyColumnScrollbar(
            listState = state,
            thumbColor = MaterialTheme.colorScheme.primary,
            thumbSelectedColor = MaterialTheme.colorScheme.primary,
        )
    }
}
