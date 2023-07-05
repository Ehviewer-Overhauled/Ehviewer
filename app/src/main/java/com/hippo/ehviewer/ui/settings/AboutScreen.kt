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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.text.parseAsHtml
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.execute
import com.hippo.ehviewer.ui.LICENSE_SCREEN
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.tools.rememberDialogState
import com.hippo.ehviewer.ui.tools.toAnnotatedString
import com.hippo.ehviewer.util.ExceptionUtils
import com.hippo.ehviewer.util.installPackage
import com.hippo.ehviewer.util.iter
import eu.kanade.tachiyomi.util.lang.withUIContext
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.Request
import okio.sink
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

private const val REPO_URL = "https://github.com/Ehviewer-Overhauled/Ehviewer"
private const val API_URL = "https://api.github.com/repos/Ehviewer-Overhauled/Ehviewer"
private const val RELEASE_URL = "https://github.com/Ehviewer-Overhauled/Ehviewer/releases"

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
        Column(modifier = Modifier.padding(paddingValues).nestedScroll(scrollBehavior.nestedScrollConnection)) {
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
                runSuspendCatching {
                    if (Settings.useCIUpdateChannel) {
                        val curSha = BuildConfig.COMMIT_SHA
                        val branch = ghRequest(API_URL).execute {
                            JSONObject(body.string()).getString("default_branch")
                        }
                        val branchUrl = "$API_URL/branches/$branch"
                        val commitSha = ghRequest(branchUrl).execute {
                            JSONObject(body.string()).getJSONObject("commit").getString("sha")
                        }
                        if (commitSha.take(7) != curSha) {
                            val archiveUrl = ghRequest("$API_URL/actions/artifacts").execute {
                                JSONObject(body.string()).getJSONArray("artifacts").iter<JSONObject>().find {
                                    val wf = it.getJSONObject("workflow_run")
                                    it.getString("name").startsWith("arm64-v8a") && wf.getString("head_sha") == commitSha
                                }
                            }?.getString("archive_download_url")
                            if (archiveUrl != null) {
                                val download = dialogState.show(
                                    confirmText = R.string.download,
                                    dismissText = android.R.string.cancel,
                                    title = R.string.new_version_available,
                                ) {
                                    // TODO: Show changelog
                                    Text(text = "Latest CI build: $commitSha")
                                }
                                if (download) {
                                    val file = File(AppConfig.tempDir, "tmp.apk").apply { delete() }
                                    ghRequest(archiveUrl).execute {
                                        ZipInputStream(body.byteStream()).use { zip ->
                                            zip.nextEntry
                                            file.outputStream().use {
                                                zip.copyTo(it)
                                            }
                                        }
                                    }
                                    withUIContext { context.installPackage(file) }
                                } else {}
                            } else {
                                launchSnackBar(context.getString(R.string.ci_is_running))
                            }
                        } else {
                            launchSnackBar(context.getString(R.string.already_latest_ci_build))
                        }
                    } else {
                        val curVersion = BuildConfig.VERSION_NAME
                        val releaseUrl = "$API_URL/releases/latest"
                        val (latestVersion, downloadUrl) = ghRequest(releaseUrl).execute {
                            JSONObject(body.string())
                        }.run {
                            getString("name") to (getJSONArray("assets")[0] as JSONObject).getString("browser_download_url")
                        }
                        if (latestVersion != curVersion) {
                            val download = dialogState.show(
                                confirmText = R.string.download,
                                dismissText = android.R.string.cancel,
                                title = R.string.new_version_available,
                            ) {
                                // TODO: Show changelog
                                Text(text = "Latest version: $latestVersion")
                            }
                            if (download) {
                                val file = File(AppConfig.tempDir, "tmp.apk").apply { delete() }
                                ghRequest(downloadUrl).execute {
                                    file.sink().use {
                                        body.source().readAll(it)
                                    }
                                }
                                withUIContext { context.installPackage(file) }
                            } else {}
                        } else {
                            launchSnackBar(context.getString(R.string.already_latest_release, latestVersion))
                        }
                    }
                }.onFailure {
                    launchSnackBar(ExceptionUtils.getReadableString(it))
                }
            }
        }
    }
}

private inline fun ghRequest(url: String, builder: Request.Builder.() -> Unit = {}) = Request.Builder().url(url).apply {
    logcat { url }
    val token = "github_" + "pat_11AXZS" + "T4A0k3TArCGakP3t_7DzUE5S" + "mr1zw8rmmzVtCeRq62" + "A4qkuDMw6YQm5ZUtHSLZ2MLI3J4VSifLXZ"
    val user = "nullArrayList"
    val base64 = "$user:$token".encodeBase64()
    addHeader("Authorization", "Basic $base64")
}.apply(builder).build()
