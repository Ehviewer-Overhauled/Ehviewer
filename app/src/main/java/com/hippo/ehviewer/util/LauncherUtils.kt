package com.hippo.ehviewer.util

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import arrow.atomic.AtomicInt
import eu.kanade.tachiyomi.util.lang.withUIContext
import rikka.core.util.ContextUtils.requireActivity
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Fuck off the silly Android launcher and callback :)

private val atomicInteger = AtomicInt()

private fun Context.getLifecycle(): Lifecycle {
    var context: Context? = this
    while (true) {
        when (context) {
            is LifecycleOwner -> return context.lifecycle
            !is ContextWrapper -> TODO()
            else -> context = context.baseContext
        }
    }
}

private suspend fun <I, O> Context.registerForActivityResultAndLaunchAndAwaitResult(contract: ActivityResultContract<I, O>, input: I): O {
    val lifecycle = getLifecycle()
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
            launcher = requireActivity<ComponentActivity>(this@registerForActivityResultAndLaunchAndAwaitResult).activityResultRegistry.register(key, contract) {
                launcher?.unregister()
                lifecycle.removeObserver(observer)
                cont.resume(it)
            }.apply { launch(input) }
        }
    }
}

suspend fun Context.requestPermission(key: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, key) == PackageManager.PERMISSION_GRANTED) return true
    return registerForActivityResultAndLaunchAndAwaitResult(ActivityResultContracts.RequestPermission(), key)
}

suspend fun Context.pickVisualMedia(type: VisualMediaType): Uri? = registerForActivityResultAndLaunchAndAwaitResult(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest.Builder().setMediaType(type).build())
