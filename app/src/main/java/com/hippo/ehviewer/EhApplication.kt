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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks2
import androidx.appcompat.app.AppCompatDelegate
import androidx.collection.LruCache
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhDns
import com.hippo.ehviewer.client.EhSSLSocketFactory
import com.hippo.ehviewer.client.EhTagDatabase
import com.hippo.ehviewer.client.data.GalleryDetail
import com.hippo.ehviewer.coil.MergeInterceptor
import com.hippo.ehviewer.dailycheck.checkDawn
import com.hippo.ehviewer.dao.buildMainDB
import com.hippo.ehviewer.download.DownloadManager
import com.hippo.ehviewer.ui.CommonOperations
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.util.ExceptionUtils
import com.hippo.util.ReadableTime
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IntIdGenerator
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.lang.launchIO
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.OkHttpClient
import java.net.Proxy
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class EhApplication : Application(), DefaultLifecycleObserver, ImageLoaderFactory {
    private val mIdGenerator = IntIdGenerator()
    private val mGlobalStuffMap = HashMap<Int, Any>()
    private val mActivityList = ArrayList<Activity>()
    val topActivity: EhActivity?
        get() = if (mActivityList.isNotEmpty()) {
            mActivityList[mActivityList.size - 1] as EhActivity
        } else {
            null
        }

    fun recreateAllActivity() {
        mActivityList.forEach { it.recreate() }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("StaticFieldLeak")
    override fun onCreate() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        application = this
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
        super<Application>.onCreate()
        System.loadLibrary("ehviewer")
        GetText.initialize(this)
        Settings.initialize()
        ReadableTime.initialize(this)
        AppConfig.initialize(this)
        EhTagDatabase.update()
        AppCompatDelegate.setDefaultNightMode(Settings.theme)
        launchIO {
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
        mIdGenerator.setNextId(Settings.getInt(KEY_GLOBAL_STUFF_NEXT_ID, 0))
    }

    private fun cleanupDownload() {
        try {
            val downloadLocation = Settings.downloadLocation
            if (Settings.mediaScan) {
                CommonOperations.removeNoMediaFile(downloadLocation)
            } else {
                CommonOperations.ensureNoMediaFile(downloadLocation)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            ExceptionUtils.throwIfFatal(t)
        }

        // Clear temp files
        try {
            clearTempDir()
        } catch (t: Throwable) {
            t.printStackTrace()
            ExceptionUtils.throwIfFatal(t)
        }
    }

    private fun clearTempDir() {
        var dir = AppConfig.getTempDir()
        if (null != dir) {
            FileUtils.deleteContent(dir)
        }
        dir = AppConfig.getExternalTempDir()
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

    fun putGlobalStuff(o: Any): Int {
        val id = mIdGenerator.nextId()
        mGlobalStuffMap[id] = o
        Settings.putInt(KEY_GLOBAL_STUFF_NEXT_ID, mIdGenerator.nextId())
        return id
    }

    fun containGlobalStuff(id: Int): Boolean {
        return mGlobalStuffMap.containsKey(id)
    }

    fun getGlobalStuff(id: Int): Any? {
        return mGlobalStuffMap[id]
    }

    fun removeGlobalStuff(id: Int): Any? {
        return mGlobalStuffMap.remove(id)
    }

    fun removeGlobalStuff(o: Any) {
        mGlobalStuffMap.values.removeAll(setOf(o))
    }

    fun registerActivity(activity: Activity) {
        mActivityList.add(activity)
    }

    fun unregisterActivity(activity: Activity) {
        mActivityList.remove(activity)
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!locked)
            locked_last_leave_time = System.currentTimeMillis() / 1000
        locked = true
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).apply {
            okHttpClient(okHttpClient)
            components { add(MergeInterceptor) }
            crossfade(300)
            if (BuildConfig.DEBUG) logger(DebugLogger())
        }.build()
    }

    companion object {
        private const val KEY_GLOBAL_STUFF_NEXT_ID = "global_stuff_next_id"
        var locked = true
        var locked_last_leave_time: Long = 0

        @JvmStatic
        lateinit var application: EhApplication
            private set

        @JvmStatic
        val ehCookieStore by lazy { EhCookieStore }

        @JvmStatic
        val ehProxySelector by lazy { EhProxySelector() }

        @JvmStatic
        val okHttpClient by lazy {
            val builder = OkHttpClient.Builder()
                .cookieJar(ehCookieStore)
                .dns(EhDns)
                .proxySelector(ehProxySelector)

            if (Settings.dF) {
                val factory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())!!
                factory.init(null as KeyStore?)
                val manager = factory.trustManagers!!

                val trustManager = manager.filterIsInstance<X509TrustManager>().first()
                builder.sslSocketFactory(EhSSLSocketFactory, trustManager)
                builder.proxy(Proxy.NO_PROXY)
            }
            builder.build()
        }

        @JvmStatic
        val galleryDetailCache by lazy {
            LruCache<Long, GalleryDetail>(25).also {
                favouriteStatusRouter.addListener { gid, slot ->
                    it[gid]?.favoriteSlot = slot
                }
            }
        }

        @JvmStatic
        val hosts by lazy { Hosts(application, "hosts.db") }

        @JvmStatic
        val favouriteStatusRouter by lazy { FavouriteStatusRouter() }

        @JvmStatic
        val readerPreferences by lazy { ReaderPreferences(AndroidPreferenceStore(application)) }

        @JvmStatic
        val ehDatabase by lazy { buildMainDB(application) }
    }
}
