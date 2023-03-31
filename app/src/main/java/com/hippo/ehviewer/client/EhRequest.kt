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
package com.hippo.ehviewer.client

import android.app.Activity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job

class EhRequest {
    internal var job: Job? = null
    val isActive
        get() = job?.isActive ?: false
    var method = 0
        private set
    var args: Array<out Any?>? = null
        private set
    var callback: EhClient.Callback<Any?>? = null
        private set

    fun setMethod(method: Int): EhRequest {
        this.method = method
        return this
    }

    fun setArgs(vararg args: Any?): EhRequest {
        this.args = args
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun setCallback(callback: EhClient.Callback<*>?): EhRequest {
        this.callback = callback as EhClient.Callback<Any?>?
        return this
    }

    fun enqueue(scope: CoroutineScope) {
        EhClient.enqueue(this, scope)
    }

    @DelicateCoroutinesApi
    fun enqueue() {
        EhClient.enqueue(this, GlobalScope)
    }

    fun enqueue(fragment: Fragment) {
        enqueue(fragment.viewLifecycleOwner.lifecycleScope)
    }

    fun enqueue(activity: Activity) {
        check(activity is FragmentActivity)
        enqueue(activity.lifecycleScope)
    }

    @MainThread
    fun cancel() {
        if (isActive) {
            job?.cancel()
            callback?.onCancel()
        }
    }
}
