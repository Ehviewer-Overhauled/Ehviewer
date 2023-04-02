/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.databinding.ActivityWebviewBinding
import com.hippo.ehviewer.widget.DialogWebChromeClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import rikka.core.res.resolveColor

class MyTagsFragment : BaseFragment() {
    private var _binding: ActivityWebviewBinding? = null
    private val binding get() = _binding!!
    private val url = EhUrl.myTagsUrl

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityWebviewBinding.inflate(inflater, container, false)
        binding.webview.run {
            setBackgroundColor(requireActivity().theme.resolveColor(android.R.attr.colorBackground))
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.javaScriptEnabled = true
            webViewClient = MyTagsWebViewClient()
            webChromeClient = DialogWebChromeClient(requireContext())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progress.visibility = View.VISIBLE
        binding.webview.loadUrl(url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        cookieManager.removeAllCookies(null)
        cookieManager.removeSessionCookies(null)

        // Copy cookies from okhttp cookie store to CookieManager
        val store = EhCookieStore
        for (cookie in store.getCookies(url.toHttpUrl())) {
            cookieManager.setCookie(url, cookie.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webview.destroy()
        _binding = null
    }

    override fun getFragmentTitle(): Int {
        return R.string.my_tags
    }

    private inner class MyTagsWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            // Never load other urls
            return request.url.toString() != this@MyTagsFragment.url
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            binding.progress.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView, url: String) {
            binding.progress.visibility = View.GONE
        }
    }
}
