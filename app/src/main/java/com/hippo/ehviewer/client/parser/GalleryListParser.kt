package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED

object GalleryListParser {
    suspend fun parse(body: String) = parseGalleryInfoList(body).apply {
        galleryInfoList.onEach {
            if (it.favoriteSlot == NOT_FAVORITED && EhDB.containLocalFavorites(it.gid)) {
                it.favoriteSlot = LOCAL_FAVORITED
            }
            it.generateSLang()
        }
    }
}

class GalleryListResult(
    val prev: String?,
    val next: String?,
    val galleryInfoList: ArrayList<BaseGalleryInfo>,
)

private external fun parseGalleryInfoList(e: String): GalleryListResult
