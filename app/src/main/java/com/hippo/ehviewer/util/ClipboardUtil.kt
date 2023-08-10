/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.textclassifier.TextClassifier
import androidx.core.os.persistableBundleOf
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.scene.BaseScene
import splitties.systemservices.clipboardManager

infix fun Context.tellClipboardWithToast(text: String?) {
    addTextToClipboard(text, false, true)
}

infix fun Context.whisperClipboard(text: String?) {
    addTextToClipboard(text, true, false)
}

fun Context.addTextToClipboard(text: CharSequence?, isSensitive: Boolean, useToast: Boolean = false) {
    clipboardManager.apply {
        setPrimaryClip(
            ClipData.newPlainText(null, text).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isSensitive) {
                    description.extras = persistableBundleOf(ClipDescription.EXTRA_IS_SENSITIVE to true)
                }
            },
        )
    }
    // Avoid double notify user since system have done that on Tiramisu above
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val activity = findActivity()
        if (activity is MainActivity) {
            activity.showTip(R.string.copied_to_clipboard, BaseScene.LENGTH_SHORT, useToast)
        }
    }
}

fun ClipboardManager.getUrlFromClipboard(context: Context): String? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && primaryClipDescription?.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE) {
        if ((primaryClipDescription?.getConfidenceScore(TextClassifier.TYPE_URL)?.let { it <= 0 }) == true) return null
    }
    val item = primaryClip?.getItemAt(0)
    val string = item?.coerceToText(context).toString()
    return string.ifEmpty { null }
}

/**
 * Find the closest Activity in a given Context.
 */
internal fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("findActivity() should be called in the context of an Activity")
}
