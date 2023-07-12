/*
 * Copyright 2015 Hippo Seven
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
package com.hippo.ehviewer.util

import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.FileReader
import java.util.regex.Pattern

object OSUtils {
    private const val PROCFS_MEMFILE = "/proc/meminfo"
    private val PROCFS_MEMFILE_FORMAT = Pattern.compile("^([a-zA-Z]*):[ \t]*([0-9]*)[ \t]kB")
    private const val MEMTOTAL_STRING = "MemTotal"
    private var sTotalMem = Long.MIN_VALUE
    val appAllocatedMemory: Long
        /**
         * Get application allocated memory size
         */
        get() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
    val appMaxMemory: Long
        /**
         * Get application max memory size
         */
        get() = Runtime.getRuntime().maxMemory()
    val totalMemory: Long
        /**
         * Get device RAM size
         */
        get() {
            if (sTotalMem == Long.MIN_VALUE) {
                runCatching {
                    BufferedReader(FileReader(PROCFS_MEMFILE), 64).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val matcher = PROCFS_MEMFILE_FORMAT.matcher(line!!)
                            if (matcher.find() && MEMTOTAL_STRING == matcher.group(1)) {
                                var mem = matcher.group(2)!!.toLongOrDefault(-1L)
                                if (mem != -1L) {
                                    mem *= 1024
                                }
                                sTotalMem = mem
                                break
                            }
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                }
                if (sTotalMem == Long.MIN_VALUE) {
                    sTotalMem = -1L
                }
            }
            return sTotalMem
        }
}

val isMainThread: Boolean
    get() = Looper.getMainLooper().thread === Thread.currentThread()

fun assertNotMainThread() {
    check(!isMainThread) { "Cannot access database on the main thread since" + " it may potentially lock the UI for a long period of time." }
}

fun <T> runAssertingNotMainThread(block: suspend CoroutineScope.() -> T) = assertNotMainThread().run { runBlocking(block = block) }
