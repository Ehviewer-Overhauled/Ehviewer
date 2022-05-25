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