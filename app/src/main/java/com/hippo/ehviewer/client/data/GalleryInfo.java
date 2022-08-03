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

package com.hippo.ehviewer.client.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.regex.Pattern;

public class GalleryInfo implements Parcelable {

    /**
     * ISO 639-1
     */
    @SuppressWarnings("unused")
    public static final String S_LANG_JA = "JA";
    public static final String S_LANG_EN = "EN";
    public static final String S_LANG_ZH = "ZH";
    public static final String S_LANG_NL = "NL";
    public static final String S_LANG_FR = "FR";
    public static final String S_LANG_DE = "DE";
    public static final String S_LANG_HU = "HU";
    public static final String S_LANG_IT = "IT";
    public static final String S_LANG_KO = "KO";
    public static final String S_LANG_PL = "PL";
    public static final String S_LANG_PT = "PT";
    public static final String S_LANG_RU = "RU";
    public static final String S_LANG_ES = "ES";
    public static final String S_LANG_TH = "TH";
    public static final String S_LANG_VI = "VI";

    public static final String[] S_LANGS = {
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
            S_LANG_NL,
    };

    public static final Pattern[] S_LANG_PATTERNS = {
            Pattern.compile("[(\\[]eng(?:lish)?[)\\]]|英訳", Pattern.CASE_INSENSITIVE),
            // [(（\[]ch(?:inese)?[)）\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳
            Pattern.compile("[(\uFF08\\[]ch(?:inese)?[)\uFF09\\]]|[汉漢]化|中[国國][语語]|中文|中国翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]spanish[)\\]]|[(\\[]Español[)\\]]|スペイン翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]korean?[)\\]]|韓国翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]rus(?:sian)?[)\\]]|ロシア翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]fr(?:ench)?[)\\]]|フランス翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]portuguese|ポルトガル翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]thai(?: ภาษาไทย)?[)\\]]|แปลไทย|タイ翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]german[)\\]]|ドイツ翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]italiano?[)\\]]|イタリア翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]vietnamese(?: Tiếng Việt)?[)\\]]|ベトナム翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]polish[)\\]]|ポーランド翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]hun(?:garian)?[)\\]]|ハンガリー翻訳", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[(\\[]dutch[)\\]]|オランダ翻訳", Pattern.CASE_INSENSITIVE),
    };

    public static final String[] S_LANG_TAGS = {
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
            "language:dutch",
    };
    public static final Parcelable.Creator<GalleryInfo> CREATOR = new Parcelable.Creator<GalleryInfo>() {

        @Override
        public GalleryInfo createFromParcel(Parcel source) {
            return new GalleryInfo(source);
        }

        @Override
        public GalleryInfo[] newArray(int size) {
            return new GalleryInfo[size];
        }
    };
    @PrimaryKey
    @ColumnInfo(name = "GID")
    public long gid;
    @ColumnInfo(name = "TOKEN")
    public String token;
    @ColumnInfo(name = "TITLE")
    public String title;
    @ColumnInfo(name = "TITLE_JPN")
    public String titleJpn;
    @ColumnInfo(name = "THUMB")
    public String thumb;
    @ColumnInfo(name = "CATEGORY")
    public int category;
    @ColumnInfo(name = "POSTED")
    public String posted;
    @ColumnInfo(name = "UPLOADER")
    public String uploader;
    @Ignore
    public boolean disowned;
    @ColumnInfo(name = "RATING")
    public float rating;
    @Ignore
    public boolean rated;
    @Nullable
    @Ignore
    public String[] simpleTags;
    @Ignore
    public int pages;
    @Ignore
    public int thumbWidth;
    @Ignore
    public int thumbHeight;
    @Ignore
    public int spanSize;
    @Ignore
    public int spanIndex;
    @Ignore
    public int spanGroupIndex;
    /**
     * language from title
     */
    @ColumnInfo(name = "SIMPLE_LANGUAGE")
    public String simpleLanguage;
    @Ignore
    public int favoriteSlot = -2;
    @Ignore
    public String favoriteName;

    public GalleryInfo() {
    }

    protected GalleryInfo(Parcel in) {
        this.gid = in.readLong();
        this.token = in.readString();
        this.title = in.readString();
        this.titleJpn = in.readString();
        this.thumb = in.readString();
        this.category = in.readInt();
        this.posted = in.readString();
        this.uploader = in.readString();
        this.rating = in.readFloat();
        this.rated = in.readByte() != 0;
        this.simpleLanguage = in.readString();
        this.simpleTags = in.createStringArray();
        this.thumbWidth = in.readInt();
        this.thumbHeight = in.readInt();
        this.spanSize = in.readInt();
        this.spanIndex = in.readInt();
        this.spanGroupIndex = in.readInt();
        this.favoriteSlot = in.readInt();
        this.favoriteName = in.readString();
        this.disowned = in.readByte() != 0;
    }

    public final void generateSLang() {
        if (simpleTags != null) {
            generateSLangFromTags();
        }
        if (simpleLanguage == null && title != null) {
            generateSLangFromTitle();
        }
    }

    private void generateSLangFromTags() {
        for (String tag : simpleTags) {
            for (int i = 0; i < S_LANGS.length; i++) {
                if (S_LANG_TAGS[i].equals(tag)) {
                    simpleLanguage = S_LANGS[i];
                    return;
                }
            }
        }
    }

    private void generateSLangFromTitle() {
        for (int i = 0; i < S_LANGS.length; i++) {
            if (S_LANG_PATTERNS[i].matcher(title).find()) {
                simpleLanguage = S_LANGS[i];
                return;
            }
        }
        simpleLanguage = null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.gid);
        dest.writeString(this.token);
        dest.writeString(this.title);
        dest.writeString(this.titleJpn);
        dest.writeString(this.thumb);
        dest.writeInt(this.category);
        dest.writeString(this.posted);
        dest.writeString(this.uploader);
        dest.writeFloat(this.rating);
        dest.writeByte(this.rated ? (byte) 1 : (byte) 0);
        dest.writeString(this.simpleLanguage);
        dest.writeStringArray(this.simpleTags);
        dest.writeInt(this.thumbWidth);
        dest.writeInt(this.thumbHeight);
        dest.writeInt(this.spanSize);
        dest.writeInt(this.spanIndex);
        dest.writeInt(this.spanGroupIndex);
        dest.writeInt(this.favoriteSlot);
        dest.writeString(this.favoriteName);
        dest.writeByte(this.disowned ? (byte) 1 : (byte) 0);
    }
}
