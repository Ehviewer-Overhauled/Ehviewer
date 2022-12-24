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

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.hippo.ehviewer.R
import com.hippo.ehviewer.ui.SettingsActivity

open class BasePreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    override fun onStart() {
        super.onStart()
        setTitle(fragmentTitle)
    }

    @get:StringRes
    open val fragmentTitle: Int
        get() = -1

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        return false
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        return false
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {}

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val fragment = when (preference.key) {
            "eh" -> EhFragment()
            "download" -> DownloadFragment()
            "privacy" -> PrivacyFragment()
            "advance" -> AdvancedFragment()
            "about" -> AboutFragment()
            "filter" -> FilterFragment()
            "mytags" -> MyTagsFragment()
            "uconfig" -> UConfigFragment()
            "hosts" -> HostsFragment()
            else -> null
        }
        fragment?.let {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment, it)
                .addToBackStack(null)
                .commitAllowingStateLoss()
        }
        return true
    }

    private fun setTitle(@StringRes string: Int) {
        requireActivity().setTitle(string)
    }

    fun showTip(@StringRes id: Int, length: Int) {
        (requireActivity() as SettingsActivity).showTip(getString(id), length)
    }

    fun showTip(message: CharSequence?, length: Int) {
        (requireActivity() as SettingsActivity).showTip(message, length)
    }
}