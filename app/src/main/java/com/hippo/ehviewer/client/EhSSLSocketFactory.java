package com.hippo.ehviewer.client;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

public class EhSSLSocketFactory extends SSLSocketFactory {
    @Override
    public String[] getDefaultCipherSuites() {
        return new String[0];
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return new String[0];
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        InetAddress address = s.getInetAddress();
        Log.d("EhSSLSocketFactory", "Host: " + host + " Address: " + address.getHostAddress());
        if (autoClose) s.close();
        if (host.endsWith("e-hentai.org") ||
                host.endsWith("exhentai.org") ||
                host.endsWith("ehgt.org") ||
                host.endsWith("hath.network")) {
            return getDefault().createSocket(address, port);
        } else {
            return getDefault().createSocket(host, port);
        }
    }

    @Override
    public Socket createSocket(String host, int port) {
        return null;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) {
        return null;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) {
        return null;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) {
        return null;
    }


}
