package com.hippo.ehviewer.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun Deferred(block: suspend () -> Unit, content: @Composable () -> Unit) {
    var completed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(key1 = Unit) {
        block()
        completed = true
    }
    if (completed) content()
}
