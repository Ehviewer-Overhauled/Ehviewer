package com.hippo.ehviewer.ui.settings

import android.text.Html
import android.text.Spanned
import android.text.style.StrikethroughSpan
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.login.LocalNavController

@Composable
fun AboutScreen() {
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_about)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Column(modifier = Modifier.padding(it).nestedScroll(scrollBehavior.nestedScrollConnection)) {
            Preference(title = stringResource(id = R.string.settings_about_declaration), summary = stringResource(id = R.string.settings_about_declaration_summary))
            val author = stringResource(R.string.settings_about_author_summary).replace('$', '@').let {
                fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
                    val spanned = this@toAnnotatedString
                    append(spanned.toString())
                    getSpans(0, spanned.length, Any::class.java).forEach { span ->
                        val start = getSpanStart(span)
                        val end = getSpanEnd(span)
                        when (span) {
                            is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                        }
                    }
                }
                remember { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toAnnotatedString() }
            }
            HtmlPreference(title = stringResource(id = R.string.settings_about_author), summary = author)
            val url = "https://github.com/Ehviewer-Overhauled/Ehviewer/releases"
            UrlPreference(title = stringResource(id = R.string.settings_about_latest_release), url = url)
            val url2 = "https://github.com/Ehviewer-Overhauled/Ehviewer"
            UrlPreference(title = stringResource(id = R.string.settings_about_source), url = url2)
            Preference(title = stringResource(id = R.string.license)) {
                navController.navigate(LICENSE_SCREEN)
            }
            Preference(title = stringResource(id = R.string.settings_about_version), summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.COMMIT_SHA})\n" + stringResource(R.string.settings_about_build_time, BuildConfig.BUILD_TIME))
            Preference(title = stringResource(id = R.string.settings_about_check_for_updates))
        }
    }
}
