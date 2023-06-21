/*
 * Copyright 2015 Hippo Seven
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

import android.content.Context
import android.content.res.Resources
import com.hippo.ehviewer.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReadableTime {
    private const val SECOND_MILLIS: Long = 1000
    private const val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS
    private const val WEEK_MILLIS = 7 * DAY_MILLIS
    private const val YEAR_MILLIS = 365 * DAY_MILLIS
    private val MULTIPLES = longArrayOf(
        YEAR_MILLIS,
        DAY_MILLIS,
        HOUR_MILLIS,
        MINUTE_MILLIS,
        SECOND_MILLIS,
    )
    private const val SIZE = 5
    private val UNITS = intArrayOf(
        R.plurals.year,
        R.plurals.day,
        R.plurals.hour,
        R.plurals.minute,
        R.plurals.second,
    )
    private val sCalendarLock = Any()
    private val DATE_FORMAT_WITHOUT_YEAR = DateTimeFormatter.ofPattern("MMM d")
    private val DATE_FORMAT_WITH_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val DATE_FORMAT_WITHOUT_YEAR_ZH = DateTimeFormatter.ofPattern("M月d日")
    private val DATE_FORMAT_WITH_YEAR_ZH = DateTimeFormatter.ofPattern("yyyy年M月d日")
    private val FILENAMABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")
    private val sDateFormatLock = Any()
    private var sResources: Resources? = null
    fun initialize(context: Context) {
        sResources = context.applicationContext.resources
    }

    fun getTimeAgo(time: Long): String {
        val resources = sResources
        val now = System.currentTimeMillis()
        if (time > now + 2 * MINUTE_MILLIS || time <= 0) {
            return resources!!.getString(R.string.from_the_future)
        }
        val diff = now - time
        if (diff < MINUTE_MILLIS) {
            return resources!!.getString(R.string.just_now)
        } else if (diff < 2 * MINUTE_MILLIS) {
            return resources!!.getQuantityString(R.plurals.some_minutes_ago, 1, 1)
        } else if (diff < 50 * MINUTE_MILLIS) {
            val minutes = (diff / MINUTE_MILLIS).toInt()
            return resources!!.getQuantityString(R.plurals.some_minutes_ago, minutes, minutes)
        } else if (diff < 90 * MINUTE_MILLIS) {
            return resources!!.getQuantityString(R.plurals.some_hours_ago, 1, 1)
        } else if (diff < 24 * HOUR_MILLIS) {
            val hours = (diff / HOUR_MILLIS).toInt()
            return resources!!.getQuantityString(R.plurals.some_hours_ago, hours, hours)
        } else if (diff < 48 * HOUR_MILLIS) {
            return resources!!.getString(R.string.yesterday)
        } else if (diff < WEEK_MILLIS) {
            val days = (diff / DAY_MILLIS).toInt()
            return resources!!.getString(R.string.some_days_ago, days)
        } else {
            synchronized(sCalendarLock) {
                val nowDate = Instant.ofEpochMilli(now).atOffset(ZoneOffset.UTC)
                val timeDate = Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC)
                val nowYear = nowDate.year
                val timeYear = timeDate.year
                val isZh = Locale.getDefault().language == "zh"
                return if (nowYear == timeYear) {
                    (if (isZh) DATE_FORMAT_WITHOUT_YEAR_ZH else DATE_FORMAT_WITHOUT_YEAR)
                        .format(timeDate.toInstant().atOffset(ZoneOffset.UTC))
                } else {
                    (if (isZh) DATE_FORMAT_WITH_YEAR_ZH else DATE_FORMAT_WITH_YEAR)
                        .format(timeDate.toInstant().atOffset(ZoneOffset.UTC))
                }
            }
        }
    }

    fun getShortTimeInterval(time: Long): String {
        val sb = StringBuilder()
        val resources = sResources
        for (i in 0 until SIZE) {
            val multiple = MULTIPLES[i]
            val quotient = time / multiple
            if (time > multiple * 1.5 || i == SIZE - 1) {
                sb.append(quotient)
                    .append(" ")
                    .append(resources!!.getQuantityString(UNITS[i], quotient.toInt()))
                break
            }
        }
        return sb.toString()
    }

    fun getFilenamableTime(time: Long): String {
        synchronized(sDateFormatLock) {
            return FILENAMABLE_DATE_FORMAT.format(
                Instant.ofEpochMilli(time).atZone(
                    ZoneId.systemDefault(),
                ),
            )
        }
    }
}
