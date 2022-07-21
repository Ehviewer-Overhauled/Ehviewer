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
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class UriArchiveAccessor {
    ParcelFileDescriptor pfd;
    FileDescriptor fd;
    long archiveAddr;
    Context ctx;
    Uri uri;

    public UriArchiveAccessor(Context ctx, Uri uri) {
        this.ctx = ctx;
        this.uri = uri;
    }

    public int open() throws Exception {
        pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
        fd = pfd.getFileDescriptor();
        archiveAddr = Os.mmap(0, pfd.getStatSize(), OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, fd, 0);
        return openArchive(archiveAddr, pfd.getStatSize());
    }

    public void extractTargetIndexToOutputStream(int index, OutputStream os) {
        extracttoOutputStream(index, os);
    }

    private native int openArchive(long addr, long size);

    private native void extracttoOutputStream(int index, OutputStream os);

    private native void closeArchive();

    public void close() throws Exception {
        closeArchive();
        Os.munmap(archiveAddr, pfd.getStatSize());
        pfd.close();
        Os.close(fd);
    }
}
