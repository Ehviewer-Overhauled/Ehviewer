package com.hippo.ehviewer.ui.settings

import android.text.Html
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.compose.observed
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.ui.login.LocalNavController
import com.hippo.ehviewer.util.whisperClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

@Composable
fun EhScreen() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
    val context = LocalContext.current
    fun launchSnackBar(content: String) = coroutineScope.launch { snackbarHostState.showSnackbar(content) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_eh)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        val signOutMessage = stringResource(id = R.string.settings_eh_sign_out_tip)
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()).nestedScroll(scrollBehavior.nestedScrollConnection).verticalScroll(rememberScrollState())) {
            Preference(
                title = stringResource(id = R.string.account_name),
                summary = Settings.displayName ?: stringResource(id = R.string.settings_eh_identity_cookies_tourist),
            ) {
                val eCookies = EhCookieStore.getCookies(EhUrl.HOST_E.toHttpUrl())
                val exCookies = EhCookieStore.getCookies(EhUrl.HOST_EX.toHttpUrl())
                var ipbMemberId: String? = null
                var ipbPassHash: String? = null
                var igneous: String? = null
                (eCookies + exCookies).forEach {
                    when (it.name) {
                        EhCookieStore.KEY_IPB_MEMBER_ID -> ipbMemberId = it.value
                        EhCookieStore.KEY_IPB_PASS_HASH -> ipbPassHash = it.value
                        EhCookieStore.KEY_IGNEOUS -> igneous = it.value
                    }
                }
                BaseDialogBuilder(context).apply {
                    if (ipbMemberId != null || ipbPassHash != null || igneous != null) {
                        val str = EhCookieStore.KEY_IPB_MEMBER_ID + ": " + ipbMemberId + "<br>" + EhCookieStore.KEY_IPB_PASS_HASH + ": " + ipbPassHash + "<br>" + EhCookieStore.KEY_IGNEOUS + ": " + igneous
                        val spanned = Html.fromHtml(context.getString(R.string.settings_eh_identity_cookies_signed, str), Html.FROM_HTML_MODE_LEGACY)
                        setMessage(spanned)
                        setNeutralButton(R.string.settings_eh_identity_cookies_copy) { _, _ -> context whisperClipboard str.replace("<br>", "\n") }
                    } else {
                        setMessage(context.getString(R.string.settings_eh_identity_cookies_tourist))
                    }
                    setPositiveButton(R.string.settings_eh_sign_out) { _, _ ->
                        EhUtils.signOut()
                        launchSnackBar(signOutMessage)
                    }
                }.show()
            }
            Preference(
                title = stringResource(id = R.string.image_limits),
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_gallery_site),
                entry = R.array.gallery_site_entries,
                entryValueRes = R.array.gallery_site_entry_values,
                value = Settings::gallerySite.observed,
            )
            Preference(
                title = stringResource(id = R.string.settings_u_config),
                summary = stringResource(id = R.string.settings_u_config_summary),
            )
            Preference(
                title = stringResource(id = R.string.settings_my_tags),
                summary = stringResource(id = R.string.settings_my_tags_summary),
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = rikka.core.R.string.dark_theme),
                entry = R.array.night_mode_entries,
                entryValueRes = R.array.night_mode_values,
                value = Settings::theme.observed,
            )
            SwitchPreference(
                title = stringResource(id = R.string.black_dark_theme),
                value = Settings::blackDarkTheme,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_launch_page),
                entry = R.array.launch_page_entries,
                entryValueRes = R.array.launch_page_entry_values,
                value = Settings::launchPage.observed,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_list_mode),
                entry = R.array.list_mode_entries,
                entryValueRes = R.array.list_mode_entry_values,
                value = Settings::listMode.observed,
            )
            IntSliderPreference(
                maxValue = 60,
                minValue = 20,
                title = stringResource(id = R.string.list_tile_thumb_size),
                value = Settings::listThumbSize,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_thumb_resolution),
                entry = R.array.thumb_resolution_entries,
                entryValueRes = R.array.thumb_resolution_entry_values,
                value = Settings::thumbResolution.observed,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_jpn_title),
                summary = stringResource(id = R.string.settings_eh_show_jpn_title_summary),
                value = Settings::showJpnTitle,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_gallery_pages),
                summary = stringResource(id = R.string.settings_eh_show_gallery_pages_summary),
                value = Settings::showGalleryPages,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_gallery_comments),
                summary = stringResource(id = R.string.settings_eh_show_gallery_comments_summary),
                value = Settings::showComments,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_show_tag_translations),
                summary = stringResource(id = R.string.settings_eh_show_tag_translations_summary),
                value = Settings::showTagTranslations,
            )
            UrlPreference(
                title = stringResource(id = R.string.settings_eh_tag_translations_source),
                url = stringResource(id = R.string.settings_eh_tag_translations_source_url),
            )
            Preference(
                title = stringResource(id = R.string.settings_eh_filter),
                summary = stringResource(id = R.string.settings_eh_filter_summary),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_metered_network_warning),
                value = Settings::meteredNetworkWarning,
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_request_news),
                value = Settings::requestNews,
            )
            Preference(
                title = stringResource(id = R.string.settings_eh_request_news_timepicker),
            )
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_hide_hv_events),
                value = Settings::requestNews,
            )
            Spacer(modifier = Modifier.size(paddingValues.calculateBottomPadding()))
        }
    }
}
