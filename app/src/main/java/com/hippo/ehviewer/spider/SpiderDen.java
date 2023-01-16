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

package com.hippo.ehviewer.spider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.gallery.PageLoader2;
import com.hippo.io.UniFileInputStreamPipe;
import com.hippo.io.UniFileOutputStreamPipe;
import com.hippo.streampipe.InputStreamPipe;
import com.hippo.streampipe.OutputStreamPipe;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import coil.disk.DiskCache;
import okio.BufferedSource;
import okio.Okio;

public final class SpiderDen {

    @Nullable
    private static DiskCache sCache; // We use data to store image file, and metadata for image type
    private final GalleryInfo mGalleryInfo;
    private final long mGid;
    @Nullable
    private UniFile mDownloadDir;
    private volatile int mMode = SpiderQueen.MODE_READ;

    public SpiderDen(GalleryInfo galleryInfo) {
        mGalleryInfo = galleryInfo;
        mGid = galleryInfo.getGid();
        mDownloadDir = getGalleryDownloadDir(galleryInfo.getGid());
    }

    public static void initialize(Context context) {
        sCache = new DiskCache.Builder().directory(new File(context.getCacheDir(), "gallery_image"))
                .maxSizeBytes((long) MathUtils.clamp(Settings.getReadCacheSize(), 40, 1280) * 1024 * 1024).build();
    }

    public static UniFile getGalleryDownloadDir(long gid) {
        UniFile dir = Settings.getDownloadLocation();
        // Read from DB
        String dirname = EhDB.getDownloadDirname(gid);
        if (dir != null && dirname != null) {
            // Some dirname may be invalid in some version
            dirname = FileUtils.sanitizeFilename(dirname);
            EhDB.putDownloadDirname(gid, dirname);
            return dir.subFile(dirname);
        } else {
            return null;
        }
    }

    /**
     * @param extension with dot
     */
    public static String generateImageFilename(int index, String extension) {
        return String.format(Locale.US, "%08d%s", index + 1, extension);
    }

    @Nullable
    private static UniFile findImageFile(UniFile dir, int index) {
        for (String extension : PageLoader2.SUPPORT_IMAGE_EXTENSIONS) {
            String filename = generateImageFilename(index, extension);
            UniFile file = dir.findFile(filename);
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    public void setMode(@SpiderQueen.Mode int mode) {
        mMode = mode;

        if (mode == SpiderQueen.MODE_DOWNLOAD) {
            ensureDownloadDir();
        }
    }

    private boolean ensureDownloadDir() {
        if (mDownloadDir == null) {
            var dirname = FileUtils.sanitizeFilename(mGid + "-" + EhUtils.getSuitableTitle(mGalleryInfo));
            EhDB.putDownloadDirname(mGid, dirname);
            mDownloadDir = getGalleryDownloadDir(mGid);
        }
        return mDownloadDir != null && mDownloadDir.ensureDir();
    }

    public boolean isReady() {
        return switch (mMode) {
            case SpiderQueen.MODE_READ -> sCache != null;
            case SpiderQueen.MODE_DOWNLOAD -> mDownloadDir != null && mDownloadDir.isDirectory();
            default -> false;
        };
    }

    @Nullable
    public UniFile getDownloadDir() {
        if (mDownloadDir == null) {
            mDownloadDir = getGalleryDownloadDir(mGid);
        }
        return mDownloadDir != null && mDownloadDir.isDirectory() ? mDownloadDir : null;
    }

    private boolean containInCache(int index) {
        if (sCache == null) {
            return false;
        }

        String key = EhCacheKeyFactory.getImageKey(mGid, index);
        var snapshot = UtilsKt.getSnapShotUninterruptible(key, sCache);
        var exist = snapshot != null;
        if (exist) snapshot.close();
        return exist;
    }

    private boolean containInDownloadDir(int index) {
        UniFile dir = getDownloadDir();
        if (dir == null) {
            return false;
        }

        // Find image file in download dir
        return findImageFile(dir, index) != null;
    }

    /**
     * @param extension with dot
     */
    private String fixExtension(String extension) {
        if (Utilities.contain(PageLoader2.SUPPORT_IMAGE_EXTENSIONS, extension)) {
            return extension;
        } else {
            return PageLoader2.SUPPORT_IMAGE_EXTENSIONS[0];
        }
    }

    private boolean copyFromCacheToDownloadDir(int index) {
        if (sCache == null) {
            return false;
        }
        UniFile dir = getDownloadDir();
        if (dir == null) {
            return false;
        }
        // Find image file in cache
        String key = EhCacheKeyFactory.getImageKey(mGid, index);
        var snapshot = UtilsKt.getSnapShotUninterruptible(key, sCache);
        if (snapshot == null) {
            return false;
        }

        OutputStream os = null;
        try (snapshot; FileInputStream is = new FileInputStream(snapshot.getData().toFile()); BufferedSource buf = Okio.buffer(Okio.source(snapshot.getMetadata().toFile()))) {
            // Get extension
            String extension = buf.readUtf8Line();
            // Copy from cache to download dir
            UniFile file = dir.createFile(generateImageFilename(index, extension));
            if (file == null) {
                return false;
            }
            os = file.openOutputStream();
            IOUtils.copy(is, os);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    public boolean contain(int index) {
        if (mMode == SpiderQueen.MODE_READ) {
            return containInCache(index) || containInDownloadDir(index);
        } else if (mMode == SpiderQueen.MODE_DOWNLOAD) {
            return containInDownloadDir(index) || copyFromCacheToDownloadDir(index);
        } else {
            return false;
        }
    }

    private boolean removeFromCache(int index) {
        if (sCache == null) {
            return false;
        }

        String key = EhCacheKeyFactory.getImageKey(mGid, index);
        return UtilsKt.removeUninterruptible(key, sCache);
    }

    private boolean removeFromDownloadDir(int index) {
        UniFile dir = getDownloadDir();
        if (dir == null) {
            return false;
        }

        boolean result = false;
        for (int i = 0, n = PageLoader2.SUPPORT_IMAGE_EXTENSIONS.length; i < n; i++) {
            String filename = generateImageFilename(index, PageLoader2.SUPPORT_IMAGE_EXTENSIONS[i]);
            UniFile file = dir.subFile(filename);
            if (file != null) {
                result |= file.delete();
            }
        }
        return result;
    }

    public boolean remove(int index) {
        boolean result = removeFromCache(index);
        result |= removeFromDownloadDir(index);
        return result;
    }

    @Nullable
    private OutputStreamPipe openCacheOutputStreamPipe(int index, @Nullable String extension) {
        if (sCache == null) {
            return null;
        }

        String key = EhCacheKeyFactory.getImageKey(mGid, index);
        var editor = UtilsKt.getEditorUninterruptible(key, sCache);
        if (editor == null) return null;
        try (var sink = Okio.buffer(Okio.sink(editor.getMetadata().toFile()))) {
            sink.writeUtf8(extension);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new OutputStreamPipe() {
            @Override
            public void obtain() {
            }

            @Override
            public void release() {
            }

            @NonNull
            @Override
            public OutputStream open() throws IOException {
                return new FileOutputStream(editor.getData().toFile());
            }

            @Override
            public void close() {
                UtilsKt.completeEditorUninterruptible(editor);
            }
        };
    }

    /**
     * @param extension without dot
     */
    @Nullable
    private OutputStreamPipe openDownloadOutputStreamPipe(int index, @Nullable String extension) {
        UniFile dir = getDownloadDir();
        if (dir == null) {
            return null;
        }

        extension = fixExtension('.' + extension);
        UniFile file = dir.createFile(generateImageFilename(index, extension));
        if (file != null) {
            return new UniFileOutputStreamPipe(file);
        } else {
            return null;
        }
    }

    @Nullable
    public OutputStreamPipe openOutputStreamPipe(int index, @Nullable String extension) {
        if (mMode == SpiderQueen.MODE_READ) {
            // Return the download pipe is the gallery has been downloaded
            OutputStreamPipe pipe = openDownloadOutputStreamPipe(index, extension);
            if (pipe == null) {
                pipe = openCacheOutputStreamPipe(index, extension);
            }
            return pipe;
        } else if (mMode == SpiderQueen.MODE_DOWNLOAD) {
            return openDownloadOutputStreamPipe(index, extension);
        } else {
            return null;
        }
    }

    @Nullable
    private InputStreamPipe openCacheInputStreamPipe(int index) {
        if (sCache == null) {
            return null;
        }

        String key = EhCacheKeyFactory.getImageKey(mGid, index);
        var snapshot = UtilsKt.getSnapShotUninterruptible(key, sCache);
        if (snapshot == null) return null;
        return new InputStreamPipe() {
            @Override
            public void obtain() {

            }

            @Override
            public void release() {

            }

            @NonNull
            @Override
            public InputStream open() throws IOException {
                return new FileInputStream(snapshot.getData().toFile());
            }

            @Override
            public void close() {
                UtilsKt.closeSnapShotUninterruptible(snapshot);
            }
        };
    }

    @Nullable
    public InputStreamPipe openDownloadInputStreamPipe(int index) {
        UniFile dir = getDownloadDir();
        if (dir == null) {
            return null;
        }

        for (int i = 0; i < 2; i++) {
            UniFile file = findImageFile(dir, index);
            if (file != null) {
                return new UniFileInputStreamPipe(file);
            } else if (!copyFromCacheToDownloadDir(index)) {
                return null;
            }
        }

        return null;
    }

    @Nullable
    public InputStreamPipe openInputStreamPipe(int index) {
        if (mMode == SpiderQueen.MODE_READ) {
            InputStreamPipe pipe = openCacheInputStreamPipe(index);
            if (pipe == null) {
                pipe = openDownloadInputStreamPipe(index);
            }
            return pipe;
        } else if (mMode == SpiderQueen.MODE_DOWNLOAD) {
            return openDownloadInputStreamPipe(index);
        } else {
            return null;
        }
    }

}
