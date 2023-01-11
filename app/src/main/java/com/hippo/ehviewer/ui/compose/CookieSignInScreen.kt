package com.hippo.ehviewer.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hippo.ehviewer.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookieSignInScene(navController: NavController) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) {
            Column(
                Modifier
                    .padding(dimensionResource(id = R.dimen.keyline_margin))
                    .padding(it)
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
                    value = "",
                    onValueChange = { },
                    Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                    label = {
                        Text(text = "")
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                    label = {
                        Text(text = "")
                    },
                    singleLine = true
                )
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    Modifier.width(dimensionResource(id = R.dimen.single_max_width)),
                    label = {
                        Text(text = "")
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("1")
                        }
                    },
                    Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
                TextButton(onClick = { /*TODO*/ }) {
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
        CircularProgressIndicator()
    }
}