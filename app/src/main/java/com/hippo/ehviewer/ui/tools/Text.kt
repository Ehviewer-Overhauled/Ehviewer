package com.hippo.ehviewer.ui.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle

val TextStyle.includeFontPadding: TextStyle
    @Composable
    @ReadOnlyComposable
    @Stable
    get() = copy(platformStyle = PlatformTextStyle(includeFontPadding = true))
