package com.hippo.ehviewer.ui.login

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.EhActivity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LoginActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Mdc3Theme {
                val navController = rememberNavController()

                CompositionLocalProvider(LocalNavController provides navController) {
                    NavHost(navController = navController, startDestination = SIGN_IN_ROUTE_NAME) {
                        composable(SIGN_IN_ROUTE_NAME) {
                            SignInScreen()
                        }

                        composable(WEBVIEW_SIGN_IN_ROUTE_NAME) {
                            WebviewSignInScreen()
                        }

                        composable(COOKIE_SIGN_IN_ROUTE_NAME) {
                            CookieSignInScene()
                        }

                        composable(SELECT_SITE_ROUTE_NAME) {
                            val selectSite by remember { mutableStateOf(Settings.getSelectSite()) }
                            if (selectSite) {
                                SelectSiteScreen()
                            } else {
                                SideEffect {
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        Settings.putNeedSignIn(false)
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }
}

suspend fun postLogin() = coroutineScope {
    launch {
        runCatching {
            EhEngine.getProfile().run {
                Settings.putDisplayName(displayName)
                Settings.putAvatar(avatar)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
    launch {
        runCatching {
            // For the `star` cookie
            EhEngine.getNews(false)

            // Sad panda check
            Settings.putGallerySite(EhUrl.SITE_EX)
            EhEngine.getUConfig()
        }.onFailure {
            Settings.putSelectSite(false)
            Settings.putGallerySite(EhUrl.SITE_E)
            launch {
                runCatching {
                    EhEngine.getUConfig()
                }.onFailure {
                    it.printStackTrace()
                }
            }
        }
    }.join()
}

val LocalNavController =
    compositionLocalOf<NavController> { error("CompositionLocal LocalNavController not present!") }

const val SIGN_IN_ROUTE_NAME = "SignIn"
const val WEBVIEW_SIGN_IN_ROUTE_NAME = "WebViewSignIn"
const val COOKIE_SIGN_IN_ROUTE_NAME = "CookieSignIn"
const val SELECT_SITE_ROUTE_NAME = "SelectSite"
