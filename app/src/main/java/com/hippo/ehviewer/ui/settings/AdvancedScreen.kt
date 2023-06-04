package com.hippo.ehviewer.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
        Column(modifier = Modifier.padding(it).nestedScroll(scrollBehavior.nestedScrollConnection)) {
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
        }
    }
}
