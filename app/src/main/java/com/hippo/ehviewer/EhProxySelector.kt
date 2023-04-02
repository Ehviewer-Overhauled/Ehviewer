/*
 * Copyright 2019 Hippo Seven
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
package com.hippo.ehviewer

import android.text.TextUtils
import com.hippo.ehviewer.Settings.proxyIp
import com.hippo.ehviewer.Settings.proxyPort
import com.hippo.ehviewer.Settings.proxyType
import com.hippo.network.InetValidator.isValidInetPort
import com.hippo.util.ExceptionUtils
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

class EhProxySelector internal constructor() : ProxySelector() {
    private var delegation: ProxySelector? = null
    private var alternative: ProxySelector?

    init {
        alternative = getDefault() ?: NullProxySelector()
        updateProxy()
    }

    fun updateProxy() {
        delegation = when (proxyType) {
            TYPE_DIRECT -> NullProxySelector()
            TYPE_SYSTEM -> alternative
            TYPE_HTTP, TYPE_SOCKS -> null
            else -> alternative
        }
    }

    override fun select(uri: URI): List<Proxy> {
        val type = proxyType
        if (type == TYPE_HTTP || type == TYPE_SOCKS) {
            runCatching {
                val ip = proxyIp
                val port = proxyPort
                if (!TextUtils.isEmpty(ip) && isValidInetPort(port)) {
                    val inetAddress = InetAddress.getByName(ip)
                    val socketAddress = InetSocketAddress(inetAddress, port)
                    return listOf(
                        Proxy(
                            if (type == TYPE_HTTP) Proxy.Type.HTTP else Proxy.Type.SOCKS,
                            socketAddress,
                        ),
                    )
                }
            }.onFailure {
                ExceptionUtils.throwIfFatal(it)
                it.printStackTrace()
            }
        }
        return delegation?.select(uri) ?: alternative!!.select(uri)
    }

    override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {
        delegation?.select(uri)
    }

    private class NullProxySelector : ProxySelector() {
        override fun select(uri: URI): List<Proxy> {
            return listOf(Proxy.NO_PROXY)
        }

        override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {}
    }

    companion object {
        const val TYPE_DIRECT = 0
        const val TYPE_SYSTEM = 1
        const val TYPE_HTTP = 2
        const val TYPE_SOCKS = 3
    }
}
