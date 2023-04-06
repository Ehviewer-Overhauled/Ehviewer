package com.hippo.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
private lateinit var callback: (Boolean) -> Unit
fun ComponentActivity.initPermission() {
    requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::permissionCallback)
}

private fun permissionCallback(v: Boolean) {
    callback(v)
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return true
    return suspendCancellableCoroutine { cont ->
        callback = {
            cont.resume(it)
        }
        requestPermissionLauncher.launch(key)
    }
}
