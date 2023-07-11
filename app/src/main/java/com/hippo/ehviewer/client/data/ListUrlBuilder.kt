/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hippo.ehviewer.client.data

import android.os.Parcelable
import androidx.annotation.IntDef
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.EhUtils
import com.hippo.ehviewer.client.addQueryParameterIfNotBlank
import com.hippo.ehviewer.client.ehUrl
import com.hippo.ehviewer.dao.QuickSearch
import com.hippo.ehviewer.ui.main.AdvanceTable
import com.hippo.ehviewer.util.encodeUTF8
import com.hippo.ehviewer.yorozuya.toIntOrDefault
import kotlinx.parcelize.Parcelize
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

@Parcelize
data class ListUrlBuilder(
    @get:Mode @Mode
    var mode: Int = MODE_NORMAL,
    private var mPrev: String? = null,
    var mNext: String? = null,
    var mJumpTo: String? = null,
    var category: Int = EhUtils.NONE,
    private var mKeyword: String? = null,
    var hash: String? = null,
    var advanceSearch: Int = -1,
    var minRating: Int = -1,
    var pageFrom: Int = -1,
    var pageTo: Int = -1,
    var imagePath: String? = null,
    var isUseSimilarityScan: Boolean = false,
    var isOnlySearchCovers: Boolean = false,
) : Parcelable {

    fun reset() {
        mode = MODE_NORMAL
        mPrev = null
        mNext = null
        mJumpTo = null
        this.category = EhUtils.NONE
        mKeyword = null
        advanceSearch = -1
        minRating = -1
        pageFrom = -1
        pageTo = -1
        imagePath = null
        isUseSimilarityScan = false
        isOnlySearchCovers = false
        hash = null
    }

    fun setIndex(index: String?, isNext: Boolean = true) {
        mNext = index?.takeIf { isNext }
        mPrev = index?.takeUnless { isNext }
    }

    fun setJumpTo(jumpTo: String?) {
        mJumpTo = jumpTo
    }

    var keyword: String?
        get() = if (MODE_UPLOADER == mode) "uploader:$mKeyword" else mKeyword
        set(keyword) {
            mKeyword = keyword
        }

    fun set(q: QuickSearch) {
        mode = q.mode
        this.category = q.category
        mKeyword = q.keyword
        advanceSearch = q.advanceSearch
        minRating = q.minRating
        pageFrom = q.pageFrom
        pageTo = q.pageTo
        imagePath = null
        isUseSimilarityScan = false
        isOnlySearchCovers = false
    }

    fun toQuickSearch(): QuickSearch {
        val q = QuickSearch()
        q.mode = mode
        q.category = this.category
        q.keyword = mKeyword
        q.advanceSearch = advanceSearch
        q.minRating = minRating
        q.pageFrom = pageFrom
        q.pageTo = pageTo
        return q
    }

    fun equalsQuickSearch(q: QuickSearch?): Boolean {
        if (null == q) {
            return false
        }
        if (q.mode != mode) {
            return false
        }
        if (q.category != this.category) {
            return false
        }
        if (q.keyword != mKeyword) {
            return false
        }
        if (q.advanceSearch != advanceSearch) {
            return false
        }
        if (q.minRating != minRating) {
            return false
        }
        return if (q.pageFrom != pageFrom) {
            false
        } else {
            q.pageTo == pageTo
        }
    }

    /**
     * @param query xxx=yyy&mmm=nnn
     */
    // TODO page
    fun setQuery(query: String?) {
        reset()
        if (query.isNullOrEmpty()) {
            return
        }
        val queries = query.split('&')
        var category = 0
        var keyword: String? = null
        var enableAdvanceSearch = false
        var advanceSearch = 0
        var enableMinRating = false
        var minRating = -1
        var enablePage = false
        var pageFrom = -1
        var pageTo = -1
        for (str in queries) {
            val index = str.indexOf('=')
            if (index < 0) {
                continue
            }
            val key = str.substring(0, index)
            val value = str.substring(index + 1)
            when (key) {
                "f_cats" -> {
                    val cats = value.toIntOrDefault(EhUtils.ALL_CATEGORY)
                    category = category or (cats.inv() and EhUtils.ALL_CATEGORY)
                }

                "f_doujinshi" -> if ("1" == value) {
                    category = category or EhUtils.DOUJINSHI
                }

                "f_manga" -> if ("1" == value) {
                    category = category or EhUtils.MANGA
                }

                "f_artistcg" -> if ("1" == value) {
                    category = category or EhUtils.ARTIST_CG
                }

                "f_gamecg" -> if ("1" == value) {
                    category = category or EhUtils.GAME_CG
                }

                "f_western" -> if ("1" == value) {
                    category = category or EhUtils.WESTERN
                }

                "f_non-h" -> if ("1" == value) {
                    category = category or EhUtils.NON_H
                }

                "f_imageset" -> if ("1" == value) {
                    category = category or EhUtils.IMAGE_SET
                }

                "f_cosplay" -> if ("1" == value) {
                    category = category or EhUtils.COSPLAY
                }

                "f_asianporn" -> if ("1" == value) {
                    category = category or EhUtils.ASIAN_PORN
                }

                "f_misc" -> if ("1" == value) {
                    category = category or EhUtils.MISC
                }

                "f_search" -> try {
                    keyword = URLDecoder.decode(value, "utf-8")
                } catch (e: UnsupportedEncodingException) {
                    // Ignore
                } catch (_: IllegalArgumentException) {
                }

                "advsearch" -> if ("1" == value) {
                    enableAdvanceSearch = true
                }

                "f_sh" -> if ("on" == value) {
                    advanceSearch = advanceSearch or AdvanceTable.SH
                }

                "f_sto" -> if ("on" == value) {
                    advanceSearch = advanceSearch or AdvanceTable.STO
                }

                "f_sfl" -> if ("on" == value) {
                    advanceSearch = advanceSearch or AdvanceTable.SFL
                }

                "f_sfu" -> if ("on" == value) {
                    advanceSearch = advanceSearch or AdvanceTable.SFU
                }

                "f_sft" -> if ("on" == value) {
                    advanceSearch = advanceSearch or AdvanceTable.SFT
                }

                "f_sr" -> if ("on" == value) {
                    enableMinRating = true
                }

                "f_srdd" -> minRating = value.toIntOrDefault(-1)
                "f_sp" -> if ("on" == value) {
                    enablePage = true
                }

                "f_spf" -> pageFrom = value.toIntOrDefault(-1)
                "f_spt" -> pageTo = value.toIntOrDefault(-1)
                "f_shash" -> hash = value
            }
        }
        this.category = category
        mKeyword = keyword
        if (enableAdvanceSearch) {
            this.advanceSearch = advanceSearch
            if (enableMinRating) {
                this.minRating = minRating
            } else {
                this.minRating = -1
            }
            if (enablePage) {
                this.pageFrom = pageFrom
                this.pageTo = pageTo
            } else {
                this.pageFrom = -1
                this.pageTo = -1
            }
        } else {
            this.advanceSearch = -1
        }
    }

    fun build(): String {
        return when (mode) {
            MODE_NORMAL, MODE_SUBSCRIPTION -> ehUrl {
                if (mode == MODE_SUBSCRIPTION) addPathSegment(EhUrl.WATCHED_PATH)
                if (category != EhUtils.NONE) {
                    addEncodedQueryParameter("f_cats", (category.inv() and EhUtils.ALL_CATEGORY).toString())
                }
                addQueryParameterIfNotBlank("f_shash", hash)
                addQueryParameterIfNotBlank("prev", mPrev)
                addQueryParameterIfNotBlank("next", mNext)
                addQueryParameterIfNotBlank("seek", mJumpTo)
                // Search key
                // the settings of ub:UrlBuilder may be overwritten by following Advance search
                mKeyword?.split('|')?.forEachIndexed { idx, kwd ->
                    val keyword = kwd.trim { it <= ' ' }
                    when (idx) {
                        0 -> addQueryParameterIfNotBlank("f_search", keyword)

                        else -> keyword.indexOf(':').takeIf { it >= 0 }?.run {
                            val key = keyword.substring(0, this).trim { it <= ' ' }
                            val value = keyword.substring(this + 1).trim { it <= ' ' }
                            addQueryParameterIfNotBlank(key, value)
                        }
                    }
                }
                // Advance search
                if (advanceSearch != -1) {
                    addQueryParameter("advsearch", "1")
                    if (advanceSearch and AdvanceTable.SH != 0) {
                        addQueryParameter("f_sh", "on")
                    }
                    if (advanceSearch and AdvanceTable.STO != 0) {
                        addQueryParameter("f_sto", "on")
                    }
                    if (advanceSearch and AdvanceTable.SFL != 0) {
                        addQueryParameter("f_sfl", "on")
                    }
                    if (advanceSearch and AdvanceTable.SFU != 0) {
                        addQueryParameter("f_sfu", "on")
                    }
                    if (advanceSearch and AdvanceTable.SFT != 0) {
                        addQueryParameter("f_sft", "on")
                    }
                    // Set min star
                    if (minRating != -1) {
                        addQueryParameter("f_sr", "on")
                        addQueryParameter("f_srdd", "$minRating")
                    }
                    // Pages
                    if (pageFrom != -1 || pageTo != -1) {
                        addQueryParameter("f_sp", "on")
                        addQueryParameter(
                            "f_spf",
                            (if (pageFrom != -1) pageFrom.toString() else "").toString(),
                        )
                        addQueryParameter(
                            "f_spt",
                            (if (pageTo != -1) pageTo.toString() else "").toString(),
                        )
                    }
                }
            }.toString()

            MODE_UPLOADER -> {
                val sb = StringBuilder(EhUrl.host)
                mKeyword?.let {
                    sb.append("uploader/")
                    sb.append(encodeUTF8(it))
                }
                mPrev?.let {
                    sb.append("?prev=").append(it)
                }
                mNext?.let {
                    sb.append("?next=").append(it)
                }
                mJumpTo?.let {
                    sb.append("&seek=").append(it)
                }
                sb.toString()
            }

            MODE_TAG -> {
                val sb = StringBuilder(EhUrl.host)
                mKeyword?.let {
                    sb.append("tag/")
                    sb.append(encodeUTF8(it))
                }
                mPrev?.let {
                    sb.append("?prev=").append(it)
                }
                mNext?.let {
                    sb.append("?next=").append(it)
                }
                mJumpTo?.let {
                    sb.append("&seek=").append(it)
                }
                sb.toString()
            }

            MODE_WHATS_HOT -> EhUrl.popularUrl
            MODE_IMAGE_SEARCH -> EhUrl.imageSearchUrl
            MODE_TOPLIST -> {
                val sb = StringBuilder(EhUrl.HOST_E)
                sb.append("toplist.php?tl=")
                mKeyword.orEmpty().let {
                    sb.append(encodeUTF8(it))
                }
                mJumpTo?.let {
                    sb.append("&p=").append(it)
                }
                sb.toString()
            }

            else -> throw IllegalStateException("Unexpected value: $mode")
        }
    }

    @IntDef(
        MODE_NORMAL,
        MODE_UPLOADER,
        MODE_TAG,
        MODE_WHATS_HOT,
        MODE_IMAGE_SEARCH,
        MODE_SUBSCRIPTION,
        MODE_TOPLIST,
    )
    @Retention(AnnotationRetention.SOURCE)
    private annotation class Mode
    companion object {
        const val MODE_NORMAL = 0x0
        const val MODE_UPLOADER = 0x1
        const val MODE_TAG = 0x2
        const val MODE_WHATS_HOT = 0x3
        const val MODE_IMAGE_SEARCH = 0x4
        const val MODE_SUBSCRIPTION = 0x5
        const val MODE_TOPLIST = 0x6
    }
}
