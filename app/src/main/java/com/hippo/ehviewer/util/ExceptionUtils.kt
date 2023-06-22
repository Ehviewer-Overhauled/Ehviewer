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
package com.hippo.ehviewer.util

import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.network.StatusCodeException
import splitties.init.appCtx
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ExceptionUtils {
    fun getReadableString(e: Throwable): String {
        e.printStackTrace()
        return when (e) {
            is MalformedURLException -> appCtx.getString(R.string.error_invalid_url)
            is SocketTimeoutException -> appCtx.getString(R.string.error_timeout)
            is UnknownHostException -> appCtx.getString(R.string.error_unknown_host)
            is StatusCodeException -> {
                val sb = StringBuilder()
                sb.append(appCtx.getString(R.string.error_bad_status_code, e.responseCode))
                if (e.isIdentifiedResponseCode) {
                    sb.append(", ").append(e.message)
                }
                sb.toString()
            }

            is ProtocolException -> {
                if (e.message!!.startsWith("Too many follow-up requests:")) {
                    appCtx.getString(R.string.error_redirection)
                } else {
                    appCtx.getString(R.string.error_socket)
                }
            }

            is SocketException, is SSLException -> appCtx.getString(R.string.error_socket)
            is EhException -> e.message!!
            else -> appCtx.getString(R.string.error_unknown)
        }
    }

    fun throwIfFatal(t: Throwable) {
        // values here derived from https://github.com/ReactiveX/RxJava/issues/748#issuecomment-32471495
        when (t) {
            is VirtualMachineError, is ThreadDeath, is LinkageError -> {
                throw t
            }
        }
    }
}
