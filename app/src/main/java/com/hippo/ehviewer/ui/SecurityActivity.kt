/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.hippo.ehviewer.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings

fun Context.isAuthenticationSupported(): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    return BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

class SecurityActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        if (isAuthenticationSupported()) {
            startAuthentication(getString(R.string.settings_privacy_require_unlock))
        } else {
            onSuccess()
        }
    }

    @Synchronized
    private fun startAuthentication(
        title: String,
        subtitle: String? = null,
        confirmationRequired: Boolean = true,
    ) {
        if (isAuthenticating) return
        isAuthenticating = true
        startClass2BiometricOrCredentialAuthentication(
            title = title,
            subtitle = subtitle,
            confirmationRequired = confirmationRequired,
            executor = ContextCompat.getMainExecutor(this),
            callback = callback(
                { _, _, _ -> onError() },
                { _, _ -> onSuccess() },
                { },
            ),
        )
    }

    inline fun callback(
        crossinline onAuthenticationError: (
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence,
        ) -> Unit,
        crossinline onAuthenticationSucceeded: (
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult,
        ) -> Unit,
        crossinline onAuthenticationFailed: (
            activity: FragmentActivity?,
        ) -> Unit,
    ) = object : AuthPromptCallback() {
        override fun onAuthenticationError(
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence,
        ) = onAuthenticationError(activity, errorCode, errString)

        override fun onAuthenticationSucceeded(
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult,
        ) = onAuthenticationSucceeded(activity, result)

        override fun onAuthenticationFailed(activity: FragmentActivity?) =
            onAuthenticationFailed(activity)
    }

    private fun onSuccess() {
        isAuthenticating = false
        locked = false
        finish()
    }

    private fun onError() {
        moveTaskToBack(true)
        isAuthenticating = false
    }
}

private var isAuthenticating = false
var locked = true
var locked_last_leave_time: Long = 0

val lockObserver = object : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) {
        if (!locked) {
            locked_last_leave_time = System.currentTimeMillis() / 1000
        }
        locked = true
    }
}

fun Context.interceptSecurityOrReturn() {
    val lockedResumeTime = System.currentTimeMillis() / 1000
    val lockedDelayTime = lockedResumeTime - locked_last_leave_time
    if (lockedDelayTime < Settings.securityDelay * 60) {
        locked = false
    } else if (Settings.security && isAuthenticationSupported() && locked) {
        startActivity(Intent(this, SecurityActivity::class.java))
    }
}
