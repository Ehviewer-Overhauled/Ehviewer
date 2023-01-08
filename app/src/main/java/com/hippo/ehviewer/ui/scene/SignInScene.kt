/*
 * Copyright 2016 Hippo Seven
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

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.parser.ProfileParser
import com.hippo.ehviewer.databinding.SceneLoginBinding
import com.hippo.util.ExceptionUtils
import eu.kanade.tachiyomi.util.lang.launchIO
import eu.kanade.tachiyomi.util.lang.withUIContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job

class SignInScene : BaseScene() {
    private var _binding: SceneLoginBinding? = null
    private val binding
        get() = _binding!!
    private var mSignInJob: Job? = null
    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SceneLoginBinding.inflate(inflater, container, false)
        binding.signInViaWebview.run {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }
        binding.signInViaCookies.run {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }
        binding.touristMode.run {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }
        binding.password.setOnEditorActionListener { v, actionId, _ ->
            if (v === binding.password) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                    signIn()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        binding.register.setOnClickListener {
            UrlOpener.openUrl(requireActivity(), EhUrl.URL_REGISTER, false)
        }
        binding.signIn.setOnClickListener { signIn() }
        binding.signInViaWebview.setOnClickListener { navigate(R.id.webViewSignInScene, null) }
        binding.signInViaCookies.setOnClickListener { navigate(R.id.cookieSignInScene, null) }
        binding.touristMode.setOnClickListener {
            // Set gallery size SITE_E if skip sign in
            Settings.putGallerySite(EhUrl.SITE_E)
            finishSignIn()
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showProgress() {
        if (View.VISIBLE != binding.progress.visibility) {
            binding.progress.run {
                alpha = 0.0f
                visibility = View.VISIBLE
                animate().alpha(1.0f).setDuration(500).start()
            }
        }
    }

    private fun hideProgress() {
        binding.progress.visibility = View.GONE
    }

    private fun signIn() {
        if (mSignInJob?.isActive == true) return
        val username = binding.username.text.toString()
        val password = binding.password.text.toString()
        if (username.isEmpty()) {
            binding.usernameLayout.error = getString(R.string.error_username_cannot_empty)
            return
        } else {
            binding.usernameLayout.error = null
        }
        if (password.isEmpty()) {
            binding.passwordLayout.error = getString(R.string.error_password_cannot_empty)
            return
        } else {
            binding.passwordLayout.error = null
        }
        hideSoftInput()
        showProgress()

        EhUtils.signOut()
        mSignInJob = lifecycleScope.launchIO {
            runCatching {
                (EhClient.execute(EhClient.METHOD_SIGN_IN, username, password) as String).let {
                    Settings.putDisplayName(it)
                }
            }.onFailure {
                it.printStackTrace()
                withUIContext {
                    hideProgress()
                    showResultErrorDialog(it)
                }
            }.onSuccess {
                // This fragment's lifecycle is to be finished
                GlobalScope.launchIO {
                    getProfile()
                }
                withUIContext {
                    finishSignIn()
                }
            }
        }
    }

    private fun finishSignIn() {
        Settings.putNeedSignIn(false)
        if (Settings.getSelectSite()) {
            navigate(R.id.selectSiteScene, null)
        } else {
            navigateToTop()
        }
    }

    private fun showResultErrorDialog(e: Throwable) {
        BaseDialogBuilder(requireContext())
            .setTitle(R.string.sign_in_failed)
            .setMessage(
                """
                    ${ExceptionUtils.getReadableString(e)}
                    ${getString(R.string.sign_in_failed_tip)}
                """.trimIndent()
            )
            .setPositiveButton(R.string.get_it, null)
            .show()
    }
}

suspend fun getProfile() {
    runCatching {
        (EhClient.execute(EhClient.METHOD_GET_PROFILE) as ProfileParser.Result).run {
            Settings.putDisplayName(displayName)
            Settings.putAvatar(avatar)
        }
    }
}