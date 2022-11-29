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
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.Native;
import com.hippo.UriArchiveAccessor;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.thread.PVLock;
import com.hippo.yorozuya.thread.PriorityThread;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    private final LinkedHashMap<Integer, ByteBuffer> streams = new LinkedHashMap<>();
    private final Thread[] decodeThread = new Thread[]{
            new Thread(new DecodeTask()),
            new Thread(new DecodeTask()),
            new Thread(new DecodeTask()),
            new Thread(new DecodeTask())
    };
    private Thread archiveThread;
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
                new ArchiveHostTask(), "ArchiveTask" + '-' + id, Process.THREAD_PRIORITY_BACKGROUND);
        archiveThread.start();
        for (Thread i : decodeThread) {
            i.start();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (archiveThread != null) {
            archiveThread.interrupt();
            archiveThread = null;
        }
        for (Thread i : decodeThread) {
            i.interrupt();
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
            inDecodeTask = streams.containsKey(index);
        }

        synchronized (requests) {
            boolean inArchiveTask = requests.contains(index);
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
        return FileUtils.getNameFromFilename(getImageFilenameWithExtension(index));
    }

    @NonNull
    @Override
    public String getImageFilenameWithExtension(int index) {
        return FileUtils.sanitizeFilename(archiveAccessor.getFilename(index));
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        int fd;
        FileOutputStream stream;
        try {
            stream = (FileOutputStream) file.openOutputStream();
            fd = Native.getFd(stream.getFD());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        archiveAccessor.extractToFd(index, fd);
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        String extension = FileUtils.getExtensionFromFilename(getImageFilenameWithExtension(index));
        UniFile dst = dir.subFile(null != extension ? filename + "." + extension : filename);
        save(index, dst);
        return dst;
    }

    private class ArchiveHostTask implements Runnable {
        public final Thread[] archiveThreads = new Thread[]{
                new Thread(new ArchiveTask()),
                new Thread(new ArchiveTask()),
                new Thread(new ArchiveTask()),
                new Thread(new ArchiveTask())
        };

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
            for (Thread i : archiveThreads) {
                i.start();
            }

            Object o = new Object();
            synchronized (o) {
                try {
                    o.wait();
                } catch (InterruptedException e) {
                    for (Thread i : archiveThreads) {
                        i.interrupt();
                    }
                }
            }
        }
    }

    private class ArchiveTask implements Runnable {
        @Override
        public void run() {
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
                }

                // Check index valid
                if (index < 0 || index >= size) {
                    notifyPageFailed(index, GetText.getString(R.string.error_out_of_range));
                    continue;
                }

                synchronized (streams) {
                    if (streams.get(index) != null) {
                        continue;
                    }
                }

                ByteBuffer buffer = archiveAccessor.extractToByteBuffer(index);

                synchronized (streams) {
                    streams.put(index, buffer);
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
                ByteBuffer buffer;
                synchronized (streams) {
                    if (streams.isEmpty()) {
                        try {
                            streams.wait();
                        } catch (InterruptedException e) {
                            break;
                        }
                        continue;
                    }

                    Iterator<Map.Entry<Integer, ByteBuffer>> iterator = streams.entrySet().iterator();
                    Map.Entry<Integer, ByteBuffer> entry = iterator.next();
                    iterator.remove();
                    index = entry.getKey();
                    buffer = entry.getValue();
                }

                Image image = null;
                if (buffer != null) {
                    try {
                        image = Image.decode(buffer, false, () -> {
                            archiveAccessor.releaseByteBuffer(buffer);
                            return null;
                        });
                    } catch (ImageDecoder.DecodeException e) {
                        archiveAccessor.releaseByteBuffer(buffer);
                        e.printStackTrace();
                    }
                }
                if (image != null) {
                    notifyPageSucceed(index, image);
                } else {
                    notifyPageFailed(index, GetText.getString(R.string.error_decoding_failed));
                }
            }
        }
    }
}