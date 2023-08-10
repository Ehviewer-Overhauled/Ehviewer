package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.EhDB
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.LOCAL_FAVORITED
import com.hippo.ehviewer.client.data.GalleryInfo.Companion.NOT_FAVORITED

object GalleryListParser {
    suspend fun parse(body: String) = runCatching {
        parseGalleryInfoList(body).apply {
            galleryInfoList.onEach {
                if (it.favoriteSlot == NOT_FAVORITED && EhDB.containLocalFavorites(it.gid)) {
                    it.favoriteSlot = LOCAL_FAVORITED
                }
                it.generateSLang()
            }
        }
    }.getOrDefault(GalleryListResult())
}

class GalleryListResult(
    val prev: String? = null,
    val next: String? = null,
    val galleryInfoList: ArrayList<BaseGalleryInfo> = arrayListOf(),
)

private external fun parseGalleryInfoList(e: String): GalleryListResult
