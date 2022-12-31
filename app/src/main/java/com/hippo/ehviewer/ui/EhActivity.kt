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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources.Theme
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import eu.kanade.tachiyomi.util.system.isNightMode
import eu.kanade.tachiyomi.util.view.setSecureScreen
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory

abstract class EhActivity : AppCompatActivity() {
    @StyleRes
    fun getThemeStyleRes(): Int {
        val isBlackDarkTheme = Settings.getBoolean("black_dark_theme", false)
        return if (isBlackDarkTheme && isNightMode()) R.style.ThemeOverlay_Black else R.style.ThemeOverlay
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
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isNightMode()
        (application as EhApplication).registerActivity(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as EhApplication).unregisterActivity(this)
    }

    override fun onResume() {
        val lockedResumeTime = System.currentTimeMillis() / 1000
        val lockedDelayTime = lockedResumeTime - EhApplication.locked_last_leave_time
        if (lockedDelayTime < Settings.getSecurityDelay() * 60) {
            EhApplication.locked = false
        } else if (Settings.getSecurity() && isAuthenticationSupported() && EhApplication.locked) {
            startActivity(Intent(this, SecurityActivity::class.java))
        }
        super.onResume()
        window.setSecureScreen(Settings.getEnabledSecurity())
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { Settings.putNotificationRequired() }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkAndRequestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        if (Settings.getNotificationRequired()) return
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
