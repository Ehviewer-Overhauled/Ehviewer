package com.hippo.ehviewer.dao

import androidx.room.Entity
import androidx.room.ForeignKey
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import java.time.Instant

@Entity(tableName = "LOCAL_FAVORITES", foreignKeys = [ForeignKey(BaseGalleryInfo::class, ["GID"], ["GID"])])
class LocalFavoriteInfo(gid: Long = 0, time: Long = Instant.now().toEpochMilli()) : TimeInfo(gid, time)
