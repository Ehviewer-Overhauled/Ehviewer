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

package com.hippo.ehviewer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;

import androidx.appcompat.app.AlertDialog;

import com.android.apksig.ApkVerifier;
import com.hippo.scene.SceneFragment;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class Analytics {
    private static final byte[] CERTIFICATE = {48, -126, 2, -51, 48, -126, 1, -75, -96, 3, 2, 1, 2, 2, 4, 118, 111, 58, 92, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11, 72, 105, 116, 111, 107, 111, 116, 111, 46, 99, 110, 48, 32, 23, 13, 49, 57, 48, 56, 50, 48, 48, 50, 51, 52, 48, 57, 90, 24, 15, 50, 49, 49, 57, 48, 55, 50, 55, 48, 50, 51, 52, 48, 57, 90, 48, 22, 49, 20, 48, 18, 6, 3, 85, 4, 10, 19, 11, 72, 105, 116, 111, 107, 111, 116, 111, 46, 99, 110, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -101, 68, 51, -99, -100, 26, 14, 54, -127, -112, 1, -64, -52, 21, -22, -56, 123, -104, 16, 28, -74, -27, 75, 12, -39, 45, -123, -103, 50, 109, 118, 92, -88, 92, -111, -101, 61, -119, 83, -50, 67, -97, 124, -80, -7, -82, 41, -65, 41, 34, -114, 23, 10, -121, -52, 17, -12, -1, 55, 125, -90, 122, -122, 42, 114, 69, -1, -1, -76, 49, -52, 46, -126, 126, 4, 43, -52, 42, -21, -39, -98, -3, -115, -91, 40, -57, 38, -30, 7, 46, 88, -6, -103, -53, 83, -110, -75, -65, 90, 18, -52, -75, -72, -12, 115, -52, -117, -28, -38, 90, 60, -45, -22, -115, -2, -86, -34, 67, -60, -104, -74, -53, 46, 88, -3, -61, 80, 58, 85, -8, -66, -110, 118, 25, -81, -90, -82, 96, -68, 74, 31, -42, 104, -49, -121, -103, -113, 43, -43, -101, 22, -28, 124, -112, -115, -85, 67, 64, 75, -37, 32, -124, -91, -76, -58, 9, 98, -30, 3, 97, 73, 18, -9, -67, 117, -54, 35, -104, 126, -29, -117, -58, -37, -19, -47, -117, 32, 19, 99, -87, -115, -104, -34, -122, 69, 23, -101, -46, -62, -44, -47, -10, -26, -1, 105, -116, 111, -51, -99, 67, -125, 47, 127, -15, -59, 99, -82, -111, -111, -94, -123, -52, -81, 121, 50, 24, 25, 73, -53, 43, -10, 110, 27, -42, -24, 84, -36, 50, -81, -44, 111, 52, 83, -87, 7, 12, -105, 34, 36, -70, -75, -75, 45, 88, 67, -63, 2, 3, 1, 0, 1, -93, 33, 48, 31, 48, 29, 6, 3, 85, 29, 14, 4, 22, 4, 20, -3, 65, -105, 57, 8, 44, -70, 80, 52, 55, 94, -73, -57, -78, -120, 111, 59, 93, -20, 30, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 3, -126, 1, 1, 0, -109, -64, -22, -50, 87, 4, -51, -112, -117, -50, -17, -79, -80, 110, -25, 50, -83, -99, 81, 28, 53, -105, 97, -93, -74, -14, 16, 44, 106, -118, -85, 33, 11, -101, -93, -69, 36, 119, -7, -37, -45, -72, 81, -78, -60, -53, -110, 61, 65, 35, 48, -60, 65, -103, 68, -48, -16, -19, 50, -78, 102, 54, 79, -38, -83, -57, 74, -7, 49, -97, -42, -17, -13, 0, 96, -55, -38, 89, -69, -33, 100, 46, 84, -114, -71, 5, 117, -92, -38, 57, 7, -55, 2, -96, -109, -114, -80, 20, 72, -66, -115, 35, 57, 65, -58, 2, -128, -48, 85, 79, -47, 45, -76, 88, 47, 102, -80, 67, 5, 120, 98, -20, 27, 11, 114, 95, 80, -76, -54, 123, -114, -23, 36, 120, 36, -62, -88, -76, 29, 78, -14, 97, -1, -100, 82, -11, -16, 72, 43, -46, 30, 9, 9, 112, -121, -90, -45, -30, 14, 36, -3, 27, -27, 63, 72, -9, -103, -75, -84, -57, -36, 61, 33, -36, 108, -100, 98, -94, -20, -38, 103, 22, -111, -68, 49, 45, 13, 20, 27, 118, -63, 5, 5, -114, -3, 69, -120, -52, -113, -88, 113, 118, 54, -117, -76, 54, -96, 122, 97, -75, 110, 116, -42, -81, -57, -16, 36, 96, 50, -53, -6, 80, -22, -67, 75, -94, -41, 18, -93, -59, 30, 94, 89, 56, 21, -4, -46, 107, 40, 53, -70, -59, 66, 96, -103, -92, -82, -55, 97, -77, -120, -52, -42, -50, 1, -87};

    public static void start(Application application) {
        if (BuildConfig.DEBUG) {
            return;
        }
        AppCenter.start(application, "1aa06805-037c-4f3c-8aaa-e23d1433986d", com.microsoft.appcenter.analytics.Analytics.class, Crashes.class, Distribute.class);
        AppCenter.setUserId(Settings.getUserID());
        com.microsoft.appcenter.analytics.Analytics.setEnabled(Settings.getEnableAnalytics());
        Distribute.setEnabledForDebuggableBuild(false);
    }

    public static void checkTampered(Activity activity) {
        new Thread(() -> {
            if (!verifySignature(activity) || !verifyPackageName(activity)) {
                Crashes.setEnabled(false);
                activity.runOnUiThread(() -> {
                    try {
                        new AlertDialog.Builder(activity)
                                .setMessage(R.string.tampered_warning)
                                .setCancelable(false)
                                .setPositiveButton(R.string.tampered_get, (dialog1, which) -> UrlOpener.openUrl(activity, "https://install.appcenter.ms/users/nekoinverter/apps/ehviewer/distribution_groups/public", false))
                                .show();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        // ignore
                    }
                });
            }
        }).start();
    }

    public static boolean isEnabled() {
        return AppCenter.isConfigured() && Settings.getEnableAnalytics();
    }

    public static void setEnabled(boolean enabled) {
        if (BuildConfig.DEBUG) {
            return;
        }
        com.microsoft.appcenter.analytics.Analytics.setEnabled(enabled);
    }

    private static boolean verifyPackageName(Context context) {
        return context.getPackageName().equals("io.github.nekoinverter.ehviewer");
    }

    private static boolean verifySignature(Context context) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            return true;
        }
        ApkVerifier verifier = new ApkVerifier.Builder(new File(context.getApplicationInfo().sourceDir))
                .setMinCheckedPlatformVersion(24)
                .build();
        try {
            ApkVerifier.Result result = verifier.verify();
            if (!result.isVerified()) {
                return false;
            }
            return Arrays.equals(result.getSignerCertificates().get(0).getEncoded(), CERTIFICATE);
        } catch (Throwable t) {
            t.printStackTrace();
            return true;
        }
    }

    public static void onSceneView(SceneFragment scene) {
        if (isEnabled()) {
            Map<String, String> properties = new HashMap<>();
            properties.put("scene_simple_class", scene.getClass().getSimpleName());
            properties.put("scene_class", scene.getClass().getName());
            com.microsoft.appcenter.analytics.Analytics.trackEvent("scene_view", properties);
        }
    }
}