package com.hippo.ehviewer.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.navigation.NavController
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.ui.LoginActivity.Companion.COOKIE_SIGN_IN_ROUTE_NAME
import com.hippo.ehviewer.ui.LoginActivity.Companion.SELECT_SITE_ROUTE_NAME
import com.hippo.ehviewer.ui.LoginActivity.Companion.WEBVIEW_SIGN_IN_ROUTE_NAME
import com.hippo.ehviewer.ui.scene.getProfile
import com.hippo.util.ExceptionUtils
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import rikka.core.util.ContextUtils.requireActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isProgressIndicatorVisible by rememberSaveable { mutableStateOf(false) }
    var showUsernameError by rememberSaveable { mutableStateOf(false) }
    var showPasswordError by rememberSaveable { mutableStateOf(false) }
    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var loginErrorException by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordHidden by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    var signInJob by rememberSaveable { mutableStateOf<Job?>(null) }

    // Basic login request
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
                (EhClient.execute(EhClient.METHOD_SIGN_IN, username, password) as String).let {
                    Settings.putDisplayName(it)
                }
            }.onFailure {
                it.printStackTrace()
                withUIContext {
                    focusManager.clearFocus()
                    isProgressIndicatorVisible = false
                    showErrorDialog = true
                    loginErrorException = it
                }
            }.onSuccess {
                // This composable is to be finished
                GlobalScope.launchIO {
                    getProfile()
                }
                withUIContext {
                    // navigate to selectSite
                }
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(dimensionResource(id = R.dimen.keyline_margin))
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                Modifier.padding(dimensionResource(id = R.dimen.keyline_margin))
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                label = { Text(stringResource(R.string.username)) },
                supportingText = { if (showUsernameError) Text(stringResource(R.string.error_username_cannot_empty)) },
                trailingIcon = {
                    if (showUsernameError) Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = showUsernameError
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                label = { Text(stringResource(R.string.password)) },
                visualTransformation = if (passwordHidden) PasswordVisualTransformation() else VisualTransformation.None,
                supportingText = { if (showPasswordError) Text(stringResource(R.string.error_password_cannot_empty)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                keyboardActions = KeyboardActions(
                    onDone = { signIn() }
                ),
                trailingIcon = {
                    if (showPasswordError)
                        Icon(imageVector = Icons.Filled.Info, contentDescription = null)
                    else
                        IconButton(onClick = { passwordHidden = !passwordHidden }) {
                            val visibilityIcon =
                                if (passwordHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            Icon(imageVector = visibilityIcon, contentDescription = null)
                        }
                },
                singleLine = true,
                isError = showPasswordError
            )

            Text(
                text = stringResource(id = R.string.app_waring),
                Modifier
                    .widthIn(max = dimensionResource(id = R.dimen.single_max_width))
                    .padding(top = 24.dp),
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(modifier = Modifier.weight(1f))

            Row {
                OutlinedButton(
                    onClick = {
                        UrlOpener.openUrl(
                            requireActivity(context),
                            EhUrl.URL_REGISTER,
                            false
                        )
                    },
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(text = stringResource(id = R.string.register))
                }

                Button(
                    onClick = { signIn() },
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(text = stringResource(id = R.string.sign_in))
                }
            }

            Row(
                Modifier.padding(horizontal = 4.dp)
            ) {
                TextButton(
                    onClick = { navController.navigate(WEBVIEW_SIGN_IN_ROUTE_NAME) },
                    Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(textDecoration = TextDecoration.Underline)
                            ) {
                                append(stringResource(id = R.string.sign_in_via_webview))
                            }
                        }
                    )
                }

                TextButton(
                    onClick = { navController.navigate(COOKIE_SIGN_IN_ROUTE_NAME) },
                    Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(textDecoration = TextDecoration.Underline)
                            ) {
                                append(stringResource(id = R.string.sign_in_via_cookies))
                            }
                        }
                    )
                }
            }

            TextButton(
                onClick = { navController.navigate(SELECT_SITE_ROUTE_NAME) },
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(textDecoration = TextDecoration.Underline)
                        ) {
                            append(stringResource(id = R.string.tourist_mode))
                        }
                    }
                )
            }
        }
        if (isProgressIndicatorVisible) {
            CircularProgressIndicator()
        }
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = {
                    showErrorDialog = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        showErrorDialog = false
                    }) {
                        Text(text = stringResource(id = R.string.get_it))
                    }
                },
                title = {
                    Text(text = stringResource(id = R.string.sign_in_failed))
                },
                text = {
                    Text(
                        text =
                        """
                            ${ExceptionUtils.getReadableString(loginErrorException!!)}
                            ${stringResource(R.string.sign_in_failed_tip)}
                        """.trimIndent()
                    )
                }
            )
        }
    }
}
