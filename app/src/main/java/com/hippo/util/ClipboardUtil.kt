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
package com.hippo.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.text.TextUtils
import android.view.textclassifier.TextClassifier
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.MainActivity
import com.hippo.ehviewer.ui.SettingsActivity
import com.hippo.ehviewer.ui.scene.BaseScene

fun Context.getClipboardManager(): ClipboardManager {
    return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
}

fun ClipboardManager.addTextToClipboard(text: String?, isSensitive: Boolean, activity: Activity?) {
    setPrimaryClip(ClipData.newPlainText(null, text).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isSensitive)
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
    })

    // Avoid double notify user since system have done that on Tiramisu above
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
        if (activity is MainActivity) {
            activity.showTip(R.string.copied_to_clipboard, BaseScene.LENGTH_SHORT)
        } else if (activity is SettingsActivity) {
            activity.showTip(R.string.copied_to_clipboard, BaseScene.LENGTH_SHORT)
        }
}

fun ClipboardManager.getTextFromClipboard(context: Context): String? {
    val item = primaryClip?.getItemAt(0)
    val string = item?.coerceToText(context).toString()
    return if (!TextUtils.isEmpty(string)) string else null
}

fun ClipboardManager.getUrlFromClipboard(context: Context): String? {
    if (primaryClipDescription?.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE) {
        if ((primaryClipDescription?.getConfidenceScore(TextClassifier.TYPE_URL)
                ?.let { it <= 0 }) == true
        ) {
            return null
        }
    }
    val item = primaryClip?.getItemAt(0)
    val string = item?.coerceToText(context).toString()
    return if (!TextUtils.isEmpty(string)) string else null
}
