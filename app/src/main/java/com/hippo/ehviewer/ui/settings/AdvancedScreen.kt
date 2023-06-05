package com.hippo.ehviewer.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.AppConfig
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.login.LocalNavController
import com.hippo.ehviewer.util.LogCat
import com.hippo.ehviewer.util.ReadableTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun AdvancedScreen() {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_advanced)) },
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
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_save_parse_error_body),
                summary = stringResource(id = R.string.settings_advanced_save_parse_error_body_summary),
                value = Settings::saveParseErrorBody,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_save_crash_log),
                summary = stringResource(id = R.string.settings_advanced_save_crash_log_summary),
                value = Settings::saveCrashLog,
            )
            val dumpLogError = stringResource(id = R.string.settings_advanced_dump_logcat_failed)
            val dumpLogcatLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                uri?.run {
                    coroutineScope.launch {
                        context.runCatching {
                            grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            contentResolver.openOutputStream(uri)?.use { outputStream ->
                                val files = ArrayList<File>()
                                AppConfig.externalParseErrorDir?.listFiles()?.let { files.addAll(it) }
                                AppConfig.externalCrashDir?.listFiles()?.let { files.addAll(it) }
                                ZipOutputStream(outputStream).use { zipOs ->
                                    files.forEach { file ->
                                        if (!file.isFile) return@forEach
                                        val entry = ZipEntry(file.name)
                                        zipOs.putNextEntry(entry)
                                        file.inputStream().use { it.copyTo(zipOs) }
                                    }
                                    val logcatEntry = ZipEntry("logcat-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt")
                                    zipOs.putNextEntry(logcatEntry)
                                    LogCat.save(zipOs)
                                }
                                snackbarHostState.showSnackbar(getString(R.string.settings_advanced_dump_logcat_to, uri.toString()))
                            }
                        }.onFailure {
                            snackbarHostState.showSnackbar(dumpLogError)
                            it.printStackTrace()
                        }
                    }
                }
            }
            Preference(
                title = stringResource(id = R.string.settings_advanced_dump_logcat),
                summary = stringResource(id = R.string.settings_advanced_dump_logcat_summary),
            ) { dumpLogcatLauncher.launch("log-" + ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".zip") }
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_advanced_read_cache_size),
                entry = R.array.read_cache_size_entries,
                entryValueRes = R.array.read_cache_size_entry_values,
                value = Settings::readCacheSize,
            )
            SimpleMenuPreference(
                title = stringResource(id = R.string.settings_advanced_app_language_title),
                entry = R.array.app_language_entries,
                entryValueRes = R.array.app_language_entry_values,
                value = Settings::language,
            )
            Preference(
                title = stringResource(id = R.string.settings_advanced_proxy),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_built_in_hosts_title),
                summary = null,
                value = Settings::builtInHosts,
            )
            Preference(
                title = stringResource(id = R.string.settings_advanced_dns_over_http_title),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_domain_fronting_title),
                summary = stringResource(id = R.string.settings_advanced_domain_fronting_summary),
                value = Settings::dF,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_advanced_bypass_vpn_title),
                summary = stringResource(id = R.string.settings_advanced_bypass_vpn_summary),
                value = Settings::bypassVpn,
            )
            SwitchPreference(
                title = stringResource(id = R.string.preload_thumb_aggressively),
                summary = null,
                value = Settings::preloadThumbAggressively,
            )
            Preference(
                title = stringResource(id = R.string.settings_advanced_export_data),
                summary = stringResource(id = R.string.settings_advanced_export_data_summary),
            )
            Preference(
                title = stringResource(id = R.string.settings_advanced_import_data),
                summary = stringResource(id = R.string.settings_advanced_import_data_summary),
            )
            Preference(
                title = stringResource(id = R.string.settings_advanced_backup_favorite),
                summary = stringResource(id = R.string.settings_advanced_backup_favorite_summary),
            )
            Preference(
                title = stringResource(id = R.string.open_by_default),
                summary = null,
            )
            Spacer(modifier = Modifier.size(paddingValues.calculateBottomPadding()))
        }
    }
}
