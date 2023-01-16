@file:OptIn(DelicateCoroutinesApi::class, ExperimentalCoilApi::class)

package com.hippo.ehviewer.spider

import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

// Avoid being interrupted when writing journal, this cause journal errors

fun completeEditorUninterruptible(editor: DiskCache.Editor) {
    runBlocking {
        GlobalScope.launchIO {
            editor.commit()
        }.join()
    }
}

fun closeSnapShotUninterruptible(snapshot: DiskCache.Snapshot) {
    runBlocking {
        GlobalScope.launchIO {
            snapshot.close()
        }.join()
    }
}

fun getSnapShotUninterruptible(key: String, cache: DiskCache): DiskCache.Snapshot? {
    return runBlocking {
        withContext(Dispatchers.IO) {
            cache[key]
        }
    }
}

fun getEditorUninterruptible(key: String, cache: DiskCache): DiskCache.Editor? {
    return runBlocking {
        withContext(Dispatchers.IO) {
            cache.edit(key)
        }
    }
}

fun removeUninterruptible(key: String, cache: DiskCache): Boolean {
    return runBlocking {
        withContext(Dispatchers.IO) {
            cache.remove(key)
        }
    }
}
