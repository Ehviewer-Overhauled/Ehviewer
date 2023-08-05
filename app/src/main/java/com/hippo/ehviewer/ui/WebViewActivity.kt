package com.hippo.ehviewer.ui

import android.os.Bundle
import com.hippo.ehviewer.ui.webview.WebViewScreen

class WebViewActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.extras?.getString(KEY_URL) ?: return
        setMD3Content {
            WebViewScreen(url = url) { finish() }
        }
    }

    companion object {
        const val KEY_URL = "url"
    }
}
