package com.hippo.ehviewer.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R

@Composable
fun BaseScreen() {
    val activity = LocalContext.current as Activity
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.padding(it)) {
        }
    }
}
