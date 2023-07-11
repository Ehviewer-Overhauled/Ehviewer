package com.hippo.ehviewer.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class IntList(private val delegate: MutableList<Int> = mutableListOf()) :
    Parcelable, MutableList<Int> by delegate
