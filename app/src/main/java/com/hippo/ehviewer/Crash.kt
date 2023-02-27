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
import com.hippo.yorozuya.IOUtils
import com.hippo.yorozuya.OSUtils
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter

private fun joinIfStringArray(any: Any?): String {
    return if (any is Array<*>) any.joinToString() else any.toString()
}

private fun collectClassStaticInfo(clazz: Class<*>): String {
    return clazz.declaredFields.joinToString("\r\n") {
        "${it.name}=${joinIfStringArray(it.get(null))}"
    }
}

object Crash {

    @Throws(IOException::class)
    private fun collectInfo(fw: FileWriter) {
        fw.write("======== PackageInfo ========\r\n")
        fw.write("PackageName=")
        fw.write(BuildConfig.APPLICATION_ID)
        fw.write("\r\n")
        fw.write("VersionName=")
        fw.write(BuildConfig.VERSION_NAME)
        fw.write("\r\n")
        fw.write("VersionCode=")
        fw.write(BuildConfig.VERSION_CODE)
        fw.write("\r\n")
        fw.write("CommitSha=")
        fw.write(BuildConfig.COMMIT_SHA)
        fw.write("\r\n")
        fw.write("BuildTime=")
        fw.write(BuildConfig.BUILD_TIME)
        fw.write("\r\n")
        fw.write("\r\n")

        // Runtime
        val topActivityClazzName = EhApplication.application.topActivity?.javaClass?.name
        fw.write("======== Runtime ========\r\n")
        fw.write("TopActivity=")
        fw.write(topActivityClazzName ?: "null")
        fw.write("\r\n")
        fw.write("\r\n")
        fw.write("\r\n")

        // Device info
        fw.write("======== DeviceInfo ========\r\n")
        fw.write("${collectClassStaticInfo(Build::class.java)}\r\n")
        fw.write("${collectClassStaticInfo(Build.VERSION::class.java)}\r\n")
        fw.write("MEMORY=")
        fw.write(
            FileUtils.humanReadableByteCount(OSUtils.getAppAllocatedMemory(), false)
        )
        fw.write("\r\n")
        fw.write("MEMORY_NATIVE=")
        fw.write(FileUtils.humanReadableByteCount(Debug.getNativeHeapAllocatedSize(), false))
        fw.write("\r\n")
        fw.write("MEMORY_MAX=")
        fw.write(FileUtils.humanReadableByteCount(OSUtils.getAppMaxMemory(), false))
        fw.write("\r\n")
        fw.write("MEMORY_TOTAL=")
        fw.write(FileUtils.humanReadableByteCount(OSUtils.getTotalMemory(), false))
        fw.write("\r\n")
        fw.write("\r\n")
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
        var fw: FileWriter? = null
        try {
            fw = FileWriter(file)
            fw.write("TIME=")
            fw.write(nowString)
            fw.write("\r\n")
            fw.write("\r\n")
            collectInfo(fw)
            fw.write("======== CrashInfo ========\r\n")
            getThrowableInfo(t, fw)
            fw.write("\r\n")
            fw.flush()
        } catch (e: Exception) {
            file.delete()
        } finally {
            IOUtils.closeQuietly(fw)
        }
    }
}