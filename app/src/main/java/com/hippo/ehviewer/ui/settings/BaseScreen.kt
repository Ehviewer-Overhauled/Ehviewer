package com.hippo.ehviewer.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
            PreferenceHeader(
                icon = painterResource(id = R.drawable.v_sad_panda_primary_x24),
                title = R.string.settings_eh,
                childRouteName = EH_SETTINGS_SCREEN,
            )
            PreferenceHeader(
                icon = Icons.Default.Download,
                title = R.string.settings_download,
                childRouteName = DOWNLOAD_SETTINGS_SCREEN,
            )
            PreferenceHeader(
                icon = Icons.Default.Security,
                title = R.string.settings_privacy,
                childRouteName = SECURITY_SETTINGS_SCREEN,
            )
            PreferenceHeader(
                icon = Icons.Default.Adb,
                title = R.string.settings_advanced,
                childRouteName = ADVANCED_SETTINGS_SCREEN,
            )
            PreferenceHeader(
                icon = Icons.Default.Info,
                title = R.string.settings_about,
                childRouteName = ABOUT_SETTINGS_SCREEN,
            )
        }
    }
}
