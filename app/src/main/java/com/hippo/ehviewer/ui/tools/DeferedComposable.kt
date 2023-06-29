package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.util.lang.withIOContext

@Composable
fun <T> Deferred(block: suspend () -> T, content: @Composable (T) -> Unit) {
    var completed by remember { mutableStateOf<T?>(null) }
    LaunchedEffect(key1 = Unit) {
        completed = withIOContext { block() }
    }
    completed?.let { content(it) }
}
