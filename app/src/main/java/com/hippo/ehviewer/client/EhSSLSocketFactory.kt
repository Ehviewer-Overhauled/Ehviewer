/*
 * Copyright 2022 Tarsin Norbin
 *
 * This file is part of EhViewer
 *
 * EhViewer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * EhViewer is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with EhViewer.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package com.hippo.ehviewer.client

import android.util.Log
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class EhSSLSocketFactory : SSLSocketFactory() {
    private val sslSocketFactory = getDefault() as SSLSocketFactory
    override fun getDefaultCipherSuites(): Array<String> {
        return sslSocketFactory.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return sslSocketFactory.supportedCipherSuites
    }

    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val address = s.inetAddress.hostAddress
        val socket = sslSocketFactory.createSocket(s, address, port, autoClose) as SSLSocket
        val sslSession = socket.session
        Log.d(
            "EhSSLSocketFactory",
            "Host: " + host + " Address: " + address + " Protocol:" + sslSession.protocol + " CipherSuite:" + sslSession.cipherSuite
        )
        return socket
    }

    override fun createSocket(host: String, port: Int): Socket {
        return sslSocketFactory.createSocket(host, port)
    }

    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int
    ): Socket {
        return sslSocketFactory.createSocket(host, port, localHost, localPort)
    }

    override fun createSocket(host: InetAddress, port: Int): Socket {
        return sslSocketFactory.createSocket(host, port)
    }

    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int
    ): Socket {
        return sslSocketFactory.createSocket(address, port, localAddress, localPort)
    }
}
