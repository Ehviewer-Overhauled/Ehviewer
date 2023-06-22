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
package com.hippo.unifile

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.IOException

internal object Contracts {
    fun queryForString(
        context: Context,
        self: Uri,
        column: String,
        defaultValue: String?,
    ): String? {
        return runCatching {
            context.contentResolver.query(self, arrayOf(column), null, null, null).use {
                if (it != null && it.moveToFirst() && !it.isNull(0)) {
                    it.getString(0)
                } else {
                    defaultValue
                }
            }
        }.getOrElse {
            Utils.throwIfFatal(it)
            defaultValue
        }
    }

    fun queryForInt(context: Context, self: Uri, column: String?, defaultValue: Int): Int {
        return queryForLong(context, self, column, defaultValue.toLong()).toInt()
    }

    fun queryForLong(context: Context, self: Uri, column: String?, defaultValue: Long): Long {
        return runCatching {
            context.contentResolver.query(self, arrayOf(column), null, null, null).use {
                if (it != null && it.moveToFirst() && !it.isNull(0)) {
                    it.getLong(0)
                } else {
                    defaultValue
                }
            }
        }.getOrElse {
            Utils.throwIfFatal(it)
            defaultValue
        }
    }

    fun openFileDescriptor(
        context: Context,
        uri: Uri?,
        mode: String?,
    ): ParcelFileDescriptor {
        return context.contentResolver.openFileDescriptor(uri!!, mode!!)
            ?: throw IOException("Can't open ParcelFileDescriptor")
    }

    fun getImageSource(context: Context, uri: Uri?): ImageDecoder.Source {
        return ImageDecoder.createSource(context.contentResolver, uri!!)
    }
}
