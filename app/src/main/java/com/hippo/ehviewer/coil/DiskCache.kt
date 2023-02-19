package com.hippo.ehviewer.coil

import coil.disk.DiskCache

inline fun DiskCache.edit(key: String, block: DiskCache.Editor.() -> Unit): Boolean {
    val editor = edit(key) ?: return false
    editor.runCatching {
        block(this)
    }.onFailure {
        editor.abort()
        throw it
    }.onSuccess {
        editor.commit()
    }
    return true
}

inline fun DiskCache.read(key: String, block: DiskCache.Snapshot.() -> Unit): Boolean {
    (get(key) ?: return false).use { block(it) }
    return true
}
