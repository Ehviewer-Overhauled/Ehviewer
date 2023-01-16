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

import androidx.annotation.NonNull;

import com.hippo.ehviewer.Settings;
import com.hippo.network.UrlBuilder;

public class EhUrl {

    public static final int SITE_E = 0;
    public static final int SITE_EX = 1;

    public static final String DOMAIN_EX = "exhentai.org";
    public static final String DOMAIN_E = "e-hentai.org";
    public static final String DOMAIN_LOFI = "lofi.e-hentai.org";

    public static final String HOST_EX = "https://" + DOMAIN_EX + "/";
    public static final String API_EX = HOST_EX + "api.php";
    public static final String URL_FAVORITES_EX = HOST_EX + "favorites.php";
    public static final String URL_UCONFIG_EX = HOST_EX + "uconfig.php";
    public static final String URL_MY_TAGS_EX = HOST_EX + "mytags";
    public static final String URL_WATCHED_EX = HOST_EX + "watched";
    public static final String HOST_E = "https://" + DOMAIN_E + "/";
    public static final String API_E = HOST_E + "api.php";
    public static final String URL_FAVORITES_E = HOST_E + "favorites.php";
    public static final String URL_UCONFIG_E = HOST_E + "uconfig.php";
    public static final String URL_MY_TAGS_E = HOST_E + "mytags";
    public static final String URL_WATCHED_E = HOST_E + "watched";
    public static final String API_SIGN_IN = "https://forums.e-hentai.org/index.php?act=Login&CODE=01";
    public static final String URL_POPULAR_E = "https://e-hentai.org/popular";
    public static final String URL_POPULAR_EX = "https://exhentai.org/popular";
    public static final String URL_IMAGE_SEARCH_E = "https://upload.e-hentai.org/image_lookup.php";
    public static final String URL_IMAGE_SEARCH_EX = "https://exhentai.org/upload/image_lookup.php";
    public static final String URL_SIGN_IN = "https://forums.e-hentai.org/index.php?act=Login";
    public static final String URL_REGISTER = "https://forums.e-hentai.org/index.php?act=Reg&CODE=00";
    public static final String URL_FORUMS = "https://forums.e-hentai.org/";
    public static final String REFERER_EX = "https://" + DOMAIN_EX;
    public static final String ORIGIN_EX = REFERER_EX;
    public static final String REFERER_E = "https://" + DOMAIN_E;
    public static final String ORIGIN_E = REFERER_E;

    public static String getGalleryDetailUrl(long gid, String token) {
        return getGalleryDetailUrl(gid, token, 0, false);
    }

    public static String getHost() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return HOST_E;
            case SITE_EX:
                return HOST_EX;
        }
    }

    public static String getFavoritesUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return URL_FAVORITES_E;
            case SITE_EX:
                return URL_FAVORITES_EX;
        }
    }

    public static String getApiUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return API_E;
            case SITE_EX:
                return API_EX;
        }
    }

    public static String getReferer() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return REFERER_E;
            case SITE_EX:
                return REFERER_EX;
        }
    }

    public static String getOrigin() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return ORIGIN_E;
            case SITE_EX:
                return ORIGIN_EX;
        }
    }

    public static String getUConfigUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return URL_UCONFIG_E;
            case SITE_EX:
                return URL_UCONFIG_EX;
        }
    }

    public static String getMyTagsUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return URL_MY_TAGS_E;
            case SITE_EX:
                return URL_MY_TAGS_EX;
        }
    }

    public static String getGalleryDetailUrl(long gid, String token, int index, boolean allComment) {
        UrlBuilder builder = new UrlBuilder(getHost() + "g/" + gid + '/' + token + '/');
        if (index != 0) {
            builder.addQuery("p", index);
        }
        if (allComment) {
            builder.addQuery("hc", 1);
        }
        return builder.build();
    }

    public static String getGalleryMultiPageViewerUrl(long gid, String token) {
        UrlBuilder builder = new UrlBuilder(getHost() + "mpv/" + gid + '/' + token + '/');
        return builder.build();
    }

    public static String getPageUrl(long gid, int index, String pToken) {
        return getHost() + "s/" + pToken + '/' + gid + '-' + (index + 1);
    }

    public static String getAddFavorites(long gid, String token) {
        return getHost() + "gallerypopups.php?gid=" + gid + "&t=" + token + "&act=addfav";
    }

    public static String getDownloadArchive(long gid, String token, String or) {
        return getHost() + "archiver.php?gid=" + gid + "&token=" + token + "&or=" + or;
    }

    public static String getTagDefinitionUrl(String tag) {
        return "https://ehwiki.org/wiki/" + tag.replace(' ', '_');
    }

    @NonNull
    public static String getPopularUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return URL_POPULAR_E;
            case SITE_EX:
                return URL_POPULAR_EX;
        }
    }

    @NonNull
    public static String getImageSearchUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return URL_IMAGE_SEARCH_E;
            case SITE_EX:
                return URL_IMAGE_SEARCH_EX;
        }
    }

    @NonNull
    public static String getWatchedUrl() {
        switch (Settings.getGallerySite()) {
            default:
            case SITE_E:
                return URL_WATCHED_E;
            case SITE_EX:
                return URL_WATCHED_EX;
        }
    }
}
