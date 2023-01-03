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

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.hippo.ehviewer.client.EhUrl;
import com.hippo.network.UrlBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FavListUrlBuilder implements Parcelable {

    public static final int FAV_CAT_ALL = -1;
    public static final int FAV_CAT_LOCAL = -2;
    public static final Parcelable.Creator<FavListUrlBuilder> CREATOR = new Parcelable.Creator<>() {

        @Override
        public FavListUrlBuilder createFromParcel(Parcel source) {
            return new FavListUrlBuilder(source);
        }

        @Override
        public FavListUrlBuilder[] newArray(int size) {
            return new FavListUrlBuilder[size];
        }
    };
    private static final String TAG = FavListUrlBuilder.class.getSimpleName();
    private String mPrev;
    private String mNext;
    private String mJumpTo;
    private String mKeyword;
    private int mFavCat = FAV_CAT_ALL;

    public FavListUrlBuilder() {
    }

    protected FavListUrlBuilder(Parcel in) {
        this.mPrev = in.readString();
        this.mNext = in.readString();
        this.mJumpTo = in.readString();
        this.mKeyword = in.readString();
        this.mFavCat = in.readInt();
    }

    public static boolean isValidFavCat(int favCat) {
        return favCat >= 0 && favCat <= 9;
    }

    public void setIndex(String index, boolean isNext) {
        if (isNext) {
            mNext = index;
            mPrev = null;
        } else {
            mPrev = index;
            mNext = null;
        }
    }

    public void setJumpTo(String jumpTo) {
        mJumpTo = jumpTo;
    }

    public String getKeyword() {
        return mKeyword;
    }

    public void setKeyword(String keyword) {
        mKeyword = keyword;
    }

    public int getFavCat() {
        return mFavCat;
    }

    public void setFavCat(int favCat) {
        mFavCat = favCat;
    }

    public boolean isLocalFavCat() {
        return mFavCat == FAV_CAT_LOCAL;
    }

    public String build() {
        UrlBuilder ub = new UrlBuilder(EhUrl.getFavoritesUrl());
        if (isValidFavCat(mFavCat)) {
            ub.addQuery("favcat", Integer.toString(mFavCat));
        } else if (mFavCat == FAV_CAT_ALL) {
            ub.addQuery("favcat", "all");
        }
        if (!TextUtils.isEmpty(mKeyword)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ub.addQuery("f_search", URLEncoder.encode(mKeyword, StandardCharsets.UTF_8));
            } else {
                try {
                    ub.addQuery("f_search", URLEncoder.encode(mKeyword, StandardCharsets.UTF_8.displayName()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        if (mPrev != null && !mPrev.isEmpty()) {
            ub.addQuery("prev", mPrev);
        }
        if (mNext != null && !mNext.isEmpty()) {
            ub.addQuery("next", mNext);
        }
        if (mJumpTo != null && !mJumpTo.isEmpty()) {
            ub.addQuery("seek", mJumpTo);
        }
        return ub.build();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPrev);
        dest.writeString(this.mNext);
        dest.writeString(this.mJumpTo);
        dest.writeString(this.mKeyword);
        dest.writeInt(this.mFavCat);
    }
}
