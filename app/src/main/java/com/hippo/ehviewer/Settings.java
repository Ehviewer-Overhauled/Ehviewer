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

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.data.FavListUrlBuilder;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.NumberUtils;

import java.util.Locale;
import java.util.Set;

public class Settings {

    /********************
     ****** Eh
     ********************/

    public static final String KEY_THEME = "theme";

    public static final String KEY_ACCOUNT = "account";
    public static final String KEY_BLACK_DARK_THEME = "black_dark_theme";
    public static final int THEME_SYSTEM = -1;
    public static final String KEY_GALLERY_SITE = "gallery_site";
    public static final String KEY_LIST_MODE = "list_mode";
    public static final String KEY_DETAIL_SIZE = "detail_size";
    public static final String KEY_THUMB_SIZE = "thumb_size_";
    public static final String KEY_THUMB_RESOLUTION = "thumb_resolution";
    public static final String KEY_SHOW_TAG_TRANSLATIONS = "show_tag_translations";
    public static final String KEY_DEFAULT_CATEGORIES = "default_categories";
    public static final int DEFAULT_DEFAULT_CATEGORIES = EhConfig.ALL_CATEGORY;
    public static final String KEY_EXCLUDED_TAG_NAMESPACES = "excluded_tag_namespaces";
    public static final String KEY_EXCLUDED_LANGUAGES = "excluded_languages";
    /********************
     ****** Privacy and Security
     ********************/
    public static final String KEY_SEC_SECURITY = "enable_secure";
    public static final boolean VALUE_SEC_SECURITY = false;
    /********************
     ****** Download
     ********************/
    public static final String KEY_DOWNLOAD_SAVE_SCHEME = "image_scheme";
    public static final String KEY_DOWNLOAD_SAVE_AUTHORITY = "image_authority";
    public static final String KEY_DOWNLOAD_SAVE_PATH = "image_path";
    public static final String KEY_NOTIFICATION_REQUIRED = "notification_required";
    public static final String KEY_DOWNLOAD_SAVE_QUERY = "image_query";
    public static final String KEY_DOWNLOAD_SAVE_FRAGMENT = "image_fragment";
    public static final String KEY_MEDIA_SCAN = "media_scan";
    public static final String KEY_IMAGE_RESOLUTION = "image_size";
    public static final String DEFAULT_IMAGE_RESOLUTION = EhConfig.IMAGE_SIZE_AUTO;
    public static final int INVALID_DEFAULT_FAV_SLOT = -2;
    /********************
     ****** Advanced
     ********************/
    public static final String KEY_SAVE_PARSE_ERROR_BODY = "save_parse_error_body";
    public static final String KEY_SECURITY = "require_unlock";
    public static final String KEY_SECURITY_DELAY = "require_unlock_delay";
    public static final String KEY_READ_CACHE_SIZE = "read_cache_size";
    public static final int DEFAULT_READ_CACHE_SIZE = 320;
    public static final String KEY_BUILT_IN_HOSTS = "built_in_hosts_2";
    public static final String KEY_DOMAIN_FRONTING = "domain_fronting";
    public static final String KEY_BYPASS_VPN = "bypass_vpn";
    public static final String KEY_LIST_THUMB_SIZE = "list_tile_size";
    private static final String KEY_HIDE_HV_EVENTS = "hide_hv_events";
    private static final boolean DEFAULT_HIDE_HV_EVENTS = true;
    private static final String KEY_SHOW_COMMENTS = "show_gallery_comments";
    private static final boolean DEFAULT_SHOW_COMMENTS = true;
    private static final String TAG = Settings.class.getSimpleName();
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String DEFAULT_DISPLAY_NAME = null;
    private static final int DEFAULT_LIST_THUMB_SIZE = 40;
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_REMOVE_IMAGE_FILES = "include_pic";
    private static final boolean DEFAULT_REMOVE_IMAGE_FILES = true;
    private static final String KEY_NEED_SIGN_IN = "need_sign_in";
    private static final boolean DEFAULT_NEED_SIGN_IN = true;
    private static final String KEY_SELECT_SITE = "select_site";
    private static final boolean DEFAULT_SELECT_SITE = true;
    private static final int DEFAULT_THEME = THEME_SYSTEM;
    private static final int DEFAULT_GALLERY_SITE = 1;
    private static final String KEY_LAUNCH_PAGE = "launch_page";
    private static final int DEFAULT_LAUNCH_PAGE = 0;
    private static final int DEFAULT_LIST_MODE = 0;
    private static final int DEFAULT_DETAIL_SIZE = 0;
    private static final int DEFAULT_THUMB_SIZE = 120;
    private static final int DEFAULT_THUMB_RESOLUTION = 0;
    private static final String KEY_SHOW_JPN_TITLE = "show_jpn_title";
    private static final boolean DEFAULT_SHOW_JPN_TITLE = false;
    private static final String KEY_SHOW_GALLERY_PAGES = "show_gallery_pages";
    private static final boolean DEFAULT_SHOW_GALLERY_PAGES = false;
    private static final boolean DEFAULT_SHOW_TAG_TRANSLATIONS = false;
    private static final int DEFAULT_EXCLUDED_TAG_NAMESPACES = 0;
    private static final String DEFAULT_EXCLUDED_LANGUAGES = null;
    private static final String KEY_METERED_NETWORK_WARNING = "cellular_network_warning";
    private static final boolean DEFAULT_METERED_NETWORK_WARNING = false;
    private static final String KEY_APP_LINK_VERIFY_TIP = "app_link_verify_tip";
    private static final boolean DEFAULT_APP_LINK_VERIFY_TIP = false;
    private static final boolean DEFAULT_MEDIA_SCAN = false;
    private static final String KEY_RECENT_DOWNLOAD_LABEL = "recent_download_label";
    private static final String DEFAULT_RECENT_DOWNLOAD_LABEL = null;
    private static final String KEY_HAS_DEFAULT_DOWNLOAD_LABEL = "has_default_download_label";
    private static final boolean DEFAULT_HAS_DOWNLOAD_LABEL = false;
    private static final String KEY_DEFAULT_DOWNLOAD_LABEL = "default_download_label";
    private static final String DEFAULT_DOWNLOAD_LABEL = null;
    private static final String KEY_MULTI_THREAD_DOWNLOAD = "download_thread";
    private static final int DEFAULT_MULTI_THREAD_DOWNLOAD = 3;
    private static final String KEY_PRELOAD_IMAGE = "preload_image";
    private static final int DEFAULT_PRELOAD_IMAGE = 5;
    private static final String KEY_DOWNLOAD_ORIGIN_IMAGE = "download_origin_image";
    private static final boolean DEFAULT_DOWNLOAD_ORIGIN_IMAGE = false;
    /********************
     ****** Favorites
     ********************/
    private static final String KEY_FAV_CAT_0 = "fav_cat_0";
    private static final String KEY_FAV_CAT_1 = "fav_cat_1";
    private static final String KEY_FAV_CAT_2 = "fav_cat_2";
    private static final String KEY_FAV_CAT_3 = "fav_cat_3";
    private static final String KEY_FAV_CAT_4 = "fav_cat_4";
    private static final String KEY_FAV_CAT_5 = "fav_cat_5";
    private static final String KEY_FAV_CAT_6 = "fav_cat_6";
    private static final String KEY_FAV_CAT_7 = "fav_cat_7";
    private static final String KEY_FAV_CAT_8 = "fav_cat_8";
    private static final String KEY_FAV_CAT_9 = "fav_cat_9";
    private static final String DEFAULT_FAV_CAT_0 = "Favorites 0";
    private static final String DEFAULT_FAV_CAT_1 = "Favorites 1";
    private static final String DEFAULT_FAV_CAT_2 = "Favorites 2";
    private static final String DEFAULT_FAV_CAT_3 = "Favorites 3";
    private static final String DEFAULT_FAV_CAT_4 = "Favorites 4";
    private static final String DEFAULT_FAV_CAT_5 = "Favorites 5";
    private static final String DEFAULT_FAV_CAT_6 = "Favorites 6";
    private static final String DEFAULT_FAV_CAT_7 = "Favorites 7";
    private static final String DEFAULT_FAV_CAT_8 = "Favorites 8";
    private static final String DEFAULT_FAV_CAT_9 = "Favorites 9";
    private static final String KEY_FAV_COUNT_0 = "fav_count_0";
    private static final String KEY_FAV_COUNT_1 = "fav_count_1";
    private static final String KEY_FAV_COUNT_2 = "fav_count_2";
    private static final String KEY_FAV_COUNT_3 = "fav_count_3";
    private static final String KEY_FAV_COUNT_4 = "fav_count_4";
    private static final String KEY_FAV_COUNT_5 = "fav_count_5";
    private static final String KEY_FAV_COUNT_6 = "fav_count_6";
    private static final String KEY_FAV_COUNT_7 = "fav_count_7";
    private static final String KEY_FAV_COUNT_8 = "fav_count_8";
    private static final String KEY_FAV_COUNT_9 = "fav_count_9";
    private static final String KEY_FAV_LOCAL = "fav_local";
    private static final String KEY_FAV_CLOUD = "fav_cloud";
    private static final int DEFAULT_FAV_COUNT = 0;
    private static final String KEY_RECENT_FAV_CAT = "recent_fav_cat";
    private static final int DEFAULT_RECENT_FAV_CAT = FavListUrlBuilder.FAV_CAT_ALL;
    // -1 for local, 0 - 9 for cloud favorite, other for no default fav slot
    private static final String KEY_DEFAULT_FAV_SLOT = "default_favorite_2";
    private static final int DEFAULT_DEFAULT_FAV_SLOT = INVALID_DEFAULT_FAV_SLOT;
    private static final boolean DEFAULT_SAVE_PARSE_ERROR_BODY = true;
    private static final String KEY_SAVE_CRASH_LOG = "save_crash_log";
    private static final boolean DEFAULT_SAVE_CRASH_LOG = true;
    private static final boolean DEFAULT_BUILT_IN_HOSTS = false;
    private static final boolean DEFAULT_FRONTING = false;
    private static final boolean DEFAULT_BYPASS_VPN = true;
    private static final String KEY_PROXY_TYPE = "proxy_type";
    private static final int DEFAULT_PROXY_TYPE = EhProxySelector.TYPE_DIRECT;
    private static final String KEY_PROXY_IP = "proxy_ip";
    private static final String DEFAULT_PROXY_IP = null;
    private static final String KEY_PROXY_PORT = "proxy_port";
    private static final int DEFAULT_PROXY_PORT = -1;
    private static final String KEY_CLIPBOARD_TEXT_HASH_CODE = "clipboard_text_hash_code";
    private static final int DEFAULT_CLIPBOARD_TEXT_HASH_CODE = 0;
    private static final String KEY_DOWNLOAD_DELAY = "download_delay";
    private static final int DEFAULT_DOWNLOAD_DELAY = 0;
    private static final String KEY_REQUEST_NEWS = "request_news";
    private static final boolean DEFAULT_REQUEST_NEWS = true;
    private static final String KEY_ARCHIVE_PASSWDS = "archive_passwds";
    private static final String KEY_QS_SAVE_PROGRESS = "qs_save_progress";
    private static final boolean DEFAULT_QS_SAVE_PROGRESS = true;
    public static boolean LIST_THUMB_SIZE_INITED = false;
    public static SharedPreferences sSettingsPre;
    private static int LIST_THUMB_SIZE = 40;
    private static EhConfig sEhConfig;

    public static void initialize() {
        sSettingsPre = PreferenceManager.getDefaultSharedPreferences(EhApplication.getApplication());
        sEhConfig = loadEhConfig();
        fixDefaultValue();
    }

    private static void fixDefaultValue() {
        if ("CN".equals(Locale.getDefault().getCountry())) {
            // Enable domain fronting if the country is CN
            if (!sSettingsPre.contains(KEY_BUILT_IN_HOSTS)) {
                putBuiltInHosts(true);
            }
            if (!sSettingsPre.contains(KEY_DOMAIN_FRONTING)) {
                putDF(true);
            }
            // Enable show tag translations if the country is CN
            if (!sSettingsPre.contains(KEY_SHOW_TAG_TRANSLATIONS)) {
                putShowTagTranslations(true);

            }
        }
    }

    private static EhConfig loadEhConfig() {
        EhConfig ehConfig = new EhConfig();
        ehConfig.imageSize = getImageResolution();
        ehConfig.excludedLanguages = getExcludedLanguages();
        ehConfig.defaultCategories = getDefaultCategories();
        ehConfig.excludedNamespaces = getExcludedTagNamespaces();
        ehConfig.setDirty();
        return ehConfig;
    }

    public static boolean getBoolean(String key, boolean defValue) {
        try {
            return sSettingsPre.getBoolean(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putBoolean(String key, boolean value) {
        sSettingsPre.edit().putBoolean(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        try {
            return sSettingsPre.getInt(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putInt(String key, int value) {
        sSettingsPre.edit().putInt(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        try {
            return sSettingsPre.getString(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putString(String key, String value) {
        sSettingsPre.edit().putString(key, value).apply();
    }

    public static Set<String> getStringSet(String key) {
        return sSettingsPre.getStringSet(key, null);
    }

    public static void putStringToStringSet(String key, String value) {
        Set<String> set = getStringSet(key);
        if (set == null)
            set = Set.of(value);
        else if (set.contains(value))
            return;
        else
            set.add(value);
        sSettingsPre.edit().putStringSet(key, set).apply();
    }

    public static int getIntFromStr(String key, int defValue) {
        try {
            return NumberUtils.parseIntSafely(sSettingsPre.getString(key, Integer.toString(defValue)), defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putIntToStr(String key, int value) {
        sSettingsPre.edit().putString(key, Integer.toString(value)).apply();
    }

    @Nullable
    public static String getDisplayName() {
        return getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME);
    }

    public static void putDisplayName(String value) {
        putString(KEY_DISPLAY_NAME, value);
    }

    public static void putAvatar(String value) {
        putString(KEY_AVATAR, value);
    }

    public static boolean getRemoveImageFiles() {
        return getBoolean(KEY_REMOVE_IMAGE_FILES, DEFAULT_REMOVE_IMAGE_FILES);
    }

    public static void putRemoveImageFiles(boolean value) {
        putBoolean(KEY_REMOVE_IMAGE_FILES, value);
    }

    public static EhConfig getEhConfig() {
        return sEhConfig;
    }

    public static boolean getNeedSignIn() {
        return getBoolean(KEY_NEED_SIGN_IN, DEFAULT_NEED_SIGN_IN);
    }

    public static void putNeedSignIn(boolean value) {
        putBoolean(KEY_NEED_SIGN_IN, value);
    }

    public static boolean getSelectSite() {
        return getBoolean(KEY_SELECT_SITE, DEFAULT_SELECT_SITE);
    }

    public static void putSelectSite(boolean value) {
        putBoolean(KEY_SELECT_SITE, value);
    }

    public static int getTheme() {
        return getIntFromStr(KEY_THEME, DEFAULT_THEME);
    }

    public static int getGallerySite() {
        return getIntFromStr(KEY_GALLERY_SITE, DEFAULT_GALLERY_SITE);
    }

    public static void putGallerySite(int value) {
        putIntToStr(KEY_GALLERY_SITE, value);
    }

    public static String getLaunchPageGalleryListSceneAction() {
        int value = getIntFromStr(KEY_LAUNCH_PAGE, DEFAULT_LAUNCH_PAGE);
        return switch (value) {
            case 0 -> GalleryListScene.ACTION_HOMEPAGE;
            case 1 -> GalleryListScene.ACTION_SUBSCRIPTION;
            case 2 -> GalleryListScene.ACTION_WHATS_HOT;
            case 3 -> GalleryListScene.ACTION_TOP_LIST;
            default -> throw new IllegalStateException("Unexpected value: " + value);
        };
    }

    public static int getListMode() {
        return getIntFromStr(KEY_LIST_MODE, DEFAULT_LIST_MODE);
    }

    public static int getDetailSize() {
        return getIntFromStr(KEY_DETAIL_SIZE, DEFAULT_DETAIL_SIZE);
    }

    @DimenRes
    public static int getDetailSizeResId() {
        return switch (getDetailSize()) {
            case 0 -> R.dimen.gallery_list_column_width_long;
            case 1 -> R.dimen.gallery_list_column_width_short;
            default -> throw new IllegalStateException("Unexpected value: " + getDetailSize());
        };
    }

    public static int getThumbSize() {
        return dip2px(getInt(KEY_THUMB_SIZE, DEFAULT_THUMB_SIZE));
    }

    public static int dip2px(int dpValue) {
        final float scale = EhApplication.getApplication().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int getThumbResolution() {
        return getIntFromStr(KEY_THUMB_RESOLUTION, DEFAULT_THUMB_RESOLUTION);
    }

    public static boolean getShowComments() {
        return getBoolean(KEY_SHOW_COMMENTS, DEFAULT_SHOW_COMMENTS);
    }

    public static boolean getRequestNews() {
        return getBoolean(KEY_REQUEST_NEWS, DEFAULT_REQUEST_NEWS);
    }

    public static boolean getHideHvEvents() {
        return getBoolean(KEY_HIDE_HV_EVENTS, DEFAULT_HIDE_HV_EVENTS);
    }

    public static boolean getShowJpnTitle() {
        return getBoolean(KEY_SHOW_JPN_TITLE, DEFAULT_SHOW_JPN_TITLE);
    }

    public static boolean getShowGalleryPages() {
        return getBoolean(KEY_SHOW_GALLERY_PAGES, DEFAULT_SHOW_GALLERY_PAGES);
    }

    public static boolean getShowTagTranslations() {
        return getBoolean(KEY_SHOW_TAG_TRANSLATIONS, DEFAULT_SHOW_TAG_TRANSLATIONS);
    }

    public static void putShowTagTranslations(boolean value) {
        putBoolean(KEY_SHOW_TAG_TRANSLATIONS, value);
    }

    public static int getDefaultCategories() {
        return getInt(KEY_DEFAULT_CATEGORIES, DEFAULT_DEFAULT_CATEGORIES);
    }

    public static int getExcludedTagNamespaces() {
        return getInt(KEY_EXCLUDED_TAG_NAMESPACES, DEFAULT_EXCLUDED_TAG_NAMESPACES);
    }

    public static String getExcludedLanguages() {
        return getString(KEY_EXCLUDED_LANGUAGES, DEFAULT_EXCLUDED_LANGUAGES);
    }

    public static boolean getMeteredNetworkWarning() {
        return getBoolean(KEY_METERED_NETWORK_WARNING, DEFAULT_METERED_NETWORK_WARNING);
    }

    public static boolean getAppLinkVerifyTip() {
        return getBoolean(KEY_APP_LINK_VERIFY_TIP, DEFAULT_APP_LINK_VERIFY_TIP);
    }

    public static void putAppLinkVerifyTip(boolean value) {
        putBoolean(KEY_APP_LINK_VERIFY_TIP, value);
    }

    public static boolean getEnabledSecurity() {
        return getBoolean(KEY_SEC_SECURITY, VALUE_SEC_SECURITY);
    }

    @Nullable
    public static UniFile getDownloadLocation() {
        UniFile dir;
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(getString(KEY_DOWNLOAD_SAVE_SCHEME, null));
        builder.encodedAuthority(getString(KEY_DOWNLOAD_SAVE_AUTHORITY, null));
        builder.encodedPath(getString(KEY_DOWNLOAD_SAVE_PATH, null));
        builder.encodedQuery(getString(KEY_DOWNLOAD_SAVE_QUERY, null));
        builder.encodedFragment(getString(KEY_DOWNLOAD_SAVE_FRAGMENT, null));
        dir = UniFile.fromUri(EhApplication.getApplication(), builder.build());
        return dir != null ? dir : UniFile.fromFile(AppConfig.getDefaultDownloadDir());
    }

    public static void putDownloadLocation(@NonNull UniFile location) {
        Uri uri = location.getUri();
        putString(KEY_DOWNLOAD_SAVE_SCHEME, uri.getScheme());
        putString(KEY_DOWNLOAD_SAVE_AUTHORITY, uri.getEncodedAuthority());
        putString(KEY_DOWNLOAD_SAVE_PATH, uri.getEncodedPath());
        putString(KEY_DOWNLOAD_SAVE_QUERY, uri.getEncodedQuery());
        putString(KEY_DOWNLOAD_SAVE_FRAGMENT, uri.getEncodedFragment());

        if (getMediaScan()) {
            CommonOperations.removeNoMediaFile(location);
        } else {
            CommonOperations.ensureNoMediaFile(location);
        }
    }

    public static boolean getMediaScan() {
        return getBoolean(KEY_MEDIA_SCAN, DEFAULT_MEDIA_SCAN);
    }

    public static String getRecentDownloadLabel() {
        return getString(KEY_RECENT_DOWNLOAD_LABEL, DEFAULT_RECENT_DOWNLOAD_LABEL);
    }

    public static void putRecentDownloadLabel(String value) {
        putString(KEY_RECENT_DOWNLOAD_LABEL, value);
    }

    public static boolean getHasDefaultDownloadLabel() {
        return getBoolean(KEY_HAS_DEFAULT_DOWNLOAD_LABEL, DEFAULT_HAS_DOWNLOAD_LABEL);
    }

    public static void putHasDefaultDownloadLabel(boolean hasDefaultDownloadLabel) {
        putBoolean(KEY_HAS_DEFAULT_DOWNLOAD_LABEL, hasDefaultDownloadLabel);
    }

    public static String getDefaultDownloadLabel() {
        return getString(KEY_DEFAULT_DOWNLOAD_LABEL, DEFAULT_DOWNLOAD_LABEL);
    }

    public static void putDefaultDownloadLabel(String value) {
        putString(KEY_DEFAULT_DOWNLOAD_LABEL, value);
    }

    public static int getMultiThreadDownload() {
        return getIntFromStr(KEY_MULTI_THREAD_DOWNLOAD, DEFAULT_MULTI_THREAD_DOWNLOAD);
    }

    public static int getDownloadDelay() {
        return getIntFromStr(KEY_DOWNLOAD_DELAY, DEFAULT_DOWNLOAD_DELAY);
    }

    public static int getPreloadImage() {
        return getIntFromStr(KEY_PRELOAD_IMAGE, DEFAULT_PRELOAD_IMAGE);
    }

    public static String getImageResolution() {
        return getString(KEY_IMAGE_RESOLUTION, DEFAULT_IMAGE_RESOLUTION);
    }

    public static void putImageResolution(String value) {
        sEhConfig.imageSize = value;
        sEhConfig.setDirty();
        putString(KEY_IMAGE_RESOLUTION, value);
    }

    public static boolean getDownloadOriginImage() {
        return getBoolean(KEY_DOWNLOAD_ORIGIN_IMAGE, DEFAULT_DOWNLOAD_ORIGIN_IMAGE);
    }

    public static String[] getFavCat() {
        String[] favCat = new String[10];
        favCat[0] = sSettingsPre.getString(KEY_FAV_CAT_0, DEFAULT_FAV_CAT_0);
        favCat[1] = sSettingsPre.getString(KEY_FAV_CAT_1, DEFAULT_FAV_CAT_1);
        favCat[2] = sSettingsPre.getString(KEY_FAV_CAT_2, DEFAULT_FAV_CAT_2);
        favCat[3] = sSettingsPre.getString(KEY_FAV_CAT_3, DEFAULT_FAV_CAT_3);
        favCat[4] = sSettingsPre.getString(KEY_FAV_CAT_4, DEFAULT_FAV_CAT_4);
        favCat[5] = sSettingsPre.getString(KEY_FAV_CAT_5, DEFAULT_FAV_CAT_5);
        favCat[6] = sSettingsPre.getString(KEY_FAV_CAT_6, DEFAULT_FAV_CAT_6);
        favCat[7] = sSettingsPre.getString(KEY_FAV_CAT_7, DEFAULT_FAV_CAT_7);
        favCat[8] = sSettingsPre.getString(KEY_FAV_CAT_8, DEFAULT_FAV_CAT_8);
        favCat[9] = sSettingsPre.getString(KEY_FAV_CAT_9, DEFAULT_FAV_CAT_9);
        return favCat;
    }

    public static void putFavCat(String[] value) {
        AssertUtils.assertEquals(10, value.length);
        sSettingsPre.edit()
                .putString(KEY_FAV_CAT_0, value[0])
                .putString(KEY_FAV_CAT_1, value[1])
                .putString(KEY_FAV_CAT_2, value[2])
                .putString(KEY_FAV_CAT_3, value[3])
                .putString(KEY_FAV_CAT_4, value[4])
                .putString(KEY_FAV_CAT_5, value[5])
                .putString(KEY_FAV_CAT_6, value[6])
                .putString(KEY_FAV_CAT_7, value[7])
                .putString(KEY_FAV_CAT_8, value[8])
                .putString(KEY_FAV_CAT_9, value[9])
                .apply();
    }

    public static int[] getFavCount() {
        int[] favCount = new int[10];
        favCount[0] = sSettingsPre.getInt(KEY_FAV_COUNT_0, DEFAULT_FAV_COUNT);
        favCount[1] = sSettingsPre.getInt(KEY_FAV_COUNT_1, DEFAULT_FAV_COUNT);
        favCount[2] = sSettingsPre.getInt(KEY_FAV_COUNT_2, DEFAULT_FAV_COUNT);
        favCount[3] = sSettingsPre.getInt(KEY_FAV_COUNT_3, DEFAULT_FAV_COUNT);
        favCount[4] = sSettingsPre.getInt(KEY_FAV_COUNT_4, DEFAULT_FAV_COUNT);
        favCount[5] = sSettingsPre.getInt(KEY_FAV_COUNT_5, DEFAULT_FAV_COUNT);
        favCount[6] = sSettingsPre.getInt(KEY_FAV_COUNT_6, DEFAULT_FAV_COUNT);
        favCount[7] = sSettingsPre.getInt(KEY_FAV_COUNT_7, DEFAULT_FAV_COUNT);
        favCount[8] = sSettingsPre.getInt(KEY_FAV_COUNT_8, DEFAULT_FAV_COUNT);
        favCount[9] = sSettingsPre.getInt(KEY_FAV_COUNT_9, DEFAULT_FAV_COUNT);
        return favCount;
    }

    public static void putFavCount(int[] count) {
        AssertUtils.assertEquals(10, count.length);
        sSettingsPre.edit()
                .putInt(KEY_FAV_COUNT_0, count[0])
                .putInt(KEY_FAV_COUNT_1, count[1])
                .putInt(KEY_FAV_COUNT_2, count[2])
                .putInt(KEY_FAV_COUNT_3, count[3])
                .putInt(KEY_FAV_COUNT_4, count[4])
                .putInt(KEY_FAV_COUNT_5, count[5])
                .putInt(KEY_FAV_COUNT_6, count[6])
                .putInt(KEY_FAV_COUNT_7, count[7])
                .putInt(KEY_FAV_COUNT_8, count[8])
                .putInt(KEY_FAV_COUNT_9, count[9])
                .apply();
    }

    public static int getFavLocalCount() {
        return sSettingsPre.getInt(KEY_FAV_LOCAL, DEFAULT_FAV_COUNT);
    }

    public static void putFavLocalCount(int count) {
        sSettingsPre.edit().putInt(KEY_FAV_LOCAL, count).apply();
    }

    public static int getFavCloudCount() {
        return sSettingsPre.getInt(KEY_FAV_CLOUD, DEFAULT_FAV_COUNT);
    }

    public static void putFavCloudCount(int count) {
        sSettingsPre.edit().putInt(KEY_FAV_CLOUD, count).apply();
    }

    public static int getRecentFavCat() {
        return getInt(KEY_RECENT_FAV_CAT, DEFAULT_RECENT_FAV_CAT);
    }

    public static void putRecentFavCat(int value) {
        putInt(KEY_RECENT_FAV_CAT, value);
    }

    public static int getDefaultFavSlot() {
        return getInt(KEY_DEFAULT_FAV_SLOT, DEFAULT_DEFAULT_FAV_SLOT);
    }

    public static void putDefaultFavSlot(int value) {
        putInt(KEY_DEFAULT_FAV_SLOT, value);
    }

    public static boolean getQSSaveProgress() {
        return getBoolean(KEY_QS_SAVE_PROGRESS, DEFAULT_QS_SAVE_PROGRESS);
    }

    public static void putQSSaveProgress(boolean value) {
        putBoolean(KEY_QS_SAVE_PROGRESS, value);
    }

    public static boolean getSaveParseErrorBody() {
        return getBoolean(KEY_SAVE_PARSE_ERROR_BODY, DEFAULT_SAVE_PARSE_ERROR_BODY);
    }

    public static boolean getSaveCrashLog() {
        return getBoolean(KEY_SAVE_CRASH_LOG, DEFAULT_SAVE_CRASH_LOG);
    }

    public static boolean getSecurity() {
        return getBoolean(KEY_SECURITY, false);
    }

    public static void putSecurity(boolean value) {
        putBoolean(KEY_SECURITY, value);
    }

    public static int getSecurityDelay() {
        return getInt(KEY_SECURITY_DELAY, 0);
    }

    public static int getReadCacheSize() {
        return getIntFromStr(KEY_READ_CACHE_SIZE, DEFAULT_READ_CACHE_SIZE);
    }

    public static boolean getBuiltInHosts() {
        return getBoolean(KEY_BUILT_IN_HOSTS, DEFAULT_BUILT_IN_HOSTS);
    }

    public static void putBuiltInHosts(boolean value) {
        putBoolean(KEY_BUILT_IN_HOSTS, value);
    }

    public static boolean getDF() {
        return getBoolean(KEY_DOMAIN_FRONTING, DEFAULT_FRONTING);
    }

    public static void putDF(boolean value) {
        putBoolean(KEY_DOMAIN_FRONTING, value);
    }

    public static boolean getBypassVPN() {
        return getBoolean(KEY_BYPASS_VPN, DEFAULT_BYPASS_VPN);
    }

    private static int getProxyTypeInternal() {
        return getInt(KEY_PROXY_TYPE, DEFAULT_PROXY_TYPE);
    }

    public static int getProxyType() {
        return getProxyTypeInternal() == EhProxySelector.TYPE_HTTP ? EhProxySelector.TYPE_HTTP : DEFAULT_PROXY_TYPE;
    }

    public static void putProxyType(int value) {
        putInt(KEY_PROXY_TYPE, value);
    }

    public static String getProxyIp() {
        return getString(KEY_PROXY_IP, DEFAULT_PROXY_IP);
    }

    public static void putProxyIp(String value) {
        putString(KEY_PROXY_IP, value);
    }

    public static int getProxyPort() {
        return getInt(KEY_PROXY_PORT, DEFAULT_PROXY_PORT);
    }

    public static void putProxyPort(int value) {
        putInt(KEY_PROXY_PORT, value);
    }

    public static int getClipboardTextHashCode() {
        return getInt(KEY_CLIPBOARD_TEXT_HASH_CODE, DEFAULT_CLIPBOARD_TEXT_HASH_CODE);
    }

    public static void putClipboardTextHashCode(int value) {
        putInt(KEY_CLIPBOARD_TEXT_HASH_CODE, value);
    }

    public static Set<String> getArchivePasswds() {
        return getStringSet(KEY_ARCHIVE_PASSWDS);
    }

    public static void putPasswdToArchivePasswds(String value) {
        putStringToStringSet(KEY_ARCHIVE_PASSWDS, value);
    }

    public static int getListThumbSize() {
        if (LIST_THUMB_SIZE_INITED) {
            return LIST_THUMB_SIZE;
        }
        int size = 3 * getInt(KEY_LIST_THUMB_SIZE, DEFAULT_LIST_THUMB_SIZE);
        LIST_THUMB_SIZE = size;
        LIST_THUMB_SIZE_INITED = true;
        return size;
    }

    public static boolean getNotificationRequired() {
        return getBoolean(KEY_NOTIFICATION_REQUIRED, false);
    }

    public static void putNotificationRequired() {
        putBoolean(KEY_NOTIFICATION_REQUIRED, true);
    }
}
