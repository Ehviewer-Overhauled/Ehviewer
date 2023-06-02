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
package com.hippo.ehviewer

import android.annotation.SuppressLint
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.yorozuya.IntIdGenerator

object FavouriteStatusRouter {
    private val idGenerator = IntIdGenerator(Settings.dataMapNextId)

    @SuppressLint("UseSparseArrays")
    private val maps = HashMap<Int, MutableMap<Long, GalleryInfo>>()
    private val listeners: MutableList<Listener> = ArrayList()
    fun saveDataMap(map: MutableMap<Long, GalleryInfo>): Int {
        val id = idGenerator.nextId()
        maps[id] = map
        Settings.dataMapNextId = idGenerator.nextId()
        return id
    }

    fun restoreDataMap(id: Int): MutableMap<Long, GalleryInfo>? {
        return maps.remove(id)
    }

    fun modifyFavourites(gid: Long, slot: Int) {
        for (map in maps.values) {
            val info = map[gid]
            if (info != null) {
                info.favoriteSlot = slot
            }
        }
        for (listener in listeners) {
            listener.onModifyFavourites(gid, slot)
        }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun interface Listener {
        fun onModifyFavourites(gid: Long, slot: Int)
    }
}
