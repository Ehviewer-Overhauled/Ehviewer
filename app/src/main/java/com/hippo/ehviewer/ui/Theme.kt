package com.hippo.ehviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.accompanist.themeadapter.material3.Mdc3Theme

// To make sure compose theme and legacy view theme are in sync, we only use Mdc3Theme
fun ComposeView.setMD3Content(content: @Composable () -> Unit) = setContent { Mdc3Theme(content = content) }
fun ComponentActivity.setMD3Content(content: @Composable () -> Unit) = setContent { Mdc3Theme(content = content) }
