package com.hippo.ehviewer.preference

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.parser.HomeParser
import com.hippo.ehviewer.ui.SettingsActivity
import com.hippo.ehviewer.ui.scene.BaseScene
import com.hippo.preference.DialogPreference
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext

class ImageLimitsPreference(context: Context, attrs: AttributeSet) :
    DialogPreference(context, attrs), View.OnClickListener {
    private val mActivity = context as SettingsActivity
    private val placeholder = context.getString(R.string.please_wait)
    private val coroutineScope = mActivity.lifecycleScope
    private lateinit var resetButton: Button
    private lateinit var mDialog: AlertDialog
    private lateinit var mLimits: HomeParser.Limits
    private lateinit var mFunds: HomeParser.Funds

    init {
        coroutineScope.launchIO {
            getImageLimits {
                summary = "${mLimits.current} / ${mLimits.maximum}"
            }
        }
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
        if (this::mLimits.isInitialized) {
            bind()
        } else {
            coroutineScope.launchIO {
                getImageLimits(true) {
                    bind()
                }
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (this::mLimits.isInitialized) {
            summary = "${mLimits.current} / ${mLimits.maximum}"
        }
    }

    private suspend fun getImageLimits(showError: Boolean = false, onSuccess: () -> Unit) {
        runCatching {
            EhEngine.getImageLimits()
        }.onFailure {
            it.printStackTrace()
            if (showError) {
                withUIContext {
                    mDialog.setMessage(it.message)
                }
            }
        }.onSuccess {
            mLimits = it.limits
            mFunds = it.funds
            withUIContext {
                onSuccess()
            }
        }
    }

    private fun bind() {
        val (current, maximum, resetCost) = mLimits
        val (fundsGP, fundsC) = mFunds
        val cost = if (fundsGP >= resetCost) "$resetCost GP" else "$resetCost Credits"
        val message = mActivity.getString(R.string.current_limits, "$current / $maximum", cost) +
                "\n" + mActivity.getString(R.string.current_funds, "$fundsGP+", fundsC)
        mDialog.setMessage(message)
        resetButton.isEnabled = resetCost in 1..maxOf(fundsGP, fundsC)
    }

    override fun onClick(v: View) {
        resetButton.isEnabled = false
        mDialog.setMessage(placeholder)
        coroutineScope.launchIO {
            runCatching {
                EhEngine.resetImageLimits()
            }.onFailure {
                it.printStackTrace()
                withUIContext {
                    mDialog.setMessage(it.message)
                }
            }.onSuccess {
                withUIContext {
                    mLimits = it ?: HomeParser.Limits(maximum = mLimits.maximum).also {
                        mActivity.showTip(R.string.reset_limits_succeed, BaseScene.LENGTH_SHORT)
                    }
                    bind()
                }
            }
        }
    }
}