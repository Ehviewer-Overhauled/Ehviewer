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

package com.hippo.ehviewer.client;

import android.util.Log;

import com.hippo.ehviewer.Settings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class EhSSLSocketFactory extends SSLSocketFactory {
    @Override
    public String[] getDefaultCipherSuites() {
        return ((SSLSocketFactory) getDefault()).getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return ((SSLSocketFactory) getDefault()).getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocketFactory defaultFactory = (SSLSocketFactory) getDefault();
        if (!Settings.getDF()) {
            return defaultFactory.createSocket(s, host, port, autoClose);
        }
        String address = s.getInetAddress().getHostAddress();
        SSLSocket socket = (SSLSocket) defaultFactory.createSocket(s, address, port, autoClose);
        SSLSession sslSession = socket.getSession();
        Log.d("EhSSLSocketFactory", "Host: " + host + " Address: " + address + " Protocol:" + sslSession.getProtocol() + " CipherSuite:" + sslSession.getCipherSuite());
        socket.setEnabledProtocols(socket.getSupportedProtocols());
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return getDefault().createSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return getDefault().createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return getDefault().createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return getDefault().createSocket(address, port, localAddress, localPort);
    }


}
