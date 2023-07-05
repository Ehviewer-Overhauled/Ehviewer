package com.hippo.ehviewer.util

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import arrow.atomic.AtomicInt
import eu.kanade.tachiyomi.util.lang.withUIContext
import rikka.core.util.ContextUtils.requireActivity
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Fuck off the silly Android launcher and callback :)

private val atomicInteger = AtomicInt()

private val Context.lifecycle: Lifecycle
    get() {
        var context: Context? = this
        while (true) {
            when (context) {
                is LifecycleOwner -> return context.lifecycle
                !is ContextWrapper -> error("This should never happen!")
                else -> context = context.baseContext
            }
        }
    }

private suspend fun <I, O> Context.awaitActivityResult(contract: ActivityResultContract<I, O>, input: I): O {
    val key = "activity_rq#${atomicInteger.getAndIncrement()}"
    var launcher: ActivityResultLauncher<I>? = null
    var observer: LifecycleEventObserver? = null
    observer = LifecycleEventObserver { _, event ->
        if (Lifecycle.Event.ON_DESTROY == event) {
            launcher?.unregister()
            if (observer != null) {
                lifecycle.removeObserver(observer!!)
            }
        }
    }
    lifecycle.addObserver(observer)
    return withUIContext {
        suspendCoroutine { cont -> // No cancellation support here since we cannot cancel a launched Intent
            val activity = requireActivity<ComponentActivity>(this@awaitActivityResult)
            launcher = activity.activityResultRegistry.register(key, contract) {
                launcher?.unregister()
                lifecycle.removeObserver(observer)
                cont.resume(it)
            }.apply { launch(input) }
        }
    }
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, key) == PackageManager.PERMISSION_GRANTED) return true
    return awaitActivityResult(ActivityResultContracts.RequestPermission(), key)
}

suspend fun Context.pickVisualMedia(type: VisualMediaType): Uri? = awaitActivityResult(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest.Builder().setMediaType(type).build())

suspend fun Context.requestInstallPermission(): Boolean {
    if (packageManager.canRequestPackageInstalls()) return true
    val granted = requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    if (!granted) {
        awaitActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName"),
            ),
        )
        requestPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES)
    }
    return packageManager.canRequestPackageInstalls()
}

suspend fun Context.installPackage(file: File) {
    val canInstall = requestInstallPermission()
    if (canInstall) {
        val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(contentUri, "application/vnd.android.package-archive")
        }
        if (packageManager.queryIntentActivities(intent, 0).size > 0) {
            startActivity(intent)
        }
    }
}
