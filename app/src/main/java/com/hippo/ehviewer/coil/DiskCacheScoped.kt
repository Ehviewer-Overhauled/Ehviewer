package com.hippo.ehviewer.coil

import coil.disk.DiskCache

suspend inline fun DiskCache.edit(key: String, crossinline block: suspend DiskCache.Editor.() -> Unit): Boolean {
    val editor = openEditor(key) ?: return false
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

inline fun <T> DiskCache.read(key: String, block: DiskCache.Snapshot.() -> T): T? = openSnapshot(key)?.use(block)
