package com.mikepenz.aboutlibraries.ui.compose

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.htmlReadyLicenseContent
import com.mikepenz.aboutlibraries.util.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Displays all provided libraries in a simple list.
 */
@Composable
fun LibrariesContainer(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    librariesBlock: (Context) -> Libs = { context ->
        Libs.Builder().withContext(context).build()
    },
    showAuthor: Boolean = true,
    showVersion: Boolean = true,
    showLicenseBadges: Boolean = true,
    padding: LibraryPadding = LibraryDefaults.libraryPadding(),
    itemContentPadding: PaddingValues = LibraryDefaults.ContentPadding,
    itemSpacing: Dp = LibraryDefaults.LibraryItemSpacing,
    header: (LazyListScope.() -> Unit)? = null,
    onLibraryClick: ((Library) -> Unit)? = null,
) {
    val context = LocalContext.current
    val libraries = produceState<Libs?>(null) {
        value = withContext(Dispatchers.IO) {
            librariesBlock(context)
        }
    }

    val libs = libraries.value?.libraries?.sortedBy { it.name }
    if (libs != null) {
        val openDialog = remember { mutableStateOf<Library?>(null) }

        Libraries(
            libraries = libs,
            modifier = modifier,
            lazyListState = lazyListState,
            contentPadding = contentPadding,
            showAuthor = showAuthor,
            showVersion = showVersion,
            showLicenseBadges = showLicenseBadges,
            padding = padding,
            itemContentPadding = itemContentPadding,
            itemSpacing = itemSpacing,
            header = header,
            onLibraryClick = { library ->
                if (onLibraryClick != null) {
                    onLibraryClick(library)
                } else if (!library.licenses.firstOrNull()?.htmlReadyLicenseContent.isNullOrBlank()) {
                    openDialog.value = library
                }
            },
        )

        val library = openDialog.value
        if (library != null) {
            LicenseDialog(library = library) {
                openDialog.value = null
            }
        }
    }
}

@Composable
fun LicenseDialog(
    library: Library,
    onDismiss: () -> Unit,
) {
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
            ) {
                HtmlText(
                    html = library.licenses.firstOrNull()?.htmlReadyLicenseContent.orEmpty(),
                )
            }
        },
    )
}

@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(modifier = modifier, factory = { context ->
        TextView(context)
    }, update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) })
}
