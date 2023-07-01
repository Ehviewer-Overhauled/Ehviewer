package com.hippo.ehviewer.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.util.setDefaultSettings

@Composable
fun MyTagsScreen() {
    val navController = LocalNavController.current
    val url = EhUrl.myTagsUrl
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.my_tags)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        val state = rememberWebViewState(url = url)
        WebView(
            state = state,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize(),
            onCreated = {
                it.setDefaultSettings()
            },
        )
    }
}
