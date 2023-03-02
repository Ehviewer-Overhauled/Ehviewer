/*
 * Copyright 2017 Hippo Seven
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
package com.hippo.network

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import okhttp3.Cookie as OkHttpCookie

@Entity(tableName = "OK_HTTP_3_COOKIE")
class Cookie(
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
    var id: Long?
)

@Dao
interface CookiesDao {
    @Query("SELECT * FROM OK_HTTP_3_COOKIE")
    fun list(): List<Cookie>

    @Delete
    fun delete(cookie: Cookie)

    @Insert
    fun insert(cookie: Cookie): Long

    @Update
    fun update(cookie: Cookie)
}

@Database(entities = [Cookie::class], version = 1, exportSchema = false)
abstract class CookiesDatabase : RoomDatabase() {
    abstract fun cookiesDao(): CookiesDao
}

internal class CookieDatabase(context: Context, name: String) {
    private val cookieIdMap: MutableMap<OkHttpCookie, Long> = HashMap()
    private val db = Room.databaseBuilder(context, CookiesDatabase::class.java, name).build()

    val allCookies by lazy {
        val now = System.currentTimeMillis()
        val map: MutableMap<String, CookieSet> = HashMap()
        val toRemove: MutableList<Long> = ArrayList()
        db.cookiesDao().list().forEach {
            if (!it.persistent || it.expiresAt <= now) {
                toRemove.add(it.id!!)
            } else {
                val okhttpCookie = it.toOkHttp3Cookie()
                cookieIdMap[okhttpCookie] = it.id!!
                val set = map[it.domain] ?: CookieSet().apply { map[it.domain] = this }
                set.add(okhttpCookie)
            }
        }
        if (toRemove.isNotEmpty()) {
            val statement = db.compileStatement("DELETE FROM OK_HTTP_3_COOKIE WHERE _id = ?;")
            db.runInTransaction {
                toRemove.forEach {
                    statement.bindLong(1, it)
                    statement.executeUpdateDelete()
                }
            }
        }
        map
    }

    fun add(cookie: OkHttpCookie) {
        db.cookiesDao().insert(cookie.toCookie()).apply { cookieIdMap[cookie] = this }
    }

    fun update(from: OkHttpCookie, to: OkHttpCookie) {
        val id = cookieIdMap[from] ?: return
        db.cookiesDao().update(to.toCookie(id))
        cookieIdMap.remove(from)
        cookieIdMap[to] = id
    }

    fun remove(cookie: OkHttpCookie) {
        val id = cookieIdMap[cookie] ?: return
        db.cookiesDao().delete(cookie.toCookie(id))
        cookieIdMap.remove(cookie)
    }

    fun clear() {
        db.clearAllTables()
        cookieIdMap.clear()
    }
}

fun Cookie.toOkHttp3Cookie(): OkHttpCookie {
    return OkHttpCookie.Builder().apply {
        name(name)
        value(value)
        expiresAt(expiresAt)
        if (hostOnly) hostOnlyDomain(domain) else domain(domain)
        path(path)
        if (secure) secure()
        if (httpOnly) httpOnly()
    }.build()
}

private fun OkHttpCookie.toCookie(id: Long? = null): Cookie {
    return Cookie(
        name, value, expiresAt, domain, path, secure, httpOnly, persistent, hostOnly, id
    )
}
