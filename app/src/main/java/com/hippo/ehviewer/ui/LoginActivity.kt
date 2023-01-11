package com.hippo.ehviewer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.ActivityNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.ui.compose.CookieSignInScene
import com.hippo.ehviewer.ui.compose.SelectSiteScreen
import com.hippo.ehviewer.ui.compose.SignInScreen

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Mdc3Theme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = SIGN_IN_ROUTE_NAME) {
                    composable(SIGN_IN_ROUTE_NAME) {
                        SignInScreen(navController)
                    }

                    composable(WEBVIEW_SIGN_IN_ROUTE_NAME) {

                    }

                    composable(COOKIE_SIGN_IN_ROUTE_NAME) {
                        CookieSignInScene(navController)
                    }

                    composable(SELECT_SITE_ROUTE_NAME) {
                        SelectSiteScreen() {
                            finish()
                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }

    companion object {
        const val SIGN_IN_ROUTE_NAME = "SignIn"
        const val WEBVIEW_SIGN_IN_ROUTE_NAME = "WebViewSignIn"
        const val COOKIE_SIGN_IN_ROUTE_NAME = "CookieSignIn"
        const val SELECT_SITE_ROUTE_NAME = "SelectSite"
    }
}
