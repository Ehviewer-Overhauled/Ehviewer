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
package com.hippo.ehviewer.ui.scene

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.ui.MainActivity

abstract class EhCallback<E : Fragment?, T>(context: Context) : EhClient.Callback<T> {
    val application: EhApplication

    init {
        application = context.applicationContext as EhApplication
    }

    val content: Context
        get() {
            var context: Context? = application.topActivity
            if (context == null) {
                context = application
            }
            return context
        }

    fun showTip(@StringRes id: Int, length: Int) {
        val activity = application.topActivity
        if (activity is MainActivity) {
            activity.showTip(id, length)
        } else {
            Toast.makeText(
                application, id,
                if (length == BaseScene.LENGTH_LONG) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun showTip(tip: String, length: Int) {
        val activity = application.topActivity
        if (activity is MainActivity) {
            activity.showTip(tip, length)
        } else {
            Toast.makeText(
                application, tip,
                if (length == BaseScene.LENGTH_LONG) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }
}