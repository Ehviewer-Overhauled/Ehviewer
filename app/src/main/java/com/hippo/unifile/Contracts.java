/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.unifile;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import java.io.IOException;

final class Contracts {
    private Contracts() {
    }

    static String queryForString(Context context, Uri self, String column,
                                 String defaultValue) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[]{column}, null, null, null);
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return c.getString(0);
            } else {
                return defaultValue;
            }
        } catch (Throwable e) {
            Utils.throwIfFatal(e);
            return defaultValue;
        } finally {
            Utils.closeQuietly(c);
        }
    }

    static int queryForInt(Context context, Uri self, String column,
                           int defaultValue) {
        return (int) queryForLong(context, self, column, defaultValue);
    }

    static long queryForLong(Context context, Uri self, String column,
                             long defaultValue) {
        final ContentResolver resolver = context.getContentResolver();

        Cursor c = null;
        try {
            c = resolver.query(self, new String[]{column}, null, null, null);
            if (c != null && c.moveToFirst() && !c.isNull(0)) {
                return c.getLong(0);
            } else {
                return defaultValue;
            }
        } catch (Throwable e) {
            Utils.throwIfFatal(e);
            return defaultValue;
        } finally {
            Utils.closeQuietly(c);
        }
    }

    @NonNull
    static ParcelFileDescriptor openFileDescriptor(Context context, Uri uri, String mode) throws IOException {
        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, mode);
        if (pfd == null) {
            throw new IOException("Can't open ParcelFileDescriptor");
        }
        return pfd;
    }

    @NonNull
    static ImageDecoder.Source getImageSource(Context context, Uri uri) {
        return ImageDecoder.createSource(context.getContentResolver(), uri);
    }
}
