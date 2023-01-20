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

package com.hippo.ehviewer.client;

import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryInfo;

import java.util.regex.Pattern;

public class EhUtils {

    public static final int NONE = -1; // Use it for homepage
    public static final int PRIVATE = 0x400;
    public static final int UNKNOWN = 0x800;

    public static final int BG_COLOR_DOUJINSHI = 0xfff44336;
    public static final int BG_COLOR_MANGA = 0xffff9800;
    public static final int BG_COLOR_ARTIST_CG = 0xfffbc02d;
    public static final int BG_COLOR_GAME_CG = 0xff4caf50;
    public static final int BG_COLOR_WESTERN = 0xff8bc34a;
    public static final int BG_COLOR_NON_H = 0xff2196f3;
    public static final int BG_COLOR_IMAGE_SET = 0xff3f51b5;
    public static final int BG_COLOR_COSPLAY = 0xff9c27b0;
    public static final int BG_COLOR_ASIAN_PORN = 0xff9575cd;
    public static final int BG_COLOR_MISC = 0xfff06292;
    public static final int BG_COLOR_UNKNOWN = Color.BLACK;

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff
    public static final Pattern PATTERN_TITLE_PREFIX = Pattern.compile(
            "^(?:\\([^)]*\\)|\\[[^]]*]|\\{[^}]*\\}|~[^~]*~|\\s+)*");
    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff and something like ch. 1-23
    public static final Pattern PATTERN_TITLE_SUFFIX = Pattern.compile(
            "(?:\\s+ch.[\\s\\d-]+)?(?:\\([^)]*\\)|\\[[^]]*]|\\{[^}]*\\}|~[^~]*~|\\s+)*$",
            Pattern.CASE_INSENSITIVE);

    private static final int[] CATEGORY_VALUES = {
            EhConfig.MISC,
            EhConfig.DOUJINSHI,
            EhConfig.MANGA,
            EhConfig.ARTIST_CG,
            EhConfig.GAME_CG,
            EhConfig.IMAGE_SET,
            EhConfig.COSPLAY,
            EhConfig.ASIAN_PORN,
            EhConfig.NON_H,
            EhConfig.WESTERN,
            PRIVATE,
            UNKNOWN};

    private static final String[][] CATEGORY_STRINGS = {
            new String[]{"misc"},
            new String[]{"doujinshi"},
            new String[]{"manga"},
            new String[]{"artistcg", "Artist CG Sets", "Artist CG"},
            new String[]{"gamecg", "Game CG Sets", "Game CG"},
            new String[]{"imageset", "Image Sets", "Image Set"},
            new String[]{"cosplay"},
            new String[]{"asianporn", "Asian Porn"},
            new String[]{"non-h"},
            new String[]{"western"},
            new String[]{"private"},
            new String[]{"unknown"}
    };

    public static int getCategory(String type) {
        int i;
        for (i = 0; i < CATEGORY_STRINGS.length - 1; i++) {
            for (String str : CATEGORY_STRINGS[i])
                if (str.equalsIgnoreCase(type))
                    return CATEGORY_VALUES[i];
        }

        return CATEGORY_VALUES[i];
    }

    @NonNull
    public static String getCategory(int type) {
        int i;
        for (i = 0; i < CATEGORY_VALUES.length - 1; i++) {
            if (CATEGORY_VALUES[i] == type)
                break;
        }
        return CATEGORY_STRINGS[i][0];
    }

    public static int getCategoryColor(int category) {
        return switch (category) {
            case EhConfig.DOUJINSHI -> BG_COLOR_DOUJINSHI;
            case EhConfig.MANGA -> BG_COLOR_MANGA;
            case EhConfig.ARTIST_CG -> BG_COLOR_ARTIST_CG;
            case EhConfig.GAME_CG -> BG_COLOR_GAME_CG;
            case EhConfig.WESTERN -> BG_COLOR_WESTERN;
            case EhConfig.NON_H -> BG_COLOR_NON_H;
            case EhConfig.IMAGE_SET -> BG_COLOR_IMAGE_SET;
            case EhConfig.COSPLAY -> BG_COLOR_COSPLAY;
            case EhConfig.ASIAN_PORN -> BG_COLOR_ASIAN_PORN;
            case EhConfig.MISC -> BG_COLOR_MISC;
            default -> BG_COLOR_UNKNOWN;
        };
    }

    public static void signOut() {
        EhApplication.getEhCookieStore().signOut();
        Settings.putAvatar(null);
        Settings.putDisplayName(null);
        Settings.putGallerySite(EhUrl.SITE_E);
        Settings.putNeedSignIn(true);
    }

    public static boolean needSignedIn() {
        return Settings.getNeedSignIn() && !EhApplication.getEhCookieStore().hasSignedIn();
    }

    public static String getSuitableTitle(GalleryInfo gi) {
        if (Settings.getShowJpnTitle()) {
            return TextUtils.isEmpty(gi.getTitleJpn()) ? gi.getTitle() : gi.getTitleJpn();
        } else {
            return TextUtils.isEmpty(gi.getTitle()) ? gi.getTitleJpn() : gi.getTitle();
        }
    }

    @Nullable
    public static String extractTitle(String title) {
        if (null == title) {
            return null;
        }
        title = PATTERN_TITLE_PREFIX.matcher(title).replaceFirst("");
        title = PATTERN_TITLE_SUFFIX.matcher(title).replaceFirst("");
        // Sometimes title is combined by romaji and english translation.
        // Only need romaji.
        // TODO But not sure every '|' means that
        int index = title.indexOf('|');
        if (index >= 0) {
            title = title.substring(0, index);
        }
        if (title.isEmpty()) {
            return null;
        } else {
            return title;
        }
    }

    public static String handleThumbUrlResolution(String url) {
        if (null == url) {
            return null;
        }

        String resolution;
        switch (Settings.getThumbResolution()) {
            default:
            case 0: // Auto
                return url;
            case 1: // 250
                resolution = "250";
                break;
            case 2: // 300
                resolution = "300";
                break;
        }

        int index1 = url.lastIndexOf('_');
        int index2 = url.lastIndexOf('.');
        if (index1 >= 0 && index2 >= 0 && index1 < index2) {
            return url.substring(0, index1 + 1) + resolution + url.substring(index2);
        } else {
            return url;
        }
    }
}
