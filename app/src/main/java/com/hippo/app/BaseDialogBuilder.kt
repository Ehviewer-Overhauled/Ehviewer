package com.hippo.app

import android.content.Context
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

open class BaseDialogBuilder constructor(
    context: Context
) : MaterialAlertDialogBuilder(context) {
    override fun create(): AlertDialog {
        return super.create().apply {
            window?.run {
                addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                attributes.blurBehindRadius = 32
            }
        }
    }
}