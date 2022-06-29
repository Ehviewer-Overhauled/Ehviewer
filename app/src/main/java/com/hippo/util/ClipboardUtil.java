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

package com.hippo.util;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.textclassifier.TextClassifier;

import androidx.annotation.Nullable;

public class ClipboardUtil {
    private static final String TAG = "ClipboardUtil";
    @Nullable
    private static ClipboardManager clipboardManager;

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    public static void initialize(Context context) {
        sContext = context;
        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            Log.e(TAG, "This device has no clipboard!");
        }
    }

    public static void addTextToClipboard(String text) {
        if (clipboardManager != null) {
            try {
                clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getTextFromClipboard() {
        if (clipboardManager != null) {
            try {
                if (!clipboardManager.hasPrimaryClip()) {
                    return null;
                }
                ClipData primaryClip = clipboardManager.getPrimaryClip();
                if (primaryClip == null) {
                    return null;
                }
                ClipData.Item item = primaryClip.getItemAt(0);
                if (item == null) {
                    return null;
                }
                String string = String.valueOf(item.coerceToText(sContext));
                if (!TextUtils.isEmpty(string)) {
                    return string;
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getUrlFromClipboard() {
        if (clipboardManager != null) {
            try {
                if (!clipboardManager.hasPrimaryClip()) {
                    return null;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ClipDescription primaryClipDescription = clipboardManager.getPrimaryClipDescription();
                    if (primaryClipDescription != null) {
                        if (primaryClipDescription.getClassificationStatus() == ClipDescription.CLASSIFICATION_COMPLETE) {
                            if (primaryClipDescription.getConfidenceScore(TextClassifier.TYPE_URL) <= 0) {
                                return null;
                            }
                        }
                    }
                }
                ClipData primaryClip = clipboardManager.getPrimaryClip();
                if (primaryClip == null) {
                    return null;
                }
                ClipData.Item item = primaryClip.getItemAt(0);
                if (item == null) {
                    return null;
                }
                String string = String.valueOf(item.coerceToText(sContext));
                if (!TextUtils.isEmpty(string)) {
                    return string;
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }
}
