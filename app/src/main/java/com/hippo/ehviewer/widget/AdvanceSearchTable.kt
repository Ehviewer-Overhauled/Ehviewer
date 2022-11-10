/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.textfield.TextInputLayout
import com.hippo.ehviewer.R
import com.hippo.yorozuya.NumberUtils

class AdvanceSearchTable @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private var mSh: CheckBox
    private var mSdt: CheckBox
    private var mSto: CheckBox
    private var mSr: CheckBox
    private var mMinRating: TextInputLayout
    private var mSp: CheckBox
    private var mSpf: EditText
    private var mSpt: EditText
    private var mSfl: CheckBox
    private var mSfu: CheckBox
    private var mSft: CheckBox
    private val mArray: Array<String> = context.resources.getStringArray(R.array.search_min_rating)

    init {
        orientation = VERTICAL
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.widget_advance_search_table, this)
        val row0 = getChildAt(0) as ViewGroup
        mSh = row0.getChildAt(0) as CheckBox
        mSdt = row0.getChildAt(1) as CheckBox
        val row1 = getChildAt(1) as ViewGroup
        mSto = row1.getChildAt(0) as CheckBox
        val row2 = getChildAt(2) as ViewGroup
        mSr = row2.getChildAt(0) as CheckBox
        mMinRating = row2.getChildAt(1) as TextInputLayout
        val row3 = getChildAt(3) as ViewGroup
        mSp = row3.getChildAt(0) as CheckBox
        mSpf = row3.getChildAt(1) as EditText
        mSpt = row3.getChildAt(3) as EditText
        val row5 = getChildAt(5) as ViewGroup
        mSfl = row5.getChildAt(0) as CheckBox
        mSfu = row5.getChildAt(1) as CheckBox
        mSft = row5.getChildAt(2) as CheckBox

        mSpt.setOnEditorActionListener { v: TextView, _: Int, _: KeyEvent? ->
            val nextView = v.focusSearch(
                FOCUS_DOWN
            )
            nextView?.requestFocus(FOCUS_DOWN)
            true
        }

        mMinRating.isEnabled = mSr.isChecked
        mSr.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            mMinRating.isEnabled = isChecked
        }

        mSpt.isEnabled = mSp.isChecked
        mSpf.isEnabled = mSp.isChecked
        mSp.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            mSpt.isEnabled = isChecked
            mSpf.isEnabled = isChecked
        }
    }

    var advanceSearch: Int
        get() {
            var advanceSearch = 0
            if (mSh.isChecked) advanceSearch = advanceSearch or SH
            if (mSdt.isChecked) advanceSearch = advanceSearch or SDT
            if (mSto.isChecked) advanceSearch = advanceSearch or STO
            if (mSfl.isChecked) advanceSearch = advanceSearch or SFL
            if (mSfu.isChecked) advanceSearch = advanceSearch or SFU
            if (mSft.isChecked) advanceSearch = advanceSearch or SFT
            return advanceSearch
        }
        set(advanceSearch) {
            mSh.isChecked = NumberUtils.int2boolean(advanceSearch and SH)
            mSdt.isChecked = NumberUtils.int2boolean(advanceSearch and SDT)
            mSto.isChecked = NumberUtils.int2boolean(advanceSearch and STO)
            mSfl.isChecked = NumberUtils.int2boolean(advanceSearch and SFL)
            mSfu.isChecked = NumberUtils.int2boolean(advanceSearch and SFU)
            mSft.isChecked = NumberUtils.int2boolean(advanceSearch and SFT)
        }
    var minRating: Int
        get() {
            val position = mArray.indexOf(mMinRating.editText!!.text.toString())
            return if (mSr.isChecked && position >= 0) {
                position + 2
            } else {
                -1
            }
        }
        set(minRating) {
            if (minRating in 2..5) {
                mSr.isChecked = true
                (mMinRating.editText!! as AutoCompleteTextView).setText(
                    mArray[minRating - 2],
                    false
                )
            } else {
                mSr.isChecked = false
            }
        }
    var pageFrom: Int
        get() = if (mSp.isChecked) {
            NumberUtils.parseIntSafely(mSpf.text.toString(), -1)
        } else -1
        set(pageFrom) {
            if (pageFrom > 0) {
                mSpf.setText(pageFrom.toString())
                mSp.isChecked = true
            } else {
                mSp.isChecked = false
                mSpf.text = null
            }
        }
    var pageTo: Int
        get() = if (mSp.isChecked) {
            NumberUtils.parseIntSafely(mSpt.text.toString(), -1)
        } else -1
        set(pageTo) {
            if (pageTo > 0) {
                mSpt.setText(pageTo.toString())
                mSp.isChecked = true
            } else {
                mSp.isChecked = false
            }
        }

    companion object {
        const val SH = 0x1
        const val SDT = 0x2
        const val STO = 0x4
        const val SFL = 0x100
        const val SFU = 0x200
        const val SFT = 0x400
    }
}