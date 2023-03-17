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
package com.hippo.ehviewer

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.annotation.DimenRes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.hippo.ehviewer.EhApplication.Companion.application
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.ui.CommonOperations.ensureNoMediaFile
import com.hippo.ehviewer.ui.CommonOperations.removeNoMediaFile
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.unifile.UniFile
import com.hippo.yorozuya.NumberUtils
import java.util.Locale

object Settings {
    /********************
     * Eh
     */
    const val KEY_THEME = "theme"
    const val KEY_ACCOUNT = "account"
    const val KEY_IMAGE_LIMITS = "image_limits"
    const val KEY_U_CONFIG = "uconfig"
    const val KEY_MY_TAGS = "mytags"
    const val KEY_BLACK_DARK_THEME = "black_dark_theme"
    const val DEFAULT_BLACK_DARK_THEME = false
    const val THEME_SYSTEM = -1
    const val KEY_GALLERY_SITE = "gallery_site"
    const val KEY_LIST_MODE = "list_mode"
    const val KEY_DETAIL_SIZE = "detail_size"
    const val KEY_THUMB_SIZE = "thumb_size_"
    const val KEY_THUMB_RESOLUTION = "thumb_resolution"
    const val KEY_SHOW_JPN_TITLE = "show_jpn_title"
    const val KEY_SHOW_TAG_TRANSLATIONS = "show_tag_translations"
    const val KEY_TAG_TRANSLATIONS_SOURCE = "tag_translations_source"
    const val KEY_REQUEST_NEWS = "request_news"
    const val KEY_REQUEST_NEWS_TIMER = "request_news_timer"
    const val KEY_REQUEST_NEWS_TIMER_HOUR = "request_news_timer_hour"
    const val KEY_REQUEST_NEWS_TIMER_MINUTE = "request_news_timer_minute"
    const val KEY_HIDE_HV_EVENTS = "hide_hv_events"
    val SIGN_IN_REQUIRED = arrayOf(
        KEY_IMAGE_LIMITS,
        KEY_GALLERY_SITE,
        KEY_U_CONFIG,
        KEY_MY_TAGS,
        KEY_SHOW_JPN_TITLE,
        KEY_REQUEST_NEWS,
        KEY_REQUEST_NEWS_TIMER,
        KEY_HIDE_HV_EVENTS
    )

    /********************
     * Privacy and Security
     */
    const val KEY_SEC_SECURITY = "enable_secure"
    const val VALUE_SEC_SECURITY = false

    /********************
     * Download
     */
    const val KEY_DOWNLOAD_SAVE_SCHEME = "image_scheme"
    const val KEY_DOWNLOAD_SAVE_AUTHORITY = "image_authority"
    const val KEY_DOWNLOAD_SAVE_PATH = "image_path"
    const val KEY_NOTIFICATION_REQUIRED = "notification_required"
    const val KEY_DOWNLOAD_SAVE_QUERY = "image_query"
    const val KEY_DOWNLOAD_SAVE_FRAGMENT = "image_fragment"
    const val KEY_MEDIA_SCAN = "media_scan"
    const val INVALID_DEFAULT_FAV_SLOT = -2

    /********************
     * Advanced
     */
    const val KEY_SAVE_PARSE_ERROR_BODY = "save_parse_error_body"
    const val KEY_SECURITY = "require_unlock"
    const val KEY_SECURITY_DELAY = "require_unlock_delay"
    const val KEY_READ_CACHE_SIZE = "read_cache_size"
    const val DEFAULT_READ_CACHE_SIZE = 640
    const val KEY_BUILT_IN_HOSTS = "built_in_hosts_2"
    const val KEY_DOMAIN_FRONTING = "domain_fronting"
    const val KEY_BYPASS_VPN = "bypass_vpn"
    const val KEY_LIST_THUMB_SIZE = "list_tile_size"
    private const val KEY_LAST_DAWN_DAY = "last_dawn_day"
    private const val DEFAULT_HIDE_HV_EVENTS = false
    private const val KEY_SHOW_COMMENTS = "show_gallery_comments"
    private const val DEFAULT_SHOW_COMMENTS = true
    private val TAG = Settings::class.java.simpleName
    private const val KEY_DISPLAY_NAME = "display_name"
    private val DEFAULT_DISPLAY_NAME: String? = null
    private const val DEFAULT_LIST_THUMB_SIZE = 40
    private const val KEY_AVATAR = "avatar"
    private const val KEY_REMOVE_IMAGE_FILES = "include_pic"
    private const val DEFAULT_REMOVE_IMAGE_FILES = true
    private const val KEY_NEED_SIGN_IN = "need_sign_in"
    private const val DEFAULT_NEED_SIGN_IN = true
    private const val KEY_SELECT_SITE = "select_site"
    private const val DEFAULT_SELECT_SITE = true
    private const val DEFAULT_THEME = THEME_SYSTEM
    private const val DEFAULT_GALLERY_SITE = 0
    private const val KEY_LAUNCH_PAGE = "launch_page"
    private const val DEFAULT_LAUNCH_PAGE = 0
    private const val DEFAULT_LIST_MODE = 0
    private const val DEFAULT_DETAIL_SIZE = 0
    private const val DEFAULT_THUMB_SIZE = 120
    private const val DEFAULT_THUMB_RESOLUTION = 0
    private const val DEFAULT_SHOW_JPN_TITLE = false
    private const val KEY_SHOW_GALLERY_PAGES = "show_gallery_pages"
    private const val DEFAULT_SHOW_GALLERY_PAGES = false
    private const val DEFAULT_SHOW_TAG_TRANSLATIONS = false
    private const val KEY_METERED_NETWORK_WARNING = "cellular_network_warning"
    private const val DEFAULT_METERED_NETWORK_WARNING = false
    private const val KEY_APP_LINK_VERIFY_TIP = "app_link_verify_tip"
    private const val DEFAULT_APP_LINK_VERIFY_TIP = false
    private const val DEFAULT_MEDIA_SCAN = false
    private const val KEY_RECENT_DOWNLOAD_LABEL = "recent_download_label"
    private val DEFAULT_RECENT_DOWNLOAD_LABEL: String? = null
    private const val KEY_HAS_DEFAULT_DOWNLOAD_LABEL = "has_default_download_label"
    private const val DEFAULT_HAS_DOWNLOAD_LABEL = false
    private const val KEY_DEFAULT_DOWNLOAD_LABEL = "default_download_label"
    private val DEFAULT_DOWNLOAD_LABEL: String? = null
    private const val KEY_MULTI_THREAD_DOWNLOAD = "download_thread"
    private const val DEFAULT_MULTI_THREAD_DOWNLOAD = 3
    private const val KEY_PRELOAD_IMAGE = "preload_image"
    private const val DEFAULT_PRELOAD_IMAGE = 5
    private const val KEY_DOWNLOAD_ORIGIN_IMAGE = "download_origin_image"
    private const val DEFAULT_DOWNLOAD_ORIGIN_IMAGE = false

    /********************
     * Favorites
     */
    private const val KEY_FAV_CAT_0 = "fav_cat_0"
    private const val KEY_FAV_CAT_1 = "fav_cat_1"
    private const val KEY_FAV_CAT_2 = "fav_cat_2"
    private const val KEY_FAV_CAT_3 = "fav_cat_3"
    private const val KEY_FAV_CAT_4 = "fav_cat_4"
    private const val KEY_FAV_CAT_5 = "fav_cat_5"
    private const val KEY_FAV_CAT_6 = "fav_cat_6"
    private const val KEY_FAV_CAT_7 = "fav_cat_7"
    private const val KEY_FAV_CAT_8 = "fav_cat_8"
    private const val KEY_FAV_CAT_9 = "fav_cat_9"
    private const val DEFAULT_FAV_CAT_0 = "Favorites 0"
    private const val DEFAULT_FAV_CAT_1 = "Favorites 1"
    private const val DEFAULT_FAV_CAT_2 = "Favorites 2"
    private const val DEFAULT_FAV_CAT_3 = "Favorites 3"
    private const val DEFAULT_FAV_CAT_4 = "Favorites 4"
    private const val DEFAULT_FAV_CAT_5 = "Favorites 5"
    private const val DEFAULT_FAV_CAT_6 = "Favorites 6"
    private const val DEFAULT_FAV_CAT_7 = "Favorites 7"
    private const val DEFAULT_FAV_CAT_8 = "Favorites 8"
    private const val DEFAULT_FAV_CAT_9 = "Favorites 9"
    private const val KEY_FAV_COUNT_0 = "fav_count_0"
    private const val KEY_FAV_COUNT_1 = "fav_count_1"
    private const val KEY_FAV_COUNT_2 = "fav_count_2"
    private const val KEY_FAV_COUNT_3 = "fav_count_3"
    private const val KEY_FAV_COUNT_4 = "fav_count_4"
    private const val KEY_FAV_COUNT_5 = "fav_count_5"
    private const val KEY_FAV_COUNT_6 = "fav_count_6"
    private const val KEY_FAV_COUNT_7 = "fav_count_7"
    private const val KEY_FAV_COUNT_8 = "fav_count_8"
    private const val KEY_FAV_COUNT_9 = "fav_count_9"
    private const val KEY_FAV_LOCAL = "fav_local"
    private const val KEY_FAV_CLOUD = "fav_cloud"
    private const val DEFAULT_FAV_COUNT = 0
    private const val KEY_RECENT_FAV_CAT = "recent_fav_cat"
    private const val DEFAULT_RECENT_FAV_CAT = FavListUrlBuilder.FAV_CAT_ALL

    // -1 for local, 0 - 9 for cloud favorite, other for no default fav slot
    private const val KEY_DEFAULT_FAV_SLOT = "default_favorite_2"
    private const val DEFAULT_DEFAULT_FAV_SLOT = INVALID_DEFAULT_FAV_SLOT
    private const val DEFAULT_SAVE_PARSE_ERROR_BODY = true
    private const val KEY_SAVE_CRASH_LOG = "save_crash_log"
    private const val DEFAULT_SAVE_CRASH_LOG = true
    private const val DEFAULT_BUILT_IN_HOSTS = false
    private const val DEFAULT_FRONTING = false
    private const val DEFAULT_BYPASS_VPN = true
    private const val KEY_PROXY_TYPE = "proxy_type"
    private const val DEFAULT_PROXY_TYPE = EhProxySelector.TYPE_SYSTEM
    private const val KEY_PROXY_IP = "proxy_ip"
    private val DEFAULT_PROXY_IP: String? = null
    private const val KEY_PROXY_PORT = "proxy_port"
    private const val DEFAULT_PROXY_PORT = -1
    private const val KEY_CLIPBOARD_TEXT_HASH_CODE = "clipboard_text_hash_code"
    private const val DEFAULT_CLIPBOARD_TEXT_HASH_CODE = 0
    private const val KEY_DOWNLOAD_DELAY = "download_delay"
    private const val DEFAULT_DOWNLOAD_DELAY = 0
    private const val DEFAULT_REQUEST_NEWS = false
    private const val KEY_ARCHIVE_PASSWDS = "archive_passwds"
    private const val KEY_QS_SAVE_PROGRESS = "qs_save_progress"
    private const val DEFAULT_QS_SAVE_PROGRESS = true
    var LIST_THUMB_SIZE_INITED = false
    private lateinit var sSettingsPre: SharedPreferences
    private var LIST_THUMB_SIZE = 40
    fun initialize() {
        sSettingsPre = PreferenceManager.getDefaultSharedPreferences(application)
        fixDefaultValue()
    }

    private fun fixDefaultValue() {
        if ("CN" == Locale.getDefault().country) {
            // Enable domain fronting if the country is CN
            if (!sSettingsPre.contains(KEY_BUILT_IN_HOSTS)) {
                putBuiltInHosts(true)
            }
            if (!sSettingsPre.contains(KEY_DOMAIN_FRONTING)) {
                putDF(true)
            }
            // Enable show tag translations if the country is CN
            if (!sSettingsPre.contains(KEY_SHOW_TAG_TRANSLATIONS)) {
                putShowTagTranslations(true)
            }
        }
    }

    private fun getBoolean(key: String, defValue: Boolean): Boolean {
        return try {
            sSettingsPre.getBoolean(key, defValue)
        } catch (e: ClassCastException) {
            Log.d(TAG, "Get ClassCastException when get $key value", e)
            defValue
        }
    }

    private fun putBoolean(key: String, value: Boolean) {
        sSettingsPre.edit().putBoolean(key, value).apply()
    }

    @JvmStatic
    fun getInt(key: String, defValue: Int): Int {
        return try {
            sSettingsPre.getInt(key, defValue)
        } catch (e: ClassCastException) {
            Log.d(TAG, "Get ClassCastException when get $key value", e)
            defValue
        }
    }

    @JvmStatic
    fun putInt(key: String, value: Int) {
        sSettingsPre.edit().putInt(key, value).apply()
    }

    private fun getString(key: String, defValue: String?): String? {
        return try {
            sSettingsPre.getString(key, defValue)
        } catch (e: ClassCastException) {
            Log.d(TAG, "Get ClassCastException when get $key value", e)
            defValue
        }
    }

    private fun putString(key: String, value: String?) {
        sSettingsPre.edit().putString(key, value).apply()
    }

    private fun getStringSet(key: String): MutableSet<String>? {
        return sSettingsPre.getStringSet(key, null)
    }

    private fun putStringToStringSet(key: String, value: String) {
        var set = getStringSet(key)
        if (set == null) set =
            mutableSetOf(value) else if (set.contains(value)) return else set.add(value)
        sSettingsPre.edit().putStringSet(key, set).apply()
    }

    private fun getIntFromStr(key: String, defValue: Int): Int {
        return try {
            NumberUtils.parseIntSafely(
                sSettingsPre.getString(key, defValue.toString()),
                defValue
            )
        } catch (e: ClassCastException) {
            Log.d(TAG, "Get ClassCastException when get $key value", e)
            defValue
        }
    }

    private fun putIntToStr(key: String, value: Int) {
        sSettingsPre.edit().putString(key, value.toString()).apply()
    }

    var lastDawnDay: Long
        get() = sSettingsPre.getLong(KEY_LAST_DAWN_DAY, 0)
        set(value) = sSettingsPre.edit().putLong(KEY_LAST_DAWN_DAY, value).apply()
    val displayName: String?
        get() = getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME)

    fun putDisplayName(value: String?) {
        putString(KEY_DISPLAY_NAME, value)
    }

    fun putAvatar(value: String?) {
        putString(KEY_AVATAR, value)
    }

    val removeImageFiles: Boolean
        get() = getBoolean(KEY_REMOVE_IMAGE_FILES, DEFAULT_REMOVE_IMAGE_FILES)

    fun putRemoveImageFiles(value: Boolean) {
        putBoolean(KEY_REMOVE_IMAGE_FILES, value)
    }

    val needSignIn: Boolean
        get() = getBoolean(KEY_NEED_SIGN_IN, DEFAULT_NEED_SIGN_IN)

    fun putNeedSignIn(value: Boolean) {
        putBoolean(KEY_NEED_SIGN_IN, value)
    }

    val selectSite: Boolean
        get() = getBoolean(KEY_SELECT_SITE, DEFAULT_SELECT_SITE)

    fun putSelectSite(value: Boolean) {
        putBoolean(KEY_SELECT_SITE, value)
    }

    val theme: Int
        get() = getIntFromStr(KEY_THEME, DEFAULT_THEME)
    val blackDarkTheme
        get() = getBoolean(KEY_BLACK_DARK_THEME, DEFAULT_BLACK_DARK_THEME)
    val gallerySite: Int
        get() = getIntFromStr(KEY_GALLERY_SITE, DEFAULT_GALLERY_SITE)

    fun putGallerySite(value: Int) {
        putIntToStr(KEY_GALLERY_SITE, value)
    }

    val launchPageGalleryListSceneAction: String
        get() {
            return when (val value = getIntFromStr(KEY_LAUNCH_PAGE, DEFAULT_LAUNCH_PAGE)) {
                0 -> GalleryListScene.ACTION_HOMEPAGE
                1 -> GalleryListScene.ACTION_SUBSCRIPTION
                2 -> GalleryListScene.ACTION_WHATS_HOT
                3 -> GalleryListScene.ACTION_TOP_LIST
                else -> throw IllegalStateException("Unexpected value: $value")
            }
        }
    val listMode: Int
        get() = getIntFromStr(KEY_LIST_MODE, DEFAULT_LIST_MODE)
    private val detailSize: Int
        get() = getIntFromStr(KEY_DETAIL_SIZE, DEFAULT_DETAIL_SIZE)

    @get:DimenRes
    val detailSizeResId: Int
        get() = when (detailSize) {
            0 -> R.dimen.gallery_list_column_width_long
            1 -> R.dimen.gallery_list_column_width_short
            else -> throw IllegalStateException("Unexpected value: $detailSize")
        }
    val thumbSize: Int
        get() = dip2px(getInt(KEY_THUMB_SIZE, DEFAULT_THUMB_SIZE))

    val thumbSizeDp: Dp
        get() = getInt(KEY_THUMB_SIZE, DEFAULT_THUMB_SIZE).dp

    private fun dip2px(dpValue: Int): Int {
        val scale = application.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    val thumbResolution: Int
        get() = getIntFromStr(KEY_THUMB_RESOLUTION, DEFAULT_THUMB_RESOLUTION)
    val showComments: Boolean
        get() = getBoolean(KEY_SHOW_COMMENTS, DEFAULT_SHOW_COMMENTS)
    val requestNews: Boolean
        get() = getBoolean(KEY_REQUEST_NEWS, DEFAULT_REQUEST_NEWS)
    val hideHvEvents: Boolean
        get() = getBoolean(KEY_HIDE_HV_EVENTS, DEFAULT_HIDE_HV_EVENTS)
    val showJpnTitle: Boolean
        get() = getBoolean(KEY_SHOW_JPN_TITLE, DEFAULT_SHOW_JPN_TITLE)
    val showGalleryPages: Boolean
        get() = getBoolean(KEY_SHOW_GALLERY_PAGES, DEFAULT_SHOW_GALLERY_PAGES)
    val showTagTranslations: Boolean
        get() = getBoolean(KEY_SHOW_TAG_TRANSLATIONS, DEFAULT_SHOW_TAG_TRANSLATIONS)

    private fun putShowTagTranslations(value: Boolean) {
        putBoolean(KEY_SHOW_TAG_TRANSLATIONS, value)
    }

    val meteredNetworkWarning: Boolean
        get() = getBoolean(KEY_METERED_NETWORK_WARNING, DEFAULT_METERED_NETWORK_WARNING)
    val appLinkVerifyTip: Boolean
        get() = getBoolean(KEY_APP_LINK_VERIFY_TIP, DEFAULT_APP_LINK_VERIFY_TIP)

    fun putAppLinkVerifyTip(value: Boolean) {
        putBoolean(KEY_APP_LINK_VERIFY_TIP, value)
    }

    val enabledSecurity: Boolean
        get() = getBoolean(KEY_SEC_SECURITY, VALUE_SEC_SECURITY)
    val downloadLocation: UniFile?
        get() {
            val dir: UniFile?
            val builder = Uri.Builder()
            builder.scheme(getString(KEY_DOWNLOAD_SAVE_SCHEME, null))
            builder.encodedAuthority(getString(KEY_DOWNLOAD_SAVE_AUTHORITY, null))
            builder.encodedPath(getString(KEY_DOWNLOAD_SAVE_PATH, null))
            builder.encodedQuery(getString(KEY_DOWNLOAD_SAVE_QUERY, null))
            builder.encodedFragment(getString(KEY_DOWNLOAD_SAVE_FRAGMENT, null))
            dir = UniFile.fromUri(application, builder.build())
            return dir ?: UniFile.fromFile(AppConfig.getDefaultDownloadDir())
        }

    fun putDownloadLocation(location: UniFile) {
        val uri = location.uri
        putString(KEY_DOWNLOAD_SAVE_SCHEME, uri.scheme)
        putString(KEY_DOWNLOAD_SAVE_AUTHORITY, uri.encodedAuthority)
        putString(KEY_DOWNLOAD_SAVE_PATH, uri.encodedPath)
        putString(KEY_DOWNLOAD_SAVE_QUERY, uri.encodedQuery)
        putString(KEY_DOWNLOAD_SAVE_FRAGMENT, uri.encodedFragment)
        if (mediaScan) {
            removeNoMediaFile(location)
        } else {
            ensureNoMediaFile(location)
        }
    }

    val mediaScan: Boolean
        get() = getBoolean(KEY_MEDIA_SCAN, DEFAULT_MEDIA_SCAN)
    val recentDownloadLabel: String?
        get() = getString(KEY_RECENT_DOWNLOAD_LABEL, DEFAULT_RECENT_DOWNLOAD_LABEL)

    fun putRecentDownloadLabel(value: String?) {
        putString(KEY_RECENT_DOWNLOAD_LABEL, value)
    }

    val hasDefaultDownloadLabel: Boolean
        get() = getBoolean(KEY_HAS_DEFAULT_DOWNLOAD_LABEL, DEFAULT_HAS_DOWNLOAD_LABEL)

    fun putHasDefaultDownloadLabel(hasDefaultDownloadLabel: Boolean) {
        putBoolean(KEY_HAS_DEFAULT_DOWNLOAD_LABEL, hasDefaultDownloadLabel)
    }

    val defaultDownloadLabel: String?
        get() = getString(KEY_DEFAULT_DOWNLOAD_LABEL, DEFAULT_DOWNLOAD_LABEL)

    fun putDefaultDownloadLabel(value: String?) {
        putString(KEY_DEFAULT_DOWNLOAD_LABEL, value)
    }

    val multiThreadDownload: Int
        get() = getIntFromStr(KEY_MULTI_THREAD_DOWNLOAD, DEFAULT_MULTI_THREAD_DOWNLOAD)
    val downloadDelay: Int
        get() = getIntFromStr(KEY_DOWNLOAD_DELAY, DEFAULT_DOWNLOAD_DELAY)
    val preloadImage: Int
        get() = getIntFromStr(KEY_PRELOAD_IMAGE, DEFAULT_PRELOAD_IMAGE)
    val downloadOriginImage: Boolean
        get() = getBoolean(KEY_DOWNLOAD_ORIGIN_IMAGE, DEFAULT_DOWNLOAD_ORIGIN_IMAGE)
    val favCat: Array<String>
        get() = arrayOf(
            sSettingsPre.getString(KEY_FAV_CAT_0, DEFAULT_FAV_CAT_0)!!,
            sSettingsPre.getString(KEY_FAV_CAT_1, DEFAULT_FAV_CAT_1)!!,
            sSettingsPre.getString(KEY_FAV_CAT_2, DEFAULT_FAV_CAT_2)!!,
            sSettingsPre.getString(KEY_FAV_CAT_3, DEFAULT_FAV_CAT_3)!!,
            sSettingsPre.getString(KEY_FAV_CAT_4, DEFAULT_FAV_CAT_4)!!,
            sSettingsPre.getString(KEY_FAV_CAT_5, DEFAULT_FAV_CAT_5)!!,
            sSettingsPre.getString(KEY_FAV_CAT_6, DEFAULT_FAV_CAT_6)!!,
            sSettingsPre.getString(KEY_FAV_CAT_7, DEFAULT_FAV_CAT_7)!!,
            sSettingsPre.getString(KEY_FAV_CAT_8, DEFAULT_FAV_CAT_8)!!,
            sSettingsPre.getString(KEY_FAV_CAT_9, DEFAULT_FAV_CAT_9)!!
        )

    fun putFavCat(value: Array<String?>) {
        check(value.size == 10)
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
            .apply()
    }

    val favCount: IntArray
        get() = intArrayOf(
            sSettingsPre.getInt(KEY_FAV_COUNT_0, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_1, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_2, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_3, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_4, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_5, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_6, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_7, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_8, DEFAULT_FAV_COUNT),
            sSettingsPre.getInt(KEY_FAV_COUNT_9, DEFAULT_FAV_COUNT)
        )

    fun putFavCount(count: IntArray) {
        check(count.size == 10)
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
            .apply()
    }

    val favLocalCount: Int
        get() = sSettingsPre.getInt(KEY_FAV_LOCAL, DEFAULT_FAV_COUNT)

    fun putFavLocalCount(count: Int) {
        sSettingsPre.edit().putInt(KEY_FAV_LOCAL, count).apply()
    }

    val favCloudCount: Int
        get() = sSettingsPre.getInt(KEY_FAV_CLOUD, DEFAULT_FAV_COUNT)

    fun putFavCloudCount(count: Int) {
        sSettingsPre.edit().putInt(KEY_FAV_CLOUD, count).apply()
    }

    val recentFavCat: Int
        get() = getInt(KEY_RECENT_FAV_CAT, DEFAULT_RECENT_FAV_CAT)

    fun putRecentFavCat(value: Int) {
        putInt(KEY_RECENT_FAV_CAT, value)
    }

    val defaultFavSlot: Int
        get() = getInt(KEY_DEFAULT_FAV_SLOT, DEFAULT_DEFAULT_FAV_SLOT)

    fun putDefaultFavSlot(value: Int) {
        putInt(KEY_DEFAULT_FAV_SLOT, value)
    }

    val qSSaveProgress: Boolean
        get() = getBoolean(KEY_QS_SAVE_PROGRESS, DEFAULT_QS_SAVE_PROGRESS)

    fun putQSSaveProgress(value: Boolean) {
        putBoolean(KEY_QS_SAVE_PROGRESS, value)
    }

    val saveParseErrorBody: Boolean
        get() = getBoolean(KEY_SAVE_PARSE_ERROR_BODY, DEFAULT_SAVE_PARSE_ERROR_BODY)
    val saveCrashLog: Boolean
        get() = getBoolean(KEY_SAVE_CRASH_LOG, DEFAULT_SAVE_CRASH_LOG)
    val security: Boolean
        get() = getBoolean(KEY_SECURITY, false)

    fun putSecurity(value: Boolean) {
        putBoolean(KEY_SECURITY, value)
    }

    val securityDelay: Int
        get() = getInt(KEY_SECURITY_DELAY, 0)
    val readCacheSize: Int
        get() = getIntFromStr(KEY_READ_CACHE_SIZE, DEFAULT_READ_CACHE_SIZE)
    val builtInHosts: Boolean
        get() = getBoolean(KEY_BUILT_IN_HOSTS, DEFAULT_BUILT_IN_HOSTS)

    private fun putBuiltInHosts(value: Boolean) {
        putBoolean(KEY_BUILT_IN_HOSTS, value)
    }

    val dF: Boolean
        get() = getBoolean(KEY_DOMAIN_FRONTING, DEFAULT_FRONTING)

    private fun putDF(value: Boolean) {
        putBoolean(KEY_DOMAIN_FRONTING, value)
    }

    val bypassVpn: Boolean
        get() = getBoolean(KEY_BYPASS_VPN, DEFAULT_BYPASS_VPN)

    @JvmStatic
    val proxyType: Int
        get() = getInt(KEY_PROXY_TYPE, DEFAULT_PROXY_TYPE)

    fun putProxyType(value: Int) {
        putInt(KEY_PROXY_TYPE, value)
    }

    @JvmStatic
    val proxyIp: String?
        get() = getString(KEY_PROXY_IP, DEFAULT_PROXY_IP)

    fun putProxyIp(value: String?) {
        putString(KEY_PROXY_IP, value)
    }

    @JvmStatic
    val proxyPort: Int
        get() = getInt(KEY_PROXY_PORT, DEFAULT_PROXY_PORT)

    fun putProxyPort(value: Int) {
        putInt(KEY_PROXY_PORT, value)
    }

    val clipboardTextHashCode: Int
        get() = getInt(KEY_CLIPBOARD_TEXT_HASH_CODE, DEFAULT_CLIPBOARD_TEXT_HASH_CODE)

    fun putClipboardTextHashCode(value: Int) {
        putInt(KEY_CLIPBOARD_TEXT_HASH_CODE, value)
    }

    val archivePasswds: Set<String>?
        get() = getStringSet(KEY_ARCHIVE_PASSWDS)

    fun putPasswdToArchivePasswds(value: String) {
        putStringToStringSet(KEY_ARCHIVE_PASSWDS, value)
    }

    val listThumbSize: Int
        get() {
            if (LIST_THUMB_SIZE_INITED) {
                return LIST_THUMB_SIZE
            }
            val size = 3 * getInt(KEY_LIST_THUMB_SIZE, DEFAULT_LIST_THUMB_SIZE)
            LIST_THUMB_SIZE = size
            LIST_THUMB_SIZE_INITED = true
            return size
        }
    val notificationRequired: Boolean
        get() = getBoolean(KEY_NOTIFICATION_REQUIRED, false)

    fun putNotificationRequired() {
        putBoolean(KEY_NOTIFICATION_REQUIRED, true)
    }
}