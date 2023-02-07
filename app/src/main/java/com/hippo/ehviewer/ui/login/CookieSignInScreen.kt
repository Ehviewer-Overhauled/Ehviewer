package com.hippo.ehviewer.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhEngine
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.util.ExceptionUtils
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withNonCancellableContext
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Cookie
import java.util.Locale

@Composable
fun CookieSignInScene() {
    val navController = LocalNavController.current
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isProgressIndicatorVisible by rememberSaveable { mutableStateOf(false) }

    var ipbMemberId by rememberSaveable { mutableStateOf("") }
    var ipbPassHash by rememberSaveable { mutableStateOf("") }
    var igneous by rememberSaveable { mutableStateOf("") }

    var ipbMemberIdErrorState by rememberSaveable { mutableStateOf(false) }
    var ipbPassHashErrorState by rememberSaveable { mutableStateOf(false) }

    var signInJob by remember { mutableStateOf<Job?>(null) }

    var showErrorDialog by rememberSaveable { mutableStateOf(false) }
    var loginErrorException by remember { mutableStateOf<Throwable?>(null) }

    val noCookies = stringResource(R.string.from_clipboard_error)

    fun newCookie(name: String, value: String, domain: String): Cookie {
        return Cookie.Builder().name(name).value(value).domain(domain).expiresAt(Long.MAX_VALUE)
            .build()
    }

    fun storeCookie(id: String, hash: String, igneous: String) {
        EhUtils.signOut()
        val store = EhApplication.ehCookieStore
        store.addCookie(newCookie(EhCookieStore.KEY_IPB_MEMBER_ID, id, EhUrl.DOMAIN_E))
        store.addCookie(newCookie(EhCookieStore.KEY_IPB_MEMBER_ID, id, EhUrl.DOMAIN_EX))
        store.addCookie(newCookie(EhCookieStore.KEY_IPB_PASS_HASH, hash, EhUrl.DOMAIN_E))
        store.addCookie(newCookie(EhCookieStore.KEY_IPB_PASS_HASH, hash, EhUrl.DOMAIN_EX))
        if (igneous.isBlank()) return
        store.addCookie(newCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_E))
        store.addCookie(newCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_EX))
    }

    fun login() {
        if (signInJob?.isActive == true) return
        if (ipbMemberId.isBlank()) {
            ipbMemberIdErrorState = true
            return
        } else {
            ipbMemberIdErrorState = false
        }
        if (ipbPassHash.isBlank()) {
            ipbPassHashErrorState = true
            return
        } else {
            ipbPassHashErrorState = false
        }
        focusManager.clearFocus()
        storeCookie(ipbMemberId, ipbPassHash, igneous)
        isProgressIndicatorVisible = true
        signInJob = coroutineScope.launchIO {
            runCatching {
                EhEngine.getProfile()
            }.onSuccess {
                withNonCancellableContext {
                    postLogin()
                }
                withUIContext {
                    navController.navigate(SELECT_SITE_ROUTE_NAME)
                }
            }.onFailure {
                EhApplication.ehCookieStore.signOut()
                loginErrorException = it
                showErrorDialog = true
                isProgressIndicatorVisible = false
            }
        }
    }

    fun fillCookiesFromClipboard() {
        focusManager.clearFocus()
        val text = clipboardManager.getText()
        if (text == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
            return
        }
        runCatching {
            val kvs: Array<String> = if (text.contains(";")) {
                text.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else if (text.contains("\n")) {
                text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
                return
            }
            if (kvs.size < 3) {
                coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
                return
            }
            for (s in kvs) {
                val kv: Array<String> = if (s.contains("=")) {
                    s.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else if (s.contains(":")) {
                    s.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                } else {
                    continue
                }
                if (kv.size != 2) {
                    continue
                }
                when (kv[0].trim { it <= ' ' }.lowercase(Locale.getDefault())) {
                    "ipb_member_id" -> ipbMemberId = kv[1].trim { it <= ' ' }
                    "ipb_pass_hash" -> ipbPassHash = kv[1].trim { it <= ' ' }
                    "igneous" -> igneous = kv[1].trim { it <= ' ' }
                }
            }
            login()
        }.onFailure {
            it.printStackTrace()
            coroutineScope.launch { snackbarHostState.showSnackbar(noCookies) }
        }
    }

    Box(
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(dimensionResource(id = R.dimen.keyline_margin))
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.v_cookie_brown_x48),
                    contentDescription = null,
                    modifier = Modifier.padding(dimensionResource(id = R.dimen.keyline_margin))
                )
                Text(
                    text = stringResource(id = R.string.cookie_explain),
                    modifier = Modifier
                        .padding(
                            horizontal = 32.dp,
                            vertical = dimensionResource(id = R.dimen.keyline_margin)
                        ),
                    fontSize = 16.sp
                )
                OutlinedTextField(
                    value = ipbMemberId,
                    onValueChange = { ipbMemberId = it.trim { char -> char <= ' ' } },
                    Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                    label = { Text(text = "ipb_member_id") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    supportingText = { if (ipbMemberIdErrorState) Text(stringResource(R.string.text_is_empty)) },
                    trailingIcon = {
                        if (ipbMemberIdErrorState) Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null
                        )
                    },
                    isError = ipbMemberIdErrorState,
                    singleLine = true
                )
                OutlinedTextField(
                    value = ipbPassHash,
                    onValueChange = { ipbPassHash = it.trim { char -> char <= ' ' } },
                    Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                    label = { Text(text = "ipb_pass_hash") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    supportingText = { if (ipbPassHashErrorState) Text(stringResource(R.string.text_is_empty)) },
                    trailingIcon = {
                        if (ipbPassHashErrorState) Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null
                        )
                    },
                    isError = ipbPassHashErrorState,
                    singleLine = true
                )
                OutlinedTextField(
                    value = igneous,
                    onValueChange = { igneous = it.trim { char -> char <= ' ' } },
                    Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                    label = { Text(text = "igneous") },
                    keyboardActions = KeyboardActions(
                        onDone = { login() }
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { login() },
                    Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
                TextButton(onClick = { fillCookiesFromClipboard() }) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(textDecoration = TextDecoration.Underline)
                            ) {
                                append(stringResource(id = R.string.from_clipboard))
                            }
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
        if (isProgressIndicatorVisible)
            CircularProgressIndicator()
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
                            ${stringResource(R.string.wrong_cookie_warning)}
                        """.trimIndent()
                    )
                }
            )
        }
    }
}