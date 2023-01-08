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

import android.content.Context
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhApplication.Companion.ehCookieStore
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.UrlOpener
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.parser.ProfileParser
import com.hippo.ehviewer.databinding.SceneLoginBinding
import com.hippo.util.ExceptionUtils
import com.hippo.yorozuya.IntIdGenerator

class SignInScene : BaseScene() {
    private var _binding: SceneLoginBinding? = null
    private val binding
        get() = _binding!!
    private var mSigningIn = false
    private var mRequestId = IntIdGenerator.INVALID_ID
    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            onInit()
        } else {
            onRestore(savedInstanceState)
        }
    }

    private fun onInit() {}
    private fun onRestore(savedInstanceState: Bundle) {
        mRequestId = savedInstanceState.getInt(KEY_REQUEST_ID)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_REQUEST_ID, mRequestId)
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
            redirectTo()
        }
        val application = EhApplication.application
        if (application.containGlobalStuff(mRequestId)) {
            mSigningIn = true
            // request exist
            showProgress(false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show IME
        if (View.INVISIBLE != binding.progress.visibility) {
            showSoftInput(binding.username)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showProgress(animation: Boolean) {
        if (View.VISIBLE != binding.progress.visibility) {
            binding.progress.run {
                if (animation) {
                    alpha = 0.0f
                    visibility = View.VISIBLE
                    animate().alpha(1.0f).setDuration(500).start()
                } else {
                    alpha = 1.0f
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private fun hideProgress() {
        binding.progress.visibility = View.GONE
    }

    private fun signIn() {
        if (mSigningIn) {
            return
        }
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
        showProgress(true)

        // Clean up for sign in
        EhUtils.signOut()
        val callback: EhCallback<*, *> = SignInListener(context)
        mRequestId =
            (requireActivity().applicationContext as EhApplication).putGlobalStuff(callback)
        val request = EhRequest()
            .setMethod(EhClient.METHOD_SIGN_IN)
            .setArgs(username, password)
            .setCallback(callback)
        request.enqueue(this)
        mSigningIn = true
    }

    private val profile: Unit
        get() {
            val context = context
            val activity = mainActivity
            if (null == context || null == activity) {
                return
            }
            hideSoftInput()
            showProgress(true)
            val callback: EhCallback<*, *> = GetProfileListener(context)
            mRequestId = (context.applicationContext as EhApplication).putGlobalStuff(callback)
            val request = EhRequest()
                .setMethod(EhClient.METHOD_GET_PROFILE)
                .setCallback(callback)
            request.enqueue()
            EhRequest().setMethod(EhClient.METHOD_GET_UCONFIG).enqueue()
        }

    private fun redirectTo() {
        Settings.putNeedSignIn(false)
        if (Settings.getSelectSite()) {
            navigate(R.id.selectSiteScene, null)
        } else {
            navigateToTop()
        }
    }

    private fun whetherToSkip(e: Exception?) {
        val context = context ?: return
        BaseDialogBuilder(context)
            .setTitle(R.string.sign_in_failed)
            .setMessage(
                """
    ${ExceptionUtils.getReadableString(e!!)}
    
    ${getString(R.string.sign_in_failed_tip)}
    """.trimIndent()
            )
            .setPositiveButton(R.string.get_it, null)
            .show()
    }

    fun onSignInEnd(e: Exception?) {
        context ?: return
        if (ehCookieStore.hasSignedIn()) {
            profile
        } else {
            mSigningIn = false
            hideProgress()
            whetherToSkip(e)
        }
    }

    fun onGetProfileEnd() {
        mSigningIn = false
        redirectTo()
    }

    private inner class SignInListener(context: Context?) :
        EhCallback<SignInScene?, String>(context) {
        override fun onSuccess(result: String) {
            application.removeGlobalStuff(this)
            Settings.putDisplayName(result)
            val scene = this@SignInScene
            scene.onSignInEnd(null)
        }

        override fun onFailure(e: Exception) {
            application.removeGlobalStuff(this)
            e.printStackTrace()
            val scene = this@SignInScene
            scene.onSignInEnd(e)
        }

        override fun onCancel() {
            application.removeGlobalStuff(this)
        }
    }

    private inner class GetProfileListener(context: Context?) :
        EhCallback<SignInScene?, ProfileParser.Result>(context) {
        override fun onSuccess(result: ProfileParser.Result) {
            application.removeGlobalStuff(this)
            Settings.putDisplayName(result.displayName)
            Settings.putAvatar(result.avatar)
            val scene = this@SignInScene
            scene.onGetProfileEnd()
        }

        override fun onFailure(e: Exception) {
            application.removeGlobalStuff(this)
            e.printStackTrace()
            val scene = this@SignInScene
            scene.onGetProfileEnd()
        }

        override fun onCancel() {
            application.removeGlobalStuff(this)
        }
    }

    companion object {
        private const val KEY_REQUEST_ID = "request_id"
    }
}