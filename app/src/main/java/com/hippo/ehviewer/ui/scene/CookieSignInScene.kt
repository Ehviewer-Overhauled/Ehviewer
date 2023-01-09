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
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hippo.app.BaseDialogBuilder
import com.hippo.ehviewer.EhApplication.Companion.ehCookieStore
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.EhClient
import com.hippo.ehviewer.client.EhCookieStore
import com.hippo.ehviewer.client.EhRequest
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.databinding.SceneCookieSignInBinding
import com.hippo.util.ExceptionUtils
import com.hippo.util.getClipboardManager
import com.hippo.util.getTextFromClipboard
import okhttp3.Cookie
import java.util.Locale

class CookieSignInScene : BaseScene() {
    private var _binding: SceneCookieSignInBinding? = null
    private val binding
        get() = _binding!!

    override fun needShowLeftDrawer(): Boolean {
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showSoftInput(binding.ipbMemberId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = SceneCookieSignInBinding.inflate(inflater, container, false)
        binding.fromClipboard.run {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
        }
        binding.ipbPassHash.setOnEditorActionListener { _, _, _ ->
            enter()
            true
        }
        binding.ok.setOnClickListener {
            enter()
        }
        binding.fromClipboard.setOnClickListener {
            fillCookiesFromClipboard()
        }

        // Try to get old version cookie info
        val sharedPreferences = requireContext().getSharedPreferences("eh_info", 0)
        val ipbMemberId = sharedPreferences.getString("ipb_member_id", null)
        val ipbPassHash = sharedPreferences.getString("ipb_pass_hash", null)
        val igneous = sharedPreferences.getString("igneous", null)
        var getIt = false
        if (!TextUtils.isEmpty(ipbMemberId)) {
            binding.ipbMemberId.setText(ipbMemberId)
            getIt = true
        }
        if (!TextUtils.isEmpty(ipbPassHash)) {
            binding.ipbPassHash.setText(ipbPassHash)
            getIt = true
        }
        if (!TextUtils.isEmpty(igneous)) {
            binding.igneous.setText(igneous)
            getIt = true
        }
        if (getIt) {
            showTip(R.string.found_cookies, LENGTH_SHORT)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun enter() {
        val ipbMemberId = binding.ipbMemberId.text.toString().trim { it <= ' ' }
        val ipbPassHash = binding.ipbPassHash.text.toString().trim { it <= ' ' }
        val igneous = binding.igneous.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(ipbMemberId)) {
            binding.ipbMemberIdLayout.error = getString(R.string.text_is_empty)
            return
        } else {
            binding.ipbMemberIdLayout.error = null
        }
        if (TextUtils.isEmpty(ipbPassHash)) {
            binding.ipbPassHashLayout.error = getString(R.string.text_is_empty)
            return
        } else {
            binding.ipbPassHashLayout.error = null
        }
        hideSoftInput()
        storeCookie(ipbMemberId, ipbPassHash, igneous)
        val request = EhRequest()
            .setMethod(EhClient.METHOD_GET_PROFILE)
            .setCallback(CookieSignInListener())
        request.enqueue(this)
    }

    private fun storeCookie(id: String, hash: String, igneous: String) {
        EhUtils.signOut()
        val store = ehCookieStore
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_MEMBER_ID, id, EhUrl.DOMAIN_E))
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_MEMBER_ID, id, EhUrl.DOMAIN_EX))
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_PASS_HASH, hash, EhUrl.DOMAIN_E))
        store.addCookie(newCookie(EhCookieStore.KEY_IPD_PASS_HASH, hash, EhUrl.DOMAIN_EX))
        if (igneous.isNotEmpty()) {
            store.addCookie(newCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_E))
            store.addCookie(newCookie(EhCookieStore.KEY_IGNEOUS, igneous, EhUrl.DOMAIN_EX))
        }
    }

    private fun fillCookiesFromClipboard() {
        val context = requireContext()
        hideSoftInput()
        val text = context.getClipboardManager().getTextFromClipboard(context)
        if (text == null) {
            showTip(R.string.from_clipboard_error, LENGTH_SHORT)
            return
        }
        try {
            val kvs: Array<String> = if (text.contains(";")) {
                text.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else if (text.contains("\n")) {
                text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            } else {
                showTip(R.string.from_clipboard_error, LENGTH_SHORT)
                return
            }
            if (kvs.size < 3) {
                showTip(R.string.from_clipboard_error, LENGTH_SHORT)
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
                    "ipb_member_id" -> binding.ipbMemberId.setText(kv[1].trim { it <= ' ' })
                    "ipb_pass_hash" -> binding.ipbPassHash.setText(kv[1].trim { it <= ' ' })
                    "igneous" -> binding.igneous.setText(kv[1].trim { it <= ' ' })
                }
            }
            enter()
        } catch (e: Exception) {
            ExceptionUtils.throwIfFatal(e)
            e.printStackTrace()
            showTip(R.string.from_clipboard_error, LENGTH_SHORT)
        }
    }

    private inner class CookieSignInListener : EhClient.Callback<Any?> {
        override fun onSuccess(result: Any?) {
            // TODO: getProfile
            navigateToTop()
        }

        override fun onFailure(e: Exception) {
            ehCookieStore.signOut()
            BaseDialogBuilder(requireContext()).setTitle(R.string.sign_in_failed)
                .setMessage(
                    """
                        ${ExceptionUtils.getReadableString(e)}
                        ${getString(R.string.wrong_cookie_warning)}
                    """.trimIndent()
                )
                .setPositiveButton(R.string.get_it, null).show()
        }

        override fun onCancel() {
            ehCookieStore.signOut()
        }
    }

    companion object {
        private fun newCookie(name: String, value: String, domain: String): Cookie {
            return Cookie.Builder().name(name).value(value)
                .domain(domain).expiresAt(Long.MAX_VALUE).build()
        }
    }
}