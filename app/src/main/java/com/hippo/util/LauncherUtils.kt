package com.hippo.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
private lateinit var pickVisualMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
private lateinit var callback: (Boolean) -> Unit
private lateinit var callback2: (Uri?) -> Unit

fun ComponentActivity.initPermission() {
    requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::permissionCallback)
    pickVisualMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia(), ::pickVisualMediaCallback)
}

private fun permissionCallback(v: Boolean) {
    callback(v)
}

private fun pickVisualMediaCallback(v: Uri?) {
    callback2(v)
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, key) == PackageManager.PERMISSION_GRANTED) return true
    return suspendCancellableCoroutine { cont ->
        callback = {
            cont.resume(it)
        }
        requestPermissionLauncher.launch(key)
    }
}

suspend fun pickVisualMedia(type: VisualMediaType): Uri? {
    return suspendCancellableCoroutine { cont ->
        callback2 = {
            cont.resume(it)
        }
        pickVisualMediaLauncher.launch(PickVisualMediaRequest.Builder().setMediaType(type).build())
    }
}
