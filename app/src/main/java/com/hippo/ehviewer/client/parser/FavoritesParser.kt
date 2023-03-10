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

import com.hippo.ehviewer.GetText.getString
import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.util.ExceptionUtils
import com.hippo.util.JsoupUtils
import org.jsoup.Jsoup

object FavoritesParser {
    fun parse(body: String): Result {
        if (body.contains("This page requires you to log on.</p>")) {
            throw EhException(getString(R.string.need_sign_in))
        }
        val catArray = arrayOfNulls<String>(10)
        val countArray = IntArray(10)
        val d = Jsoup.parse(body)
        runCatching {
            val ido = JsoupUtils.getElementByClass(d, "ido")
            val fps = ido!!.getElementsByClass("fp")
            // Last one is "fp fps"
            check(fps.size == 11)
            for (i in 0..9) {
                val fp = fps[i]
                countArray[i] = ParserUtils.parseInt(fp.child(0).text(), 0)
                catArray[i] = ParserUtils.trim(fp.child(2).text())
            }
        }.onFailure {
            ExceptionUtils.throwIfFatal(it)
            it.printStackTrace()
            throw ParseException("Parse favorites error", body)
        }
        val result = GalleryListParser.parse(d, body)
        return Result(catArray, countArray, result)
    }

    class Result(
        val catArray: Array<String?>,
        val countArray: IntArray,
        galleryListResult: GalleryListParser.Result
    ) {
        val prev = galleryListResult.prev
        val next = galleryListResult.next
        val galleryInfoList = galleryListResult.galleryInfoList
    }
}