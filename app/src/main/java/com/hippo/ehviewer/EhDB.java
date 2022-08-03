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

package com.hippo.ehviewer;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.dao.DownloadDirname;
import com.hippo.ehviewer.dao.DownloadDirnameDao;
import com.hippo.ehviewer.dao.DownloadInfo;
import com.hippo.ehviewer.dao.DownloadLabel;
import com.hippo.ehviewer.dao.DownloadLabelDao;
import com.hippo.ehviewer.dao.DownloadsDao;
import com.hippo.ehviewer.dao.EhDatabase;
import com.hippo.ehviewer.dao.Filter;
import com.hippo.ehviewer.dao.HistoryDao;
import com.hippo.ehviewer.dao.HistoryInfo;
import com.hippo.ehviewer.dao.LocalFavoriteInfo;
import com.hippo.ehviewer.dao.LocalFavoritesDao;
import com.hippo.ehviewer.dao.QuickSearch;
import com.hippo.ehviewer.dao.QuickSearchDao;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.collect.SparseJLArray;

import java.util.ArrayList;
import java.util.List;

public class EhDB {

    private static final String TAG = EhDB.class.getSimpleName();

    private static final int MAX_HISTORY_COUNT = 200;

    private static EhDatabase db;

    private static boolean sHasOldDB;
    private static boolean sNewDB;

    private static void upgradeDB(SQLiteDatabase db, int oldVersion) {
        switch (oldVersion) {
            case 1: // 1 to 2, add FILTER
                db.execSQL("CREATE TABLE IF NOT EXISTS \"FILTER\" (" + //
                        "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                        "\"MODE\" INTEGER NOT NULL ," + // 1: mode
                        "\"TEXT\" TEXT," + // 2: text
                        "\"ENABLE\" INTEGER);"); // 3: enable
            case 2: // 2 to 3, add ENABLE column to table FILTER
                db.execSQL("CREATE TABLE " + "\"FILTER2\" (" +
                        "\"_id\" INTEGER PRIMARY KEY ," +
                        "\"MODE\" INTEGER NOT NULL ," +
                        "\"TEXT\" TEXT," +
                        "\"ENABLE\" INTEGER);");
                db.execSQL("INSERT INTO \"FILTER2\" (" +
                        "_id, MODE, TEXT, ENABLE)" +
                        "SELECT _id, MODE, TEXT, 1 FROM FILTER;");
                db.execSQL("DROP TABLE FILTER");
                db.execSQL("ALTER TABLE FILTER2 RENAME TO FILTER");
            case 3: // 3 to 4, add PAGE_FROM and PAGE_TO column to QUICK_SEARCH
                db.execSQL("CREATE TABLE " + "\"QUICK_SEARCH2\" (" +
                        "\"_id\" INTEGER PRIMARY KEY ," +
                        "\"NAME\" TEXT," +
                        "\"MODE\" INTEGER NOT NULL ," +
                        "\"CATEGORY\" INTEGER NOT NULL ," +
                        "\"KEYWORD\" TEXT," +
                        "\"ADVANCE_SEARCH\" INTEGER NOT NULL ," +
                        "\"MIN_RATING\" INTEGER NOT NULL ," +
                        "\"PAGE_FROM\" INTEGER NOT NULL ," +
                        "\"PAGE_TO\" INTEGER NOT NULL ," +
                        "\"TIME\" INTEGER NOT NULL );");
                db.execSQL("INSERT INTO \"QUICK_SEARCH2\" (" +
                        "_id, NAME, MODE, CATEGORY, KEYWORD, ADVANCE_SEARCH, MIN_RATING, PAGE_FROM, PAGE_TO, TIME)" +
                        "SELECT _id, NAME, MODE, CATEGORY, KEYWORD, ADVANCE_SEARCH, MIN_RATING, -1, -1, TIME FROM QUICK_SEARCH;");
                db.execSQL("DROP TABLE QUICK_SEARCH");
                db.execSQL("ALTER TABLE QUICK_SEARCH2 RENAME TO QUICK_SEARCH");
        }
    }

    public static void initialize(Context context) {
        sHasOldDB = context.getDatabasePath("data").exists();
        db = Room.databaseBuilder(context, EhDatabase.class, "eh.db").allowMainThreadQueries().build();
    }

    public static boolean needMerge() {
        return sNewDB && sHasOldDB;
    }

    public static void mergeOldDB(Context context) {
        sNewDB = false;

        OldDBHelper oldDBHelper = new OldDBHelper(context);
        SQLiteDatabase oldDB;
        try {
            oldDB = oldDBHelper.getReadableDatabase();
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            return;
        }

        // Get GalleryInfo list
        SparseJLArray<GalleryInfo> map = new SparseJLArray<>();
        try {
            Cursor cursor = oldDB.rawQuery("select * from " + OldDBHelper.TABLE_GALLERY, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        GalleryInfo gi = new GalleryInfo();
                        gi.gid = cursor.getInt(0);
                        gi.token = cursor.getString(1);
                        gi.title = cursor.getString(2);
                        gi.posted = cursor.getString(3);
                        gi.category = cursor.getInt(4);
                        gi.thumb = cursor.getString(5);
                        gi.uploader = cursor.getString(6);
                        try {
                            // In 0.6.x version, NaN is stored
                            gi.rating = cursor.getFloat(7);
                        } catch (Throwable e) {
                            ExceptionUtils.throwIfFatal(e);
                            gi.rating = -1.0f;
                        }

                        map.put(gi.gid, gi);

                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
        }

        // Merge local favorites
        try {
            Cursor cursor = oldDB.rawQuery("select * from " + OldDBHelper.TABLE_LOCAL_FAVOURITE, null);
            if (cursor != null) {
                LocalFavoritesDao dao = db.localFavoritesDao();
                if (cursor.moveToFirst()) {
                    long i = 0L;
                    while (!cursor.isAfterLast()) {
                        // Get GalleryInfo first
                        long gid = cursor.getInt(0);
                        GalleryInfo gi = map.get(gid);
                        if (gi == null) {
                            Log.e(TAG, "Can't get GalleryInfo with gid: " + gid);
                            cursor.moveToNext();
                            continue;
                        }

                        LocalFavoriteInfo info = new LocalFavoriteInfo(gi);
                        info.setTime(i);
                        dao.insert(info);
                        cursor.moveToNext();
                        i++;
                    }
                }
                cursor.close();
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
        }


        // Merge quick search
        try {
            Cursor cursor = oldDB.rawQuery("select * from " + OldDBHelper.TABLE_TAG, null);
            if (cursor != null) {
                QuickSearchDao dao = db.quickSearchDao();
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        QuickSearch quickSearch = new QuickSearch();

                        int mode = cursor.getInt(2);
                        String search = cursor.getString(4);
                        String tag = cursor.getString(7);
                        if (mode == ListUrlBuilder.MODE_UPLOADER && search != null &&
                                search.startsWith("uploader:")) {
                            search = search.substring("uploader:".length());
                        }

                        quickSearch.setTime(cursor.getInt(0));
                        quickSearch.setName(cursor.getString(1));
                        quickSearch.setMode(mode);
                        quickSearch.setCategory(cursor.getInt(3));
                        quickSearch.setKeyword(mode == ListUrlBuilder.MODE_TAG ? tag : search);
                        quickSearch.setAdvanceSearch(cursor.getInt(5));
                        quickSearch.setMinRating(cursor.getInt(6));

                        dao.insert(quickSearch);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
        }

        // Merge download info
        try {
            Cursor cursor = oldDB.rawQuery("select * from " + OldDBHelper.TABLE_DOWNLOAD, null);
            if (cursor != null) {
                DownloadsDao dao = db.downloadsDao();
                if (cursor.moveToFirst()) {
                    long i = 0L;
                    while (!cursor.isAfterLast()) {
                        // Get GalleryInfo first
                        long gid = cursor.getInt(0);
                        GalleryInfo gi = map.get(gid);
                        if (gi == null) {
                            Log.e(TAG, "Can't get GalleryInfo with gid: " + gid);
                            cursor.moveToNext();
                            continue;
                        }

                        DownloadInfo info = new DownloadInfo(gi);
                        int state = cursor.getInt(2);
                        int legacy = cursor.getInt(3);
                        if (state == DownloadInfo.STATE_FINISH && legacy > 0) {
                            state = DownloadInfo.STATE_FAILED;
                        }
                        info.setState(state);
                        info.setLegacy(legacy);
                        if (cursor.getColumnCount() == 5) {
                            info.setTime(cursor.getLong(4));
                        } else {
                            info.setTime(i);
                        }
                        dao.insert(info);
                        cursor.moveToNext();
                        i++;
                    }
                }
                cursor.close();
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
        }

        try {
            // Merge history info
            Cursor cursor = oldDB.rawQuery("select * from " + OldDBHelper.TABLE_HISTORY, null);
            if (cursor != null) {
                HistoryDao dao = db.historyDao();
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        // Get GalleryInfo first
                        long gid = cursor.getInt(0);
                        GalleryInfo gi = map.get(gid);
                        if (gi == null) {
                            Log.e(TAG, "Can't get GalleryInfo with gid: " + gid);
                            cursor.moveToNext();
                            continue;
                        }

                        HistoryInfo info = new HistoryInfo(gi);
                        info.setMode(cursor.getInt(1));
                        info.setTime(cursor.getLong(2));
                        dao.insert(info);
                        cursor.moveToNext();
                    }
                }
                cursor.close();
            }
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
        }

        try {
            oldDBHelper.close();
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
        }
    }

    public static synchronized List<DownloadInfo> getAllDownloadInfo() {
        DownloadsDao dao = db.downloadsDao();
        List<DownloadInfo> list = dao.list();
        // Fix state
        for (DownloadInfo info : list) {
            if (info.state == DownloadInfo.STATE_WAIT || info.state == DownloadInfo.STATE_DOWNLOAD) {
                info.state = DownloadInfo.STATE_NONE;
            }
        }
        return list;
    }

    // Insert or update
    public static synchronized void putDownloadInfo(DownloadInfo downloadInfo) {
        DownloadsDao dao = db.downloadsDao();
        if (null != dao.load(downloadInfo.gid)) {
            // Update
            dao.update(downloadInfo);
        } else {
            // Insert
            dao.insert(downloadInfo);
        }
    }

    public static synchronized void removeDownloadInfo(DownloadInfo downloadInfo) {
        db.downloadsDao().delete(downloadInfo);
    }

    @Nullable
    public static synchronized String getDownloadDirname(long gid) {
        DownloadDirnameDao dao = db.downloadDirnameDao();
        DownloadDirname raw = dao.load(gid);
        if (raw != null) {
            return raw.getDirname();
        } else {
            return null;
        }
    }

    /**
     * Insert or update
     */
    public static synchronized void putDownloadDirname(long gid, String dirname) {
        DownloadDirnameDao dao = db.downloadDirnameDao();
        DownloadDirname raw = dao.load(gid);
        if (raw != null) { // Update
            raw.setDirname(dirname);
            dao.update(raw);
        } else { // Insert
            raw = new DownloadDirname();
            raw.setGid(gid);
            raw.setDirname(dirname);
            dao.insert(raw);
        }
    }

    public static synchronized void removeDownloadDirname(long gid) {
        DownloadDirnameDao dao = db.downloadDirnameDao();
        dao.deleteByKey(gid);
    }

    public static synchronized void clearDownloadDirname() {
        DownloadDirnameDao dao = db.downloadDirnameDao();
        dao.deleteAll();
    }

    @NonNull
    public static synchronized List<DownloadLabel> getAllDownloadLabelList() {
        DownloadLabelDao dao = db.downloadLabelDao();
        return dao.list();
    }

    public static synchronized DownloadLabel addDownloadLabel(String label) {
        DownloadLabelDao dao = db.downloadLabelDao();
        DownloadLabel raw = new DownloadLabel();
        raw.setLabel(label);
        raw.setTime(System.currentTimeMillis());
        raw.setId(dao.insert(raw));
        return raw;
    }

    public static synchronized DownloadLabel addDownloadLabel(DownloadLabel raw) {
        // Reset id
        raw.setId(null);
        DownloadLabelDao dao = db.downloadLabelDao();
        raw.setId(dao.insert(raw));
        return raw;
    }

    public static synchronized void updateDownloadLabel(DownloadLabel raw) {
        DownloadLabelDao dao = db.downloadLabelDao();
        dao.update(raw);
    }

    public static synchronized void moveDownloadLabel(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        boolean reverse = fromPosition > toPosition;
        int offset = reverse ? toPosition : fromPosition;
        int limit = reverse ? fromPosition - toPosition + 1 : toPosition - fromPosition + 1;

        DownloadLabelDao dao = db.downloadLabelDao();
        List<DownloadLabel> list = dao.list(offset, limit);

        int step = reverse ? 1 : -1;
        int start = reverse ? limit - 1 : 0;
        int end = reverse ? 0 : limit - 1;
        long toTime = list.get(end).getTime();
        for (int i = end; reverse ? i < start : i > start; i += step) {
            list.get(i).setTime(list.get(i + step).getTime());
        }
        list.get(start).setTime(toTime);

        dao.update(list);
    }

    public static synchronized void removeDownloadLabel(DownloadLabel raw) {
        DownloadLabelDao dao = db.downloadLabelDao();
        dao.delete(raw);
    }

    public static synchronized List<GalleryInfo> getAllLocalFavorites() {
        LocalFavoritesDao dao = db.localFavoritesDao();
        List<LocalFavoriteInfo> list = dao.list();
        return new ArrayList<>(list);
    }

    public static synchronized List<GalleryInfo> searchLocalFavorites(String query) {
        LocalFavoritesDao dao = db.localFavoritesDao();
        List<LocalFavoriteInfo> list = dao.list();
        return new ArrayList<>(list);
    }

    public static synchronized void removeLocalFavorites(long gid) {
        db.localFavoritesDao().deleteByKey(gid);
    }

    public static synchronized void removeLocalFavorites(long[] gidArray) {
        LocalFavoritesDao dao = db.localFavoritesDao();
        for (long gid : gidArray) {
            dao.deleteByKey(gid);
        }
    }

    public static synchronized boolean containLocalFavorites(long gid) {
        LocalFavoritesDao dao = db.localFavoritesDao();
        return null != dao.load(gid);
    }

    public static synchronized void putLocalFavorites(GalleryInfo galleryInfo) {
        LocalFavoritesDao dao = db.localFavoritesDao();
        if (null == dao.load(galleryInfo.gid)) {
            LocalFavoriteInfo info;
            if (galleryInfo instanceof LocalFavoriteInfo) {
                info = (LocalFavoriteInfo) galleryInfo;
            } else {
                info = new LocalFavoriteInfo(galleryInfo);
                info.time = System.currentTimeMillis();
            }
            dao.insert(info);
        }
    }

    public static synchronized void putLocalFavorites(List<GalleryInfo> galleryInfoList) {
        for (GalleryInfo gi : galleryInfoList) {
            putLocalFavorites(gi);
        }
    }

    public static synchronized List<QuickSearch> getAllQuickSearch() {
        QuickSearchDao dao = db.quickSearchDao();
        return dao.list();
    }

    public static synchronized void insertQuickSearch(QuickSearch quickSearch) {
        QuickSearchDao dao = db.quickSearchDao();
        quickSearch.id = null;
        quickSearch.time = System.currentTimeMillis();
        quickSearch.id = dao.insert(quickSearch);
    }

    public static synchronized void importQuickSearch(List<QuickSearch> quickSearchList) {
        QuickSearchDao dao = db.quickSearchDao();
        for (QuickSearch quickSearch : quickSearchList) {
            dao.insert(quickSearch);
        }
    }

    public static synchronized void updateQuickSearch(QuickSearch quickSearch) {
        QuickSearchDao dao = db.quickSearchDao();
        dao.update(quickSearch);
    }

    public static synchronized void deleteQuickSearch(QuickSearch quickSearch) {
        QuickSearchDao dao = db.quickSearchDao();
        dao.delete(quickSearch);
    }

    public static synchronized void moveQuickSearch(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }

        boolean reverse = fromPosition > toPosition;
        int offset = reverse ? toPosition : fromPosition;
        int limit = reverse ? fromPosition - toPosition + 1 : toPosition - fromPosition + 1;

        QuickSearchDao dao = db.quickSearchDao();
        List<QuickSearch> list = dao.list(offset, limit);

        int step = reverse ? 1 : -1;
        int start = reverse ? limit - 1 : 0;
        int end = reverse ? 0 : limit - 1;
        long toTime = list.get(end).getTime();
        for (int i = end; reverse ? i < start : i > start; i += step) {
            list.get(i).setTime(list.get(i + step).getTime());
        }
        list.get(start).setTime(toTime);

        dao.update(list);
    }

    public static synchronized List<HistoryInfo> getHistoryLazyList() {
        return db.historyDao().list();
    }

    public static synchronized void putHistoryInfo(GalleryInfo galleryInfo) {
        HistoryDao dao = db.historyDao();
        HistoryInfo info = dao.load(galleryInfo.gid);
        if (null != info) {
            // Update time
            info.time = System.currentTimeMillis();
            dao.update(info);
        } else {
            // New history
            info = new HistoryInfo(galleryInfo);
            info.time = System.currentTimeMillis();
            dao.insert(info);
            List<HistoryInfo> list = dao.list(MAX_HISTORY_COUNT, -1);
            dao.delete(list);
        }
    }

    public static synchronized void putHistoryInfo(List<HistoryInfo> historyInfoList) {
        HistoryDao dao = db.historyDao();
        for (HistoryInfo info : historyInfoList) {
            if (null == dao.load(info.gid)) {
                dao.insert(info);
            }
        }

        List<HistoryInfo> list = dao.list(MAX_HISTORY_COUNT, -1);
        dao.delete(list);
    }

    public static synchronized void deleteHistoryInfo(HistoryInfo info) {
        HistoryDao dao = db.historyDao();
        dao.delete(info);
    }

    public static synchronized void clearHistoryInfo() {
        HistoryDao dao = db.historyDao();
        dao.deleteAll();
    }

    public static synchronized List<Filter> getAllFilter() {
        return db.filterDao().list();
    }

    public static synchronized boolean addFilter(Filter filter) {
        db.filterDao().delete(filter);
        db.filterDao().insert(filter);
        return true;
    }

    public static synchronized void deleteFilter(Filter filter) {
        db.filterDao().delete(filter);
    }

    public static synchronized void triggerFilter(Filter filter) {
        filter.setEnable(!filter.enable);
        db.filterDao().update(filter);
    }

    public static synchronized boolean exportDB(Context context, Uri uri) {
        return false;
    }

    /**
     * @return error string, null for no error
     */
    public static synchronized String importDB(Context context, Uri uri) {
        return "";
    }

    private static class OldDBHelper extends SQLiteOpenHelper {

        private static final String DB_NAME = "data";
        private static final int VERSION = 4;

        private static final String TABLE_GALLERY = "gallery";
        private static final String TABLE_LOCAL_FAVOURITE = "local_favourite";
        private static final String TABLE_TAG = "tag";
        private static final String TABLE_DOWNLOAD = "download";
        private static final String TABLE_HISTORY = "history";

        public OldDBHelper(Context context) {
            super(context, DB_NAME, null, VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
