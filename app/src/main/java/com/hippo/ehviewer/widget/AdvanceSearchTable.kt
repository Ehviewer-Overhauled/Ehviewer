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
    private var mSname: CheckBox
    private var mStags: CheckBox
    private var mSdesc: CheckBox
    private var mStorr: CheckBox
    private var mSto: CheckBox
    private var mSdt1: CheckBox
    private var mSdt2: CheckBox
    private var mSh: CheckBox
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
        mSname = row0.getChildAt(0) as CheckBox
        mStags = row0.getChildAt(1) as CheckBox
        val row1 = getChildAt(1) as ViewGroup
        mSdesc = row1.getChildAt(0) as CheckBox
        mStorr = row1.getChildAt(1) as CheckBox
        val row2 = getChildAt(2) as ViewGroup
        mSto = row2.getChildAt(0) as CheckBox
        mSdt1 = row2.getChildAt(1) as CheckBox
        val row3 = getChildAt(3) as ViewGroup
        mSdt2 = row3.getChildAt(0) as CheckBox
        mSh = row3.getChildAt(1) as CheckBox
        val row4 = getChildAt(4) as ViewGroup
        mSr = row4.getChildAt(0) as CheckBox
        mMinRating = row4.getChildAt(1) as TextInputLayout
        val row5 = getChildAt(5) as ViewGroup
        mSp = row5.getChildAt(0) as CheckBox
        mSpf = row5.getChildAt(1) as EditText
        mSpt = row5.getChildAt(3) as EditText
        val row7 = getChildAt(7) as ViewGroup
        mSfl = row7.getChildAt(0) as CheckBox
        mSfu = row7.getChildAt(1) as CheckBox
        mSft = row7.getChildAt(2) as CheckBox

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
            if (mSname.isChecked) advanceSearch = advanceSearch or SNAME
            if (mStags.isChecked) advanceSearch = advanceSearch or STAGS
            if (mSdesc.isChecked) advanceSearch = advanceSearch or SDESC
            if (mStorr.isChecked) advanceSearch = advanceSearch or STORR
            if (mSto.isChecked) advanceSearch = advanceSearch or STO
            if (mSdt1.isChecked) advanceSearch = advanceSearch or SDT1
            if (mSdt2.isChecked) advanceSearch = advanceSearch or SDT2
            if (mSh.isChecked) advanceSearch = advanceSearch or SH
            if (mSfl.isChecked) advanceSearch = advanceSearch or SFL
            if (mSfu.isChecked) advanceSearch = advanceSearch or SFU
            if (mSft.isChecked) advanceSearch = advanceSearch or SFT
            return advanceSearch
        }
        set(advanceSearch) {
            mSname.isChecked =
                NumberUtils.int2boolean(advanceSearch and SNAME)
            mStags.isChecked =
                NumberUtils.int2boolean(advanceSearch and STAGS)
            mSdesc.isChecked =
                NumberUtils.int2boolean(advanceSearch and SDESC)
            mStorr.isChecked = NumberUtils.int2boolean(advanceSearch and STORR)
            mSto.isChecked = NumberUtils.int2boolean(advanceSearch and STO)
            mSdt1.isChecked = NumberUtils.int2boolean(advanceSearch and SDT1)
            mSdt2.isChecked = NumberUtils.int2boolean(advanceSearch and SDT2)
            mSh.isChecked = NumberUtils.int2boolean(advanceSearch and SH)
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
        const val SNAME = 0x1
        const val STAGS = 0x2
        const val SDESC = 0x4
        const val STORR = 0x8
        const val STO = 0x10
        const val SDT1 = 0x20
        const val SDT2 = 0x40
        const val SH = 0x80
        const val SFL = 0x100
        const val SFU = 0x200
        const val SFT = 0x400
    }
}