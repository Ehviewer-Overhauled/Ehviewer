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

import android.util.Log
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.util.ExceptionUtils
import org.jsoup.Jsoup

object ProfileParser {
    private val TAG = ProfileParser::class.java.simpleName
    fun parse(body: String): Result {
        return runCatching {
            val d = Jsoup.parse(body)
            val profilename = d.getElementById("profilename")
            val displayName = profilename!!.child(0).text()
            val avatar = runCatching {
                val avatar =
                    profilename.nextElementSibling()!!.nextElementSibling()!!.child(0).attr("src")
                if (avatar.isNullOrEmpty()) {
                    null
                } else if (!avatar.startsWith("http")) {
                    EhUrl.URL_FORUMS + avatar
                } else {
                    avatar
                }
            }.getOrElse {
                ExceptionUtils.throwIfFatal(it)
                Log.i(TAG, "No avatar")
                null
            }
            Result(displayName, avatar)
        }.getOrElse {
            ExceptionUtils.throwIfFatal(it)
            throw ParseException("Parse forums error", body)
        }
    }

    class Result(val displayName: String?, val avatar: String?)
}
