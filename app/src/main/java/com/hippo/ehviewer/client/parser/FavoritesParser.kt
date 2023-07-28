package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.R
import com.hippo.ehviewer.client.exception.EhException
import com.hippo.ehviewer.client.exception.ParseException
import splitties.init.appCtx

object FavoritesParser {
    suspend fun parse(body: String): Result {
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
        galleryListResult: GalleryListResult,
    ) {
        val prev = galleryListResult.prev
        val next = galleryListResult.next
        val galleryInfoList = galleryListResult.galleryInfoList
    }
}

private external fun parseFav(body: String, favCat: Array<String?>): IntArray
