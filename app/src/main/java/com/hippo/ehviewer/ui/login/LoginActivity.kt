package com.hippo.ehviewer.ui.login

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.ActivityNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.themeadapter.material3.Mdc3Theme
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.EhActivity

class LoginActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Mdc3Theme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = SIGN_IN_ROUTE_NAME) {
                    composable(SIGN_IN_ROUTE_NAME) {
                        SignInScreen(navController) {
                            postLogin()
                        }
                    }

                    composable(WEBVIEW_SIGN_IN_ROUTE_NAME) {
                        WebviewSignInScreen(navController) {
                            postLogin()
                        }
                    }

                    composable(COOKIE_SIGN_IN_ROUTE_NAME) {
                        CookieSignInScene(navController) {
                            postLogin()
                        }
                    }

                    composable(SELECT_SITE_ROUTE_NAME) {
                        val selectSite by remember { mutableStateOf(Settings.getSelectSite()) }
                        if (selectSite) {
                            SelectSiteScreen {
                                finish()
                            }
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

    override fun finish() {
        super.finish()
        Settings.putNeedSignIn(false)
        ActivityNavigator.applyPopAnimationsToPendingTransition(this)
    }

    private suspend fun postLogin() {
        runCatching {
            EhEngine.getProfile().run {
                Settings.putDisplayName(displayName)
                Settings.putAvatar(avatar)
            }
        }.onFailure {
            it.printStackTrace()
        }

        runCatching {
            Settings.putGallerySite(EhUrl.SITE_EX)
            EhEngine.getUConfig()
        }.onFailure {
            Settings.putSelectSite(false)
            Settings.putGallerySite(EhUrl.SITE_E)
            runCatching {
                EhEngine.getUConfig()
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}

const val SIGN_IN_ROUTE_NAME = "SignIn"
const val WEBVIEW_SIGN_IN_ROUTE_NAME = "WebViewSignIn"
const val COOKIE_SIGN_IN_ROUTE_NAME = "CookieSignIn"
const val SELECT_SITE_ROUTE_NAME = "SelectSite"
