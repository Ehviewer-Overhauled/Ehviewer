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
package com.hippo.ehviewer.ui.legacy

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import com.hippo.ehviewer.FavouriteStatusRouter
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.ui.legacy.ContentLayout.ContentHelper
import com.hippo.ehviewer.yorozuya.IntIdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

abstract class GalleryInfoContentHelper : ContentHelper() {
    var jumpTo: String? = null
    val scope = CoroutineScope(Dispatchers.IO).apply {
        launch {
            FavouriteStatusRouter.globalFlow.collect { (gid, slot) -> map[gid]?.favoriteSlot = slot }
        }
    }

    @SuppressLint("UseSparseArrays")
    private var map: MutableMap<Long, GalleryInfo> = HashMap()

    fun destroy() = scope.cancel()

    override fun onAddData(data: List<GalleryInfo>) {
        data.forEach { map[it.gid] = it }
    }

    override fun onRemoveData(data: List<GalleryInfo>) {
        data.forEach { map.remove(it.gid) }
    }

    override fun onClearData() {
        map.clear()
    }

    override fun saveInstanceState(superState: Parcelable?): Parcelable {
        val bundle = super.saveInstanceState(superState) as Bundle

        // TODO It's a bad design
        val id = FavouriteStatusRouter.saveDataMap(map)
        bundle.putInt(KEY_DATA_MAP, id)
        return bundle
    }

    override fun restoreInstanceState(state: Parcelable): Parcelable? {
        val bundle = state as Bundle
        val id = bundle.getInt(KEY_DATA_MAP, IntIdGenerator.INVALID_ID)
        if (id != IntIdGenerator.INVALID_ID) {
            FavouriteStatusRouter.restoreDataMap(id)?.let { this.map = it }
        }
        return super.restoreInstanceState(state)
    }

    fun goTo(time: Long, isNext: Boolean) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
        jumpTo = formatter.format(Instant.ofEpochMilli(time))
        if (isNext) {
            goTo(mNext ?: "2", true)
        } else {
            goTo(mPrev, false)
        }
        jumpTo = null
    }

    companion object {
        private const val KEY_DATA_MAP = "data_map"
    }
}
