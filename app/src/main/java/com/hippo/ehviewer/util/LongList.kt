package com.hippo.ehviewer.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LongList(private val delegate: MutableList<Long> = mutableListOf()) :
    Parcelable, MutableList<Long> by delegate
