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
import com.hippo.ehviewer.widget.DialogWebChromeClient;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import rikka.core.res.ResourcesKt;

public class MyTagsFragment extends BaseFragment {

    private WebView webView;
    private CircularProgressIndicator progress;
    private String url;

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
        webView.setWebViewClient(new MyTagsWebViewClient());
        webView.setWebChromeClient(new DialogWebChromeClient(requireContext()));
        progress = view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progress.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.flush();
        cookieManager.removeAllCookies(null);
        cookieManager.removeSessionCookies(null);


        // Copy cookies from okhttp cookie store to CookieManager
        url = EhUrl.getMyTagsUrl();
        EhCookieStore store = EhApplication.getEhCookieStore();
        for (Cookie cookie : store.getCookies(HttpUrl.parse(url))) {
            cookieManager.setCookie(url, cookie.toString());
        }
    }

    @Override
    public int getFragmentTitle() {
        return R.string.my_tags;
    }

    private class MyTagsWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Never load other urls
            return !url.equals(MyTagsFragment.this.url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progress.setVisibility(View.GONE);
        }
    }
}
