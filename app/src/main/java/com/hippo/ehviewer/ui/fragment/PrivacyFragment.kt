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
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.isAuthenticationSupported

/**
 * Created by Mo10 on 2018/2/10.
 */
class PrivacyFragment : BasePreferenceFragment() {
    private lateinit var requireUnlock: SwitchPreferenceCompat
    private lateinit var unlockDelay: SeekBarPreference
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.privacy_settings)
        requireUnlock = findPreference("require_unlock")!!
        unlockDelay = findPreference("require_unlock_delay")!!
        requireUnlock.setOnPreferenceChangeListener { _, newValue ->
            unlockDelay.isEnabled = newValue as Boolean
            setUnlockDelaySummary(unlockDelay.value)
            true
        }
        unlockDelay.setOnPreferenceChangeListener { _, newValue ->
            setUnlockDelaySummary(newValue as Int)
            true
        }
    }

    override fun onStart() {
        super.onStart()
        if (!requireContext().isAuthenticationSupported()) {
            Settings.putSecurity(false)
            requireUnlock.isEnabled = false
            requireUnlock.isChecked = false
            unlockDelay.isEnabled = false
        } else {
            unlockDelay.isEnabled = requireUnlock.isChecked == true
            setUnlockDelaySummary(unlockDelay.value)
        }
    }

    @get:StringRes
    override val fragmentTitle: Int
        get() = R.string.settings_privacy

    private fun setUnlockDelaySummary(value: Int) {
        unlockDelay.summary = if (value == 0) {
            getString(R.string.settings_privacy_require_unlock_delay_summary_immediately)
        } else {
            getString(R.string.settings_privacy_require_unlock_delay_summary, value.toString())
        }
    }
}