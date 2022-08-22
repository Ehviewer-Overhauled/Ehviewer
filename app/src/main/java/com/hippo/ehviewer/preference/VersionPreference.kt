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
package com.hippo.ehviewer.preference

import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import com.hippo.Native
import com.hippo.ehviewer.R
import com.hippo.preference.MessagePreference

class VersionPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : MessagePreference(context, attrs) {
    init {
        setTitle(R.string.settings_about_version)
        @Suppress("DEPRECATION") val version: String = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            context.getString(R.string.error_unknown)
        }
        summary = version
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setTitle(R.string.show_library_version)
        val info = NativeLibInfo().addLib(Native.getlibarchiveVersion())
            .addLib("lzma(xz-utils)", Native.getliblzmaVersion())
            .addLib("zlib", Native.getzlibVersion())
            .addLib(Native.getnettleVersion())
        builder.setMessage(info.getMessage())
    }

    inner class NativeLibInfo {
        private var message: String = ""
        fun addLib(libname: String, libversion: String): NativeLibInfo {
            message = "$message$libname $libversion\n"
            return this
        }

        fun getMessage(): String {
            return message
        }

        fun addLib(msg: String): NativeLibInfo {
            message = "$message$msg\n"
            return this
        }
    }
}