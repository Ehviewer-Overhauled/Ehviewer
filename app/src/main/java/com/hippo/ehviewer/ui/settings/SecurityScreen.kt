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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.isAuthenticationSupported
import com.hippo.ehviewer.ui.tools.observed
import com.hippo.ehviewer.ui.tools.rememberedAccessor

@Composable
fun SecurityScreen() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_privacy)) },
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
                title = stringResource(id = R.string.settings_privacy_require_unlock),
                value = Settings::security,
                enabled = LocalContext.current.isAuthenticationSupported(),
            )
            val securityDelay = Settings::securityDelay.observed
            val summary = if (securityDelay.value == 0) stringResource(id = R.string.settings_privacy_require_unlock_delay_summary_immediately) else stringResource(id = R.string.settings_privacy_require_unlock_delay_summary, securityDelay.value)
            IntSliderPreference(
                maxValue = 30,
                title = stringResource(id = R.string.settings_privacy_require_unlock_delay),
                summary = summary,
                value = securityDelay.rememberedAccessor,
                enabled = LocalContext.current.isAuthenticationSupported(),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_privacy_secure),
                summary = stringResource(id = R.string.settings_privacy_secure_summary),
                value = Settings::enabledSecurity,
            )
        }
    }
}
