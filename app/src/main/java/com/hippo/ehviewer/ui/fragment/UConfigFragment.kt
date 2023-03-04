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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.hippo.ehviewer.EhApplication.Companion.ehCookieStore
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.databinding.ActivityWebviewBinding
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.ehviewer.widget.DialogWebChromeClient
import eu.kanade.tachiyomi.util.lang.launchIO
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import rikka.core.res.resolveColor

class UConfigFragment : BaseFragment() {
    private var _binding: ActivityWebviewBinding? = null
    private val binding get() = _binding!!
    private val url = EhUrl.uConfigUrl
    private var loaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityWebviewBinding.inflate(inflater, container, false)
        binding.webview.run {
            setBackgroundColor(requireActivity().theme.resolveColor(android.R.attr.colorBackground))
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.javaScriptEnabled = true
            webViewClient = UConfigWebViewClient()
            webChromeClient = DialogWebChromeClient(requireContext())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progress.visibility = View.VISIBLE
        binding.webview.loadUrl(url)
        showTip(R.string.apply_tip, BaseScene.LENGTH_LONG)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.activity_u_config, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_apply) {
                    if (loaded) {
                        apply()
                    }
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
        val cookieManager = CookieManager.getInstance()
        cookieManager.flush()
        cookieManager.removeAllCookies(null)
        cookieManager.removeSessionCookies(null)

        // Copy cookies from okhttp cookie store to CookieManager
        val store = ehCookieStore
        for (cookie in store.getCookies(url.toHttpUrl())) {
            cookieManager.setCookie(url, cookie.toString())
        }
    }

    private fun apply() {
        binding.webview.loadUrl(
            """javascript:(function() {
    var apply = document.getElementById("apply").children[0];
    apply.click();
})();"""
        )
    }

    private fun longLive(cookie: Cookie): Cookie {
        return Cookie.Builder()
            .name(cookie.name)
            .value(cookie.value)
            .domain(cookie.domain)
            .path(cookie.path)
            .expiresAt(Long.MAX_VALUE)
            .build()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webview.destroy()

        // Put cookies back to okhttp cookie store
        val cookieManager = CookieManager.getInstance()
        val cookiesString = cookieManager.getCookie(url)
        if (!cookiesString.isNullOrEmpty()) {
            val store = ehCookieStore
            val eUrl = EhUrl.HOST_E.toHttpUrl()
            val exUrl = EhUrl.HOST_EX.toHttpUrl()

            lifecycleScope.launchIO {
                // The cookies saved in the uconfig page should be shared between e and ex
                for (header in cookiesString.split(";".toRegex()).dropLastWhile { it.isEmpty() }) {
                    Cookie.parse(eUrl, header)?.let {
                        store.addCookie(longLive(it))
                    }
                    Cookie.parse(exUrl, header)?.let {
                        store.addCookie(longLive(it))
                    }
                }
            }
        }
        _binding = null
    }

    override fun getFragmentTitle(): Int {
        return R.string.u_config
    }

    private inner class UConfigWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            // Never load other urls
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            binding.progress.visibility = View.VISIBLE
            loaded = false
        }

        override fun onPageFinished(view: WebView, url: String) {
            binding.progress.visibility = View.GONE
            loaded = true
        }
    }
}