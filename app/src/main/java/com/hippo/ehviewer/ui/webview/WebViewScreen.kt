package com.hippo.ehviewer.ui.webview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.util.setDefaultSettings

@Composable
fun WebViewScreen(url: String, onNavigateUp: () -> Unit) {
    val state = rememberWebViewState(url = url)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.pageTitle ?: url) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        WebView(
            state = state,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize(),
            onCreated = { it.setDefaultSettings() },
        )
        DisposableEffect(Unit) {
            onDispose {
                EhCookieStore.flush()
            }
        }
    }
}
