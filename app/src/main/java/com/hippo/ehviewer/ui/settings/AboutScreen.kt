package com.hippo.ehviewer.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.core.text.parseAsHtml
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.ehRequest
import com.hippo.ehviewer.client.execute
import com.hippo.ehviewer.ui.LICENSE_SCREEN
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.tools.toAnnotatedString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private const val REPO_URL = "https://github.com/Ehviewer-Overhauled/Ehviewer"
private const val RELEASE_URL = "https://github.com/Ehviewer-Overhauled/Ehviewer/releases"

@Composable
@Stable
private fun versionCode() = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + stringResource(R.string.settings_about_build_time, BuildConfig.BUILD_TIME)

@Composable
@Stable
private fun author() = stringResource(R.string.settings_about_author_summary).replace('$', '@').parseAsHtml().toAnnotatedString()

@Composable
fun AboutScreen() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
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
    ) {
        Column(modifier = Modifier.padding(it).nestedScroll(scrollBehavior.nestedScrollConnection)) {
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
            WorkPreference(title = stringResource(id = R.string.settings_about_check_for_updates)) {
                if (Settings.useCIUpdateChannel) {
                    val curSha = BuildConfig.COMMIT_SHA
                    val ciUrl = "https://api.github.com/repos/Ehviewer-Overhauled/Ehviewer"
                    val branch = ehRequest(ciUrl).execute {
                        JSONObject(body.string()).getString("default_branch")
                    }
                    val branchUrl = "https://api.github.com/repos/Ehviewer-Overhauled/Ehviewer/branches/$branch"
                    val commitSha = ehRequest(branchUrl).execute {
                        JSONObject(body.string()).getJSONObject("commit").getString("sha")
                    }.take(7)
                    if (commitSha != curSha) launchSnackBar(commitSha)
                } else {
                    val curVersion = BuildConfig.VERSION_NAME
                    val releaseUrl = "https://api.github.com/repos/Ehviewer-Overhauled/Ehviewer/releases"
                    val latestVersion = ehRequest(releaseUrl).execute {
                        JSONArray(body.string())[0] as JSONObject
                    }.getString("name")
                    if (latestVersion != curVersion) launchSnackBar(latestVersion)
                }
            }
        }
    }
}
