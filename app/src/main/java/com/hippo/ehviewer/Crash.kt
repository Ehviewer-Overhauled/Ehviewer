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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import com.hippo.util.PackageUtils
import com.hippo.util.ReadableTime
import com.hippo.yorozuya.FileUtils
import com.hippo.yorozuya.IOUtils
import com.hippo.yorozuya.OSUtils
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.util.Arrays

object Crash {
    private fun avoidNull(str: String?): String {
        return str ?: "null"
    }

    @Throws(IOException::class)
    private fun collectInfo(context: Context, fw: FileWriter) {
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            if (pi != null) {
                val versionName = if (pi.versionName == null) "null" else pi.versionName
                val versionCode = pi.versionCode.toString()
                fw.write("======== PackageInfo ========\r\n")
                fw.write("PackageName=")
                fw.write(pi.packageName)
                fw.write("\r\n")
                fw.write("VersionName=")
                fw.write(versionName)
                fw.write("\r\n")
                fw.write("VersionCode=")
                fw.write(versionCode)
                fw.write("\r\n")
                val signature = PackageUtils.getSignature(context, pi.packageName)
                fw.write("Signature=")
                fw.write(signature ?: "null")
                fw.write("\r\n")
                fw.write("\r\n")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            fw.write("======== PackageInfo ========\r\n")
            fw.write("Can't get package information\r\n")
            fw.write("\r\n")
        }

        // Runtime
        val topActivityClazzName = "null"
        fw.write("======== Runtime ========\r\n")
        fw.write("TopActivity=")
        fw.write(avoidNull(topActivityClazzName))
        fw.write("\r\n")
        fw.write("\r\n")
        fw.write("\r\n")

        // Device info
        fw.write("======== DeviceInfo ========\r\n")
        fw.write("BOARD=")
        fw.write(Build.BOARD)
        fw.write("\r\n")
        fw.write("BOOTLOADER=")
        fw.write(Build.BOOTLOADER)
        fw.write("\r\n")
        fw.write("SUPPORTED_ABIS=")
        fw.write(Arrays.toString(Build.SUPPORTED_ABIS))
        fw.write("\r\n")
        fw.write("DEVICE=")
        fw.write(Build.DEVICE)
        fw.write("\r\n")
        fw.write("DISPLAY=")
        fw.write(Build.DISPLAY)
        fw.write("\r\n")
        fw.write("FINGERPRINT=")
        fw.write(Build.FINGERPRINT)
        fw.write("\r\n")
        fw.write("HARDWARE=")
        fw.write(Build.HARDWARE)
        fw.write("\r\n")
        fw.write("HOST=")
        fw.write(Build.HOST)
        fw.write("\r\n")
        fw.write("ID=")
        fw.write(Build.ID)
        fw.write("\r\n")
        fw.write("MANUFACTURER=")
        fw.write(Build.MANUFACTURER)
        fw.write("\r\n")
        fw.write("MODEL=")
        fw.write(Build.MODEL)
        fw.write("\r\n")
        fw.write("PRODUCT=")
        fw.write(Build.PRODUCT)
        fw.write("\r\n")
        fw.write("RADIO=")
        fw.write(Build.getRadioVersion())
        fw.write("\r\n")
        fw.write("TAGS=")
        fw.write(Build.TAGS)
        fw.write("\r\n")
        fw.write("TYPE=")
        fw.write(Build.TYPE)
        fw.write("\r\n")
        fw.write("USER=")
        fw.write(Build.USER)
        fw.write("\r\n")
        fw.write("CODENAME=")
        fw.write(Build.VERSION.CODENAME)
        fw.write("\r\n")
        fw.write("INCREMENTAL=")
        fw.write(Build.VERSION.INCREMENTAL)
        fw.write("\r\n")
        fw.write("RELEASE=")
        fw.write(Build.VERSION.RELEASE)
        fw.write("\r\n")
        fw.write("SDK=")
        fw.write(Integer.toString(Build.VERSION.SDK_INT))
        fw.write("\r\n")
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

    fun saveCrashLog(context: Context, t: Throwable) {
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
            collectInfo(context, fw)
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