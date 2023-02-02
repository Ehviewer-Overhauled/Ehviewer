package moe.tarsin.ehviewer

import platform.android.ANDROID_LOG_WARN
import platform.android.__android_log_print

private val KNI_NATIVE_TAG = "KNI"

fun Warn(message: String) {
    __android_log_print(ANDROID_LOG_WARN.toInt(), KNI_NATIVE_TAG, "%s", message)
}
