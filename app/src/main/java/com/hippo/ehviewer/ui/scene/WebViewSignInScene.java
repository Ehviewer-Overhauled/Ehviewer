/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.widget.DialogWebChromeClient;
import com.hippo.yorozuya.AssertUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import rikka.core.res.ResourcesKt;

public class WebViewSignInScene extends SolidScene {

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private WebView mWebView;

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Nullable
    @Override
    @SuppressLint("SetJavaScriptEnabled")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        AssertUtils.assertNotNull(context);

        EhUtils.signOut();

        // http://stackoverflow.com/questions/32284642/how-to-handle-an-uncatched-exception
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.flush();
        cookieManager.removeAllCookies(null);
        cookieManager.removeSessionCookies(null);

        mWebView = new WebView(context);
        mWebView.setBackgroundColor(ResourcesKt.resolveColor(getTheme(), android.R.attr.colorBackground));
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new LoginWebViewClient());
        mWebView.setWebChromeClient(new DialogWebChromeClient(context));
        mWebView.loadUrl(EhUrl.URL_SIGN_IN);
        return mWebView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mWebView) {
            mWebView.destroy();
            mWebView = null;
        }
    }

    private class LoginWebViewClient extends WebViewClient {

        public List<Cookie> parseCookies(HttpUrl url, String cookieStrings) {
            if (cookieStrings == null) {
                return Collections.emptyList();
            }

            List<Cookie> cookies = null;
            String[] pieces = cookieStrings.split(";");
            for (String piece : pieces) {
                Cookie cookie = Cookie.parse(url, piece);
                if (cookie == null) {
                    continue;
                }
                if (cookies == null) {
                    cookies = new ArrayList<>();
                }
                cookies.add(cookie);
            }

            return cookies != null ? cookies : Collections.emptyList();
        }

        private void addCookie(String domain, Cookie cookie) {
            EhApplication.getEhCookieStore().addCookie(EhCookieStore.newCookie(cookie, domain, true, true, true));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                return;
            }

            String cookieString = CookieManager.getInstance().getCookie(EhUrl.HOST_E);
            List<Cookie> cookies = parseCookies(httpUrl, cookieString);
            boolean getId = false;
            boolean getHash = false;
            for (Cookie cookie : cookies) {
                if (EhCookieStore.KEY_IPD_MEMBER_ID.equals(cookie.name())) {
                    getId = true;
                } else if (EhCookieStore.KEY_IPD_PASS_HASH.equals(cookie.name())) {
                    getHash = true;
                }
                addCookie(EhUrl.DOMAIN_EX, cookie);
                addCookie(EhUrl.DOMAIN_E, cookie);
            }

            if (getId && getHash) {
                setResult(RESULT_OK, null);
                finish();
            }
        }
    }
}
