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
package com.hippo.unifile

/**
 * An interface for filtering [UniFile] objects based on their names
 * or the directory they reside in.
 *
 * @see UniFile
 *
 * @see UniFile.listFiles
 */
interface FilenameFilter {
    /**
     * Indicates if a specific filename matches this filter.
     *
     * @param dir      the directory in which the `filename` was found.
     * @param filename the name of the file in `dir` to test.
     * @return `true` if the filename matches the filter
     * and can be included in the list, `false`
     * otherwise.
     */
    fun accept(dir: UniFile?, filename: String?): Boolean
}
