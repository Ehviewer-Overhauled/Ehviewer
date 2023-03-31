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
package com.hippo.ehviewer.client.data

import android.os.Parcelable
import com.hippo.ehviewer.client.EhUrl
import com.hippo.network.UrlBuilder
import com.hippo.util.encodeUTF8
import kotlinx.parcelize.Parcelize

@Parcelize
class FavListUrlBuilder(
    private var mPrev: String? = null,
    private var mNext: String? = null,
    var jumpTo: String? = null,
    var keyword: String? = null,
    var favCat: Int = FAV_CAT_ALL,
) : Parcelable {
    fun setIndex(index: String?, isNext: Boolean) {
        mNext = index.takeIf { isNext }
        mPrev = index.takeUnless { isNext }
    }

    fun build(): String {
        val ub = UrlBuilder(EhUrl.favoritesUrl)
        if (isValidFavCat(favCat)) {
            ub.addQuery("favcat", favCat.toString())
        } else if (favCat == FAV_CAT_ALL) {
            ub.addQuery("favcat", "all")
        }
        keyword?.takeIf { it.isNotBlank() }?.let { ub.addQuery("f_search", encodeUTF8(it)) }
        mPrev?.takeIf { it.isNotEmpty() }?.let { ub.addQuery("prev", it) }
        mNext?.takeIf { it.isNotEmpty() }?.let { ub.addQuery("next", it) }
        jumpTo?.takeIf { it.isNotEmpty() }?.let { ub.addQuery("seek", it) }
        return ub.build()
    }

    companion object {
        const val FAV_CAT_ALL = -1
        const val FAV_CAT_LOCAL = -2
        fun isValidFavCat(favCat: Int): Boolean {
            return favCat in 0..9
        }
    }
}
