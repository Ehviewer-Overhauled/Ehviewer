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
package com.hippo.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.hippo.ehviewer.R
import com.hippo.yorozuya.ViewUtils

class ListCheckBoxDialogBuilder(
    context: Context,
    items: List<CharSequence>,
    listener: (ListCheckBoxDialogBuilder?, AlertDialog?, Int) -> Unit,
    checkText: String?,
    checked: Boolean,
) : BaseDialogBuilder(
    context,
) {
    private val mCheckBox: CheckBox
    private var mDialog: AlertDialog? = null
    val isChecked: Boolean
        get() = mCheckBox.isChecked

    override fun create(): AlertDialog {
        mDialog = super.create()
        return mDialog as AlertDialog
    }

    init {
        val view =
            LayoutInflater.from(getContext()).inflate(R.layout.dialog_list_checkbox_builder, null)
        setView(view)
        val listView = ViewUtils.`$$`(view, R.id.list_view) as ListView
        mCheckBox = ViewUtils.`$$`(view, R.id.checkbox) as CheckBox
        listView.adapter = ArrayAdapter(getContext(), R.layout.item_select_dialog, items)
        listView.onItemClickListener =
            AdapterView.OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                listener(this@ListCheckBoxDialogBuilder, mDialog, position)
                mDialog?.dismiss()
            }
        mCheckBox.text = checkText
        mCheckBox.isChecked = checked
    }
}
