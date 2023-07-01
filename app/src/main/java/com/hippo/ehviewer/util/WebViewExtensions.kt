package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import android.webkit.WebView

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() = settings.run {
    builtInZoomControls = true
    displayZoomControls = false
    javaScriptEnabled = true
}
