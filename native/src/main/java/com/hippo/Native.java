package com.hippo;

public class Native {

    public static void initialize() {
        System.loadLibrary("ehviewer");
    }

    public static native String getlibarchiveVersion();

    public static native String getliblzmaVersion();

    public static native String getlibzstdVersion();

    public static native String getlibjpeg_turboVersion();

    public static native String getzlibVersion();
}
