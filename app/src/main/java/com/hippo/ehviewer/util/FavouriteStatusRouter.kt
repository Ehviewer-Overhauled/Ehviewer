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
package com.hippo.ehviewer.util

import com.hippo.ehviewer.EhDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

object FavouriteStatusRouter {
    fun modifyFavourites(gid: Long, slot: Int) {
        _globalFlow.tryEmit(gid to slot)
    }

    private val listenerScope = CoroutineScope(Dispatchers.IO)

    private val _globalFlow = MutableSharedFlow<Pair<Long, Int>>(extraBufferCapacity = 1).apply {
        listenerScope.launch {
            collect { (gid, slot) ->
                EhDB.modifyHistoryInfoFavslotNonRefresh(gid, slot)
            }
        }
    }

    val globalFlow = _globalFlow.asSharedFlow()

    fun stateFlow(targetGid: Long) = globalFlow.transform { (gid, slot) -> if (targetGid == gid) emit(slot) }
}
