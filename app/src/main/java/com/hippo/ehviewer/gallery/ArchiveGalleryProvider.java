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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hippo.unifile.UniFile;

public class ArchiveGalleryProvider extends GalleryProvider2 {

    public ArchiveGalleryProvider(Context context, Uri uri) {

    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    protected void onRequest(int index) {
    }

    @Override
    protected void onForceRequest(int index) {
        onRequest(index);
    }

    @Override
    protected void onCancelRequest(int index) {
    }

    @Override
    public String getError() {
        return null;
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
}
