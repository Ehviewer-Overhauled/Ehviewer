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

package com.hippo.util;

import android.content.Context;
import android.content.res.Resources;

import com.hippo.ehviewer.R;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReadableTime {

    public static final long SECOND_MILLIS = 1000;
    public static final long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
    public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
    public static final long WEEK_MILLIS = 7 * DAY_MILLIS;
    public static final long YEAR_MILLIS = 365 * DAY_MILLIS;
    public static final long[] MULTIPLES = {
            YEAR_MILLIS,
            DAY_MILLIS,
            HOUR_MILLIS,
            MINUTE_MILLIS,
            SECOND_MILLIS
    };
    public static final int SIZE = 5;
    public static final int[] UNITS = {
            R.plurals.year,
            R.plurals.day,
            R.plurals.hour,
            R.plurals.minute,
            R.plurals.second
    };
    private static final Object sCalendarLock = new Object();
    private static final DateTimeFormatter DATE_FORMAT_WITHOUT_YEAR = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter DATE_FORMAT_WITH_YEAR = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter DATE_FORMAT_WITHOUT_YEAR_ZH = DateTimeFormatter.ofPattern("M月d日");
    private static final DateTimeFormatter DATE_FORMAT_WITH_YEAR_ZH = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final DateTimeFormatter FILENAMABLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");
    private static final Object sDateFormatLock = new Object();
    private static Resources sResources;

    public static void initialize(Context context) {
        sResources = context.getApplicationContext().getResources();
    }

    public static String getTimeAgo(long time) {
        Resources resources = sResources;

        long now = System.currentTimeMillis();
        if (time > now + (2 * MINUTE_MILLIS) || time <= 0) {
            return resources.getString(R.string.from_the_future);
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return resources.getString(R.string.just_now);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return resources.getQuantityString(R.plurals.some_minutes_ago, 1, 1);
        } else if (diff < 50 * MINUTE_MILLIS) {
            int minutes = (int) (diff / MINUTE_MILLIS);
            return resources.getQuantityString(R.plurals.some_minutes_ago, minutes, minutes);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return resources.getQuantityString(R.plurals.some_hours_ago, 1, 1);
        } else if (diff < 24 * HOUR_MILLIS) {
            int hours = (int) (diff / HOUR_MILLIS);
            return resources.getQuantityString(R.plurals.some_hours_ago, hours, hours);
        } else if (diff < 48 * HOUR_MILLIS) {
            return resources.getString(R.string.yesterday);
        } else if (diff < WEEK_MILLIS) {
            int days = (int) (diff / DAY_MILLIS);
            return resources.getString(R.string.some_days_ago, days);
        } else {
            synchronized (sCalendarLock) {
                OffsetDateTime nowDate = Instant.ofEpochMilli(now).atOffset(ZoneOffset.UTC);
                OffsetDateTime timeDate = Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC);
                int nowYear = nowDate.getYear();
                int timeYear = timeDate.getYear();
                boolean isZh = Locale.getDefault().getLanguage().equals("zh");

                if (nowYear == timeYear) {
                    return (isZh ? DATE_FORMAT_WITHOUT_YEAR_ZH : DATE_FORMAT_WITHOUT_YEAR)
                            .format(timeDate.toInstant().atOffset(ZoneOffset.UTC));
                } else {
                    return (isZh ? DATE_FORMAT_WITH_YEAR_ZH : DATE_FORMAT_WITH_YEAR)
                            .format(timeDate.toInstant().atOffset(ZoneOffset.UTC));
                }
            }
        }
    }

    public static String getShortTimeInterval(long time) {
        StringBuilder sb = new StringBuilder();
        Resources resources = sResources;

        for (int i = 0; i < SIZE; i++) {
            long multiple = MULTIPLES[i];
            long quotient = time / multiple;
            if (time > multiple * 1.5 || i == SIZE - 1) {
                sb.append(quotient)
                        .append(" ")
                        .append(resources.getQuantityString(UNITS[i], (int) quotient));
                break;
            }
        }

        return sb.toString();
    }

    public static String getFilenamableTime(long time) {
        synchronized (sDateFormatLock) {
            return FILENAMABLE_DATE_FORMAT.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
        }
    }
}
