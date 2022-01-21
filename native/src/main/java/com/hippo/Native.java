package com.hippo;

import android.content.Context;

import com.getkeepsafe.relinker.ReLinker;

public class Native {

    public static void initialize(Context context) {
        ReLinker.loadLibrary(context, "ehviewer");
    }
}
