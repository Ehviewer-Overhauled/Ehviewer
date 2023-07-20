package com.hippo.ehviewer.dao

import androidx.room.Entity
import androidx.room.ForeignKey
import com.hippo.ehviewer.client.data.BaseGalleryInfo
import java.time.Instant

@Entity(tableName = "HISTORY", foreignKeys = [ForeignKey(BaseGalleryInfo::class, ["GID"], ["GID"])])
class HistoryInfo(gid: Long, time: Long = Instant.now().toEpochMilli()) : TimeInfo(gid, time)
