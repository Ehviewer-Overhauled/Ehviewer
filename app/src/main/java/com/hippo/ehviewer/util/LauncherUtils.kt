package com.hippo.ehviewer.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.core.content.ContextCompat
import arrow.atomic.Atomic
import arrow.atomic.update
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Fuck off the silly Android launcher and callback :)

private typealias LauncherAndCallback<K, V> = Atomic<Pair<ActivityResultLauncher<K>, (V) -> Unit>>
private fun <K, V> LauncherAndCallback<K, V>.cleanup() = update { it.copy {} }
private lateinit var requestPermission: LauncherAndCallback<String, Boolean>
private lateinit var pickVisualMedia: LauncherAndCallback<PickVisualMediaRequest, Uri?>
private suspend fun <K, V> LauncherAndCallback<K, V>.await(key: K) = withUIContext {
    suspendCancellableCoroutine { cont ->
        updateAndGet { prev -> prev.copy { cont.resume(it) } }.first.launch(key)
        cont.invokeOnCancellation { cleanup() } // Drop continuation when cancelled
    }.apply { cleanup() } // Drop continuation when completed
}

fun ComponentActivity.initLauncher() {
    requestPermission = Atomic(registerForActivityResult(ActivityResultContracts.RequestPermission()) { requestPermission.get().second(it) } to {})
    pickVisualMedia = Atomic(registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { pickVisualMedia.get().second(it) } to {})
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, key) == PackageManager.PERMISSION_GRANTED) return true
    return requestPermission.await(key)
}

suspend fun pickVisualMedia(type: VisualMediaType): Uri? = pickVisualMedia.await(PickVisualMediaRequest.Builder().setMediaType(type).build())
