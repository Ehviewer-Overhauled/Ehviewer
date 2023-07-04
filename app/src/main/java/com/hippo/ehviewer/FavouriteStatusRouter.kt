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

import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.yorozuya.IntIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

object FavouriteStatusRouter {
    private val idGenerator = IntIdGenerator(Settings.dataMapNextId)
    private val maps = HashMap<Int, MutableMap<Long, GalleryInfo>>()
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
        collector.trySend(gid to slot)
    }

    private val listenerScope = CoroutineScope(Dispatchers.IO)

    private lateinit var collector: ProducerScope<Pair<Long, Int>>

    val globalFlow = callbackFlow<Pair<Long, Int>> {
        collector = this
        awaitClose { error("Unreachable!!!") }
    }.shareIn(listenerScope, SharingStarted.Eagerly).apply {
        listenerScope.launch {
            collect { (gid, slot) ->
                EhDB.modifyHistoryInfoFavslotNonRefresh(gid, slot)
            }
        }
    }

    fun stateFlow(targetGid: Long) = globalFlow.transform { (gid, slot) -> if (targetGid == gid) emit(slot) }
}
