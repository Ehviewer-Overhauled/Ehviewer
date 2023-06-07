package com.hippo.ehviewer.ui.settings

import android.content.DialogInterface
import android.os.Build
import android.text.Html
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.dailycheck.schedHour
import com.hippo.ehviewer.dailycheck.schedMinute
import com.hippo.ehviewer.dailycheck.updateDailyCheckWork
import com.hippo.ehviewer.ui.FILTER_SCREEN
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.MYTAGS_SCREEN
import com.hippo.ehviewer.ui.SIGN_IN_ROUTE_NAME
import com.hippo.ehviewer.ui.UCONFIG_SCREEN
import com.hippo.ehviewer.ui.compose.observed
import com.hippo.ehviewer.ui.compose.rememberedAccessor
import com.hippo.ehviewer.ui.legacy.BaseDialogBuilder
import com.hippo.ehviewer.util.whisperClipboard
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import moe.tarsin.coroutines.runSuspendCatching
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
        val touristMode = stringResource(id = R.string.settings_eh_identity_cookies_tourist)
        val copiedToClipboard = stringResource(id = R.string.copied_to_clipboard)
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState()),
        ) {
            Preference(
                title = stringResource(id = R.string.account_name),
                summary = Settings.displayName ?: touristMode,
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
                        setNeutralButton(R.string.settings_eh_identity_cookies_copy) { _, _ ->
                            context whisperClipboard str.replace("<br>", "\n")
                            // Avoid double notify user since system have done that on Tiramisu above
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) launchSnackBar(copiedToClipboard)
                        }
                    } else {
                        setMessage(touristMode)
                    }
                    setPositiveButton(R.string.settings_eh_sign_out) { _, _ ->
                        EhUtils.signOut()
                        navController.navigate(SIGN_IN_ROUTE_NAME)
                    }
                }.show()
            }
            val placeholder = stringResource(id = R.string.please_wait)
            val resetImageLimitSucceed = stringResource(id = R.string.reset_limits_succeed)
            val noImageLimits = stringResource(id = R.string.image_limits_summary, 0, 0)
            var summary by rememberSaveable { mutableStateOf(noImageLimits) }
            suspend fun getImageLimits() = EhEngine.getImageLimits().also {
                summary = context.getString(R.string.image_limits_summary, it.limits.current, it.limits.maximum)
            }
            val deferredResult = remember { coroutineScope.async { runSuspendCatching { getImageLimits() } } }
            Preference(
                title = stringResource(id = R.string.image_limits),
                summary = summary,
            ) {
                val builder = BaseDialogBuilder(context).setMessage(placeholder)
                    .setPositiveButton(R.string.reset, null)
                    .setNegativeButton(android.R.string.cancel, null)
                val dialog = builder.show()
                val resetButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                fun bind(result: HomeParser.Result) {
                    val (current, maximum, resetCost) = result.limits
                    val (fundsGP, fundsC) = result.funds
                    val cost = if (fundsGP >= resetCost) "$resetCost GP" else "$resetCost Credits"
                    val message = context.getString(R.string.current_limits, "$current / $maximum", cost) + "\n" + context.getString(R.string.current_funds, "$fundsGP+", fundsC)
                    dialog.setMessage(message)
                    resetButton.isEnabled = resetCost in 1..maxOf(fundsGP, fundsC)
                }
                coroutineScope.launch {
                    runSuspendCatching {
                        val result = deferredResult.await().getOrNull() ?: getImageLimits()
                        withUIContext { bind(result) }
                    }.onFailure {
                        dialog.setMessage(it.localizedMessage)
                    }
                }
                resetButton.isEnabled = false
                resetButton.setOnClickListener { button ->
                    button.isEnabled = false
                    dialog.setMessage(placeholder)
                    coroutineScope.launch {
                        runSuspendCatching {
                            EhEngine.resetImageLimits()
                            getImageLimits()
                        }.onSuccess {
                            launchSnackBar(resetImageLimitSucceed)
                            withUIContext { bind(it) }
                        }.onFailure {
                            dialog.setMessage(it.localizedMessage)
                        }
                    }
                }
            }
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_gallery_site),
                entry = R.array.gallery_site_entries,
                entryValueRes = R.array.gallery_site_entry_values,
                value = Settings::gallerySite.observed,
            )
            Preference(
                title = stringResource(id = R.string.settings_u_config),
                summary = stringResource(id = R.string.settings_u_config_summary),
            ) { navController.navigate(UCONFIG_SCREEN) }
            Preference(
                title = stringResource(id = R.string.settings_my_tags),
                summary = stringResource(id = R.string.settings_my_tags_summary),
            ) { navController.navigate(MYTAGS_SCREEN) }
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
                step = 7,
                title = stringResource(id = R.string.list_tile_thumb_size),
                value = Settings::listThumbSize,
            )
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_detail_size),
                entry = R.array.detail_size_entries,
                entryValueRes = R.array.detail_size_entry_values,
                value = Settings::detailSize.observed,
            )
            IntSliderPreference(
                maxValue = 400,
                minValue = 80,
                step = 7,
                title = stringResource(id = R.string.settings_eh_thumb_size),
                value = Settings::thumbSizeDp,
            )
            val thumbResolution = Settings::thumbResolution.observed
            val summary2 = stringResource(id = R.string.settings_eh_thumb_resolution_summary, stringArrayResource(id = R.array.thumb_resolution_entries)[thumbResolution.value])
            SimpleMenuPreferenceInt(
                title = stringResource(id = R.string.settings_eh_thumb_resolution),
                summary = summary2,
                entry = R.array.thumb_resolution_entries,
                entryValueRes = R.array.thumb_resolution_entry_values,
                value = thumbResolution,
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
            ) { navController.navigate(FILTER_SCREEN) }
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_metered_network_warning),
                value = Settings::meteredNetworkWarning,
            )
            val reqNews = Settings::requestNews.observed
            SwitchPreference(
                title = stringResource(id = R.string.settings_eh_request_news),
                value = reqNews.rememberedAccessor,
            )
            AnimatedVisibility(visible = reqNews.value) {
                val pickerTitle = stringResource(id = R.string.settings_eh_request_news_timepicker)
                var showPicker by rememberSaveable { mutableStateOf(false) }
                val state = rememberTimePickerState(schedHour, schedMinute)
                if (showPicker) {
                    TimePickerDialog(
                        title = pickerTitle,
                        onCancel = { showPicker = false },
                        onConfirm = {
                            showPicker = false
                            Settings.requestNewsTimerHour = state.hour
                            Settings.requestNewsTimerMinute = state.minute
                            updateDailyCheckWork(context)
                        },
                    ) {
                        TimePicker(state = state)
                    }
                }
                Preference(title = pickerTitle) {
                    showPicker = true
                }
            }
            AnimatedVisibility(visible = reqNews.value) {
                SwitchPreference(
                    title = stringResource(id = R.string.settings_eh_hide_hv_events),
                    value = Settings::requestNews,
                )
            }
            Spacer(modifier = Modifier.size(paddingValues.calculateBottomPadding()))
        }
    }
}
