package com.hippo.ehviewer.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.accompanist.themeadapter.material3.Mdc3Theme

fun ComposeView.setMD3Content(content: @Composable () -> Unit) = setContent { Mdc3Theme(content = content) }
