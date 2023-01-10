package com.hippo.ehviewer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hippo.ehviewer.ui.compose.SignInScreen
import com.hippo.ehviewer.ui.compose.theme.EhViewerTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EhViewerTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = SIGN_IN_ROUTE_NAME) {
                    composable(SIGN_IN_ROUTE_NAME) {
                        SignInScreen()
                    }

                    composable(WEBVIEW_SIGN_IN_ROUTE_NAME) {

                    }

                    composable(COOKIE_SIGN_IN_ROUTE_NAME) {

                    }

                    composable(SELECT_SITE_ROUTE_NAME) {

                    }
                }
            }
        }
    }

    companion object {
        const val SIGN_IN_ROUTE_NAME = "SignIn"
        const val WEBVIEW_SIGN_IN_ROUTE_NAME = "WebViewSignIn"
        const val COOKIE_SIGN_IN_ROUTE_NAME = "CookieSignIn"
        const val SELECT_SITE_ROUTE_NAME = "SelectSite"
    }
}
