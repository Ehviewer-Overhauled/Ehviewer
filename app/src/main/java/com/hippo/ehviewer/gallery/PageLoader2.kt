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
package com.hippo.ehviewer.gallery

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader

abstract class PageLoader2 : PageLoader() {
    open val startPage: Int
        get() = 0

    open fun putStartPage(page: Int) {}

    /**
     * @return without extension
     */
    abstract fun getImageFilename(index: Int): String

    /**
     * @return with extension
     */
    abstract fun getImageFilenameWithExtension(index: Int): String
    abstract fun save(index: Int, file: UniFile): Boolean

    /**
     * @param filename without extension
     */
    abstract fun save(index: Int, dir: UniFile, filename: String): UniFile?
}

val SUPPORT_IMAGE_EXTENSIONS = arrayOf(
    ".jpg", // Joint Photographic Experts Group
    ".png", // Portable Network Graphics
    ".gif",
)
