package com.hippo.ehviewer.ui.settings

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import arrow.atomic.Atomic
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.LocalNavController

private const val applyJs = "javascript:(function(){var apply = document.getElementById(\"apply\").children[0];apply.click();})();"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun UConfigScreen() {
    val navController = LocalNavController.current
    val url = EhUrl.uConfigUrl
    val webview = remember { Atomic<WebView?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.u_config)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webview.get()?.loadUrl(applyJs)
                            navController.popBackStack()
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val state = rememberWebViewState(url = url)
        WebView(
            state = state,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize(),
            onCreated = {
                it.settings.run {
                    builtInZoomControls = true
                    displayZoomControls = false
                    javaScriptEnabled = true
                }
            },
            factory = { WebView(it).apply { webview.set(this) } },
        )
        val applyTip = stringResource(id = R.string.apply_tip)
        LaunchedEffect(Unit) { snackbarHostState.showSnackbar(applyTip) }
        DisposableEffect(Unit) {
            onDispose {
                EhCookieStore.flush()
            }
        }
    }
}
