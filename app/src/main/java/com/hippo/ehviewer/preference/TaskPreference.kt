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
import android.util.AttributeSet
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.SettingsActivity
import com.hippo.preference.DialogPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class TaskPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : DialogPreference(context, attrs), CoroutineScope {
    override val coroutineContext = Dispatchers.IO + Job()
    protected lateinit var dialog: AlertDialog
    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setTitle(null)
        builder.setView(R.layout.preference_dialog_task)
        builder.setCancelable(false)
    }

    abstract fun launchJob()

    override fun onDialogCreated(dialog: AlertDialog) {
        this.dialog = dialog
        launchJob()
    }

    protected fun showTip(msg: String) {
        (context as SettingsActivity).showTip(
            msg,
            Snackbar.LENGTH_SHORT,
        )
    }
}
