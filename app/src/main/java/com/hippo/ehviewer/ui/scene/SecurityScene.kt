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

package com.hippo.ehviewer.ui.scene

import android.content.Context
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.auth.AuthPromptCallback
import androidx.biometric.auth.startClass2BiometricOrCredentialAuthentication
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.hippo.ehviewer.R

class SecurityScene : SolidScene() {
    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isAuthenticationSupported(requireContext()))
            startAuthentication(getString(R.string.settings_privacy_require_unlock))
        else
            onSuccess()
    }

    private fun startAuthentication(
        title: String,
        subtitle: String? = null,
        confirmationRequired: Boolean = true,
    ) {
        startClass2BiometricOrCredentialAuthentication(
            title = title,
            subtitle = subtitle,
            confirmationRequired = confirmationRequired,
            executor = ContextCompat.getMainExecutor(requireContext()),
            callback = AuthenticationCallback()
        )
    }

    inner class AuthenticationCallback : AuthPromptCallback() {
        override fun onAuthenticationSucceeded(
            activity: FragmentActivity?,
            result: BiometricPrompt.AuthenticationResult
        ) {
            onSuccess()
        }

        override fun onAuthenticationError(
            activity: FragmentActivity?,
            errorCode: Int,
            errString: CharSequence
        ) {
            requireActivity().finish()
        }
    }

    companion object {
        fun isAuthenticationSupported(context: Context): Boolean {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
            return BiometricManager.from(context)
                .canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        }
    }

    private fun onSuccess() {
        if (stackIndex == 0)
            startSceneForCheckStep(CHECK_STEP_SECURITY, arguments)
        finish()
    }
}