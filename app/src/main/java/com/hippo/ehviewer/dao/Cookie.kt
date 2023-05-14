package com.hippo.ehviewer.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "OK_HTTP_3_COOKIE")
data class Cookie(
    @ColumnInfo(name = "NAME")
    var name: String,
    @ColumnInfo(name = "VALUE")
    var value: String,
    @ColumnInfo(name = "EXPIRES_AT")
    var expiresAt: Long,
    @ColumnInfo(name = "DOMAIN")
    var domain: String,
    @ColumnInfo(name = "PATH")
    var path: String,
    @ColumnInfo(name = "SECURE")
    var secure: Boolean,
    @ColumnInfo(name = "HTTP_ONLY")
    var httpOnly: Boolean,
    @ColumnInfo(name = "PERSISTENT")
    var persistent: Boolean,
    @ColumnInfo(name = "HOST_ONLY")
    var hostOnly: Boolean,
    @PrimaryKey
    @ColumnInfo(name = "_id")
    var id: Long?,
)
