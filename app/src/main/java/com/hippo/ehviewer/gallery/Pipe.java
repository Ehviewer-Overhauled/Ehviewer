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

import android.os.ParcelFileDescriptor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class Pipe {
    private ParcelFileDescriptor[] fd;
    {
        try {
            fd = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final InputStream inputStream = new FileInputStream(fd[0].getFileDescriptor());

    InputStream getInputStream() {
        return inputStream;
    }

    int getOutputFd() {
        return fd[1].getFd();
    }

    void closeOutputFd() {
        try {
            fd[1].close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
