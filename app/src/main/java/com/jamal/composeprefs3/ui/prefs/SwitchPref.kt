package com.jamal.composeprefs3.ui.prefs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SwitchPref(
    checked: Boolean,
    onMutate: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    TextPref(
        title = title,
        modifier = modifier,
        textColor = textColor,
        summary = summary,
        darkenOnDisable = true,
        leadingIcon = leadingIcon,
        enabled = enabled,
        onClick = onMutate,
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onMutate() },
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
