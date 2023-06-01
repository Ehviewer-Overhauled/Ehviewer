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
import androidx.preference.PreferenceManager
import com.hippo.ehviewer.client.data.FavListUrlBuilder
import com.hippo.ehviewer.ui.scene.GalleryListScene
import com.hippo.ehviewer.yorozuya.LayoutUtils.dp2pix
import com.hippo.ehviewer.yorozuya.NumberUtils
import com.hippo.unifile.UniFile
import splitties.experimental.ExperimentalSplittiesApi
import splitties.init.appCtx
import splitties.preferences.DefaultPreferences
import splitties.preferences.StringPref
import java.util.Locale
import kotlin.reflect.KProperty

@OptIn(ExperimentalSplittiesApi::class)
object Settings : DefaultPreferences() {
    const val KEY_THEME = "theme"
    const val KEY_ACCOUNT = "account"
    const val KEY_IMAGE_LIMITS = "image_limits"
    const val KEY_U_CONFIG = "uconfig"
    const val KEY_MY_TAGS = "mytags"
    const val KEY_BLACK_DARK_THEME = "black_dark_theme"
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
        KEY_HIDE_HV_EVENTS,
    )
    const val KEY_DOWNLOAD_SAVE_SCHEME = "image_scheme"
    const val KEY_DOWNLOAD_SAVE_AUTHORITY = "image_authority"
    const val KEY_DOWNLOAD_SAVE_PATH = "image_path"
    const val KEY_DOWNLOAD_SAVE_QUERY = "image_query"
    const val KEY_DOWNLOAD_SAVE_FRAGMENT = "image_fragment"
    const val KEY_MEDIA_SCAN = "media_scan"
    const val INVALID_DEFAULT_FAV_SLOT = -2
    const val KEY_READ_CACHE_SIZE = "read_cache_size"
    const val DEFAULT_READ_CACHE_SIZE = 640
    const val KEY_BUILT_IN_HOSTS = "built_in_hosts_2"
    const val KEY_DOMAIN_FRONTING = "domain_fronting"
    const val KEY_BYPASS_VPN = "bypass_vpn"
    const val KEY_LIST_THUMB_SIZE = "list_tile_size"
    private val TAG = Settings::class.java.simpleName
    private const val KEY_LAUNCH_PAGE = "launch_page"
    private const val DEFAULT_LAUNCH_PAGE = 0
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
    private const val KEY_ARCHIVE_PASSWDS = "archive_passwds"
    private lateinit var sSettingsPre: SharedPreferences

    fun initialize() {
        sSettingsPre = PreferenceManager.getDefaultSharedPreferences(appCtx)
        fixDefaultValue()
    }

    private fun fixDefaultValue() {
        if ("CN" == Locale.getDefault().country) {
            // Enable domain fronting if the country is CN
            if (!sSettingsPre.contains(KEY_BUILT_IN_HOSTS)) {
                builtInHosts = true
            }
            if (!sSettingsPre.contains(KEY_DOMAIN_FRONTING)) {
                dF = true
            }
            // Enable show tag translations if the country is CN
            if (!sSettingsPre.contains(KEY_SHOW_TAG_TRANSLATIONS)) {
                showTagTranslations = true
            }
        }
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

    private fun getIntFromStr(key: String, defValue: Int): Int {
        return try {
            NumberUtils.parseIntSafely(
                sSettingsPre.getString(key, defValue.toString()),
                defValue,
            )
        } catch (e: ClassCastException) {
            Log.d(TAG, "Get ClassCastException when get $key value", e)
            defValue
        }
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

    @get:DimenRes
    val detailSizeResId: Int
        get() = when (detailSize) {
            0 -> R.dimen.gallery_list_column_width_long
            1 -> R.dimen.gallery_list_column_width_short
            else -> throw IllegalStateException("Unexpected value: $detailSize")
        }

    val thumbSize: Int
        get() = dp2pix(appCtx, thumbSizeDp.toFloat())

    val downloadLocation: UniFile?
        get() {
            val dir: UniFile?
            val builder = Uri.Builder()
            builder.scheme(getString(KEY_DOWNLOAD_SAVE_SCHEME, null))
            builder.encodedAuthority(getString(KEY_DOWNLOAD_SAVE_AUTHORITY, null))
            builder.encodedPath(getString(KEY_DOWNLOAD_SAVE_PATH, null))
            builder.encodedQuery(getString(KEY_DOWNLOAD_SAVE_QUERY, null))
            builder.encodedFragment(getString(KEY_DOWNLOAD_SAVE_FRAGMENT, null))
            dir = UniFile.fromUri(appCtx, builder.build())
            return dir ?: UniFile.fromFile(AppConfig.getDefaultDownloadDir())
        }

    fun putDownloadLocation(location: UniFile) {
        val uri = location.uri
        putString(KEY_DOWNLOAD_SAVE_SCHEME, uri.scheme)
        putString(KEY_DOWNLOAD_SAVE_AUTHORITY, uri.encodedAuthority)
        putString(KEY_DOWNLOAD_SAVE_PATH, uri.encodedPath)
        putString(KEY_DOWNLOAD_SAVE_QUERY, uri.encodedQuery)
        putString(KEY_DOWNLOAD_SAVE_FRAGMENT, uri.encodedFragment)
    }

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
            sSettingsPre.getString(KEY_FAV_CAT_9, DEFAULT_FAV_CAT_9)!!,
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
            sSettingsPre.getInt(KEY_FAV_COUNT_0, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_1, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_2, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_3, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_4, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_5, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_6, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_7, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_8, 0),
            sSettingsPre.getInt(KEY_FAV_COUNT_9, 0),
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

    var archivePasswds by stringSetOrNullPref(KEY_ARCHIVE_PASSWDS)

    fun putPasswdToArchivePasswds(value: String) {
        archivePasswds = archivePasswds?.toMutableSet()?.apply { add(value) } ?: setOf(value)
    }

    private val _listThumbSize by intPref(KEY_LIST_THUMB_SIZE, 40)
    val listThumbSize: Int
        get() = 3 * _listThumbSize

    val downloadDelay by intFrom(0) { stringPref("download_delay", it) }
    var gallerySite by intFrom(0) { stringPref(KEY_GALLERY_SITE, it) }
    val multiThreadDownload by intFrom(3) { stringPref("download_thread", it) }
    val preloadImage by intFrom(5) { stringPref("preload_image", it) }
    val theme by intFrom(-1) { stringPref(KEY_THEME, it) }
    val listMode by intFrom(0) { stringPref(KEY_LIST_MODE, it) }
    val detailSize by intFrom(0) { stringPref(KEY_DETAIL_SIZE, it) }
    val thumbResolution by intFrom(0) { stringPref(KEY_THUMB_RESOLUTION, it) }
    val readCacheSize by intFrom(DEFAULT_READ_CACHE_SIZE) { stringPref(KEY_READ_CACHE_SIZE, it) }

    val showComments by boolPref("show_gallery_comments", true)
    val requestNews by boolPref(KEY_REQUEST_NEWS, false)
    val hideHvEvents by boolPref(KEY_HIDE_HV_EVENTS, false)
    val showJpnTitle by boolPref(KEY_SHOW_JPN_TITLE, false)
    val showGalleryPages by boolPref("show_gallery_pages", false)
    var showTagTranslations by boolPref(KEY_SHOW_TAG_TRANSLATIONS, false)
    val meteredNetworkWarning by boolPref("cellular_network_warning", false)
    var appLinkVerifyTip by boolPref("app_link_verify_tip", false)
    val enabledSecurity by boolPref("enable_secure", false)
    val mediaScan by boolPref(KEY_MEDIA_SCAN, false)
    var hasDefaultDownloadLabel by boolPref("has_default_download_label", false)
    var qSSaveProgress by boolPref("qs_save_progress", true)
    val saveParseErrorBody by boolPref("save_parse_error_body", true)
    val saveCrashLog by boolPref("save_crash_log", true)
    var security by boolPref("require_unlock", false)
    var builtInHosts by boolPref(KEY_BUILT_IN_HOSTS, false)
    var removeImageFiles by boolPref("include_pic", true)
    var needSignIn by boolPref("need_sign_in", true)
    var selectSite by boolPref("select_site", true)
    val blackDarkTheme by boolPref(KEY_BLACK_DARK_THEME, false)
    val preloadThumbAggressively by boolPref("preload_thumb_aggressively", false)
    var dF by boolPref(KEY_DOMAIN_FRONTING, false)
    val downloadOriginImage by boolPref("download_origin_image", false)
    val bypassVpn by boolPref(KEY_BYPASS_VPN, true)

    val thumbSizeDp by intPref(KEY_THUMB_SIZE, 120)
    var favLocalCount by intPref("fav_local", 0)
    var favCloudCount by intPref("fav_cloud", 0)
    var recentFavCat by intPref("recent_fav_cat", FavListUrlBuilder.FAV_CAT_ALL)
    var defaultFavSlot by intPref("default_favorite_2", INVALID_DEFAULT_FAV_SLOT) // -1 for local, 0 - 9 for cloud favorite, other for no default fav slot
    val securityDelay by intPref("require_unlock_delay", 0)
    var proxyType by intPref("proxy_type", EhProxySelector.TYPE_SYSTEM)
    var proxyPort by intPref("proxy_port", -1)
    var clipboardTextHashCode by intPref("clipboard_text_hash_code", 0)

    var recentDownloadLabel by stringOrNullPref("recent_download_label", null)
    var defaultDownloadLabel by stringOrNullPref("default_download_label", null)
    var displayName by stringOrNullPref("display_name", null)
    var avatar by stringOrNullPref("avatar", null)
    var proxyIp by stringOrNullPref("proxy_ip", null)

    var dohUrl by stringPref("doh_url", "")
    var lastDawnDay by longPref("last_dawn_day", 0)
}

interface Delegate<R> {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>?): R
    operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: R)
}

private inline fun intFrom(defValue: Int, crossinline getter: (String) -> StringPref) = object : Delegate<Int> {
    private var _value by getter(defValue.toString())
    override fun getValue(thisRef: Any?, prop: KProperty<*>?) = _value.toIntOrNull() ?: defValue
    override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Int) { _value = value.toString() }
}
