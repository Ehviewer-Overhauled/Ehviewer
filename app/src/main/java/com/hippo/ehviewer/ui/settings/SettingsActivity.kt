package com.hippo.ehviewer.ui.settings

import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.ehviewer.ui.compose.setMD3Content
import com.hippo.ehviewer.ui.login.LocalNavController

class SettingsActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setMD3Content {
            val navController = rememberNavController()

            CompositionLocalProvider(LocalNavController provides navController) {
                NavHost(
                    navController = navController,
                    startDestination = BASE_SETTINGS_SCREEN,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(200)) },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(200)) },
                ) {
                    composable(BASE_SETTINGS_SCREEN) {
                        BaseScreen()
                    }
                    composable(ABOUT_SETTINGS_SCREEN) {
                        AboutScreen()
                    }
                    composable(LICENSE_SCREEN) {
                        LicenseScreen()
                    }
                    composable(SECURITY_SETTINGS_SCREEN) {
                        SecurityScreen()
                    }
                    composable(ADVANCED_SETTINGS_SCREEN) {
                        AdvancedScreen()
                    }
                    composable(DOWNLOAD_SETTINGS_SCREEN) {
                        DownloadScreen()
                    }
                    composable(EH_SETTINGS_SCREEN) {
                        EhScreen()
                    }
                    composable(UCONFIG_SCREEN) {
                        UConfigScreen()
                    }
                    composable(MYTAGS_SCREEN) {
                        MyTagsScreen()
                    }
                }
            }
        }
    }
}

const val BASE_SETTINGS_SCREEN = "Base"
const val EH_SETTINGS_SCREEN = "Eh"
const val DOWNLOAD_SETTINGS_SCREEN = "Download"
const val SECURITY_SETTINGS_SCREEN = "Security"
const val ADVANCED_SETTINGS_SCREEN = "Advanced"
const val ABOUT_SETTINGS_SCREEN = "About"
const val LICENSE_SCREEN = "License"
const val UCONFIG_SCREEN = "UConfig"
const val MYTAGS_SCREEN = "Mytags"
