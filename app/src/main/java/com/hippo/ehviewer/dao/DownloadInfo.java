package com.hippo.ehviewer.dao;

import android.os.Parcel;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.hippo.ehviewer.client.data.GalleryInfo;

@Entity(tableName = "DOWNLOADS")
public class DownloadInfo extends GalleryInfo {
    public static final Creator<DownloadInfo> CREATOR = new Creator<>() {
        @Override
        public DownloadInfo createFromParcel(Parcel source) {
            return new DownloadInfo(source);
        }

        @Override
        public DownloadInfo[] newArray(int size) {
            return new DownloadInfo[size];
        }
    };
    public static final int STATE_INVALID = -1;
    public static final int STATE_NONE = 0;
    public static final int STATE_WAIT = 1;
    public static final int STATE_DOWNLOAD = 2;
    public static final int STATE_FINISH = 3;
    public static final int STATE_FAILED = 4;
    @ColumnInfo(name = "STATE")
    public int state;
    @ColumnInfo(name = "LEGACY")
    public int legacy;
    @ColumnInfo(name = "TIME")
    public long time;
    @ColumnInfo(name = "LABEL")
    public String label;
    @Ignore
    public long speed;
    @Ignore
    public long remaining;
    @Ignore
    public int finished;
    @Ignore
    public int downloaded;
    @Ignore
    public int total;

    DownloadInfo() {
    }

    protected DownloadInfo(Parcel in) {
        super(in);
        this.state = in.readInt();
        this.legacy = in.readInt();
        this.time = in.readLong();
        this.label = in.readString();
    }

    public DownloadInfo(GalleryInfo galleryInfo) {
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

    public DownloadInfo(int state, int legacy, long time, String label, long speed, long remaining,
                        int finished, int downloaded, int total) {
        this.state = state;
        this.legacy = legacy;
        this.time = time;
        this.label = label;
        this.speed = speed;
        this.remaining = remaining;
        this.finished = finished;
        this.downloaded = downloaded;
        this.total = total;
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

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getLegacy() {
        return legacy;
    }

    public void setLegacy(int legacy) {
        this.legacy = legacy;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.state);
        dest.writeInt(this.legacy);
        dest.writeLong(this.time);
        dest.writeString(this.label);
    }

    public long getSpeed() {
        return this.speed;
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getRemaining() {
        return this.remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public int getFinished() {
        return this.finished;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public int getDownloaded() {
        return this.downloaded;
    }

    public void setDownloaded(int downloaded) {
        this.downloaded = downloaded;
    }

    public int getTotal() {
        return this.total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
