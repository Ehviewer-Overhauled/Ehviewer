@file:Suppress("DEPRECATION")

package com.hippo.ehviewer.util

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable

// inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String?): T? = BundleCompat.getParcelable(this, key, T::class.java)
// inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String?): ArrayList<T>? = BundleCompat.getParcelableArrayList(this, key, T::class.java)
// inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? = IntentCompat.getParcelableExtra(this, key, T::class.java)

// Avoid using new API on Tiramisu
// See https://issuetracker.google.com/issues/240585930 & https://github.com/Ehviewer-Overhauled/Ehviewer/issues/1065

inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String?): T? = getParcelable(key)
inline fun <reified T : Parcelable> Bundle.getParcelableArrayListCompat(key: String?): ArrayList<T>? = getParcelableArrayList(key)
inline fun <reified T : Parcelable> Intent.getParcelableExtraCompat(key: String?): T? = getParcelableExtra(key)
