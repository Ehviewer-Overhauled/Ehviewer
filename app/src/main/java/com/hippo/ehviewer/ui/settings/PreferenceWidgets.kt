package com.hippo.ehviewer.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.hippo.ehviewer.ui.login.LocalNavController
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.settings.PreferenceTokens.PreferenceMinHeight
import com.hippo.ehviewer.ui.settings.PreferenceTokens.PreferencePadding
import com.hippo.ehviewer.ui.settings.PreferenceTokens.PreferenceTextPadding

@Composable
fun PreferenceHeader(icon: Painter, @StringRes title: Int, childRouteName: String) {
    val navController = LocalNavController.current
    Row(
        modifier = Modifier.clickable { navController.navigate(childRouteName) }.fillMaxWidth().height(
            PreferenceTokens.PreferenceHeaderHeight,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.size(PreferenceTokens.PreferenceIconPadding))
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(PreferenceTokens.PreferenceIconSize), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.size(PreferenceTokens.PreferenceIconPadding))
        Text(text = stringResource(id = title), modifier = Modifier.padding(PreferenceTextPadding), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PreferenceHeader(icon: ImageVector, @StringRes title: Int, childRouteName: String) = PreferenceHeader(icon = rememberVectorPainter(image = icon), title = title, childRouteName = childRouteName)

@Composable
fun Preference(title: String, summary: String? = null, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.defaultMinSize(minHeight = PreferenceMinHeight).fillMaxWidth().clickable(onClick = onClick).padding(PreferencePadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        summary?.let {
            Spacer(modifier = Modifier.size(PreferenceTextPadding))
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun UrlPreference(title: String, url: String) {
    val context = LocalContext.current
    Preference(title, url) { context.openBrowser(url) }
}

@Composable
fun HtmlPreference(title: String, summary: AnnotatedString? = null, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.defaultMinSize(minHeight = PreferenceMinHeight).fillMaxWidth().clickable(onClick = onClick).padding(PreferencePadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        summary?.let {
            Spacer(modifier = Modifier.size(PreferenceTextPadding))
            Text(text = summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
