package com.hippo.ehviewer.util

import android.annotation.SuppressLint
import android.webkit.WebView
import com.hippo.ehviewer.client.CHROME_USER_AGENT

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() = settings.run {
    builtInZoomControls = true
    displayZoomControls = false
    javaScriptEnabled = true
    userAgentString = CHROME_USER_AGENT
}
