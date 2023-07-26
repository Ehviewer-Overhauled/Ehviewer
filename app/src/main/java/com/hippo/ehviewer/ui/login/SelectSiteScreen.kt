package com.hippo.ehviewer.ui.login

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.FINISH_ROUTE_NAME
import com.hippo.ehviewer.ui.LocalNavController

@Composable
fun SelectSiteScreen() {
    var siteEx by remember { mutableStateOf(true) }
    val navController = LocalNavController.current
    val context = LocalContext.current

    BackHandler {
        (context as Activity).moveTaskToBack(true)
    }

    Column(
        modifier = Modifier.padding(WindowInsets.systemBars.asPaddingValues()).padding(horizontal = dimensionResource(R.dimen.keyline_margin)).padding(top = dimensionResource(R.dimen.keyline_margin)).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.select_scene),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(selected = !siteEx, onClick = { siteEx = false }) {
                Text(text = stringResource(id = R.string.site_e))
            }
            SegmentedButton(selected = siteEx, onClick = { siteEx = true }) {
                Text(text = stringResource(id = R.string.site_ex))
            }
        }
        Text(
            text = stringResource(id = R.string.select_scene_explain),
            modifier = Modifier.padding(top = 16.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 4.dp, bottom = 20.dp).fillMaxWidth()) {
            Button(
                onClick = {
                    // Gallery site was set to ex in sad panda check
                    if (!siteEx) Settings.gallerySite = EhUrl.SITE_E
                    Settings.needSignIn = false
                    navController.navigate(FINISH_ROUTE_NAME)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    }
}
