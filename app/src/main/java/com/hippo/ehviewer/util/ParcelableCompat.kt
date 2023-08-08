package com.hippo.ehviewer.util

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.SparseArray
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String?): T? = BundleCompat.getParcelable(this, key, T::class.java)
inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String?): ArrayList<T>? = BundleCompat.getParcelableArrayList(this, key, T::class.java)
inline fun <reified T : Parcelable> Bundle.getSparseParcelableArrayCompat(key: String?): SparseArray<T>? = BundleCompat.getSparseParcelableArray(this, key, T::class.java)
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? = IntentCompat.getParcelableExtra(this, key, T::class.java)
