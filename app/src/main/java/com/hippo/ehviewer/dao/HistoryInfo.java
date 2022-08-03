package com.hippo.ehviewer.dao;

import android.os.Parcel;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import com.hippo.ehviewer.client.data.GalleryInfo;

@Entity(tableName = "HISTORY")
public class HistoryInfo extends GalleryInfo {
    public static final Creator<HistoryInfo> CREATOR = new Creator<>() {
        @Override
        public HistoryInfo createFromParcel(Parcel source) {
            return new HistoryInfo(source);
        }

        @Override
        public HistoryInfo[] newArray(int size) {
            return new HistoryInfo[size];
        }
    };
    @ColumnInfo(name = "MODE")
    public int mode;
    @ColumnInfo(name = "TIME")
    public long time;

    protected HistoryInfo(Parcel in) {
        super(in);
        this.mode = in.readInt();
        this.time = in.readLong();
    }

    public HistoryInfo(GalleryInfo galleryInfo) {
        this.gid = galleryInfo.gid;
        this.token = galleryInfo.token;
        this.title = galleryInfo.title;
        this.titleJpn = galleryInfo.titleJpn;
        this.thumb = galleryInfo.thumb;
        this.category = galleryInfo.category;
        this.posted = galleryInfo.posted;
        this.uploader = galleryInfo.uploader;
        this.rating = galleryInfo.rating;
        this.simpleTags = galleryInfo.simpleTags;
        this.simpleLanguage = galleryInfo.simpleLanguage;
    }

    public HistoryInfo(int mode, long time) {
        this.mode = mode;
        this.time = time;
    }

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleJpn() {
        return titleJpn;
    }

    public void setTitleJpn(String titleJpn) {
        this.titleJpn = titleJpn;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    public String getPosted() {
        return posted;
    }

    public void setPosted(String posted) {
        this.posted = posted;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public String getSimpleLanguage() {
        return simpleLanguage;
    }

    public void setSimpleLanguage(String simpleLanguage) {
        this.simpleLanguage = simpleLanguage;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.mode);
        dest.writeLong(this.time);
    }
}
