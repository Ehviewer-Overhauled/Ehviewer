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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.room.Room;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.dao.BasicDao;
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
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class EhDB {
    private static final int CUR_DB_VER = 4;

    private static EhDatabase db;

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
        db = Room.databaseBuilder(context, EhDatabase.class, "eh.db").allowMainThreadQueries().build();
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

    public static synchronized List<HistoryInfo> getHistoryList() {
        return db.historyDao().list();
    }

    public static synchronized PagingSource<Integer, HistoryInfo> getHistoryLazyList() {
        return db.historyDao().listLazy();
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
        }
    }

    public static synchronized void putHistoryInfo(List<HistoryInfo> historyInfoList) {
        HistoryDao dao = db.historyDao();
        for (HistoryInfo info : historyInfoList) {
            if (null == dao.load(info.gid)) {
                dao.insert(info);
            }
        }
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
        Filter existFilter;
        try {
            existFilter = db.filterDao().load(filter.text, filter.mode);
        } catch (Exception e) {
            existFilter = null;
        }
        if (existFilter == null) {
            filter.setId(null);
            filter.setId(db.filterDao().insert(filter));
            return true;
        } else {
            return false;
        }
    }

    public static synchronized void deleteFilter(Filter filter) {
        db.filterDao().delete(filter);
    }

    public static synchronized void triggerFilter(Filter filter) {
        filter.setEnable(!filter.enable);
        db.filterDao().update(filter);
    }

    private static <T> boolean copyDao(BasicDao<T> from, BasicDao<T> to) {
        List<T> list = from.list();
        for (T item : list)
            to.insert(item);
        return false;
    }

    public static synchronized boolean exportDB(Context context, Uri uri) {
        final String ehExportName = "eh.export.db";

        // Delete old export db
        context.deleteDatabase(ehExportName);
        EhDatabase newDb = Room.databaseBuilder(context, EhDatabase.class, ehExportName).allowMainThreadQueries().build();

        try {
            // Copy data to a export db
            if (copyDao(db.downloadsDao(), newDb.downloadsDao()))
                return false;
            if (copyDao(db.downloadLabelDao(), newDb.downloadLabelDao()))
                return false;
            if (copyDao(db.downloadDirnameDao(), newDb.downloadDirnameDao()))
                return false;
            if (copyDao(db.historyDao(), newDb.historyDao()))
                return false;
            if (copyDao(db.quickSearchDao(), newDb.quickSearchDao()))
                return false;
            if (copyDao(db.localFavoritesDao(), newDb.localFavoritesDao()))
                return false;
            if (copyDao(db.filterDao(), newDb.filterDao()))
                return false;

            // Copy export db to data dir
            File dbFile = context.getDatabasePath(ehExportName);
            if (dbFile == null || !dbFile.isFile()) {
                return false;
            }
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new FileInputStream(dbFile);
                os = context.getContentResolver().openOutputStream(uri);
                IOUtils.copy(is, os);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
            }
            // Delete failed file
            return false;
        } finally {
            context.deleteDatabase(ehExportName);
        }
    }

    /**
     * @return error string, null for no error
     */
    public static synchronized String importDB(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            File file = File.createTempFile("importDatabase", "");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buff = new byte[1024];
            int read;
            if (inputStream != null) {
                while ((read = inputStream.read(buff, 0, buff.length)) > 0) {
                    outputStream.write(buff, 0, read);
                }
            } else {
                return context.getString(R.string.cant_read_the_file);
            }
            inputStream.close();
            outputStream.close();

            SQLiteDatabase oldDB = SQLiteDatabase.openDatabase(
                    file.getPath(), null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            int newVersion = CUR_DB_VER;
            int oldVersion = oldDB.getVersion();
            if (oldVersion < newVersion) {
                upgradeDB(oldDB, oldVersion);
                oldDB.setVersion(newVersion);
            } else if (oldVersion > newVersion) {
                return context.getString(R.string.cant_read_the_file);
            }

            String tmpDBName = "tmp.db";
            context.deleteDatabase(tmpDBName);
            EhDatabase oldRoomDatabase = Room.databaseBuilder(context, EhDatabase.class, tmpDBName)
                    .createFromFile(file).allowMainThreadQueries().build();

            // Download label
            DownloadManager manager = EhApplication.getDownloadManager();
            try {
                List<DownloadLabel> downloadLabelList = oldRoomDatabase.downloadLabelDao().list();
                manager.addDownloadLabel(downloadLabelList);
            } catch (Exception ignored) {
            }

            // Downloads
            try {
                List<DownloadInfo> downloadInfoList = oldRoomDatabase.downloadsDao().list();
                manager.addDownload(downloadInfoList, false);
            } catch (Exception ignored) {
            }

            // Download dirname
            try {
                List<DownloadDirname> downloadDirnameList = oldRoomDatabase.downloadDirnameDao().list();
                for (DownloadDirname dirname : downloadDirnameList) {
                    putDownloadDirname(dirname.getGid(), dirname.getDirname());
                }
            } catch (Exception ignored) {
            }

            // History
            try {
                List<HistoryInfo> historyInfoList = oldRoomDatabase.historyDao().list();
                putHistoryInfo(historyInfoList);
            } catch (Exception ignored) {
            }

            // QuickSearch
            try {
                List<QuickSearch> quickSearchList = oldRoomDatabase.quickSearchDao().list();
                List<QuickSearch> currentQuickSearchList = db.quickSearchDao().list();
                List<QuickSearch> importList = new ArrayList<>();
                for (QuickSearch quickSearch : quickSearchList) {
                    String name = quickSearch.name;
                    for (QuickSearch q : currentQuickSearchList) {
                        if (ObjectUtils.equal(q.name, name)) {
                            // The same name
                            name = null;
                            break;
                        }
                    }
                    if (null == name) {
                        continue;
                    }
                    importList.add(quickSearch);
                }
                importQuickSearch(importList);
            } catch (Exception ignored) {
            }

            // LocalFavorites
            try {
                List<LocalFavoriteInfo> localFavoriteInfoList = oldRoomDatabase.localFavoritesDao().list();
                for (LocalFavoriteInfo info : localFavoriteInfoList) {
                    putLocalFavorites(info);
                }
            } catch (Exception ignored) {
            }

            // Filter
            try {
                List<Filter> filterList = oldRoomDatabase.filterDao().list();
                List<Filter> currentFilterList = db.filterDao().list();
                for (Filter filter : filterList) {
                    if (!currentFilterList.contains(filter)) {
                        addFilter(filter);
                    }
                }
            } catch (Exception ignored) {
            }

            return null;
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            // Ignore
            return context.getString(R.string.cant_read_the_file);
        }
    }
}
