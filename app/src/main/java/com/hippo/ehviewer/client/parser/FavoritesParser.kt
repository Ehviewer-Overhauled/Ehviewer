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

import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.ParseException
import splitties.init.appCtx

object FavoritesParser {
    fun parse(body: String): Result {
        if (body.contains("This page requires you to log on.</p>")) {
            throw EhException(appCtx.getString(R.string.need_sign_in))
        }
        val catArray = arrayOfNulls<String>(10)
        val countArray = parseFav(body, catArray)
        if (countArray.isEmpty()) throw ParseException("Parse favorites error", body)
        val result = GalleryListParser.parse(body)
        return Result(catArray.requireNoNulls(), countArray, result)
    }

    class Result(
        val catArray: Array<String>,
        val countArray: IntArray,
        galleryListResult: GalleryListParser.Result,
    ) {
        val prev = galleryListResult.prev
        val next = galleryListResult.next
        val galleryInfoList = galleryListResult.galleryInfoList
    }
}

private external fun parseFav(body: String, favCat: Array<String?>): IntArray
