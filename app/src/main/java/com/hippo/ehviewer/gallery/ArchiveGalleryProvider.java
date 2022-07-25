/*
 * Copyright 2019 Hippo Seven
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

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.UriArchiveAccessor;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.glgallery.GalleryPageView;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.thread.PVLock;
import com.hippo.yorozuya.thread.PriorityThread;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class ArchiveGalleryProvider extends GalleryProvider2 {
    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    public static Handler showPasswd;
    public static String passwd;
    public static PVLock pv = new PVLock(0);
    private final UriArchiveAccessor archiveAccessor;
    private final Stack<Integer> requests = new Stack<>();
    private final AtomicInteger extractingIndex = new AtomicInteger(GalleryPageView.INVALID_INDEX);
    private final LinkedHashMap<Integer, Long> streams = new LinkedHashMap<>();
    private final AtomicInteger decodingIndex = new AtomicInteger(GalleryPageView.INVALID_INDEX);
    private Thread archiveThread;
    private Thread decodeThread;
    private volatile int size = STATE_WAIT;
    private String error;

    public ArchiveGalleryProvider(Context context, Uri uri) {
        UriArchiveAccessor archiveAccessor1;
        try {
            archiveAccessor1 = new UriArchiveAccessor(context, uri);
        } catch (Exception e) {
            archiveAccessor1 = null;
            e.printStackTrace();
        }
        archiveAccessor = archiveAccessor1;
    }

    @Override
    public void start() {
        super.start();

        int id = sIdGenerator.incrementAndGet();

        archiveThread = new PriorityThread(
                new ArchiveTask(), "ArchiveTask" + '-' + id, Process.THREAD_PRIORITY_BACKGROUND);
        archiveThread.start();

        decodeThread = new PriorityThread(
                new DecodeTask(), "DecodeTask" + '-' + id, Process.THREAD_PRIORITY_BACKGROUND);
        decodeThread.start();
    }

    @Override
    public void stop() {
        super.stop();

        if (archiveThread != null) {
            archiveThread.interrupt();
            archiveThread = null;
        }
        if (decodeThread != null) {
            decodeThread.interrupt();
            decodeThread = null;
        }
        try {
            archiveAccessor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    protected void onRequest(int index) {
        boolean inDecodeTask;
        synchronized (streams) {
            inDecodeTask = streams.containsKey(index) || index == decodingIndex.get();
        }

        synchronized (requests) {
            boolean inArchiveTask = requests.contains(index) || index == extractingIndex.get();
            if (!inArchiveTask && !inDecodeTask) {
                requests.add(index);
                requests.notify();
            }
        }
        notifyPageWait(index);
    }

    @Override
    protected void onForceRequest(int index) {
        onRequest(index);
    }

    @Override
    protected void onCancelRequest(int index) {
        synchronized (requests) {
            requests.remove(Integer.valueOf(index));
        }
    }

    @Override
    public String getError() {
        return error;
    }

    @NonNull
    @Override
    public String getImageFilename(int index) {
        // TODO
        return Integer.toString(index);
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        // TODO
        return false;
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        // TODO
        return null;
    }

    @NonNull
    @Override
    public String getImageFilenameWithExtension(int index) {
        // TODO
        return Integer.toString(index);
    }

    private class ArchiveTask implements Runnable {
        private void waitPasswd() throws InterruptedException {
            showPasswd.sendEmptyMessage(0);
            pv.p();
        }

        private void notifyError() {
            showPasswd.sendEmptyMessage(1);
        }

        private void notifyDismiss() {
            showPasswd.sendEmptyMessage(2);
        }

        @Override
        public void run() {
            try {
                size = archiveAccessor.open();
            } catch (Exception e) {
                e.printStackTrace();
                size = 0;
            }
            if (size <= 0) {
                size = STATE_ERROR;
                error = GetText.getString(R.string.error_reading_failed);
                notifyDataChanged();
                return;
            }
            // Update size and notify changed
            notifyDataChanged();

            if (archiveAccessor.needPassword()) {
                boolean need_request = true;
                Set<String> set = Settings.getArchivePasswds();
                if (set != null) {
                    for (String passwd : set) {
                        if (archiveAccessor.providePassword(passwd)) {
                            need_request = false;
                            break;
                        }
                    }
                }
                while (!Thread.currentThread().isInterrupted() && need_request) {
                    try {
                        waitPasswd();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                    if (!archiveAccessor.providePassword(passwd))
                        notifyError();
                    else {
                        Settings.putPasswdToArchivePasswds(passwd);
                        notifyDismiss();
                        break;
                    }
                }
            }

            while (!Thread.currentThread().isInterrupted()) {
                int index;
                synchronized (requests) {
                    if (requests.isEmpty()) {
                        try {
                            requests.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            break;
                        }
                        continue;
                    }
                    index = requests.pop();
                    extractingIndex.lazySet(index);
                }

                // Check index valid
                if (index < 0 || index >= size) {
                    extractingIndex.lazySet(GalleryPageView.INVALID_INDEX);
                    notifyPageFailed(index, GetText.getString(R.string.error_out_of_range));
                    continue;
                }

                synchronized (streams) {
                    if (streams.get(index) != null) {
                        continue;
                    }
                }

                long addr = archiveAccessor.extracttoOutputStream(index);

                synchronized (streams) {
                    streams.put(index, addr);
                    streams.notify();
                }
            }
        }
    }

    private class DecodeTask implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                int index;
                Long addr;
                synchronized (streams) {
                    if (streams.isEmpty()) {
                        try {
                            streams.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            break;
                        }
                        continue;
                    }

                    Iterator<Map.Entry<Integer, Long>> iterator = streams.entrySet().iterator();
                    Map.Entry<Integer, Long> entry = iterator.next();
                    iterator.remove();
                    index = entry.getKey();
                    addr = entry.getValue();
                    decodingIndex.lazySet(index);
                }

                try {
                    Image image = Image.decodeAddr(addr, true);
                    if (image != null) {
                        notifyPageSucceed(index, image);
                    } else {
                        notifyPageFailed(index, GetText.getString(R.string.error_decoding_failed));
                    }
                } finally {
                    decodingIndex.lazySet(GalleryPageView.INVALID_INDEX);
                }
            }
        }
    }
}