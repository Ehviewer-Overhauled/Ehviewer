package com.hippo.ehviewer.client

import com.hippo.ehviewer.Settings
import okhttp3.Dns
import java.net.InetAddress

private typealias HostsMap = MutableMap<String, List<InetAddress>>
private fun hostsDsl(builder: HostsMap.() -> Unit): HostsMap = mutableMapOf<String, List<InetAddress>>().apply(builder)
private fun interface HostMapBuilder {
    infix fun String.blockedInCN(boolean: Boolean)
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
    override fun lookup(hostname: String): List<InetAddress> = builtInHosts[hostname].takeIf { Settings.builtInHosts } ?: Dns.SYSTEM.lookup(hostname)

    operator fun contains(hostname: String) = builtInHosts.contains(hostname) && Settings.builtInHosts
}

private val builtInHosts = hostsDsl {
    // ip: String!, blockedByCCP: Boolean!
    hosts("ehgt.org", "gt0.ehgt.org", "gt1.ehgt.org", "gt2.ehgt.org", "gt3.ehgt.org") {
        "37.48.89.44" blockedInCN false
        "81.171.10.48" blockedInCN false
        "178.162.139.24" blockedInCN false
        "178.162.140.212" blockedInCN false
        "2001:1af8:4700:a062:8::47de" blockedInCN false
        "2001:1af8:4700:a062:9::47de" blockedInCN true
        "2001:1af8:4700:a0c9:4::47de" blockedInCN false
        "2001:1af8:4700:a0c9:3::47de" blockedInCN true
    }
    hosts("e-hentai.org") {
        "104.20.134.21" blockedInCN false
        "104.20.135.21" blockedInCN false
        "172.67.0.127" blockedInCN false
    }
    hosts("exhentai.org") {
        "178.175.128.252" blockedInCN false
        "178.175.129.252" blockedInCN false
        "178.175.129.254" blockedInCN false
        "178.175.128.254" blockedInCN false
        "178.175.132.20" blockedInCN false
        "178.175.132.22" blockedInCN false
    }
    hosts("s.exhentai.org") {
        "178.175.129.254" blockedInCN false
        "178.175.128.254" blockedInCN false
        "178.175.132.22" blockedInCN false
    }
    hosts("repo.e-hentai.org") {
        "94.100.28.57" blockedInCN true
        "94.100.29.73" blockedInCN true
    }
    hosts("forums.e-hentai.org") {
        "94.100.18.243" blockedInCN false
    }
    hosts("ul.ehgt.org") {
        "94.100.24.82" blockedInCN true
        "94.100.24.72" blockedInCN true
    }
    hosts("raw.githubusercontent.com") {
        "151.101.0.133" blockedInCN false
        "151.101.64.133" blockedInCN false
        "151.101.128.133" blockedInCN false
        "151.101.192.133" blockedInCN false
    }
}
