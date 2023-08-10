package com.hippo.ehviewer.ui

import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.ui.login.CookieSignInScene
import com.hippo.ehviewer.ui.login.SelectSiteScreen
import com.hippo.ehviewer.ui.login.SignInScreen
import com.hippo.ehviewer.ui.login.WebViewSignInScreen
import com.hippo.ehviewer.ui.settings.AboutScreen
import com.hippo.ehviewer.ui.settings.AdvancedScreen
import com.hippo.ehviewer.ui.settings.BaseScreen
import com.hippo.ehviewer.ui.settings.DownloadScreen
import com.hippo.ehviewer.ui.settings.EhScreen
import com.hippo.ehviewer.ui.settings.FilterScreen
import com.hippo.ehviewer.ui.settings.LicenseScreen
import com.hippo.ehviewer.ui.settings.MyTagsScreen
import com.hippo.ehviewer.ui.settings.PrivacyScreen
import com.hippo.ehviewer.ui.settings.UConfigScreen

class ConfigureActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setMD3Content {
            val navController = rememberNavController()
            val windowSizeClass = calculateWindowSizeClass(this)
            CompositionLocalProvider(LocalNavController provides navController) {
                NavHost(
                    navController = navController,
                    startDestination = if (Settings.needSignIn) {
                        if (EhCookieStore.hasSignedIn()) SELECT_SITE_ROUTE_NAME else SIGN_IN_ROUTE_NAME
                    } else {
                        BASE_SETTINGS_SCREEN
                    },
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(200)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(200)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(200)) },
                ) {
                    composable(SIGN_IN_ROUTE_NAME) {
                        SignInScreen(windowSizeClass)
                    }
                    composable(WEBVIEW_SIGN_IN_ROUTE_NAME) {
                        WebViewSignInScreen()
                    }
                    composable(COOKIE_SIGN_IN_ROUTE_NAME) {
                        CookieSignInScene(windowSizeClass)
                    }
                    composable(SELECT_SITE_ROUTE_NAME) {
                        SelectSiteScreen()
                    }
                    composable(BASE_SETTINGS_SCREEN) {
                        BaseScreen()
                    }
                    composable(ABOUT_SETTINGS_SCREEN) {
                        AboutScreen()
                    }
                    composable(LICENSE_SCREEN) {
                        LicenseScreen()
                    }
                    composable(PRIVACY_SETTINGS_SCREEN) {
                        PrivacyScreen()
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
                    composable(FILTER_SCREEN) {
                        FilterScreen()
                    }
                    composable(FINISH_ROUTE_NAME) {
                        SideEffect {
                            finish()
                        }
                    }
                }
            }
        }
    }
}

val LocalNavController = compositionLocalOf<NavController> { error("CompositionLocal LocalNavController not present!") }

const val BASE_SETTINGS_SCREEN = "Base"
const val EH_SETTINGS_SCREEN = "Eh"
const val DOWNLOAD_SETTINGS_SCREEN = "Download"
const val PRIVACY_SETTINGS_SCREEN = "Privacy"
const val ADVANCED_SETTINGS_SCREEN = "Advanced"
const val ABOUT_SETTINGS_SCREEN = "About"
const val LICENSE_SCREEN = "License"
const val UCONFIG_SCREEN = "UConfig"
const val MYTAGS_SCREEN = "Mytags"
const val FILTER_SCREEN = "Filter"
const val SIGN_IN_ROUTE_NAME = "SignIn"
const val WEBVIEW_SIGN_IN_ROUTE_NAME = "WebViewSignIn"
const val COOKIE_SIGN_IN_ROUTE_NAME = "CookieSignIn"
const val SELECT_SITE_ROUTE_NAME = "SelectSite"
const val FINISH_ROUTE_NAME = "Finish"
