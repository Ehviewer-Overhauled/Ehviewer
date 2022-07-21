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

public class UriArchiveAccessor implements AutoCloseable {
    public static int OPEN_OK = 1;
    public static int OPEN_ERR = 2;
    private final OsReadableFile osf;

    public UriArchiveAccessor(Context ctx, Uri uri) throws FileNotFoundException {
        osf = new OsReadableFile(ctx, uri);
    }

    public int open() {
        return openArchive(osf);
    }

    public void extractTargetIndexToOutputStream(int index, OutputStream os) {
        extracttoOutputStream(index, os);
    }

    private native int openArchive(OsReadableFile osf);

    private native void extracttoOutputStream(int index, OutputStream os);

    private native void closeArchive();

    @Override
    public void close() throws IOException {
        closeArchive();
        osf.close();
    }

    static class OsReadableFile {
        ParcelFileDescriptor pfd;
        FileDescriptor fd;

        public OsReadableFile(Context ctx, Uri uri) throws FileNotFoundException {
            pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
            fd = pfd.getFileDescriptor();
        }

        public int read(byte[] b, int off, int len) throws IOException, ErrnoException {
            return Os.read(fd, b, off, len);
        }

        public int read(byte[] b) throws IOException, ErrnoException {
            return read(b, 0, b.length);
        }

        public long length() throws IOException, ErrnoException {
            return Os.fstat(fd).st_size;
        }

        public long seek(long pos, int whence) throws IOException, ErrnoException {
            try {
                return Os.lseek(fd, pos, whence);
            } catch (Exception e) {
                e.printStackTrace();
                return -30;
            }
        }

        public void rewind() throws IOException, ErrnoException {
            Os.lseek(fd, 0, OsConstants.SEEK_SET);
        }

        public long skip(long n) throws IOException, ErrnoException {
            long pos;
            long len;
            long newpos;

            if (n <= 0) {
                return 0;
            }
            pos = Os.lseek(fd, 0L, OsConstants.SEEK_CUR);
            len = length();
            newpos = pos + n;
            if (newpos > len) {
                newpos = len;
            }
            seek(newpos, OsConstants.SEEK_SET);

            /* return the actual number of bytes skipped */
            return newpos - pos;
        }

        public void close() throws IOException {
            pfd.close();
            try {
                Os.close(fd);
            } catch (ErrnoException e) {
                throw new IOException("An ErrnoException occurs", e);
            }
        }
    }
}
