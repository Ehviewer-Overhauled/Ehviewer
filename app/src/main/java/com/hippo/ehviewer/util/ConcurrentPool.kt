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
package com.hippo.ehviewer.util

class ConcurrentPool<T>(size: Int) {
    private val mArray: Array<T?>
    private val mMaxSize: Int
    private var mSize: Int

    init {
        check(size > 0) { "Pool size must > 0, it is $size" }
        mArray = arrayOfNulls<Any>(size) as Array<T?>
        mMaxSize = size
        mSize = 0
    }

    @Synchronized
    fun push(t: T?) {
        if (t != null && mSize < mMaxSize) {
            mArray[mSize++] = t
        }
    }

    @Synchronized
    fun pop(): T? {
        return if (mSize > 0) {
            val t = mArray[--mSize]
            mArray[mSize] = null
            t
        } else {
            null
        }
    }
}
