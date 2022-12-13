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

package com.hippo.ehviewer.gallery;

import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.image.Image;
import com.hippo.unifile.FilenameFilter;
import com.hippo.unifile.UniFile;
import com.hippo.util.NaturalComparator;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;
import com.hippo.yorozuya.StringUtils;
import com.hippo.yorozuya.thread.PriorityThread;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import eu.kanade.tachiyomi.ui.reader.loader.PageLoader;

public class DirPageLoader extends PageLoader2 implements Runnable {

    private static final String TAG = DirPageLoader.class.getSimpleName();
    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    private static final FilenameFilter imageFilter =
            (dir, name) -> StringUtils.endsWith(name.toLowerCase(), SUPPORT_IMAGE_EXTENSIONS);
    private static final Comparator<UniFile> naturalComparator = new Comparator<UniFile>() {
        private final NaturalComparator comparator = new NaturalComparator();

        @Override
        public int compare(UniFile o1, UniFile o2) {
            return comparator.compare(o1.getName(), o2.getName());
        }
    };
    private final UniFile mDir;
    private final Stack<Integer> mRequests = new Stack<>();
    private final AtomicInteger mDecodingIndex = new AtomicInteger(-1);
    private final AtomicReference<UniFile[]> mFileList = new AtomicReference<>();
    @Nullable
    private Thread mBgThread;
    private volatile int mSize = PageLoader.STATE_WAIT;
    private String mError;

    public DirPageLoader(@NonNull UniFile dir) {
        mDir = dir;
    }

    @Override
    public void start() {
        mBgThread = new PriorityThread(this, TAG + '-' + sIdGenerator.incrementAndGet(),
                Process.THREAD_PRIORITY_BACKGROUND);
        mBgThread.start();
    }

    @Override
    public void stop() {
        if (mBgThread != null) {
            mBgThread.interrupt();
            mBgThread = null;
        }
    }

    @Override
    public int size() {
        return mSize;
    }

    @Override
    protected void onRequest(int index) {
        synchronized (mRequests) {
            if (!mRequests.contains(index) && index != mDecodingIndex.get()) {
                mRequests.add(index);
                mRequests.notify();
            }
        }
        notifyPageWait(index);
    }

    @Override
    protected void onForceRequest(int index) {
        onRequest(index);
    }

    @Override
    public void onCancelRequest(int index) {
        synchronized (mRequests) {
            mRequests.remove(Integer.valueOf(index));
        }
    }

    @Override
    public String getError() {
        return mError;
    }

    @NonNull
    @Override
    public String getImageFilename(int index) {
        UniFile[] fileList = mFileList.get();
        if (null == fileList || index < 0 || index >= fileList.length) {
            return Integer.toString(index);
        }
        UniFile src = fileList[index];
        String name = src.getName();
        if (name == null) {
            return Integer.toString(index);
        }
        return name;
    }

    @NonNull
    @Override
    public String getImageFilenameWithExtension(int index) {
        UniFile[] fileList = mFileList.get();
        if (null == fileList || index < 0 || index >= fileList.length) {
            return Integer.toString(index);
        }
        UniFile src = fileList[index];
        String extension = FileUtils.getExtensionFromFilename(src.getName());
        String name = src.getName();
        if (name == null) {
            return Integer.toString(index);
        }
        name = null != extension ? name + "." + extension : name;
        return name;
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        UniFile[] fileList = mFileList.get();
        if (null == fileList || index < 0 || index >= fileList.length) {
            return false;
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            is = fileList[index].openInputStream();
            os = file.openOutputStream();
            IOUtils.copy(is, os);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        UniFile[] fileList = mFileList.get();
        if (null == fileList || index < 0 || index >= fileList.length) {
            return null;
        }

        UniFile src = fileList[index];
        String extension = FileUtils.getExtensionFromFilename(src.getName());
        UniFile dst = dir.subFile(null != extension ? filename + "." + extension : filename);
        if (null == dst) {
            return null;
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            is = src.openInputStream();
            os = dst.openOutputStream();
            IOUtils.copy(is, os);
            return dst;
        } catch (IOException e) {
            return null;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public void run() {
        // It may take a long time, so run it in new thread
        UniFile[] files = mDir.listFiles(imageFilter);

        if (files == null) {
            mSize = PageLoader.STATE_ERROR;
            mError = GetText.getString(R.string.error_not_folder_path);

            // Notify to to show error
            notifyDataChanged();

            Log.i(TAG, "ImageDecoder end with error");
            return;
        }

        // Sort it
        Arrays.sort(files, naturalComparator);

        // Put file list
        mFileList.lazySet(files);

        // Set state normal and notify
        mSize = files.length;
        notifyDataChanged();

        while (!Thread.currentThread().isInterrupted()) {
            int index;
            synchronized (mRequests) {
                if (mRequests.isEmpty()) {
                    try {
                        mRequests.wait();
                    } catch (InterruptedException e) {
                        // Interrupted
                        break;
                    }
                    continue;
                }
                index = mRequests.pop();
                mDecodingIndex.lazySet(index);
            }

            // Check index valid
            if (index < 0 || index >= files.length) {
                mDecodingIndex.lazySet(-1);
                notifyPageFailed(index);
                continue;
            }

            InputStream is = null;
            try {
                is = files[index].openInputStream();
                Image image = Image.decode((FileInputStream) is);
                mDecodingIndex.lazySet(-1);
                notifyPageSucceed(index, image);
            } catch (IOException e) {
                mDecodingIndex.lazySet(-1);
                notifyPageFailed(index);
            } finally {
                IOUtils.closeQuietly(is);
            }
            mDecodingIndex.lazySet(-1);
        }

        // Clear file list
        mFileList.lazySet(null);

        Log.i(TAG, "ImageDecoder end");
    }
}
