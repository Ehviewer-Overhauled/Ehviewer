package com.hippo.ehviewer.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.hippo.ehviewer.ui.settings.PreferenceTokens.PreferenceTextPadding
import com.jamal.composeprefs3.ui.prefs.SliderPref
import com.jamal.composeprefs3.ui.prefs.SpannedTextPref
import com.jamal.composeprefs3.ui.prefs.SwitchPref
import com.jamal.composeprefs3.ui.prefs.TextPref
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

@Composable
fun PreferenceHeader(icon: Painter, @StringRes title: Int, childRouteName: String) {
    val navController = LocalNavController.current
    Row(
        modifier = Modifier.clickable { navController.navigate(childRouteName) }.fillMaxWidth().height(PreferenceTokens.PreferenceHeaderHeight),
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
    TextPref(title = title, summary = summary, onClick = onClick)
}

@Composable
fun SwitchPreference(title: String, summary: String?, value: KMutableProperty0<Boolean>, enabled: Boolean = true) {
    var v by remember { mutableStateOf(value.get()) }
    fun mutate() = value.set((!value.get()).also { v = it })
    SwitchPref(checked = v, onMutate = ::mutate, title = title, summary = summary, enabled = enabled)
}

@Composable
fun IntSliderPreference(maxValue: Int, minValue: Int = 0, step: Int = maxValue - minValue - 1, title: String, value: KMutableProperty0<Int>, enabled: Boolean = true) {
    var v by remember { mutableIntStateOf(value.get()) }
    fun set(float: Float) = value.set(float.roundToInt().also { v = it })
    SliderPref(title = title, defaultValue = v.toFloat(), onValueChangeFinished = ::set, valueRange = minValue.toFloat()..maxValue.toFloat(), showValue = true, steps = step, enabled = enabled)
}

@Composable
fun UrlPreference(title: String, url: String) {
    val context = LocalContext.current
    Preference(title, url) { context.openBrowser(url) }
}

@Composable
fun HtmlPreference(title: String, summary: AnnotatedString? = null, onClick: () -> Unit = {}) {
    SpannedTextPref(title = title, summary = summary, onClick = onClick)
}
