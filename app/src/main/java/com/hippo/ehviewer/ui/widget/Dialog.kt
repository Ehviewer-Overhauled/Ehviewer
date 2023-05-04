package com.hippo.ehviewer.ui.widget

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface DialogScope {
    fun dismiss()
}

class DialogState : DialogScope {
    private var content: (@Composable () -> Unit)? by mutableStateOf(null)

    @Composable
    fun Handler() = content?.invoke()

    override fun dismiss() {
        content = null
    }

    suspend fun show(block: @Composable DialogScope.() -> Unit) {
        suspendCancellableCoroutine<Unit> {
            content = {
                AlertDialog(onDismissRequest = { dismiss() }, confirmButton = { /*TODO*/ })
                block()
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
}

@Composable
fun rememberDialogState(): DialogState {
    return remember { DialogState() }
}
