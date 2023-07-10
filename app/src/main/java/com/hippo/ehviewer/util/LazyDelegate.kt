@file:Suppress("NOTHING_TO_INLINE")

package com.hippo.ehviewer.util

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

interface Lazy<T> {
    var value: T
}

fun <T> lazyMut(init: () -> KMutableProperty0<T>) = object : Lazy<T> {
    private val lazy by lazy { init() }
    override var value
        get() = lazy.get()
        set(value) = lazy.set(value)
}

inline operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value
inline operator fun <T> Lazy<T>.setValue(thisRef: Any?, prop: KProperty<*>?, newValue: T) {
    value = newValue
}
