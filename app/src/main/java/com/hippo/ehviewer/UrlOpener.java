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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

import com.hippo.ehviewer.client.EhUrlOpener;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.ui.MainActivity;
import com.hippo.scene.Announcer;
import com.hippo.scene.StageActivity;

import rikka.core.res.ResourcesKt;

public final class UrlOpener {

    private UrlOpener() {
    }

    public static void openUrl(@NonNull Context context, String url, boolean ehUrl) {
        openUrl(context, url, ehUrl, null);
    }

    public static void openUrl(@NonNull Context context, String url, boolean ehUrl, GalleryDetail galleryDetail) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        Intent intent;
        Uri uri = Uri.parse(url);

        if (ehUrl) {
            if (galleryDetail != null) {
                GalleryPageUrlParser.Result result = GalleryPageUrlParser.parse(url);
                if (result != null) {
                    if (result.gid == galleryDetail.gid) {
                        intent = new Intent(context, GalleryActivity.class);
                        intent.setAction(GalleryActivity.ACTION_EH);
                        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, galleryDetail);
                        intent.putExtra(GalleryActivity.KEY_PAGE, result.page);
                        context.startActivity(intent);
                        return;
                    }
                } else if (url.startsWith("#c")) {
                    try {
                        intent = new Intent(context, GalleryActivity.class);
                        intent.setAction(GalleryActivity.ACTION_EH);
                        intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, galleryDetail);
                        intent.putExtra(GalleryActivity.KEY_PAGE, Integer.parseInt(url.replace("#c", "")) - 1);
                        context.startActivity(intent);
                        return;
                    } catch (NumberFormatException e) {
                        //
                    }
                }
            }
            Announcer announcer = EhUrlOpener.parseUrl(url);
            if (null != announcer) {
                intent = new Intent(context, MainActivity.class);
                intent.setAction(StageActivity.ACTION_START_SCENE);
                intent.putExtra(StageActivity.KEY_SCENE_NAME, announcer.getClazz().getName());
                intent.putExtra(StageActivity.KEY_SCENE_ARGS, announcer.getArgs());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        boolean isNight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) > 0;
        CustomTabsIntent.Builder customTabsIntent = new CustomTabsIntent.Builder();
        customTabsIntent.setShowTitle(true);
        CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ResourcesKt.resolveColor(context.getTheme(), R.attr.toolbarColor))
                .build();
        customTabsIntent.setDefaultColorSchemeParams(params);
        customTabsIntent.setColorScheme(isNight ? CustomTabsIntent.COLOR_SCHEME_DARK : CustomTabsIntent.COLOR_SCHEME_LIGHT);
        try {
            customTabsIntent.build().launchUrl(context, uri);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_browser_installed, Toast.LENGTH_LONG).show();
        }
    }
}
