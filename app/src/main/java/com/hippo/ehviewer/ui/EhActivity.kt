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
package com.hippo.ehviewer.ui

import android.content.res.Resources.Theme
import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ActivityNavigator
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.util.system.isNightMode
import eu.kanade.tachiyomi.util.view.setSecureScreen
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory

abstract class EhActivity : AppCompatActivity() {
    @StyleRes
    fun getThemeStyleRes(): Int {
        return if (Settings.blackDarkTheme && isNightMode()) R.style.ThemeOverlay_Black else R.style.ThemeOverlay
    }

    override fun onApplyThemeResource(theme: Theme, resid: Int, first: Boolean) {
        theme.applyStyle(resid, true)
        theme.applyStyle(getThemeStyleRes(), true)
    }

    override fun onNightModeChanged(mode: Int) {
        theme.applyStyle(getThemeStyleRes(), true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        layoutInflater.factory2 = LayoutInflaterFactory(delegate).addOnViewCreatedListener(WindowInsetsHelper.LISTENER)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        interceptSecurityOrReturn()
        super.onResume()
        window.setSecureScreen(Settings.enabledSecurity)
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}
