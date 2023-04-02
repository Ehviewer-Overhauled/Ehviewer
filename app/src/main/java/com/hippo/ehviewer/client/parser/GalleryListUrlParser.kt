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

package com.hippo.ehviewer.client.parser;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.yorozuya.Utilities;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class GalleryListUrlParser {

    private static final String[] VALID_HOSTS = {EhUrl.DOMAIN_EX, EhUrl.DOMAIN_E, EhUrl.DOMAIN_LOFI};

    private static final String PATH_NORMAL = "/";
    private static final String PATH_UPLOADER = "/uploader/";
    private static final String PATH_TAG = "/tag/";
    private static final String PATH_TOPLIST = "/toplist.php";

    public static ListUrlBuilder parse(String urlStr) {
        URL url;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }

        if (!Utilities.contain(VALID_HOSTS, url.getHost())) {
            return null;
        }

        String path = url.getPath();
        if (path == null) {
            return null;
        }
        if (PATH_NORMAL.equals(path) || path.length() == 0) {
            ListUrlBuilder builder = new ListUrlBuilder();
            builder.setQuery(url.getQuery());
            return builder;
        } else if (path.startsWith(PATH_UPLOADER)) {
            return parseUploader(path);
        } else if (path.startsWith(PATH_TAG)) {
            return parseTag(path);
        } else if (path.startsWith(PATH_TOPLIST)) {
            return parseToplist(urlStr);
        } else if (path.startsWith("/")) {
            int category;
            try {
                category = Integer.parseInt(path.substring(1));
            } catch (NumberFormatException e) {
                return null;
            }
            ListUrlBuilder builder = new ListUrlBuilder();
            builder.setQuery(url.getQuery());
            builder.setCategory(category);
            return builder;
        } else {
            return null;
        }
    }

    // TODO get page
    private static ListUrlBuilder parseUploader(String path) {
        String uploader;
        int prefixLength = PATH_UPLOADER.length();
        int index = path.indexOf('/', prefixLength);

        if (index < 0) {
            uploader = path.substring(prefixLength);
        } else {
            uploader = path.substring(prefixLength, index);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            uploader = URLDecoder.decode(uploader, StandardCharsets.UTF_8);
        } else {
            try {
                uploader = URLDecoder.decode(uploader, StandardCharsets.UTF_8.displayName());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        if (TextUtils.isEmpty(uploader)) {
            return null;
        }

        ListUrlBuilder builder = new ListUrlBuilder();
        builder.setMode(ListUrlBuilder.MODE_UPLOADER);
        builder.setKeyword(uploader);
        return builder;
    }

    // TODO get page
    private static ListUrlBuilder parseTag(String path) {
        String tag;
        int prefixLength = PATH_TAG.length();
        int index = path.indexOf('/', prefixLength);


        if (index < 0) {
            tag = path.substring(prefixLength);
        } else {
            tag = path.substring(prefixLength, index);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            tag = URLDecoder.decode(tag, StandardCharsets.UTF_8);
        } else {
            try {
                tag = URLDecoder.decode(tag, StandardCharsets.UTF_8.displayName());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        if (TextUtils.isEmpty(tag)) {
            return null;
        }

        ListUrlBuilder builder = new ListUrlBuilder();
        builder.setMode(ListUrlBuilder.MODE_TAG);
        builder.setKeyword(tag);
        return builder;
    }

    // TODO get page
    private static ListUrlBuilder parseToplist(String path) {
        Uri uri = Uri.parse(path);

        if (uri == null || TextUtils.isEmpty(uri.getQueryParameter("tl"))) {
            return null;
        }

        int tl = Integer.parseInt(uri.getQueryParameter("tl"));

        if (tl > 15 || tl < 11) {
            return null;
        }

        ListUrlBuilder builder = new ListUrlBuilder();
        builder.setMode(ListUrlBuilder.MODE_TOPLIST);
        builder.setKeyword(String.valueOf(tl));
        return builder;
    }

}
