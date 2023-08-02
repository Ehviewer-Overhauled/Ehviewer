package com.hippo.ehviewer.ui.login

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.COOKIE_SIGN_IN_ROUTE_NAME
import com.hippo.ehviewer.ui.FINISH_ROUTE_NAME
import com.hippo.ehviewer.ui.LocalNavController
import com.hippo.ehviewer.ui.SELECT_SITE_ROUTE_NAME
import com.hippo.ehviewer.ui.WEBVIEW_SIGN_IN_ROUTE_NAME
import com.hippo.ehviewer.ui.openBrowser
import com.hippo.ehviewer.ui.tools.rememberDialogState
import com.hippo.ehviewer.util.ExceptionUtils
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Job

@Composable
fun SignInScreen(windowSizeClass: WindowSizeClass) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isProgressIndicatorVisible by rememberSaveable { mutableStateOf(false) }
    var showUsernameError by rememberSaveable { mutableStateOf(false) }
    var showPasswordError by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    var signInJob by remember { mutableStateOf<Job?>(null) }

    val dialogState = rememberDialogState()
    dialogState.Intercept()

    BackHandler {
        (context as Activity).moveTaskToBack(true)
    }

    fun signIn() {
        if (signInJob?.isActive == true) return
        if (username.isEmpty()) {
            showUsernameError = true
            return
        } else {
            showUsernameError = false
        }
        if (password.isEmpty()) {
            showPasswordError = true
            return
        } else {
            showPasswordError = false
        }
        focusManager.clearFocus()
        isProgressIndicatorVisible = true

        EhUtils.signOut()
        signInJob = coroutineScope.launchIO {
            runCatching {
                EhEngine.signIn(username, password)
            }.onFailure {
                withUIContext {
                    focusManager.clearFocus()
                    dialogState.awaitPermissionOrCancel(
                        confirmText = R.string.get_it,
                        title = R.string.sign_in_failed,
                        text = {
                            Text(
                                """
                                ${ExceptionUtils.getReadableString(it)}
                                ${stringResource(R.string.sign_in_failed_tip)}
                                """.trimIndent(),
                            )
                        },
                    )
                    isProgressIndicatorVisible = false
                }
            }.onSuccess {
                val canEx = withNonCancellableContext { postLogin() }
                withUIContext { navController.navigate(if (canEx) SELECT_SITE_ROUTE_NAME else FINISH_ROUTE_NAME) }
            }
        }
    }

    @Composable
    fun UsernameAndPasswordTextField() {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
            label = { Text(stringResource(R.string.username)) },
            supportingText = { if (showUsernameError) Text(stringResource(R.string.error_username_cannot_empty)) },
            trailingIcon = { if (showUsernameError) Icon(imageVector = Icons.Filled.Info, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true,
            isError = showUsernameError,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = if (passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
            supportingText = { if (showPasswordError) Text(stringResource(R.string.error_password_cannot_empty)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            keyboardActions = KeyboardActions(onDone = { signIn() }),
            trailingIcon = {
                if (showPasswordError) {
                    Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                } else {
                    IconButton(onClick = { passwordHidden = !passwordHidden }) {
                        val visibilityIcon = if (passwordHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        Icon(imageVector = visibilityIcon, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            isError = showPasswordError,
        )
    }

    Box(contentAlignment = Alignment.Center) {
        when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact, WindowWidthSizeClass.Medium -> {
                Column(
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)).fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)),
                    )
                    UsernameAndPasswordTextField()
                    Text(
                        text = stringResource(id = R.string.app_waring),
                        modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.single_max_width)).padding(top = 24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(id = R.string.app_waring_2),
                        modifier = Modifier.widthIn(max = dimensionResource(id = R.dimen.single_max_width)).padding(top = 12.dp),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.padding(top = dimensionResource(R.dimen.keyline_margin))) {
                        FilledTonalButton(
                            onClick = { context.openBrowser(EhUrl.URL_REGISTER) },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(id = R.string.register))
                        }
                        Button(
                            onClick = ::signIn,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        ) {
                            Text(text = stringResource(id = R.string.sign_in))
                        }
                    }
                    Row(modifier = Modifier.padding(horizontal = 4.dp)) {
                        TextButton(
                            onClick = { navController.navigate(WEBVIEW_SIGN_IN_ROUTE_NAME) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                                    ) {
                                        append(stringResource(id = R.string.sign_in_via_webview))
                                    }
                                },
                            )
                        }
                        TextButton(
                            onClick = { navController.navigate(COOKIE_SIGN_IN_ROUTE_NAME) },
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                                    ) {
                                        append(stringResource(id = R.string.sign_in_via_cookies))
                                    }
                                },
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            Settings.needSignIn = false
                            Settings.gallerySite = EhUrl.SITE_E
                            navController.navigate(FINISH_ROUTE_NAME)
                        },
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(textDecoration = TextDecoration.Underline),
                                ) {
                                    append(stringResource(id = R.string.tourist_mode))
                                }
                            },
                        )
                    }
                }
            }
            WindowWidthSizeClass.Expanded -> {
                Row(
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)).fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.width(dimensionResource(id = R.dimen.signinscreen_landscape_caption_frame_width)).padding(dimensionResource(id = R.dimen.keyline_margin)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            alignment = Alignment.Center,
                            modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin)),
                        )
                        Text(
                            text = stringResource(id = R.string.app_waring),
                            modifier = Modifier.widthIn(max = 360.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(id = R.string.app_waring_2),
                            modifier = Modifier.widthIn(max = 360.dp),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        UsernameAndPasswordTextField()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            Button(
                                onClick = ::signIn,
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(text = stringResource(id = R.string.sign_in))
                            }
                            FilledTonalButton(
                                onClick = { context.openBrowser(EhUrl.URL_REGISTER) },
                                modifier = Modifier.padding(horizontal = 4.dp).width(128.dp),
                            ) {
                                Text(text = stringResource(id = R.string.register))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            TextButton(
                                onClick = { navController.navigate(COOKIE_SIGN_IN_ROUTE_NAME) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(textDecoration = TextDecoration.Underline),
                                        ) {
                                            append(stringResource(id = R.string.sign_in_via_cookies))
                                        }
                                    },
                                )
                            }
                            TextButton(
                                onClick = { navController.navigate(WEBVIEW_SIGN_IN_ROUTE_NAME) },
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            style = SpanStyle(textDecoration = TextDecoration.Underline),
                                        ) {
                                            append(stringResource(id = R.string.sign_in_via_webview))
                                        }
                                    },
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                Settings.needSignIn = false
                                Settings.gallerySite = EhUrl.SITE_E
                                navController.navigate(FINISH_ROUTE_NAME)
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                                    ) {
                                        append(stringResource(id = R.string.tourist_mode))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        if (isProgressIndicatorVisible) {
            CircularProgressIndicator()
        }
    }
}
