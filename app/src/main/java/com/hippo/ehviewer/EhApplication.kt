/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.LruCache
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import coil.util.DebugLogger
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.net.cronet.okhttptransport.CronetInterceptor
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.coil.MergeInterceptor
import com.hippo.ehviewer.coil.diskCache
import com.hippo.ehviewer.coil.imageLoader
import com.hippo.ehviewer.dailycheck.checkDawn
import com.hippo.ehviewer.dao.EhDatabase
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.legacy.cleanObsoleteCache
import com.hippo.ehviewer.legacy.migrateCookies
import com.hippo.ehviewer.okhttp.cache
import com.hippo.ehviewer.okhttp.httpClient
import com.hippo.ehviewer.spider.cronetHttpClient
import com.hippo.ehviewer.ui.keepNoMediaFileStatus
import com.hippo.ehviewer.ui.lockObserver
import com.hippo.ehviewer.util.ReadableTime
import com.hippo.ehviewer.yorozuya.FileUtils
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.AsyncDns
import okhttp3.android.AndroidAsyncDns
import okio.Path.Companion.toOkioPath
import splitties.arch.room.roomDb
import splitties.init.appCtx

class EhApplication : Application(), ImageLoaderFactory {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(lockObserver)
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                if (Settings.saveCrashLog) {
                    Crash.saveCrashLog(e)
                }
            } catch (ignored: Throwable) {
            }
            handler?.uncaughtException(t, e)
        }
        super.onCreate()
        System.loadLibrary("ehviewer")
        System.loadLibrary("ehviewer_rust")
        ReadableTime.initialize(this)
        launchIO {
            val theme = Settings.theme
            withUIContext {
                AppCompatDelegate.setDefaultNightMode(theme)
            }
        }
        launchIO {
            launchIO {
                migrateCookies()
            }
            launchIO {
                EhTagDatabase.update()
            }
            launchIO {
                ehDatabase
            }
            launchIO {
                DownloadManager.isIdle
            }
            launchIO {
                cleanupDownload()
            }
            if (Settings.requestNews) {
                launchIO {
                    checkDawn()
                }
            }
        }
        cleanObsoleteCache(this)
    }

    private suspend fun cleanupDownload() {
        runCatching {
            keepNoMediaFileStatus()
        }.onFailure {
            it.printStackTrace()
        }
        runCatching {
            clearTempDir()
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun clearTempDir() {
        var dir = AppConfig.tempDir
        if (null != dir) {
            FileUtils.deleteContent(dir)
        }
        dir = AppConfig.externalTempDir
        if (null != dir) {
            FileUtils.deleteContent(dir)
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            galleryDetailCache.evictAll()
        }
    }

    override fun newImageLoader() = imageLoader {
        okHttpClient(nonCacheOkHttpClient)
        components {
            add { result, options, _ -> ImageDecoderDecoder(result.source, options, false) }
            add(MergeInterceptor)
        }
        diskCache(imageCache)
        crossfade(300)
        error(R.drawable.image_failed)
        if (BuildConfig.DEBUG) logger(DebugLogger())
    }

    companion object {
        val nonCacheOkHttpClient by lazy {
            httpClient {
                cookieJar(EhCookieStore)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) dns(AsyncDns.toDns(AndroidAsyncDns.IPv4, AndroidAsyncDns.IPv6))
                addInterceptor(
                    ChuckerInterceptor.Builder(appCtx).apply {
                        alwaysReadResponseBody(false)
                    }.build(),
                )

                // TODO: Rewrite CronetInterceptor to use android.net.http.HttpEngine and make it Android 14 only when released
                addInterceptor(CronetInterceptor.newBuilder(cronetHttpClient).build())
            }
        }

        // Never use this okhttp client to download large blobs!!!
        val okHttpClient by lazy {
            httpClient(nonCacheOkHttpClient) {
                cache(
                    appCtx.cacheDir.toOkioPath() / "http_cache",
                    20L * 1024L * 1024L,
                )
            }
        }

        val galleryDetailCache by lazy {
            LruCache<Long, GalleryDetail>(25).also {
                FavouriteStatusRouter.addListener { gid, slot ->
                    it[gid]?.favoriteSlot = slot
                }
            }
        }

        val ehDatabase by lazy { roomDb<EhDatabase>("eh.db") { allowMainThreadQueries() } }

        val imageCache by lazy {
            diskCache {
                directory(appCtx.cacheDir.toOkioPath() / "image_cache")
                maxSizeBytes(Settings.readCacheSize.coerceIn(320, 5120).toLong() * 1024 * 1024)
            }
        }
    }
}
