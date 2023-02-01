package com.hippo.util

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String?): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}

inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? {
    return IntentCompat.getParcelableExtra(this, key, T::class.java)
}
