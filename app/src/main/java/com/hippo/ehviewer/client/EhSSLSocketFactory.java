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
