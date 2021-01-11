package com.hippo.util;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

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
}
