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
package com.hippo.util

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.hippo.ehviewer.R

object AppHelper {
    fun share(from: Activity, text: String?): Boolean {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, text)
        sendIntent.type = "text/plain"
        val chooser = Intent.createChooser(sendIntent, from.getString(R.string.share))
        return runCatching {
            from.startActivity(chooser)
            true
        }.getOrElse {
            ExceptionUtils.throwIfFatal(it)
            Toast.makeText(from, R.string.error_cant_find_activity, Toast.LENGTH_SHORT).show()
            false
        }
    }
}
