/*
 * Copyright 2015 Hippo Seven
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

import com.hippo.ehviewer.client.exception.ParseException
import org.json.JSONException
import org.json.JSONObject

object RateGalleryParser {
    fun parse(body: String): Result {
        return try {
            val jsonObject = JSONObject(body)
            val rating = jsonObject.getDouble("rating_avg").toFloat()
            val ratingCount = jsonObject.getInt("rating_cnt")
            Result(rating, ratingCount)
        } catch (e: JSONException) {
            throw ParseException("Can't parse rate gallery", body, e)
        }
    }

    class Result(val rating: Float, val ratingCount: Int)
}
