package com.hippo.ehviewer.ui.login

import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.ui.EhActivity
import com.hippo.ehviewer.ui.compose.setMD3Content
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class LoginActivity : EhActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMD3Content {
            val windowSizeClass = calculateWindowSizeClass(this)
            val navController = rememberNavController()

            CompositionLocalProvider(LocalNavController provides navController) {
                NavHost(
                    navController = navController,
                    startDestination = SIGN_IN_ROUTE_NAME,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(200)) },
                    exitTransition = { fadeOut(tween(0)) },
                    popEnterTransition = { fadeIn(tween(0)) },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(200)) },
                ) {
                    composable(SIGN_IN_ROUTE_NAME) {
                        SignInScreen(windowSizeClass)
                    }

                    composable(WEBVIEW_SIGN_IN_ROUTE_NAME) {
                        WebviewSignInScreen()
                    }

                    composable(COOKIE_SIGN_IN_ROUTE_NAME) {
                        CookieSignInScene(windowSizeClass)
                    }

                    composable(SELECT_SITE_ROUTE_NAME) {
                        val selectSite by remember { mutableStateOf(Settings.selectSite) }
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

    override fun finish() {
        super.finish()
        Settings.needSignIn = false
    }
}

suspend fun postLogin() = coroutineScope {
    launch {
        runCatching {
            EhEngine.getProfile().run {
                Settings.displayName = displayName
                Settings.avatar = avatar
            }
        }.onFailure {
            it.printStackTrace()
        }
    }
    launch {
        runCatching {
            // For the `star` cookie
            EhEngine.getNews(false)
            EhCookieStore.copyCookie(EhUrl.DOMAIN_E, EhUrl.DOMAIN_EX, EhCookieStore.KEY_STAR)

            // Sad panda check
            Settings.gallerySite = EhUrl.SITE_EX
            EhEngine.getUConfig()
        }.onFailure {
            Settings.selectSite = false
            Settings.gallerySite = EhUrl.SITE_E
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

val LocalNavController = compositionLocalOf<NavController> { error("CompositionLocal LocalNavController not present!") }

const val SIGN_IN_ROUTE_NAME = "SignIn"
const val WEBVIEW_SIGN_IN_ROUTE_NAME = "WebViewSignIn"
const val COOKIE_SIGN_IN_ROUTE_NAME = "CookieSignIn"
const val SELECT_SITE_ROUTE_NAME = "SelectSite"
