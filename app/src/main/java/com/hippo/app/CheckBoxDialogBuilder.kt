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
import android.widget.CheckBox
import com.hippo.ehviewer.R

class CheckBoxDialogBuilder constructor(
    context: Context,
    message: String?,
    checkText: String?,
    checked: Boolean,
) : BaseDialogBuilder(
    context,
) {
    private val mCheckBox: CheckBox
    val isChecked: Boolean
        get() = mCheckBox.isChecked

    init {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_checkbox_builder, null)
        setView(view)
        setMessage(message)
        mCheckBox = view.findViewById(R.id.checkbox)
        mCheckBox.text = checkText
        mCheckBox.isChecked = checked
        view.setOnClickListener { mCheckBox.toggle() }
    }
}
