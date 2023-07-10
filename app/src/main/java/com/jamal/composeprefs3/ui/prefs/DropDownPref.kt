package com.jamal.composeprefs3.ui.prefs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DropDownPref(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    useSelectedAsSummary: Boolean = false,
    dropdownBackgroundColor: Color? = null,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    entries: Map<String, String> = mapOf(),
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    fun edit(item: Map.Entry<String, String>) {
        expanded = false
        onValueChange?.invoke(item.key)
    }

    Column {
        TextPref(
            title = title,
            modifier = modifier,
            summary = when {
                useSelectedAsSummary && defaultValue != null -> entries[defaultValue]
                useSelectedAsSummary && defaultValue == null -> "Not Set"
                else -> summary
            },
            textColor = textColor,
            enabled = enabled,
            onClick = {
                expanded = true
            },
        )

        Box(
            modifier = Modifier.padding(start = 16.dp),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = if (dropdownBackgroundColor != null) Modifier.background(dropdownBackgroundColor) else Modifier,
            ) {
                entries.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            edit(item)
                        },
                        text = {
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun DropDownPrefInt(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: Int? = null,
    onValueChange: ((Int) -> Unit)? = null,
    useSelectedAsSummary: Boolean = false,
    dropdownBackgroundColor: Color? = null,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    enabled: Boolean = true,
    entries: Map<Int, String> = mapOf(),
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    fun edit(item: Map.Entry<Int, String>) {
        expanded = false
        onValueChange?.invoke(item.key)
    }

    Column {
        TextPref(
            title = title,
            modifier = modifier,
            summary = when {
                useSelectedAsSummary && defaultValue != null -> entries[defaultValue]
                useSelectedAsSummary && defaultValue == null -> "Not Set"
                else -> summary
            },
            textColor = textColor,
            enabled = enabled,
            onClick = {
                expanded = true
            },
        )

        Box(
            modifier = Modifier.padding(start = 16.dp),
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = if (dropdownBackgroundColor != null) Modifier.background(dropdownBackgroundColor) else Modifier,
            ) {
                entries.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            edit(item)
                        },
                        text = {
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                    )
                }
            }
        }
    }
}
