package com.hippo.ehviewer.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.text.parseAsHtml
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.LICENSE_SCREEN
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.tools.DialogState
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberDialogState
import com.hippo.ehviewer.ui.tools.toAnnotatedString
import com.hippo.ehviewer.updater.AppUpdater
import com.hippo.ehviewer.updater.Release
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.installPackage
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import java.io.File

private const val REPO_URL = "https://github.com/${BuildConfig.REPO_NAME}"
private const val RELEASE_URL = "$REPO_URL/releases"

@Composable
@Stable
private fun versionCode() = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + stringResource(R.string.settings_about_build_time, BuildConfig.BUILD_TIME)

@Composable
@Stable
private fun author() = stringResource(R.string.settings_about_author_summary).replace('$', '@').parseAsHtml().toAnnotatedString()

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val dialogState = rememberDialogState()
    dialogState.Handler()
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_about)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState())) {
            Preference(
                title = stringResource(id = R.string.settings_about_declaration),
                summary = stringResource(id = R.string.settings_about_declaration_summary),
            )
            HtmlPreference(
                title = stringResource(id = R.string.settings_about_author),
                summary = author(),
            )
            UrlPreference(
                title = stringResource(id = R.string.settings_about_latest_release),
                url = RELEASE_URL,
            )
            UrlPreference(
                title = stringResource(id = R.string.settings_about_source),
                url = REPO_URL,
            )
            Preference(title = stringResource(id = R.string.license)) {
                navController.navigate(LICENSE_SCREEN)
            }
            Preference(
                title = stringResource(id = R.string.settings_about_version),
                summary = versionCode(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.use_ci_update_channel),
                value = Settings::useCIUpdateChannel,
            )
            SimpleMenuPreferenceInt2(
                title = stringResource(id = R.string.auto_updates),
                entry = R.array.update_frequency,
                entryValueRes = R.array.update_frequency_values,
                value = Settings::updateIntervalDays.observed,
            )
            WorkPreference(title = stringResource(id = R.string.settings_about_check_for_updates)) {
                runSuspendCatching {
                    AppUpdater.checkForUpdate(true)?.let {
                        dialogState.showNewVersion(context, it)
                    } ?: launchSnackBar(context.getString(R.string.already_latest_version))
                }.onFailure {
                    launchSnackBar(ExceptionUtils.getReadableString(it))
                }
            }
            Spacer(modifier = Modifier.size(paddingValues.calculateBottomPadding()))
        }
    }
}

suspend fun DialogState.showNewVersion(context: Context, release: Release) {
    val download = show(
        confirmText = R.string.download,
        dismissText = android.R.string.cancel,
        title = R.string.new_version_available,
    ) {
        Column {
            Text(
                text = release.version,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = release.changelog)
        }
    }
    // TODO: Download in the background and show progress in notification
    if (download) {
        val file = File(AppConfig.tempDir, "update.apk").apply { delete() }
        AppUpdater.downloadUpdate(release.downloadLink, file)
        withUIContext { context.installPackage(file) }
    }
}
