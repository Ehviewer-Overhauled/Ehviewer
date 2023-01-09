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
package com.hippo.ehviewer.ui.scene

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hippo.ehviewer.EhApplication.Companion.ehCookieStore
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.databinding.SceneSelectSiteBinding

class SelectSiteScene : BaseScene() {
    private var _binding: SceneSelectSiteBinding? = null
    private val binding
        get() = _binding!!

    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SceneSelectSiteBinding.inflate(inflater, container, false)
        if (ehCookieStore.hasSignedIn()) {
            binding.siteEx.isChecked = true
        } else {
            binding.siteE.isChecked = true
        }
        binding.ok.setOnClickListener {
            when (binding.buttonGroup.checkedButtonId) {
                R.id.site_e -> {
                    Settings.putSelectSite(false)
                    Settings.putGallerySite(EhUrl.SITE_E)
                    navigateToTop()
                }

                R.id.site_ex -> {
                    Settings.putSelectSite(false)
                    Settings.putGallerySite(EhUrl.SITE_EX)
                    navigateToTop()
                }

                else -> {
                    showTip(R.string.no_select, LENGTH_SHORT)
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}