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
import androidx.preference.SwitchPreferenceCompat
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.ui.scene.SecurityScene.Companion.isAuthenticationSupported

/**
 * Created by Mo10 on 2018/2/10.
 */
class PrivacyFragment : BasePreferenceFragment() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.privacy_settings)
    }

    override fun onStart() {
        super.onStart()
        if (!isAuthenticationSupported(requireContext())) {
            Settings.putSecurity(false)
            findPreference<SwitchPreferenceCompat>("require_unlock")?.isEnabled = false
            findPreference<SwitchPreferenceCompat>("require_unlock")?.isChecked = false
        }
    }

    override fun getFragmentTitle(): Int {
        return R.string.settings_privacy
    }
}