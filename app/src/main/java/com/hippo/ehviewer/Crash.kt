/*
 * Copyright 2019 Hippo Seven
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

import android.os.Build
import android.os.Debug
import com.hippo.util.ReadableTime
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.OSUtils
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

private fun joinIfStringArray(any: Any?): String {
    return if (any is Array<*>) any.joinToString() else any.toString()
}

private fun collectClassStaticInfo(clazz: Class<*>): String {
    return clazz.declaredFields.joinToString("\n") {
        "${it.name}=${joinIfStringArray(it.get(null))}"
    }
}

object Crash {
    private fun collectInfo(fw: FileWriter) {
        fw.write("======== PackageInfo ========\n")
        fw.write("PackageName=${BuildConfig.APPLICATION_ID}\n")
        fw.write("VersionName=${BuildConfig.VERSION_NAME}\n")
        fw.write("VersionCode=${BuildConfig.VERSION_CODE}\n")
        fw.write("CommitSha=${BuildConfig.COMMIT_SHA}\n")
        fw.write("BuildTime=${BuildConfig.BUILD_TIME}\n")
        fw.write("\n")

        // Runtime
        fw.write("======== Runtime ========\n")
        fw.write("TopActivity=${EhApplication.application.topActivity?.javaClass?.name}\n")
        fw.write("\n")

        // Device info
        fw.write("======== DeviceInfo ========\n")
        fw.write("${collectClassStaticInfo(Build::class.java)}\n")
        fw.write("${collectClassStaticInfo(Build.VERSION::class.java)}\n")
        fw.write("MEMORY=")
        fw.write(FileUtils.humanReadableByteCount(OSUtils.getAppAllocatedMemory(), false))
        fw.write("\n")
        fw.write("MEMORY_NATIVE=")
        fw.write(FileUtils.humanReadableByteCount(Debug.getNativeHeapAllocatedSize(), false))
        fw.write("\n")
        fw.write("MEMORY_MAX=")
        fw.write(FileUtils.humanReadableByteCount(OSUtils.getAppMaxMemory(), false))
        fw.write("\n")
        fw.write("MEMORY_TOTAL=")
        fw.write(FileUtils.humanReadableByteCount(OSUtils.getTotalMemory(), false))
        fw.write("\n")
        fw.write("\n")
    }

    private fun getThrowableInfo(t: Throwable, fw: FileWriter) {
        val printWriter = PrintWriter(fw)
        t.printStackTrace(printWriter)
        var cause = t.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
    }

    fun saveCrashLog(t: Throwable) {
        val dir = AppConfig.getExternalCrashDir() ?: return
        val nowString = ReadableTime.getFilenamableTime(System.currentTimeMillis())
        val fileName = "crash-$nowString.log"
        val file = File(dir, fileName)
        runCatching {
            FileWriter(file).use { fw ->
                fw.write("TIME=${nowString}\n")
                fw.write("\n")
                collectInfo(fw)
                fw.write("======== CrashInfo ========\n")
                getThrowableInfo(t, fw)
                fw.write("\n")
                fw.flush()
            }
        }.onFailure {
            it.printStackTrace()
            file.delete()
        }
    }
}
