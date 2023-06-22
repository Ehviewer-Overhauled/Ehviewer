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
import android.net.Uri
import android.provider.MediaStore

internal object MediaContract {
    fun getName(context: Context, self: Uri): String? {
        return Contracts.queryForString(context, self, MediaStore.MediaColumns.DISPLAY_NAME, null)
    }

    fun getType(context: Context, self: Uri): String? {
        return Contracts.queryForString(context, self, MediaStore.MediaColumns.MIME_TYPE, null)
    }

    fun lastModified(context: Context, self: Uri): Long {
        return Contracts.queryForLong(context, self, MediaStore.MediaColumns.DATE_MODIFIED, 0)
    }

    fun length(context: Context, self: Uri): Long {
        return Contracts.queryForLong(context, self, MediaStore.MediaColumns.SIZE, 0)
    }
}
