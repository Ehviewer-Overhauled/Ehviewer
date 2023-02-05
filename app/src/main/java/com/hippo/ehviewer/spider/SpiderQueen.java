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

import android.os.Process;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.collection.LongSparseArray;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhRequestBuilder;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.parser.GalleryMultiPageViewerPTokenParser;
import com.hippo.ehviewer.client.parser.GalleryPageUrlParser;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.util.ExceptionUtils;
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.thread.PriorityThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import eu.kanade.tachiyomi.ui.reader.loader.PageLoader;
import kotlinx.coroutines.CoroutineScopeKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class SpiderQueen implements Runnable {

    public static final int MODE_READ = 0;
    public static final int MODE_DOWNLOAD = 1;
    public static final int STATE_NONE = 0;
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_FAILED = 3;
    public static final int DECODE_THREAD_NUM = 1;
    public static final String SPIDER_INFO_FILENAME = ".ehviewer";
    private static final String TAG = SpiderQueen.class.getSimpleName();
    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    private static final boolean DEBUG_LOG = true;
    private static final boolean DEBUG_PTOKEN = true;
    private static final String[] URL_509_SUFFIX_ARRAY = {
            "/509.gif",
            "/509s.gif"
    };
    private static final LongSparseArray<SpiderQueen> sQueenMap = new LongSparseArray<>();
    public static String PTOKEN_FAILED_MESSAGE = GetText.getString(R.string.error_get_ptoken_error);
    public static String ERROR_509 = GetText.getString(R.string.error_509);
    public static String NETWORK_ERROR = GetText.getString(R.string.error_socket);
    @NonNull
    public final SpiderDen mSpiderDen;
    public final AtomicReference<SpiderInfo> mSpiderInfo = new AtomicReference<>();
    // Store page download percent
    public final ConcurrentHashMap<Integer, Float> mPagePercentMap = new ConcurrentHashMap<>();
    @NonNull
    private final OkHttpClient mHttpClient;
    @NonNull
    private final GalleryInfo mGalleryInfo;
    private final Object mQueenLock = new Object();
    private final Thread[] mDecodeThreadArray = new Thread[DECODE_THREAD_NUM];
    private final int[] mDecodeIndexArray = new int[DECODE_THREAD_NUM];
    private final Queue<Integer> mDecodeRequestQueue = new LinkedList<>();
    private final Object mPageStateLock = new Object();
    private final AtomicInteger mDownloadedPages = new AtomicInteger(0);
    private final AtomicInteger mFinishedPages = new AtomicInteger(0);
    // Store page error
    private final ConcurrentHashMap<Integer, String> mPageErrorMap = new ConcurrentHashMap<>();
    private final List<OnSpiderListener> mSpiderListeners = new ArrayList<>();
    public volatile int[] mPageStateArray;
    private int mReadReference = 0;
    private int mDownloadReference = 0;
    // It mQueenThread is null, failed or stopped
    @Nullable
    private volatile Thread mQueenThread;
    private SpiderQueenWorker mWorkerScope;
    private boolean mStoped = false;

    private SpiderQueen(@NonNull GalleryInfo galleryInfo) {
        mHttpClient = EhApplication.getOkHttpClient();
        mGalleryInfo = galleryInfo;
        mSpiderDen = new SpiderDen(mGalleryInfo);

        for (int i = 0; i < DECODE_THREAD_NUM; i++) {
            mDecodeIndexArray[i] = -1;
        }

        mWorkerScope = new SpiderQueenWorker(this);
    }

    @UiThread
    public static SpiderQueen obtainSpiderQueen(@NonNull GalleryInfo galleryInfo, @Mode int mode) {
        OSUtils.checkMainLoop();

        SpiderQueen queen = sQueenMap.get(galleryInfo.getGid());
        if (queen == null) {
            queen = new SpiderQueen(galleryInfo);
            sQueenMap.put(galleryInfo.getGid(), queen);
            // Set mode
            queen.setMode(mode);
            queen.start();
        } else {
            // Set mode
            queen.setMode(mode);
        }
        return queen;
    }

    @UiThread
    public static void releaseSpiderQueen(@NonNull SpiderQueen queen, @Mode int mode) {
        OSUtils.checkMainLoop();

        // Clear mode
        queen.clearMode(mode);

        if (queen.mReadReference == 0 && queen.mDownloadReference == 0) {
            // Stop and remove if there is no reference
            queen.stop();
            sQueenMap.remove(queen.mGalleryInfo.getGid());
        }
    }

    public static boolean contain(int[] array, int value) {
        for (int v : array) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    // Mark as suspend fun when kotlinize, need IO
    public static int getStartPage(long gid) {
        var queen = sQueenMap.get(gid);
        SpiderInfo spiderInfo = null;

        // Fast Path: read existing queen
        if (queen != null) {
            spiderInfo = queen.mSpiderInfo.get();
        }

        // Slow path, read diskcache
        if (spiderInfo == null) {
            spiderInfo = SpiderInfoUtilsKt.readFromCache(gid);
        }

        if (spiderInfo == null) return 0;
        return spiderInfo.getStartPage();
    }

    public void addOnSpiderListener(OnSpiderListener listener) {
        synchronized (mSpiderListeners) {
            mSpiderListeners.add(listener);
        }
    }

    public void removeOnSpiderListener(OnSpiderListener listener) {
        synchronized (mSpiderListeners) {
            mSpiderListeners.remove(listener);
        }
    }

    private void notifyGetPages(int pages) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGetPages(pages);
            }
        }
    }

    public void notifyGet509(int index) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGet509(index);
            }
        }
    }

    public void notifyPageDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onPageDownload(index, contentLength, receivedSize, bytesRead);
            }
        }
    }

    private void notifyPageSuccess(int index) {
        int size = -1;
        int[] temp = mPageStateArray;
        if (temp != null) {
            size = temp.length;
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onPageSuccess(index, mFinishedPages.get(), mDownloadedPages.get(), size);
            }
        }
    }

    private void notifyPageFailure(int index, String error) {
        int size = -1;
        int[] temp = mPageStateArray;
        if (temp != null) {
            size = temp.length;
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onPageFailure(index, error, mFinishedPages.get(), mDownloadedPages.get(), size);
            }
        }
    }

    private void notifyFinish() {
        int size = -1;
        int[] temp = mPageStateArray;
        if (temp != null) {
            size = temp.length;
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onFinish(mFinishedPages.get(), mDownloadedPages.get(), size);
            }
        }
    }

    private void notifyGetImageSuccess(int index, Image image) {
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGetImageSuccess(index, image);
            }
        }
    }

    private void notifyGetImageFailure(int index, String error) {
        if (error == null) {
            error = GetText.getString(R.string.error_unknown);
        }
        synchronized (mSpiderListeners) {
            for (OnSpiderListener listener : mSpiderListeners) {
                listener.onGetImageFailure(index, error);
            }
        }
    }

    private boolean shouldIntoDownload = false;

    private void updateMode() {
        int mode;
        if (mDownloadReference > 0) {
            mode = MODE_DOWNLOAD;
        } else {
            mode = MODE_READ;
        }

        mSpiderDen.setMode(mode);

        // Update download page
        boolean intoDownloadMode = mode == MODE_DOWNLOAD;

        if (intoDownloadMode && mPageStateArray != null) {
            // Clear download state
            synchronized (mPageStateLock) {
                int[] temp = mPageStateArray;
                for (int i = 0, n = temp.length; i < n; i++) {
                    int oldState = temp[i];
                    if (STATE_DOWNLOADING != oldState) {
                        temp[i] = STATE_NONE;
                    }
                }
                mDownloadedPages.lazySet(0);
                mFinishedPages.lazySet(0);
                mPageErrorMap.clear();
                mPagePercentMap.clear();
            }
        } else if (intoDownloadMode && mPageStateArray == null) {
            shouldIntoDownload = true;
        }
    }

    private void setMode(@Mode int mode) {
        switch (mode) {
            case MODE_READ -> mReadReference++;
            case MODE_DOWNLOAD -> mDownloadReference++;
        }

        if (mDownloadReference > 1) {
            throw new IllegalStateException("mDownloadReference can't more than 0");
        }

        updateMode();
    }

    private void clearMode(@Mode int mode) {
        switch (mode) {
            case MODE_READ -> mReadReference--;
            case MODE_DOWNLOAD -> mDownloadReference--;
        }

        if (mReadReference < 0 || mDownloadReference < 0) {
            throw new IllegalStateException("Mode reference < 0");
        }

        updateMode();
    }

    private void start() {
        Thread queenThread = new PriorityThread(this, TAG + '-' + sIdGenerator.incrementAndGet(),
                Process.THREAD_PRIORITY_BACKGROUND);
        mQueenThread = queenThread;
        queenThread.start();
    }

    private void stop() {
        mStoped = true;
        synchronized (mQueenLock) {
            mQueenLock.notifyAll();
        }
        synchronized (mDecodeRequestQueue) {
            mDecodeRequestQueue.notifyAll();
        }

        CoroutineScopeKt.cancel(mWorkerScope, null);
        mWorkerScope = null;
    }

    public int size() {
        if (mQueenThread == null) {
            return PageLoader.STATE_ERROR;
        } else if (mPageStateArray == null) {
            return PageLoader.STATE_WAIT;
        } else {
            return mPageStateArray.length;
        }
    }

    public String getError() {
        if (mQueenThread == null) {
            return "Error";
        } else {
            return null;
        }
    }

    public Object forceRequest(int index) {
        return request(index, true, true);
    }

    public Object request(int index) {
        return request(index, true, false);
    }

    private int getPageState(int index) {
        synchronized (mPageStateLock) {
            if (mPageStateArray != null && index >= 0 && index < mPageStateArray.length) {
                return mPageStateArray[index];
            } else {
                return STATE_NONE;
            }
        }
    }

    public void cancelRequest(int index) {
        if (mQueenThread == null) {
            return;
        }

        mWorkerScope.cancel(index);
        synchronized (mDecodeRequestQueue) {
            mDecodeRequestQueue.remove(index);
        }
    }

    public void preloadPages(@NonNull List<Integer> pages) {
        if (mQueenThread == null) {
            return;
        }

        mWorkerScope.updateRAList(pages);
    }

    /**
     * @return String for error<br>
     * Float for download percent<br>
     * null for wait
     */
    private Object request(int index, boolean ignoreError, boolean force) {
        if (mQueenThread == null) {
            return null;
        }

        // Get page state
        int state = getPageState(index);

        // Fix state for force
        if ((force && (state == STATE_FINISHED || state == STATE_FAILED)) ||
                (ignoreError && state == STATE_FAILED)) {
            // Update state to none at once
            updatePageState(index, STATE_NONE);
            state = STATE_NONE;
        }

        mWorkerScope.launch(index, force);

        Object result;

        switch (state) {
            case STATE_NONE -> result = null;
            case STATE_DOWNLOADING -> result = mPagePercentMap.get(index);
            case STATE_FAILED -> {
                String error = mPageErrorMap.get(index);
                if (error == null) {
                    error = GetText.getString(R.string.error_unknown);
                }
                result = error;
            }
            case STATE_FINISHED -> {
                synchronized (mDecodeRequestQueue) {
                    if (!contain(mDecodeIndexArray, index) && !mDecodeRequestQueue.contains(index)) {
                        mDecodeRequestQueue.add(index);
                        mDecodeRequestQueue.notifyAll();
                    }
                }
                result = null;
            }
            default -> throw new IllegalStateException("Unexpected value: " + state);
        }

        return result;
    }

    public boolean save(int index, @NonNull UniFile file) {
        int state = getPageState(index);
        if (STATE_FINISHED != state) {
            return false;
        }

        return mSpiderDen.saveToUniFile(index, file);
    }

    @Nullable
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        int state = getPageState(index);
        if (STATE_FINISHED != state) {
            return null;
        }

        var ext = mSpiderDen.getExtension(index);
        UniFile dst = dir.subFile(null != ext ? filename + "." + ext : filename);
        if (null == dst) {
            return null;
        }
        if (!mSpiderDen.saveToUniFile(index, dst)) return null;
        return dst;
    }

    @Nullable
    public String getExtension(int index) {
        int state = getPageState(index);
        if (STATE_FINISHED != state) {
            return null;
        }

        return mSpiderDen.getExtension(index);
    }

    public int getStartPage() {
        SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo == null) spiderInfo = readSpiderInfoFromLocal();
        if (spiderInfo == null) return 0;
        return spiderInfo.getStartPage();
    }

    public void putStartPage(int page) {
        final SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo != null) {
            spiderInfo.setStartPage(page);
        }
    }

    private synchronized SpiderInfo readSpiderInfoFromLocal() {
        SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo != null) {
            return spiderInfo;
        }

        // Read from download dir
        UniFile downloadDir = mSpiderDen.getDownloadDir();
        if (downloadDir != null) {
            UniFile file = downloadDir.findFile(SPIDER_INFO_FILENAME);
            if (file != null) {
                spiderInfo = SpiderInfoUtilsKt.readCompatFromUniFile(file);
                if (spiderInfo != null && spiderInfo.getGid() == mGalleryInfo.getGid() &&
                        spiderInfo.getToken().equals(mGalleryInfo.getToken())) {
                    return spiderInfo;
                }
            }
        }

        // Read from cache
        spiderInfo = SpiderInfoUtilsKt.readFromCache(mGalleryInfo.getGid());
        if (spiderInfo != null && spiderInfo.getGid() == mGalleryInfo.getGid() && spiderInfo.getToken().equals(mGalleryInfo.getToken())) {
            return spiderInfo;
        }
        return null;
    }

    private void readPreviews(String body, int index, SpiderInfo spiderInfo) throws ParseException {
        spiderInfo.setPreviewPages(GalleryDetailParser.parsePreviewPages(body));
        PreviewSet previewSet = GalleryDetailParser.parsePreviewSet(body);

        if (previewSet.size() > 0) {
            if (index == 0) {
                spiderInfo.setPreviewPerPage(previewSet.size());
            } else {
                spiderInfo.setPreviewPerPage(previewSet.getPosition(0) / index);
            }
        }

        for (int i = 0, n = previewSet.size(); i < n; i++) {
            GalleryPageUrlParser.Result result = GalleryPageUrlParser.parse(previewSet.getPageUrlAt(i));
            if (result != null) {
                spiderInfo.getPTokenMap().put(result.page, result.pToken);
            }
        }
    }

    private SpiderInfo readSpiderInfoFromInternet() {
        Request request = new EhRequestBuilder(EhUrl.getGalleryDetailUrl(
                mGalleryInfo.getGid(), mGalleryInfo.getToken(), 0, false), EhUrl.getReferer()).build();
        try (Response response = mHttpClient.newCall(request).execute()) {
            String body = response.body().string();

            var pages = GalleryDetailParser.parsePages(body);
            SpiderInfo spiderInfo = new SpiderInfo(mGalleryInfo.getGid(), pages);
            spiderInfo.setToken(mGalleryInfo.getToken());
            readPreviews(body, 0, spiderInfo);
            return spiderInfo;
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            return null;
        }
    }

    public String getPTokenFromMultiPageViewer(int index) {
        SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo == null) {
            return null;
        }

        String url = EhUrl.getGalleryMultiPageViewerUrl(
                mGalleryInfo.getGid(), mGalleryInfo.getToken());
        if (DEBUG_PTOKEN) {
            Log.d(TAG, "getPTokenFromMultiPageViewer index " + index + ", url " + url);
        }
        String referer = EhUrl.getReferer();
        Request request = new EhRequestBuilder(url, referer).build();
        try (Response response = mHttpClient.newCall(request).execute()) {
            String body = response.body().string();

            ArrayList<String> list = GalleryMultiPageViewerPTokenParser.parse(body);

            for (int i = 0; i < list.size(); i++) {
                spiderInfo.getPTokenMap().put(i, list.get(i));
            }

            return spiderInfo.getPTokenMap().get(index);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            return null;
        }
    }

    public String getPTokenFromInternet(int index) {
        SpiderInfo spiderInfo = mSpiderInfo.get();
        if (spiderInfo == null) {
            return null;
        }

        // Check previewIndex
        int previewIndex;
        if (spiderInfo.getPreviewPerPage() >= 0) {
            previewIndex = index / spiderInfo.getPreviewPerPage();
        } else {
            previewIndex = 0;
        }
        if (spiderInfo.getPreviewPages() > 0) {
            previewIndex = Math.min(previewIndex, spiderInfo.getPreviewPages() - 1);
        }

        String url = EhUrl.getGalleryDetailUrl(
                mGalleryInfo.getGid(), mGalleryInfo.getToken(), previewIndex, false);
        String referer = EhUrl.getReferer();
        if (DEBUG_PTOKEN) {
            Log.d(TAG, "index " + index + ", previewIndex " + previewIndex +
                    ", previewPerPage " + spiderInfo.getPreviewPerPage() + ", url " + url);
        }
        Request request = new EhRequestBuilder(url, referer).build();
        try (Response response = mHttpClient.newCall(request).execute()) {
            String body = response.body().string();
            readPreviews(body, previewIndex, spiderInfo);

            return spiderInfo.getPTokenMap().get(index);
        } catch (Throwable e) {
            ExceptionUtils.throwIfFatal(e);
            return null;
        }
    }

    private synchronized void writeSpiderInfoToLocal() {
        var spiderInfo = mSpiderInfo.get();
        if (spiderInfo != null) {
            UniFile downloadDir = mSpiderDen.getDownloadDir();
            if (downloadDir != null) {
                UniFile file = downloadDir.createFile(SPIDER_INFO_FILENAME);
                SpiderInfoUtilsKt.write(spiderInfo, file);
            }
            SpiderInfoUtilsKt.saveToCache(spiderInfo);
        }
    }

    private void runInternal() {
        // Read spider info
        SpiderInfo spiderInfo = readSpiderInfoFromLocal();

        // Check Stopped
        if (mStoped) {
            return;
        }

        // Spider info from internet
        if (spiderInfo == null) {
            spiderInfo = readSpiderInfoFromInternet();
        }

        // Error! Can't get spiderInfo
        if (spiderInfo == null) {
            return;
        }
        mSpiderInfo.set(spiderInfo);

        // Check Stopped
        if (mStoped) {
            return;
        }

        // Setup page state
        synchronized (mPageStateLock) {
            mPageStateArray = new int[spiderInfo.getPages()];
        }

        // Notify get pages
        notifyGetPages(spiderInfo.getPages());

        if (shouldIntoDownload) mWorkerScope.enterDownloadMode();

        // Start decoder
        for (int i = 0; i < DECODE_THREAD_NUM; i++) {
            Thread decoderThread = new PriorityThread(new SpiderDecoder(i),
                    "SpiderDecoder-" + i, Process.THREAD_PRIORITY_DEFAULT);
            mDecodeThreadArray[i] = decoderThread;
            decoderThread.start();
        }

        // Wait finish
        synchronized (mQueenLock) {
            try {
                mQueenLock.wait();
            } catch (InterruptedException ignored) {
            }
        }

        writeSpiderInfoToLocal();
    }

    @Override
    public void run() {
        if (DEBUG_LOG) {
            Log.i(TAG, Thread.currentThread().getName() + ": start");
        }

        runInternal();

        // Set mQueenThread null
        mQueenThread = null;

        notifyFinish();

        if (DEBUG_LOG) {
            Log.i(TAG, Thread.currentThread().getName() + ": end");
        }
    }

    public void updatePageState(int index, @State int state) {
        updatePageState(index, state, null);
    }

    private boolean isStateDone(int state) {
        return state == STATE_FINISHED || state == STATE_FAILED;
    }

    public void updatePageState(int index, @State int state, String error) {
        int oldState;
        synchronized (mPageStateLock) {
            oldState = mPageStateArray[index];
            mPageStateArray[index] = state;

            if (!isStateDone(oldState) && isStateDone(state)) {
                mDownloadedPages.incrementAndGet();
            } else if (isStateDone(oldState) && !isStateDone(state)) {
                mDownloadedPages.decrementAndGet();
            }
            if (oldState != STATE_FINISHED && state == STATE_FINISHED) {
                mFinishedPages.incrementAndGet();
            } else if (oldState == STATE_FINISHED && state != STATE_FINISHED) {
                mFinishedPages.decrementAndGet();
            }

            // Clear
            if (state == STATE_DOWNLOADING) {
                mPageErrorMap.remove(index);
            } else if (state == STATE_FINISHED || state == STATE_FAILED) {
                mPagePercentMap.remove(index);
            }

            // Get default error
            if (state == STATE_FAILED) {
                if (error == null) {
                    error = GetText.getString(R.string.error_unknown);
                }
                mPageErrorMap.put(index, error);
            }
        }

        // Notify listeners
        if (state == STATE_FAILED) {
            notifyPageFailure(index, error);
        } else if (state == STATE_FINISHED) {
            notifyPageSuccess(index);
        }
    }

    @IntDef({MODE_READ, MODE_DOWNLOAD})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    @IntDef({STATE_NONE, STATE_DOWNLOADING, STATE_FINISHED, STATE_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    public interface OnSpiderListener {

        void onGetPages(int pages);

        void onGet509(int index);

        /**
         * @param contentLength -1 for unknown
         */
        void onPageDownload(int index, long contentLength, long receivedSize, int bytesRead);

        void onPageSuccess(int index, int finished, int downloaded, int total);

        void onPageFailure(int index, String error, int finished, int downloaded, int total);

        /**
         * All workers end
         */
        void onFinish(int finished, int downloaded, int total);

        void onGetImageSuccess(int index, Image image);

        void onGetImageFailure(int index, String error);
    }

    private class SpiderDecoder implements Runnable {

        private final int mThreadIndex;

        public SpiderDecoder(int index) {
            mThreadIndex = index;
        }

        private void resetDecodeIndex() {
            synchronized (mDecodeRequestQueue) {
                mDecodeIndexArray[mThreadIndex] = -1;
            }
        }

        @Override
        public void run() {
            if (DEBUG_LOG) {
                Log.i(TAG, Thread.currentThread().getName() + ": start");
            }

            while (!mStoped) {
                int index;
                synchronized (mDecodeRequestQueue) {
                    if (mStoped) break;
                    if (mDecodeRequestQueue.isEmpty()) {
                        try {
                            mDecodeRequestQueue.wait();
                        } catch (InterruptedException e) {
                            // Interrupted
                            break;
                        }
                        if (mStoped) break;
                        continue;
                    }
                    index = mDecodeRequestQueue.remove();
                    mDecodeIndexArray[mThreadIndex] = index;
                }

                // Check index valid
                if (index < 0 || index >= mPageStateArray.length) {
                    resetDecodeIndex();
                    notifyGetImageFailure(index, GetText.getString(R.string.error_out_of_range));
                    continue;
                }

                Image.ByteBufferSource src = mSpiderDen.getImageSource(index);
                if (src == null) {
                    resetDecodeIndex();
                    // Can't find the file, it might be removed from cache,
                    // Reset it state and request it
                    updatePageState(index, STATE_NONE, null);
                    request(index, false, false);
                    continue;
                }

                Image image = Image.decode(src);
                String error = null;

                if (image == null) {
                    error = GetText.getString(R.string.error_decoding_failed);
                }

                // Notify
                if (image != null) {
                    notifyGetImageSuccess(index, image);
                } else {
                    notifyGetImageFailure(index, error);
                }

                resetDecodeIndex();
            }

            if (DEBUG_LOG) {
                Log.i(TAG, Thread.currentThread().getName() + ": end");
            }
        }
    }
}
