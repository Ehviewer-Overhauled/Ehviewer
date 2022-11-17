/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.ui.fragment

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hippo.ehviewer.ui.SettingsActivity

abstract class BaseFragment : Fragment() {
    override fun onStart() {
        super.onStart()
        setTitle(getFragmentTitle())
        (requireActivity() as SettingsActivity).resetAppbarLiftStatus()
    }

    override fun onDestroyView() {
        hideFabAndClearTip()
        super.onDestroyView()
    }

    abstract fun getFragmentTitle(): Int

    private fun setTitle(@StringRes string: Int) {
        requireActivity().setTitle(string)
    }

    fun showTip(@StringRes id: Int, length: Int) {
        (requireActivity() as SettingsActivity).showTip(getString(id), length)
    }

    fun showTip(message: CharSequence?, length: Int) {
        (requireActivity() as SettingsActivity).showTip(message, length)
    }

    fun getTipView(@StringRes res: Int? = null): TextView {
        return (requireActivity() as SettingsActivity).getMContentText().apply {
            visibility = View.VISIBLE
            res?.let { text = getString(it) }
        }
    }

    fun getFabViewAndShow(): FloatingActionButton {
        return (requireActivity() as SettingsActivity).getMFab().apply { visibility = View.VISIBLE }
    }

    private fun hideFabAndClearTip() {
        getFabViewAndShow().apply {
            visibility = View.GONE
            setOnClickListener(null)
        }
        getTipView().apply {
            visibility = View.GONE
        }
    }
}