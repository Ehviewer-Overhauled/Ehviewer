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
package com.hippo.ehviewer.ui.legacy

import android.content.Context
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import com.hippo.ehviewer.R

class EditTextDialogBuilder(
    context: Context,
    text: String?,
    hint: String?,
) : BaseDialogBuilder(
    context,
),
    OnEditorActionListener {
    private val mTextInputLayout: TextInputLayout
    val editText: EditText
    private var mDialog: AlertDialog? = null
    val text: String
        get() = editText.text.toString()

    fun setError(error: CharSequence?) {
        mTextInputLayout.error = error
    }

    override fun create(): AlertDialog {
        mDialog = super.create()
        return mDialog as AlertDialog
    }

    override fun onEditorAction(v: TextView?, p1: Int, event: KeyEvent?): Boolean {
        return if (mDialog != null) {
            val button = mDialog!!.getButton(DialogInterface.BUTTON_POSITIVE)
            button?.performClick()
            true
        } else {
            false
        }
    }

    init {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edittext_builder, null)
        setView(view)
        mTextInputLayout = view as TextInputLayout
        editText = view.findViewById(R.id.edit_text)
        editText.setText(text)
        editText.setSelection(editText.text.length)
        editText.setOnEditorActionListener(this)
        mTextInputLayout.hint = hint
    }
}
