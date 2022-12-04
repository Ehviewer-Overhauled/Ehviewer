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

import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.Hosts
import com.hippo.ehviewer.Settings
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

class EhDns : Dns {
    private val hosts = EhApplication.hosts
    private val builtInHosts: MutableMap<String, List<InetAddress>> = mutableMapOf()

    init {
        /* Pair(ip: String!, blockedByCCP: Boolean!) */
        val ehgtHosts = arrayOf(
            Pair("37.48.89.44", false),
            Pair("81.171.10.48", false),
            Pair("178.162.139.24", false),
            Pair("178.162.140.212", false),
            Pair("2001:1af8:4700:a062:8::47de", false),
            Pair("2001:1af8:4700:a062:9::47de", true),
            Pair("2001:1af8:4700:a0c9:4::47de", false),
            Pair("2001:1af8:4700:a0c9:3::47de", true),
        )

        put(
            builtInHosts,
            "e-hentai.org",
            Pair("104.20.134.21", true),
            Pair("104.20.135.21", false),
            Pair("172.67.0.127", false),
        )
        put(
            builtInHosts,
            "exhentai.org",
            Pair("178.175.128.252", false),
            Pair("178.175.129.252", false),
            Pair("178.175.129.254", false),
            Pair("178.175.128.254", false),
            Pair("178.175.132.20", false),
            Pair("178.175.132.22", false),
        )
        put(
            builtInHosts,
            "repo.e-hentai.org",
            Pair("94.100.28.57", true),
            Pair("94.100.29.73", true),
        )
        put(
            builtInHosts,
            "forums.e-hentai.org",
            Pair("94.100.18.243", false),
        )
        put(
            builtInHosts,
            "ehgt.org",
            *ehgtHosts
        )
        put(
            builtInHosts,
            "gt0.ehgt.org",
            *ehgtHosts
        )
        put(
            builtInHosts,
            "gt1.ehgt.org",
            *ehgtHosts
        )
        put(
            builtInHosts,
            "gt2.ehgt.org",
            *ehgtHosts
        )
        put(
            builtInHosts,
            "gt3.ehgt.org",
            *ehgtHosts
        )
        put(
            builtInHosts,
            "ul.ehgt.org",
            Pair("94.100.24.82", true),
            Pair("94.100.24.72", true),
        )
        put(
            builtInHosts,
            "raw.githubusercontent.com",
            Pair("151.101.0.133", false),
            Pair("151.101.64.133", false),
            Pair("151.101.128.133", false),
            Pair("151.101.192.133", false),
        )
    }

    private fun put(
        map: MutableMap<String, List<InetAddress>>,
        host: String,
        vararg ips: Pair<String, Boolean>
    ) {
        map[host] = ips.mapNotNull { pair ->
            Hosts.toInetAddress(host, pair.first).takeUnless { Settings.getDF() && pair.second }
        }
    }

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        return hosts[hostname] ?: builtInHosts[hostname].takeIf { Settings.getBuiltInHosts() }
        ?: Dns.SYSTEM.lookup(hostname)
    }
}