package com.hippo.ehviewer.ui.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface DialogScope<R> {
    var expectedValue: R
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

    suspend fun showSelectItemWithIcon(
        vararg items: Pair<ImageVector, Int>,
        title: String,
    ): Int {
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { dismiss() }
            content = {
                AlertDialog(
                    onDismissRequest = {
                        cont.cancel()
                        dismiss()
                    },
                    content = {
                        Surface(
                            modifier = Modifier.width(280.dp),
                            shape = AlertDialogDefaults.shape,
                            color = AlertDialogDefaults.containerColor,
                            tonalElevation = AlertDialogDefaults.TonalElevation,
                        ) {
                            Column {
                                Text(text = title, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
                                items.forEachIndexed { index, (icon, text) ->
                                    Row(
                                        modifier = Modifier.clickable {
                                            dismiss()
                                            cont.resume(index)
                                        }.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.padding(16.dp), tint = AlertDialogDefaults.iconContentColor)
                                        Text(text = stringResource(id = text), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun rememberDialogState(): DialogState {
    return remember { DialogState() }
}
