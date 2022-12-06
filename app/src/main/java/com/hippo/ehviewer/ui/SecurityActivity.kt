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
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R

fun Context.isAuthenticationSupported(): Boolean {
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    return BiometricManager.from(this)
        .canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
}

class SecurityActivity : AppCompatActivity() {
    override fun onResume() {
        super.onResume()
        if (isAuthenticationSupported())
            startAuthentication(getString(R.string.settings_privacy_require_unlock))
        else
            onSuccess()
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
                { _, _, _ -> onFailed() },
                { _, _ -> onSuccess() },
                { onFailed() }
            )
        )
    }

    inline fun callback(
        crossinline onAuthenticationError: (
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence
        ) -> Unit,
        crossinline onAuthenticationSucceeded: (
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult
        ) -> Unit,
        crossinline onAuthenticationFailed: (
            activity: FragmentActivity?
        ) -> Unit
    ) = object : AuthPromptCallback() {
        override fun onAuthenticationError(
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence
        ) = onAuthenticationError(activity, errorCode, errString)

        override fun onAuthenticationSucceeded(
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult
        ) = onAuthenticationSucceeded(activity, result)

        override fun onAuthenticationFailed(activity: FragmentActivity?) =
            onAuthenticationFailed(activity)
    }

    private fun onSuccess() {
        isAuthenticating = false
        EhApplication.locked = false
        finish()
    }

    private fun onFailed() {
        moveTaskToBack(true)
        isAuthenticating = false
    }

    companion object {
        private var isAuthenticating = false
    }
}