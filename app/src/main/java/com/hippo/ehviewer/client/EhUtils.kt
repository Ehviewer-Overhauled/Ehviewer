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
package com.hippo.ehviewer.client

import android.text.TextUtils
import com.hippo.ehviewer.EhApplication.Companion.ehCookieStore
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.data.GalleryInfo
import java.util.regex.Pattern

object EhUtils {
    const val NONE = -1 // Use it for homepage
    const val PRIVATE = 0x400
    const val UNKNOWN = 0x800

    // https://youtrack.jetbrains.com/issue/KT-4749
    private const val BG_COLOR_DOUJINSHI = 0xfff44336u
    private const val BG_COLOR_MANGA = 0xffff9800u
    private const val BG_COLOR_ARTIST_CG = 0xfffbc02du
    private const val BG_COLOR_GAME_CG = 0xff4caf50u
    private const val BG_COLOR_WESTERN = 0xff8bc34au
    private const val BG_COLOR_NON_H = 0xff2196f3u
    private const val BG_COLOR_IMAGE_SET = 0xff3f51b5u
    private const val BG_COLOR_COSPLAY = 0xff9c27b0u
    private const val BG_COLOR_ASIAN_PORN = 0xff9575cdu
    private const val BG_COLOR_MISC = 0xfff06292u
    private const val BG_COLOR_UNKNOWN = 0xff000000u

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff
    private val PATTERN_TITLE_PREFIX = Pattern.compile(
        "^(?:\\([^)]*\\)|\\[[^]]*]|\\{[^}]*\\}|~[^~]*~|\\s+)*"
    )

    // Remove [XXX], (XXX), {XXX}, ~XXX~ stuff and something like ch. 1-23
    private val PATTERN_TITLE_SUFFIX = Pattern.compile(
        "(?:\\s+ch.[\\s\\d-]+)?(?:\\([^)]*\\)|\\[[^]]*]|\\{[^}]*\\}|~[^~]*~|\\s+)*$",
        Pattern.CASE_INSENSITIVE
    )

    private val CATEGORY_VALUES = hashMapOf(
        EhConfig.MISC to arrayOf("misc"),
        EhConfig.DOUJINSHI to arrayOf("doujinshi"),
        EhConfig.MANGA to arrayOf("manga"),
        EhConfig.ARTIST_CG to arrayOf("artistcg", "Artist CG Sets", "Artist CG"),
        EhConfig.GAME_CG to arrayOf("gamecg", "Game CG Sets", "Game CG"),
        EhConfig.IMAGE_SET to arrayOf("imageset", "Image Sets", "Image Set"),
        EhConfig.COSPLAY to arrayOf("cosplay"),
        EhConfig.ASIAN_PORN to arrayOf("asianporn", "Asian Porn"),
        EhConfig.NON_H to arrayOf("non-h"),
        EhConfig.WESTERN to arrayOf("western"),
        PRIVATE to arrayOf("private"),
        UNKNOWN to arrayOf("unknown")
    )
    private val CATEGORY_STRINGS = CATEGORY_VALUES.entries.map { (k, v) -> v to k }

    @JvmStatic
    fun getCategory(type: String?): Int {
        for (entry in CATEGORY_STRINGS) {
            for (str in entry.first)
                if (str.equals(type, ignoreCase = true))
                    return entry.second
        }
        return UNKNOWN
    }

    fun getCategory(type: Int): String {
        return CATEGORY_VALUES.getOrDefault(type, CATEGORY_VALUES[UNKNOWN])!![0]
    }

    fun getCategoryColor(category: Int): Int {
        return when (category) {
            EhConfig.DOUJINSHI -> BG_COLOR_DOUJINSHI
            EhConfig.MANGA -> BG_COLOR_MANGA
            EhConfig.ARTIST_CG -> BG_COLOR_ARTIST_CG
            EhConfig.GAME_CG -> BG_COLOR_GAME_CG
            EhConfig.WESTERN -> BG_COLOR_WESTERN
            EhConfig.NON_H -> BG_COLOR_NON_H
            EhConfig.IMAGE_SET -> BG_COLOR_IMAGE_SET
            EhConfig.COSPLAY -> BG_COLOR_COSPLAY
            EhConfig.ASIAN_PORN -> BG_COLOR_ASIAN_PORN
            EhConfig.MISC -> BG_COLOR_MISC
            else -> BG_COLOR_UNKNOWN
        }.toInt()
    }

    fun signOut() {
        ehCookieStore.signOut()
        Settings.putAvatar(null)
        Settings.putDisplayName(null)
        Settings.putGallerySite(EhUrl.SITE_E)
        Settings.putNeedSignIn(true)
    }

    fun needSignedIn(): Boolean {
        return Settings.getNeedSignIn() && !ehCookieStore.hasSignedIn()
    }

    @JvmStatic
    fun getSuitableTitle(gi: GalleryInfo): String {
        return if (Settings.getShowJpnTitle()) {
            if (TextUtils.isEmpty(gi.titleJpn)) gi.title else gi.titleJpn
        } else {
            if (TextUtils.isEmpty(gi.title)) gi.titleJpn else gi.title
        }.orEmpty()
    }

    fun extractTitle(fullTitle: String?): String? {
        var title: String = fullTitle ?: return null
        title = PATTERN_TITLE_PREFIX.matcher(title).replaceFirst("")
        title = PATTERN_TITLE_SUFFIX.matcher(title).replaceFirst("")
        // Sometimes title is combined by romaji and english translation.
        // Only need romaji.
        // TODO But not sure every '|' means that
        val index = title.indexOf('|')
        if (index >= 0) {
            title = title.substring(0, index)
        }
        return title.ifEmpty { null }
    }

    fun fixThumbUrl(url: String): String {
        return EhUrl.getThumbUrlPrefix() +
                url.removePrefix(EhUrl.URL_PREFIX_THUMB_E).removePrefix(EhUrl.URL_PREFIX_THUMB_EX)
    }

    @JvmStatic
    fun handleThumbUrlResolution(url: String?): String? {
        if (null == url) {
            return null
        }
        val resolution = when (Settings.getThumbResolution()) {
            0 -> return url
            1 -> "250"
            2 -> "300"
            else -> return url
        }
        val index1 = url.lastIndexOf('_')
        val index2 = url.lastIndexOf('.')
        return if (index1 >= 0 && index2 >= 0 && index1 < index2) {
            url.substring(0, index1 + 1) + resolution + url.substring(index2)
        } else {
            url
        }
    }
}