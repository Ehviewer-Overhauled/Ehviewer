package com.hippo.ehviewer.legacy

import android.app.Application
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File

private val OBSOLETE_CACHE_DIRS = arrayOf(
    "image",
    "thumb",
    "gallery_image",
    "spider_info",
)

@OptIn(DelicateCoroutinesApi::class)
fun cleanObsoleteCache(application: Application) {
    launchIO {
        application.deleteDatabase("hosts.db")
        val dir = application.cacheDir
        for (subdir in OBSOLETE_CACHE_DIRS) {
            val file = File(dir, subdir)
            if (file.exists()) {
                file.deleteRecursively()
            }
        }
    }
}
