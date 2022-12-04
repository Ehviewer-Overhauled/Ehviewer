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

package com.hippo.ehviewer.ui.fragment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.ehviewer.widget.DialogWebChromeClient;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import rikka.core.res.ResourcesKt;

public class UConfigFragment extends BaseFragment {

    private WebView webView;
    private CircularProgressIndicator progress;
    private String url;
    private boolean loaded;

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_webview, container, false);
        webView = view.findViewById(R.id.webview);
        webView.setBackgroundColor(ResourcesKt.resolveColor(requireActivity().getTheme(), android.R.attr.colorBackground));
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new UConfigWebViewClient());
        webView.setWebChromeClient(new DialogWebChromeClient(requireContext()));
        progress = view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
        showTip(R.string.apply_tip, BaseScene.LENGTH_LONG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.flush();
        cookieManager.removeAllCookies(null);
        cookieManager.removeSessionCookies(null);


        // Copy cookies from okhttp cookie store to CookieManager
        url = EhUrl.getUConfigUrl();
        EhCookieStore store = EhApplication.getEhCookieStore();
        for (Cookie cookie : store.getCookies(HttpUrl.parse(url))) {
            cookieManager.setCookie(url, cookie.toString());
        }
    }

    private void apply() {
        webView.loadUrl("javascript:"
                + "(function() {\n"
                + "    var apply = document.getElementById(\"apply\").children[0];\n"
                + "    apply.click();\n"
                + "})();");
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_u_config, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_apply) {
            if (loaded) {
                apply();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Cookie longLive(Cookie cookie) {
        return new Cookie.Builder()
                .name(cookie.name())
                .value(cookie.value())
                .domain(cookie.domain())
                .path(cookie.path())
                .expiresAt(Long.MAX_VALUE)
                .build();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        webView.destroy();

        // Put cookies back to okhttp cookie store
        CookieManager cookieManager = CookieManager.getInstance();
        String cookiesString = cookieManager.getCookie(url);

        if (cookiesString != null && !cookiesString.isEmpty()) {
            EhCookieStore store = EhApplication.getEhCookieStore();
            HttpUrl eUrl = HttpUrl.parse(EhUrl.HOST_E);
            HttpUrl exUrl = HttpUrl.parse(EhUrl.HOST_EX);
            if (eUrl == null || exUrl == null) {
                return;
            }

            // The cookies saved in the uconfig page should be shared between e and ex
            for (String header : cookiesString.split(";")) {
                Cookie eCookie = Cookie.parse(eUrl, header);
                if (eCookie != null) {
                    store.addCookie(longLive(eCookie));
                }

                Cookie exCookie = Cookie.parse(exUrl, header);
                if (exCookie != null) {
                    store.addCookie(longLive(exCookie));
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int getFragmentTitle() {
        return R.string.u_config;
    }

    private class UConfigWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Never load other urls
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progress.setVisibility(View.VISIBLE);
            loaded = false;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.GONE);
            loaded = true;
        }
    }
}
