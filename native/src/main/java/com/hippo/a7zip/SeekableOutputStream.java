/*
 * Copyright 2020 Hippo Seven
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

package com.hippo.a7zip;

import java.io.IOException;
import java.io.OutputStream;

public abstract class SeekableOutputStream extends OutputStream {

    /**
     * Sets the position, measured from the beginning,
     * at which the next write occurs. The offset may be
     * set beyond the end of the file.
     */
    public abstract void seek(long pos) throws IOException;

    /**
     * Returns current position, measured from the beginning.
     */
    public abstract long tell() throws IOException;

    /**
     * Returns the size.
     */
    public abstract long size() throws IOException;

    /**
     * Sets the size.
     */
    public abstract void truncate(long size) throws IOException;
}
