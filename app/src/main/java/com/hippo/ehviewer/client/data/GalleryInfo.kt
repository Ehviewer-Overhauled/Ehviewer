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
import androidx.room.ColumnInfo
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.regex.Pattern

@Parcelize
open class GalleryInfo(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = "GID")
    var gid: Long = 0,

    @JvmField
    @ColumnInfo(name = "TOKEN")
    var token: String? = null,

    @JvmField
    @ColumnInfo(name = "TITLE")
    var title: String? = null,

    @JvmField
    @ColumnInfo(name = "TITLE_JPN")
    var titleJpn: String? = null,

    @JvmField
    @ColumnInfo(name = "THUMB")
    var thumb: String? = null,

    @JvmField
    @ColumnInfo(name = "CATEGORY")
    var category: Int = 0,

    @JvmField
    @ColumnInfo(name = "POSTED")
    var posted: String? = null,

    @JvmField
    @ColumnInfo(name = "UPLOADER")
    var uploader: String? = null,

    @JvmField
    @Ignore
    var disowned: Boolean = false,

    @JvmField
    @ColumnInfo(name = "RATING")
    var rating: Float = 0f,

    @JvmField
    @Ignore
    var rated: Boolean = false,

    @JvmField
    @Ignore
    var simpleTags: Array<String>? = null,

    @JvmField
    @Ignore
    var pages: Int = 0,

    @JvmField
    @Ignore
    var thumbWidth: Int = 0,

    @JvmField
    @Ignore
    var thumbHeight: Int = 0,

    @Ignore
    var spanSize: Int = 0,

    @Ignore
    var spanIndex: Int = 0,

    @Ignore
    var spanGroupIndex: Int = 0,

    /**
     * language from title
     */
    @JvmField
    @ColumnInfo(name = "SIMPLE_LANGUAGE")
    var simpleLanguage: String? = null,

    @JvmField
    @Ignore
    var favoriteSlot: Int = -2,

    @JvmField
    @Ignore
    var favoriteName: String? = null

) : Parcelable {
    fun generateSLang() {
        simpleLanguage = simpleTags?.let { generateSLangFromTags(it) } ?: title?.let { generateSLangFromTitle(it) }
    }

    private fun generateSLangFromTags(simpleTags: Array<String>): String? {
        for (tag in simpleTags) {
            for (i in S_LANGS.indices) {
                if (S_LANG_TAGS[i] == tag) {
                    return S_LANGS[i]
                }
            }
        }
        return null
    }

    private fun generateSLangFromTitle(title: String): String? {
        for (i in S_LANGS.indices) {
            if (S_LANG_PATTERNS[i].matcher(title).find()) {
                return S_LANGS[i]
            }
        }
        return null
    }

    companion object {
        /**
         * ISO 639-1
         */
        const val S_LANG_JA = "JA"
        const val S_LANG_EN = "EN"
        const val S_LANG_ZH = "ZH"
        const val S_LANG_NL = "NL"
        const val S_LANG_FR = "FR"
        const val S_LANG_DE = "DE"
        const val S_LANG_HU = "HU"
        const val S_LANG_IT = "IT"
        const val S_LANG_KO = "KO"
        const val S_LANG_PL = "PL"
        const val S_LANG_PT = "PT"
        const val S_LANG_RU = "RU"
        const val S_LANG_ES = "ES"
        const val S_LANG_TH = "TH"
        const val S_LANG_VI = "VI"

        @JvmField
        val S_LANGS = arrayOf(
            S_LANG_EN,
            S_LANG_ZH,
            S_LANG_ES,
            S_LANG_KO,
            S_LANG_RU,
            S_LANG_FR,
            S_LANG_PT,
            S_LANG_TH,
            S_LANG_DE,
            S_LANG_IT,
            S_LANG_VI,
            S_LANG_PL,
            S_LANG_HU,
            S_LANG_NL
        )
        val S_LANG_PATTERNS = arrayOf(
            Pattern.compile(
                "[(\\[]eng(?:lish)?[)\\]]|英訳",
                Pattern.CASE_INSENSITIVE
            ),  // [(（\[]ch(?:inese)?[)）\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳
            Pattern.compile(
                "[(\uFF08\\[]ch(?:inese)?[)\uFF09\\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                "[(\\[]spanish[)\\]]|[(\\[]Español[)\\]]|スペイン翻訳",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile("[(\\[]korean?[)\\]]|韓国翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]rus(?:sian)?[)\\]]|ロシア翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]fr(?:ench)?[)\\]]|フランス翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]portuguese|ポルトガル翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                "[(\\[]thai(?: ภาษาไทย)?[)\\]]|แปลไทย|タイ翻訳",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile("[(\\[]german[)\\]]|ドイツ翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]italiano?[)\\]]|イタリア翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                "[(\\[]vietnamese(?: Tiếng Việt)?[)\\]]|ベトナム翻訳",
                Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile("[(\\[]polish[)\\]]|ポーランド翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]hun(?:garian)?[)\\]]|ハンガリー翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]dutch[)\\]]|オランダ翻訳", Pattern.CASE_INSENSITIVE)
        )
        val S_LANG_TAGS = arrayOf(
            "language:english",
            "language:chinese",
            "language:spanish",
            "language:korean",
            "language:russian",
            "language:french",
            "language:portuguese",
            "language:thai",
            "language:german",
            "language:italian",
            "language:vietnamese",
            "language:polish",
            "language:hungarian",
            "language:dutch"
        )
    }
}