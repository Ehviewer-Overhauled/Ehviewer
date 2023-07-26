package com.hippo.ehviewer.util

inline fun runIf(predicate: Boolean, block: () -> Unit) = if (predicate) block() else Unit
