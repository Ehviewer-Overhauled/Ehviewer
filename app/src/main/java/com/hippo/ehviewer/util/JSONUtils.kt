package com.hippo.ehviewer.util

import org.json.JSONArray

fun <T> JSONArray.iter() = object : Iterable<T> {
    override fun iterator() = object : Iterator<T> {
        var index = 0
        override fun hasNext() = index != length()

        @Suppress("UNCHECKED_CAST")
        override fun next() = get(index++) as T
    }
}
