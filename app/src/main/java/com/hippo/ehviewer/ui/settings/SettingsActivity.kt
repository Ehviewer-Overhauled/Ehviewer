package com.hippo.ehviewer.ui.settings

import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.ActivityNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.ehviewer.ui.compose.setMD3Content
import com.hippo.ehviewer.ui.login.LocalNavController

class SettingsActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMD3Content {
            val navController = rememberNavController()

            CompositionLocalProvider(LocalNavController provides navController) {
                NavHost(navController = navController, startDestination = BASE_SETTINGS_SCREEN) {
                    composable(BASE_SETTINGS_SCREEN) {
                        BaseScreen()
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}

const val BASE_SETTINGS_SCREEN = "Base"
const val EH_SETTINGS_SCREEN = "Eh"
const val DOWNLOAD_SETTINGS_SCREEN = "Download"
const val SECURITY_SETTINGS_SCREEN = "Security"
const val ADVANCED_SETTINGS_SCREEN = "Advanced"
const val ABOUT_SETTINGS_SCREEN = "About"
