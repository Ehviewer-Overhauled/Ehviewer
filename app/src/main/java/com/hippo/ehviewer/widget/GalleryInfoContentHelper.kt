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
package com.hippo.ehviewer.widget

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.FavouriteStatusRouter
import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.widget.ContentLayout.ContentHelper
import com.hippo.yorozuya.IntIdGenerator
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

abstract class GalleryInfoContentHelper : ContentHelper<GalleryInfo?>() {
    @JvmField
    var maxGid = -1

    @JvmField
    var minGid = -1

    @JvmField
    var jumpTo: String? = null
    private val listener: FavouriteStatusRouter.Listener

    @SuppressLint("UseSparseArrays")
    private var map: MutableMap<Long, GalleryInfo> = HashMap()

    init {
        listener = FavouriteStatusRouter.Listener { gid: Long, slot: Int ->
            val info = map[gid]
            if (info != null) {
                info.favoriteSlot = slot
            }
        }
        EhApplication.favouriteStatusRouter.addListener(listener)
    }

    fun destroy() {
        EhApplication.favouriteStatusRouter.removeListener(listener)
    }

    override fun onAddData(data: GalleryInfo?) {
        data?.let { map[data.gid] = data }
    }

    override fun onAddData(data: List<GalleryInfo?>) {
        for (info in data) {
            info?.let {
                if (maxGid == -1) maxGid = info.gid.toInt()
                if (minGid == -1) minGid = info.gid.toInt()
                maxGid = max(maxGid.toLong(), info.gid).toInt()
                minGid = min(minGid.toLong(), info.gid).toInt()
                map[info.gid] = info
            }
        }
    }

    override fun onRemoveData(data: GalleryInfo?) {
        data?.let { map.remove(data.gid) }
    }

    override fun onRemoveData(data: List<GalleryInfo?>) {
        for (info in data) {
            info?.let { map.remove(info.gid) }
        }
    }

    override fun onClearData() {
        map.clear()
        maxGid = -1
        minGid = -1
    }

    override fun beforeRefresh() {
        super.beforeRefresh()
        maxGid = -1
        minGid = -1
    }

    override fun saveInstanceState(superState: Parcelable): Parcelable {
        val bundle = super.saveInstanceState(superState) as Bundle

        // TODO It's a bad design
        val router = EhApplication.favouriteStatusRouter
        val id = router.saveDataMap(map)
        bundle.putInt(KEY_DATA_MAP, id)
        bundle.putInt(KEY_MIN_GID, minGid)
        bundle.putInt(KEY_MAX_GID, maxGid)
        return bundle
    }

    override fun restoreInstanceState(state: Parcelable): Parcelable {
        val bundle = state as Bundle
        val id = bundle.getInt(KEY_DATA_MAP, IntIdGenerator.INVALID_ID)
        if (id != IntIdGenerator.INVALID_ID) {
            val router = EhApplication.favouriteStatusRouter
            val map = router.restoreDataMap(id)
            if (map != null) {
                this.map = map
            }
        }
        minGid = bundle.getInt(KEY_MIN_GID)
        maxGid = bundle.getInt(KEY_MAX_GID)
        return super.restoreInstanceState(state)
    }

    fun goTo(time: Long) {
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd", Locale.US).withZone(ZoneOffset.UTC)
        jumpTo = formatter.format(Instant.ofEpochMilli(time))
        doGoToPage(Int.MAX_VALUE / 2)
    }

    fun goToPage(page: Int) {
        jumpTo = page.toString()
        doGoToPage(page)
    }

    companion object {
        private const val KEY_DATA_MAP = "data_map"
        private const val KEY_MAX_GID = "max_gid"
        private const val KEY_MIN_GID = "min_gid"
        const val KEY_UPPER_PAGE = "upper_page"
        const val KEY_LOWER_PAGE = "lower_page"
    }
}