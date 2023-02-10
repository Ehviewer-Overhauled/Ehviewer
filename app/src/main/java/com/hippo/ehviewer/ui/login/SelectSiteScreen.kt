package com.hippo.ehviewer.ui.login

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import eu.kanade.tachiyomi.util.lang.launchNonCancellable

@Composable
fun SelectSiteScreen() {
    val activity = LocalContext.current as Activity
    val coroutineScope = rememberCoroutineScope()
    var siteEx by remember { mutableStateOf(true) }

    Column(
        Modifier
            .padding(horizontal = dimensionResource(R.dimen.keyline_margin))
            .padding(top = dimensionResource(R.dimen.keyline_margin))
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.select_scene),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.weight(1f))

        // TODO: Replace this dummy impl with official one when available
        // See https://m3.material.io/components/segmented-buttons/overview
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(90f, 90f, 90f, 90f)
                )
                .clip(RoundedCornerShape(90f, 90f, 90f, 90f))
        ) {
            Box(
                modifier = Modifier
                    .background(if (!siteEx) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { siteEx = false }
                    .size(100.dp, 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.site_e),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Divider(
                color = MaterialTheme.colorScheme.outline, modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            Box(
                modifier = Modifier
                    .background(if (siteEx) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { siteEx = true }
                    .size(100.dp, 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.site_ex),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        // Dummy impl end

        Text(
            text = stringResource(id = R.string.select_scene_explain),
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(top = 4.dp, bottom = 20.dp)
                .fillMaxWidth()
        ) {
            Button(onClick = {
                Settings.putSelectSite(false)
                coroutineScope.launchNonCancellable {
                    runCatching {
                        if (!siteEx) {
                            Settings.putGallerySite(EhUrl.SITE_E)
                            EhEngine.getUConfig()
                        } else {
                            // Get cookies for image limits
                            EhEngine.getUConfig(EhUrl.URL_UCONFIG_E)
                        }
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
                activity.finish()
            }, Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    }
}
