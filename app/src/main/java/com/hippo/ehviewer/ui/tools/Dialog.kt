package com.hippo.ehviewer.ui.tools

import android.graphics.Typeface
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface DialogScope<R> {
    var expectedValue: R
}

fun interface DismissDialogScope<R> {
    fun dismissWith(value: R)
}

class DialogState {
    private var content: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    fun Handler() = content?.invoke()

    fun dismiss() {
        content = null
    }

    suspend fun <R> show(initial: R, @StringRes title: Int? = null, block: @Composable DialogScope<R>.() -> Unit): R {
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { dismiss() }
            content = {
                val state = remember(cont) { mutableStateOf(initial) }
                val impl = remember(cont) { object : DialogScope<R> { override var expectedValue by state } }
                AlertDialog(
                    onDismissRequest = {
                        cont.cancel()
                        dismiss()
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            dismiss()
                            cont.resume(state.value)
                        }) {
                            Text(text = stringResource(id = android.R.string.ok))
                        }
                    },
                    title = title?.let { { Text(text = stringResource(id = title)) } },
                    text = { block(impl) },
                )
            }
        }
    }

    suspend fun show(
        @StringRes confirmText: Int? = null,
        @StringRes dismissText: Int? = null,
        @StringRes title: Int? = null,
        text: (@Composable () -> Unit)? = null,
    ): Boolean {
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { dismiss() }
            content = {
                AlertDialog(
                    onDismissRequest = {
                        dismiss()
                        cont.resume(false)
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            dismiss()
                            cont.resume(true)
                        }) {
                            Text(text = stringResource(id = confirmText ?: android.R.string.ok))
                        }
                    },
                    dismissButton = dismissText?.let {
                        {
                            TextButton(onClick = {
                                dismiss()
                                cont.resume(false)
                            }) {
                                Text(text = stringResource(id = dismissText))
                            }
                        }
                    },
                    title = title?.let { { Text(text = stringResource(id = title)) } },
                    text = text,
                )
            }
        }
    }

    private suspend fun <R> showNoButton(respectDefaultWidth: Boolean = true, block: @Composable DismissDialogScope<R>.() -> Unit): R {
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { dismiss() }
            content = {
                val impl = remember(cont) {
                    DismissDialogScope<R> {
                        dismiss()
                        cont.resume(it)
                    }
                }
                AlertDialog(
                    onDismissRequest = {
                        cont.cancel()
                        dismiss()
                    },
                    content = {
                        Surface(
                            modifier = with(Modifier) { if (!respectDefaultWidth) defaultMinSize(280.dp) else width(280.dp) },
                            shape = AlertDialogDefaults.shape,
                            color = AlertDialogDefaults.containerColor,
                            tonalElevation = AlertDialogDefaults.TonalElevation,
                            content = { block(impl) },
                        )
                    },
                )
            }
        }
    }

    suspend fun showSelectItem(
        vararg items: String,
        @StringRes title: Int,
    ): Int = showNoButton {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = stringResource(id = title), style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.size(16.dp))
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.tertiary) {
                items.forEachIndexed { index, text ->
                    Text(text = text, modifier = Modifier.clickable { dismissWith(index) }.fillMaxWidth().padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    suspend fun showSelectItemWithIcon(
        vararg items: Pair<ImageVector, Int>,
        title: String,
    ): Int = showNoButton {
        LazyColumn {
            stickyHeader {
                Text(text = title, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            }
            itemsIndexed(items) { index, (icon, text) ->
                ListItem(
                    headlineContent = {
                        Text(text = stringResource(id = text), style = MaterialTheme.typography.titleMedium)
                    },
                    modifier = Modifier.clickable { dismissWith(index) },
                    leadingContent = {
                        Icon(imageVector = icon, contentDescription = null, tint = AlertDialogDefaults.iconContentColor)
                    },
                )
            }
        }
    }

    suspend fun showSelectItemWithIconAndTextField(
        vararg items: Pair<ImageVector, String>,
        @StringRes title: Int,
        @StringRes hint: Int,
        maxChar: Int,
    ): Pair<Int, String> = showNoButton(false) {
        Column {
            Text(text = stringResource(id = title), modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
            CircularLayout(
                radius = 112.dp,
                modifier = Modifier.fillMaxWidth().aspectRatio(1F),
                placeFirstItemInCenter = true,
            ) {
                var note by remember { mutableStateOf("") }
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.width(128.dp),
                    textStyle = TextStyle(fontFamily = FontFamily(Typeface.MONOSPACE)),
                    label = { Text(text = stringResource(id = hint)) },
                    trailingIcon = {
                        if (note.isNotEmpty()) {
                            IconButton(onClick = { note = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    supportingText = {
                        Text(
                            text = "${note.length} / $maxChar",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                    maxLines = 6,
                    shape = ShapeDefaults.ExtraSmall,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                items.forEachIndexed { index, (icon, text) ->
                    Column(
                        modifier = Modifier.clip(IconWithTextCorner).clickable { dismissWith(index to note) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = AlertDialogDefaults.iconContentColor)
                        Text(text = text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private val IconWithTextCorner = RoundedCornerShape(8.dp)

@Composable
fun rememberDialogState(): DialogState {
    return remember { DialogState() }
}
