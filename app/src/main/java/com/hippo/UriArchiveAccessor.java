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

package com.hippo;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class UriArchiveAccessor {
    ParcelFileDescriptor pfd;
    Context ctx;
    Uri uri;

    public UriArchiveAccessor(Context ctx, Uri uri) {
        this.ctx = ctx;
        this.uri = uri;
    }

    public int open() throws Exception {
        pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
        return openArchive(pfd.getFd(), pfd.getStatSize());
    }

    private native int openArchive(int fd, long size);

    public native long extracttoOutputStream(int index);

    public native boolean needPassword();

    public native boolean providePassword(String str);

    private native void closeArchive();

    public void close() throws Exception {
        closeArchive();
        pfd.close();
    }
}
