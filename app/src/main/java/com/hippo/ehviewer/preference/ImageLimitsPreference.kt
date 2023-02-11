package com.hippo.ehviewer.preference

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.SettingsActivity
import com.hippo.preference.DialogPreference
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext

class ImageLimitsPreference(context: Context, attrs: AttributeSet) :
    DialogPreference(context, attrs), View.OnClickListener {
    private val mActivity = context as SettingsActivity
    private val placeholder = context.getString(R.string.please_wait)
    private lateinit var resetButton: Button
    private lateinit var mDialog: AlertDialog

    init {
        updateSummary()
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setMessage(placeholder)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)
        mDialog = dialog
        resetButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        resetButton.setOnClickListener(this)
        resetButton.isEnabled = false
        launchIOCatching {
            mActivity.getImageLimits()
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        updateSummary()
    }

    private fun updateSummary() {
        summary = mActivity.mLimits?.run {
            mActivity.getString(R.string.image_limits_summary, current, maximum)
        } ?: mActivity.getString(R.string.image_limits_summary, 0, 0)
    }

    private fun launchIOCatching(block: suspend () -> Unit) = mActivity.lifecycleScope.launchIO {
        runCatching {
            block()
        }.onFailure {
            it.printStackTrace()
            withUIContext {
                mDialog.setMessage(it.message)
            }
        }.onSuccess {
            withUIContext {
                bind()
            }
        }
    }

    private fun bind() {
        val (current, maximum, resetCost) = mActivity.mLimits!!
        val (fundsGP, fundsC) = mActivity.mFunds!!
        val cost = if (fundsGP >= resetCost) "$resetCost GP" else "$resetCost Credits"
        val message = mActivity.getString(R.string.current_limits, "$current / $maximum", cost) +
                "\n" + mActivity.getString(R.string.current_funds, "$fundsGP+", fundsC)
        mDialog.setMessage(message)
        resetButton.isEnabled = resetCost in 1..maxOf(fundsGP, fundsC)
    }

    override fun onClick(v: View) {
        resetButton.isEnabled = false
        mDialog.setMessage(placeholder)
        launchIOCatching {
            mActivity.resetImageLimits()
        }
    }
}