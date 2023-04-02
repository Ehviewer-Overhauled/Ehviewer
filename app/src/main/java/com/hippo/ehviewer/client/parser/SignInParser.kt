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
import com.hippo.ehviewer.client.exception.ParseException
import java.util.regex.Pattern

object SignInParser {
    private val NAME_PATTERN = Pattern.compile("<p>You are now logged in as: (.+?)<")
    private val ERROR_PATTERN = Pattern.compile(
        "<h4>The error returned was:</h4>\\s*<p>(.+?)</p>" +
            "|<span class=\"postcolor\">(.+?)</span>",
    )

    fun parse(body: String): String {
        var m = NAME_PATTERN.matcher(body)
        return if (m.find()) {
            m.group(1)!!
        } else {
            m = ERROR_PATTERN.matcher(body)
            if (m.find()) {
                throw EhException(m.group(1) ?: m.group(2))
            } else {
                throw ParseException("Can't parse sign in", body)
            }
        }
    }
}
