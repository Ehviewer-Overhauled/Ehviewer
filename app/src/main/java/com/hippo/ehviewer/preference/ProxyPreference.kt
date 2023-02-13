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

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhProxySelector
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.network.InetValidator
import com.hippo.preference.DialogPreference
import com.hippo.yorozuya.MathUtils
import com.hippo.yorozuya.ViewUtils

class ProxyPreference @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : DialogPreference(context, attrs), View.OnClickListener {
    private var mType: TextInputLayout? = null
    private var mIpInputLayout: TextInputLayout? = null
    private var mIp: EditText? = null
    private var mPortInputLayout: TextInputLayout? = null
    private var mPort: EditText? = null
    private val mArray: Array<String>

    init {
        mArray = context.resources.getStringArray(R.array.proxy_types)
        dialogLayoutResource = R.layout.preference_dialog_proxy
        updateSummary(Settings.getProxyType(), Settings.getProxyIp(), Settings.getProxyPort())
    }

    private fun getProxyTypeText(type: Int): String {
        return mArray[MathUtils.clamp(type, 0, mArray.size - 1)]
    }

    private fun updateSummary(type: Int, ip: String?, port: Int) {
        var type1 = type
        if ((type1 == EhProxySelector.TYPE_HTTP || type1 == EhProxySelector.TYPE_SOCKS)
            && (TextUtils.isEmpty(ip) || !InetValidator.isValidInetPort(port))
        ) {
            type1 = EhProxySelector.TYPE_SYSTEM
        }
        summary = if (type1 == EhProxySelector.TYPE_HTTP || type1 == EhProxySelector.TYPE_SOCKS) {
            val context = context
            context.getString(
                R.string.settings_advanced_proxy_summary_1,
                getProxyTypeText(type1),
                ip,
                port
            )
        } else {
            val context = context
            context.getString(
                R.string.settings_advanced_proxy_summary_2,
                getProxyTypeText(type1)
            )
        }
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)
        builder.setPositiveButton(android.R.string.ok, null)
    }

    @SuppressLint("SetTextI18n")
    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(this)
        mType = ViewUtils.`$$`(dialog, R.id.type) as TextInputLayout
        val array = context.resources.getStringArray(R.array.proxy_types)
        mIpInputLayout = ViewUtils.`$$`(dialog, R.id.ip_input_layout) as TextInputLayout
        mIp = ViewUtils.`$$`(dialog, R.id.ip) as EditText
        mPortInputLayout = ViewUtils.`$$`(dialog, R.id.port_input_layout) as TextInputLayout
        mPort = ViewUtils.`$$`(dialog, R.id.port) as EditText
        val type = Settings.getProxyType()
        (mType!!.editText as AutoCompleteTextView).setText(
            array[MathUtils.clamp(
                type,
                0,
                array.size
            )], false
        )
        mIp!!.setText(Settings.getProxyIp())
        val portString: String?
        val port = Settings.getProxyPort()
        portString = if (!InetValidator.isValidInetPort(port)) {
            null
        } else {
            port.toString()
        }
        mPort!!.setText(portString)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        mType = null
        mIpInputLayout = null
        mIp = null
        mPortInputLayout = null
        mPort = null
    }

    override fun onClick(v: View) {
        val dialog = dialog
        val context: Context = context
        if (null == dialog || null == mType || null == mIpInputLayout || null == mIp || null == mPortInputLayout || null == mPort) {
            return
        }
        val type = mArray.indexOf(mType!!.editText!!.text.toString())
        val ip = mIp!!.text.toString().trim { it <= ' ' }
        if (ip.isEmpty()) {
            if (type == EhProxySelector.TYPE_HTTP || type == EhProxySelector.TYPE_SOCKS) {
                mIpInputLayout!!.error = context.getString(R.string.text_is_empty)
                return
            }
        }
        mIpInputLayout!!.error = null
        val port: Int
        val portString = mPort!!.text.toString().trim { it <= ' ' }
        if (portString.isEmpty()) {
            if (type == EhProxySelector.TYPE_HTTP || type == EhProxySelector.TYPE_SOCKS) {
                mPortInputLayout!!.error = context.getString(R.string.text_is_empty)
                return
            } else {
                port = -1
            }
        } else {
            port = try {
                portString.toInt()
            } catch (e: NumberFormatException) {
                -1
            }
            if (!InetValidator.isValidInetPort(port)) {
                mPortInputLayout!!.error = context.getString(R.string.proxy_invalid_port)
                return
            }
        }
        mPortInputLayout!!.error = null
        Settings.putProxyType(type)
        Settings.putProxyIp(ip)
        Settings.putProxyPort(port)
        updateSummary(type, ip, port)
        EhApplication.ehProxySelector.updateProxy()
        dialog.dismiss()
    }
}