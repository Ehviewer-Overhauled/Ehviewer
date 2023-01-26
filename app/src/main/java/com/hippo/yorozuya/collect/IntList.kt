package com.hippo.yorozuya.collect

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class IntList : Parcelable, MutableList<Int> by mutableListOf()