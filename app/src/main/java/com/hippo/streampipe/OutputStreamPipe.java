/*
 * Copyright 2015-2016 Hippo Seven
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

package com.hippo.streampipe;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A pipe to open {@code OutputStream}
 */
public interface OutputStreamPipe {

    /**
     * Obtain the {@code OutputStreamPipe}.
     * Call it before opening {@code OutputStream}.
     */
    void obtain();

    /**
     * Release the {@code OutputStreamPipe}.
     */
    void release();

    /**
     * Open {@code OutputStream}
     *
     * @return the {@code OutputStream}
     */
    @NonNull
    OutputStream open() throws IOException;

    /**
     * Close the {@code OutputStream} opened before.
     */
    void close();
}
