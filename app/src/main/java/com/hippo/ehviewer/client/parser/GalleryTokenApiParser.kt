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
package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.EhException
import com.hippo.util.ExceptionUtils
import org.json.JSONObject

object GalleryTokenApiParser {
    /**
     * {
     * "tokenlist": [
     * {
     * "gid":618395,
     * "token":"0439fa3666"
     * }
     * ]
     * }
     */
    fun parse(body: String): String {
        val jo = JSONObject(body).getJSONArray("tokenlist").getJSONObject(0)
        return runCatching {
            jo.getString("token")
        }.getOrElse {
            ExceptionUtils.throwIfFatal(it)
            throw EhException(jo.getString("error"))
        }
    }
}
