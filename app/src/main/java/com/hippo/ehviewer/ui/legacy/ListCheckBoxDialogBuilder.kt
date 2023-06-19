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
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import com.hippo.ehviewer.R
import com.hippo.ehviewer.databinding.DialogListCheckboxBuilderBinding

class ListCheckBoxDialogBuilder(
    context: Context,
    items: List<CharSequence>,
    listener: (ListCheckBoxDialogBuilder, AlertDialog?, Int) -> Unit,
    checkText: String?,
    checked: Boolean,
) : BaseDialogBuilder(
    context,
) {
    private val binding =
        DialogListCheckboxBuilderBinding.inflate(LayoutInflater.from(getContext()))
    private var mDialog: AlertDialog? = null
    val isChecked get() = binding.checkbox.isChecked

    override fun create(): AlertDialog {
        mDialog = super.create()
        return mDialog as AlertDialog
    }

    init {
        setView(binding.root)
        binding.listView.adapter = ArrayAdapter(getContext(), R.layout.item_select_dialog, items)
        binding.listView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                listener(this@ListCheckBoxDialogBuilder, mDialog, position)
                mDialog?.dismiss()
            }
        binding.checkbox.text = checkText
        binding.checkbox.isChecked = checked
    }
}
