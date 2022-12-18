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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.spider.SpiderQueen;
import com.hippo.image.Image;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.SimpleHandler;

import java.util.Locale;

import eu.kanade.tachiyomi.ui.reader.loader.PageLoader;

public class EhPageLoader extends PageLoader2 implements SpiderQueen.OnSpiderListener {

    private final Context mContext;
    private final GalleryInfo mGalleryInfo;
    @Nullable
    private SpiderQueen mSpiderQueen;

    public EhPageLoader(Context context, GalleryInfo galleryInfo) {
        mContext = context;
        mGalleryInfo = galleryInfo;
    }

    @Override
    public void start() {
        mSpiderQueen = SpiderQueen.obtainSpiderQueen(mContext, mGalleryInfo, SpiderQueen.MODE_READ);
        mSpiderQueen.addOnSpiderListener(this);
        if (mSpiderQueen.size() > 0)
            notifyDataChanged();
    }

    @Override
    public void stop() {
        if (mSpiderQueen != null) {
            mSpiderQueen.removeOnSpiderListener(this);
            // Activity recreate may called, so wait 3000s
            SimpleHandler.getInstance().postDelayed(new ReleaseTask(mSpiderQueen), 3000);
            mSpiderQueen = null;
        }
    }

    @Override
    public int getStartPage() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.getStartPage();
        } else {
            return super.getStartPage();
        }
    }

    @NonNull
    @Override
    public String getImageFilename(int index) {
        return String.format(Locale.US, "%d-%s-%08d", mGalleryInfo.gid, mGalleryInfo.token, index + 1);
    }

    @NonNull
    @Override
    public String getImageFilenameWithExtension(int index) {
        if (null != mSpiderQueen) {
            String extension = mSpiderQueen.getExtension(index);
            if (extension != null) {
                return String.format(Locale.US, "%d-%s-%08d.%s", mGalleryInfo.gid, mGalleryInfo.token, index + 1, extension);
            }
        }
        return String.format(Locale.US, "%d-%s-%08d", mGalleryInfo.gid, mGalleryInfo.token, index + 1);
    }

    @Override
    public boolean save(int index, @NonNull UniFile file) {
        if (null != mSpiderQueen) {
            return mSpiderQueen.save(index, file);
        } else {
            return false;
        }
    }

    @Nullable
    @Override
    public UniFile save(int index, @NonNull UniFile dir, @NonNull String filename) {
        if (null != mSpiderQueen) {
            return mSpiderQueen.save(index, dir, filename);
        } else {
            return null;
        }
    }

    @Override
    public void putStartPage(int page) {
        if (mSpiderQueen != null) {
            mSpiderQueen.putStartPage(page);
        }
    }

    @Override
    public int size() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.size();
        } else {
            return PageLoader.STATE_ERROR;
        }
    }

    @Override
    protected void onRequest(int index) {
        if (mSpiderQueen != null) {
            Object object = mSpiderQueen.request(index, true);
            if (object instanceof Float) {
                notifyPagePercent(index, (Float) object);
            } else if (object instanceof String) {
                notifyPageFailed(index);
            } else if (object == null) {
                notifyPageWait(index);
            }
        }
    }

    @Override
    protected void onForceRequest(int index) {
        if (mSpiderQueen != null) {
            Object object = mSpiderQueen.forceRequest(index);
            if (object instanceof Float) {
                notifyPagePercent(index, (Float) object);
            } else if (object instanceof String) {
                notifyPageFailed(index);
            } else if (object == null) {
                notifyPageWait(index);
            }
        }
    }

    @Override
    protected void onCancelRequest(int index) {
        if (mSpiderQueen != null) {
            mSpiderQueen.cancelRequest(index);
        }
    }

    @Override
    public String getError() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.getError();
        } else {
            return "Error"; // TODO
        }
    }

    @Override
    public void onGetPages(int pages) {
        notifyDataChanged();
    }

    @Override
    public void onGet509(int index) {
        // TODO
    }

    @Override
    public void onPageDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        if (contentLength > 0) {
            notifyPagePercent(index, (float) receivedSize / contentLength);
        }
    }

    @Override
    public void onPageSuccess(int index, int finished, int downloaded, int total) {
        if (mSpiderQueen != null) {
            mSpiderQueen.request(index, false);
        }
    }

    @Override
    public void onPageFailure(int index, String error, int finished, int downloaded, int total) {
        notifyPageFailed(index);
    }

    @Override
    public void onFinish(int finished, int downloaded, int total) {
    }

    @Override
    public void onGetImageSuccess(int index, Image image) {
        notifyPageSucceed(index, image);
    }

    @Override
    public void onGetImageFailure(int index, String error) {
        notifyPageFailed(index);
    }

    private static class ReleaseTask implements Runnable {

        private SpiderQueen mSpiderQueen;

        public ReleaseTask(SpiderQueen spiderQueen) {
            mSpiderQueen = spiderQueen;
        }

        @Override
        public void run() {
            if (null != mSpiderQueen) {
                SpiderQueen.releaseSpiderQueen(mSpiderQueen, SpiderQueen.MODE_READ);
                mSpiderQueen = null;
            }
        }
    }
}
