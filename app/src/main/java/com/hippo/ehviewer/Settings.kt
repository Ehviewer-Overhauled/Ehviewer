@file:Suppress("SameParameterValue")

package com.hippo.ehviewer

import com.hippo.ehviewer.client.data.FavListUrlBuilder
import splitties.preferences.DefaultPreferences
import splitties.preferences.edit
import java.util.Locale
import kotlin.reflect.KProperty

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
    const val KEY_HIDE_HV_EVENTS = "hide_hv_events"
    const val KEY_MEDIA_SCAN = "media_scan"
    const val INVALID_DEFAULT_FAV_SLOT = -2
    const val KEY_READ_CACHE_SIZE = "read_cache_size"
    const val KEY_BUILT_IN_HOSTS = "built_in_hosts_2"
    const val KEY_DOMAIN_FRONTING = "domain_fronting"
    const val KEY_BYPASS_VPN = "bypass_vpn"
    const val KEY_LIST_THUMB_SIZE = "list_tile_size"
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

    var downloadScheme by stringOrNullPref("image_scheme", null)
    var downloadAuthority by stringOrNullPref("image_authority", null)
    var downloadPath by stringOrNullPref("image_path", null)
    var downloadQuery by stringOrNullPref("image_query", null)
    var downloadFragment by stringOrNullPref("image_fragment", null)

    var favCat by stringArrayPref("fav_cat", 10, "Favorites")
    var favCount by intArrayPref("fav_count", 10)
    var archivePasswds by stringSetOrNullPref("archive_passwds")

    val downloadDelay by intFromStrPref("download_delay", 0)
    var gallerySite by intFromStrPref(KEY_GALLERY_SITE, 0)
    val multiThreadDownload by intFromStrPref("download_thread", 3)
    val preloadImage by intFromStrPref("preload_image", 5)
    val theme by intFromStrPref(KEY_THEME, -1)
    val listMode by intFromStrPref(KEY_LIST_MODE, 0)
    val detailSize by intFromStrPref(KEY_DETAIL_SIZE, 0)
    val thumbResolution by intFromStrPref(KEY_THUMB_RESOLUTION, 0)
    var readCacheSize by intFromStrPref(KEY_READ_CACHE_SIZE, 640)
    var launchPage by intFromStrPref("launch_page", 0)

    val showComments by boolPref("show_gallery_comments", true)
    val requestNews by boolPref(KEY_REQUEST_NEWS, false)
    val hideHvEvents by boolPref(KEY_HIDE_HV_EVENTS, false)
    val showJpnTitle by boolPref(KEY_SHOW_JPN_TITLE, false)
    val showGalleryPages by boolPref("show_gallery_pages", false)
    var showTagTranslations by boolPref(KEY_SHOW_TAG_TRANSLATIONS, false)
    val meteredNetworkWarning by boolPref("cellular_network_warning", false)
    var appLinkVerifyTip by boolPref("app_link_verify_tip", false)
    var enabledSecurity by boolPref("enable_secure", false)
    val mediaScan by boolPref(KEY_MEDIA_SCAN, false)
    var hasDefaultDownloadLabel by boolPref("has_default_download_label", false)
    var qSSaveProgress by boolPref("qs_save_progress", true)
    var saveParseErrorBody by boolPref("save_parse_error_body", true)
    var saveCrashLog by boolPref("save_crash_log", true)
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
    var securityDelay by intPref("require_unlock_delay", 0)
    var proxyType by intPref("proxy_type", EhProxySelector.TYPE_SYSTEM)
    var proxyPort by intPref("proxy_port", -1)
    var clipboardTextHashCode by intPref("clipboard_text_hash_code", 0)
    val listThumbSize by intPref(KEY_LIST_THUMB_SIZE, 40)
    var requestNewsTimerHour by intPref("request_news_timer_hour", -1)
    var requestNewsTimerMinute by intPref("request_news_timer_minute", -1)
    var dataMapNextId by intPref("data_map_next_id", 0)

    var recentDownloadLabel by stringOrNullPref("recent_download_label", null)
    var defaultDownloadLabel by stringOrNullPref("default_download_label", null)
    var displayName by stringOrNullPref("display_name", null)
    var avatar by stringOrNullPref("avatar", null)
    var proxyIp by stringOrNullPref("proxy_ip", null)

    var dohUrl by stringPref("doh_url", "")
    var language by stringPref("app_language", "system").apply { changesFlow().observe { updateWhenLocaleChanges() } }
    var lastDawnDay by longPref("last_dawn_day", 0)

    init {
        if ("CN" == Locale.getDefault().country) {
            edit {
                if (KEY_BUILT_IN_HOSTS !in prefs) builtInHosts = true
                if (KEY_DOMAIN_FRONTING !in prefs) dF = true
                if (KEY_SHOW_TAG_TRANSLATIONS !in prefs) showTagTranslations = true
            }
        }
    }

    private interface Delegate<R> {
        operator fun getValue(thisRef: Any?, prop: KProperty<*>?): R
        operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: R)
    }

    private fun intFromStrPref(key: String, defValue: Int) = object : Delegate<Int> {
        private var _value by stringPref(key, defValue.toString())
        override fun getValue(thisRef: Any?, prop: KProperty<*>?) = _value.toIntOrNull() ?: defValue
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Int) { _value = value.toString() }
    }

    private fun intArrayPref(key: String, count: Int) = object : Delegate<IntArray> {
        private var _value = (0 until count).map { intPref("${key}_$it", 0) }.toTypedArray()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?): IntArray = _value.map { it.value }.toIntArray()
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: IntArray) {
            check(value.size == count)
            edit { value.zip(_value) { v, d -> d.value = v } }
        }
    }

    private fun stringArrayPref(key: String, count: Int, defMetaValue: String) = object : Delegate<Array<String>> {
        private var _value = (0 until count).map { stringPref("${key}_$it", "$defMetaValue $it") }.toTypedArray()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?): Array<String> = _value.map { it.value }.toTypedArray()
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Array<String>) {
            check(value.size == count)
            edit { value.zip(_value) { v, d -> d.value = v } }
        }
    }
}
