package com.hippo.yorozuya.collect

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class IntList constructor(private val delegate: MutableList<Int> = mutableListOf()) :
    Parcelable, MutableList<Int> by delegate