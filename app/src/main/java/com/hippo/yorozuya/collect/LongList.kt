package com.hippo.yorozuya.collect

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LongList constructor(private val delegate: MutableList<Long> = mutableListOf()) :
    Parcelable, MutableList<Long> by delegate