/*
 * Copyright 2018 Hippo Seven
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

import com.hippo.ehviewer.Settings
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

private typealias HostsMap = MutableMap<String, List<InetAddress>>
private fun hostsDsl(builder: HostsMap.() -> Unit): HostsMap = mutableMapOf<String, List<InetAddress>>().apply(builder)
private fun interface HostMapBuilder {
    infix fun String.availableInCN(boolean: Boolean)
}

private fun HostsMap.hosts(vararg hosts: String, builder: HostMapBuilder.() -> Unit) = apply {
    hosts.forEach { host ->
        fun String.toInetAddress() = InetAddress.getByName(this).let { InetAddress.getByAddress(host, it.address) }
        mutableListOf<InetAddress>().apply {
            HostMapBuilder { if (!(Settings.dF && it)) add(toInetAddress()) }.apply(builder)
            put(host, this)
        }
    }
}

object EhDns : Dns {
    private val builtInHosts = hostsDsl {
        // ip: String!, blockedByCCP: Boolean!
        hosts("ehgt.org", "gt0.ehgt.org", "gt1.ehgt.org", "gt2.ehgt.org", "gt3.ehgt.org") {
            "37.48.89.44" availableInCN false
            "81.171.10.48" availableInCN false
            "178.162.139.24" availableInCN false
            "178.162.140.212" availableInCN false
            "2001:1af8:4700:a062:8::47de" availableInCN false
            "2001:1af8:4700:a062:9::47de" availableInCN true
            "2001:1af8:4700:a0c9:4::47de" availableInCN false
            "2001:1af8:4700:a0c9:3::47de" availableInCN true
        }
        hosts("e-hentai.org") {
            "104.20.134.21" availableInCN false
            "104.20.135.21" availableInCN false
            "172.67.0.127" availableInCN false
        }
        hosts("exhentai.org") {
            "178.175.128.252" availableInCN false
            "178.175.129.252" availableInCN false
            "178.175.129.254" availableInCN false
            "178.175.128.254" availableInCN false
            "178.175.132.20" availableInCN false
            "178.175.132.22" availableInCN false
        }
        hosts("s.exhentai.org") {
            "178.175.129.254" availableInCN false
            "178.175.128.254" availableInCN false
            "178.175.132.22" availableInCN false
        }
        hosts("repo.e-hentai.org") {
            "94.100.28.57" availableInCN true
            "94.100.29.73" availableInCN true
        }
        hosts("forums.e-hentai.org") {
            "94.100.18.243" availableInCN false
        }
        hosts("ul.ehgt.org") {
            "94.100.24.82" availableInCN true
            "94.100.24.72" availableInCN true
        }
        hosts("raw.githubusercontent.com") {
            "151.101.0.133" availableInCN false
            "151.101.64.133" availableInCN false
            "151.101.128.133" availableInCN false
            "151.101.192.133" availableInCN false
        }
    }

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        return builtInHosts[hostname].takeIf { Settings.builtInHosts } ?: Dns.SYSTEM.lookup(hostname)
    }

    fun isInHosts(hostname: String): Boolean {
        return builtInHosts.contains(hostname) && Settings.builtInHosts
    }
}
