package com.hippo.ehviewer.ui.settings

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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.login.LocalNavController

@Composable
fun AdvancedScreen() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
    ) {
        Column(modifier = Modifier.padding(top = it.calculateTopPadding()).nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState())) {
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
            Preference(
                title = stringResource(id = R.string.settings_advanced_dump_logcat),
                summary = stringResource(id = R.string.settings_advanced_dump_logcat_summary),
            )
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
            Spacer(modifier = Modifier.size(it.calculateBottomPadding()))
        }
    }
}
