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
package com.hippo.ehviewer.ui.fragment

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhTagDatabase

class EhFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.eh_settings)
        val account = findPreference<Preference>(Settings.KEY_ACCOUNT)
        val theme = findPreference<Preference>(Settings.KEY_THEME)
        val blackDarkTheme = findPreference<Preference>(Settings.KEY_BLACK_DARK_THEME)
        val gallerySite = findPreference<Preference>(Settings.KEY_GALLERY_SITE)
        val listMode = findPreference<Preference>(Settings.KEY_LIST_MODE)
        val listThumbSize = findPreference<Preference>(Settings.KEY_LIST_THUMB_SIZE)
        val detailSize = findPreference<Preference>(Settings.KEY_DETAIL_SIZE)
        val thumbSize = findPreference<Preference>(Settings.KEY_THUMB_SIZE)
        val showTagTranslations = findPreference<Preference>(Settings.KEY_SHOW_TAG_TRANSLATIONS)
        val tagTranslationsSource = findPreference<Preference>("tag_translations_source")
        Settings.getDisplayName()?.let { account?.summary = it }
        theme!!.onPreferenceChangeListener = this
        gallerySite!!.onPreferenceChangeListener = this
        listMode!!.onPreferenceChangeListener = this
        listThumbSize!!.onPreferenceChangeListener = this
        detailSize!!.onPreferenceChangeListener = this
        thumbSize!!.onPreferenceChangeListener = this
        showTagTranslations!!.onPreferenceChangeListener = this
        blackDarkTheme!!.onPreferenceChangeListener = this
        if (!EhTagDatabase.isPossible(requireActivity())) {
            preferenceScreen.removePreference(showTagTranslations)
            preferenceScreen.removePreference(tagTranslationsSource!!)
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val key = preference.key
        if (Settings.KEY_THEME == key) {
            AppCompatDelegate.setDefaultNightMode((newValue as String).toInt())
            requireActivity().recreate()
            return true
        } else if (Settings.KEY_GALLERY_SITE == key) {
            requireActivity().setResult(Activity.RESULT_OK)
            return true
        } else if (Settings.KEY_LIST_MODE == key) {
            requireActivity().setResult(Activity.RESULT_OK)
            return true
        } else if (Settings.KEY_LIST_THUMB_SIZE == key) {
            Settings.LIST_THUMB_SIZE_INITED = false
            requireActivity().setResult(Activity.RESULT_OK)
            return true
        } else if (Settings.KEY_DETAIL_SIZE == key) {
            requireActivity().setResult(Activity.RESULT_OK)
        } else if (Settings.KEY_THUMB_SIZE == key) {
            requireActivity().setResult(Activity.RESULT_OK)
        } else if (Settings.KEY_SHOW_TAG_TRANSLATIONS == key) {
            if (java.lang.Boolean.TRUE == newValue) {
                EhTagDatabase.update(requireActivity())
            }
        } else if (Settings.KEY_BLACK_DARK_THEME == key) {
            if (requireActivity().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES > 0) {
                requireActivity().recreate()
            }
            return true
        }
        return true
    }

    override fun getFragmentTitle(): Int {
        return R.string.settings_eh
    }
}