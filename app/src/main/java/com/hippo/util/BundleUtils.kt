package com.hippo.util

import android.os.Bundle
import androidx.core.os.BundleCompat

inline fun <reified T> Bundle.getParcelableCompat(key: String?): T? {
    return BundleCompat.getParcelable(this, key, T::class.java)
}
