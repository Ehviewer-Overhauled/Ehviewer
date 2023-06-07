@file:Suppress("SameParameterValue")

package com.hippo.ehviewer

import com.hippo.ehviewer.client.data.FavListUrlBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import splitties.preferences.DataStorePreferences
import splitties.preferences.DataStorePreferencesPreview
import splitties.preferences.edit
import java.util.Locale
import kotlin.reflect.KProperty

@OptIn(DataStorePreferencesPreview::class)
object Settings : DataStorePreferences(null) {
    const val KEY_SHOW_TAG_TRANSLATIONS = "show_tag_translations"
    const val INVALID_DEFAULT_FAV_SLOT = -2
    const val KEY_BUILT_IN_HOSTS = "built_in_hosts_2"
    const val KEY_DOMAIN_FRONTING = "domain_fronting"

    var downloadScheme by stringOrNullPref("image_scheme", null)
    var downloadAuthority by stringOrNullPref("image_authority", null)
    var downloadPath by stringOrNullPref("image_path", null)
    var downloadQuery by stringOrNullPref("image_query", null)
    var downloadFragment by stringOrNullPref("image_fragment", null)
    var favCat by stringArrayPref("fav_cat", 10, "Favorites")
    var favCount by intArrayPref("fav_count", 10)
    var archivePasswds by stringSetOrNullPref("archive_passwds")
    var downloadDelay by intFromStrPref("download_delay", 0)
    var gallerySite by intFromStrPref("gallery_site", 0).observed { updateWhenGallerySiteChanges() }
    var multiThreadDownload by intFromStrPref("download_thread", 3)
    var preloadImage by intFromStrPref("preload_image", 5)
    var theme by intFromStrPref("theme", -1).observed { updateWhenThemeChanges() }
    var listMode by intFromStrPref("list_mode", 0)
    var detailSize by intFromStrPref("detail_size", 0)
    var thumbResolution by intFromStrPref("thumb_resolution", 0)
    var readCacheSize by intFromStrPref("read_cache_size", 640)
    var launchPage by intFromStrPref("launch_page", 0)
    var showComments by boolPref("show_gallery_comments", true)
    var requestNews by boolPref("request_news", false).observed { updateWhenRequestNewsChanges() }
    var hideHvEvents by boolPref("hide_hv_events", false)
    var showJpnTitle by boolPref("show_jpn_title", false)
    var showGalleryPages by boolPref("show_gallery_pages", false)
    var showTagTranslations by boolPref(KEY_SHOW_TAG_TRANSLATIONS, false).observed { updateWhenTagTranslationChanges() }
    var meteredNetworkWarning by boolPref("cellular_network_warning", false)
    var appLinkVerifyTip by boolPref("app_link_verify_tip", false)
    var enabledSecurity by boolPref("enable_secure", false)
    var mediaScan by boolPref("media_scan", false).observed { updateWhenKeepMediaStatusChanges() }
    var hasDefaultDownloadLabel by boolPref("has_default_download_label", false)
    var qSSaveProgress by boolPref("qs_save_progress", true)
    var saveParseErrorBody by boolPref("save_parse_error_body", true)
    var saveCrashLog by boolPref("save_crash_log", true)
    var security by boolPref("require_unlock", false)
    var builtInHosts by boolPref(KEY_BUILT_IN_HOSTS, false)
    var removeImageFiles by boolPref("include_pic", true)
    var needSignIn by boolPref("need_sign_in", true)
    var blackDarkTheme by boolPref("black_dark_theme", false).observed { updateWhenAmoledModeChanges() }
    var preloadThumbAggressively by boolPref("preload_thumb_aggressively", false)
    var dF by boolPref(KEY_DOMAIN_FRONTING, false)
    var downloadOriginImage by boolPref("download_origin_image", false)
    var bypassVpn by boolPref("bypass_vpn", true)
    var thumbSizeDp by intPref("thumb_size_", 120)
    var favLocalCount by intPref("fav_local", 0)
    var favCloudCount by intPref("fav_cloud", 0)
    var recentFavCat by intPref("recent_fav_cat", FavListUrlBuilder.FAV_CAT_ALL)
    var defaultFavSlot by intPref("default_favorite_2", INVALID_DEFAULT_FAV_SLOT) // -1 for local, 0 - 9 for cloud favorite, other for no default fav slot
    var securityDelay by intPref("require_unlock_delay", 0)
    var clipboardTextHashCode by intPref("clipboard_text_hash_code", 0)
    var listThumbSize by intPref("list_tile_size", 40)
    var requestNewsTimerHour by intPref("request_news_timer_hour", -1)
    var requestNewsTimerMinute by intPref("request_news_timer_minute", -1)
    var dataMapNextId by intPref("data_map_next_id", 0)
    var recentDownloadLabel by stringOrNullPref("recent_download_label", null)
    var defaultDownloadLabel by stringOrNullPref("default_download_label", null)
    var displayName by stringOrNullPref("display_name", null)
    var avatar by stringOrNullPref("avatar", null)
    var dohUrl by stringPref("doh_url", "")
    var language by stringPref("app_language", "system").observed { updateWhenLocaleChanges() }
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

    interface Delegate<R> {
        val flowGetter: () -> Flow<Unit>
        operator fun getValue(thisRef: Any?, prop: KProperty<*>?): R
        operator fun setValue(thisRef: Any?, prop: KProperty<*>?, value: R)
    }

    private fun intFromStrPref(key: String, defValue: Int) = object : Delegate<Int> {
        override val flowGetter: () -> Flow<Unit>
        private var _value by stringPref(key, defValue.toString()).also { flowGetter = { it.changesFlow() } }
        override fun getValue(thisRef: Any?, prop: KProperty<*>?) = _value.toIntOrNull() ?: defValue
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Int) { _value = value.toString() }
    }

    private fun intArrayPref(key: String, count: Int) = object : Delegate<IntArray> {
        override val flowGetter: () -> Flow<Unit> = { flow { } }
        private var _value = (0 until count).map { intPref("${key}_$it", 0) }.toTypedArray()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?): IntArray = _value.map { it.value }.toIntArray()
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: IntArray) {
            check(value.size == count)
            edit { value.zip(_value) { v, d -> d.value = v } }
        }
    }

    private fun stringArrayPref(key: String, count: Int, defMetaValue: String) = object : Delegate<Array<String>> {
        override val flowGetter: () -> Flow<Unit> = { flow { } }
        private var _value = (0 until count).map { stringPref("${key}_$it", "$defMetaValue $it") }.toTypedArray()
        override fun getValue(thisRef: Any?, prop: KProperty<*>?): Array<String> = _value.map { it.value }.toTypedArray()
        override fun setValue(thisRef: Any?, prop: KProperty<*>?, value: Array<String>) {
            check(value.size == count)
            edit { value.zip(_value) { v, d -> d.value = v } }
        }
    }
}
