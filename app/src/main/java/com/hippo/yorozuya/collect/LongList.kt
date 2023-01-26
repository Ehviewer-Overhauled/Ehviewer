package com.hippo.yorozuya.collect

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LongList : Parcelable, MutableList<Long> by mutableListOf()