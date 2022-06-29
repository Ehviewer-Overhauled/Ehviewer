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

package com.hippo.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import androidx.webkit.WebViewCompat;

public class WebViewUtil {
    private static final String TAG = "WebViewUtil";

    public static boolean available(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        PackageInfo webViewPackageInfo = WebViewCompat.getCurrentWebViewPackage(context);
        if (webViewPackageInfo == null) {
            Log.d(TAG, "WebView not available");
            return false;
        }
        Log.d(TAG, "WebView version: " + webViewPackageInfo.versionName);
        return true;
    }
}
