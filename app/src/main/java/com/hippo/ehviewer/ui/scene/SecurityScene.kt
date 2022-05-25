/*
 * Copyright 2022 tarsin norbin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.ui.scene

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
        if (isAuthenticationSupported())
            startAuthentication(getString(R.string.settings_privacy_require_unlock))
        else
            startSceneForCheckStep(CHECK_STEP_SECURITY, arguments)
    }

    private fun isAuthenticationSupported(): Boolean {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return BiometricManager.from(requireContext())
            .canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
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
            super.onAuthenticationSucceeded(activity, result)
            startSceneForCheckStep(CHECK_STEP_SECURITY, arguments)
        }
    }
}