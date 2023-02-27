package com.hippo.ehviewer.dailycheck

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhEngine
import eu.kanade.tachiyomi.util.lang.withIOContext
import java.time.Duration
import java.time.LocalDateTime

private val signedIn
    get() = EhApplication.ehCookieStore.hasSignedIn()

private const val CHANNEL_ID = "DailyCheckNotification"

class DailyCheckWork(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withIOContext {
        runCatching {
            if (signedIn) {
                EhEngine.getNews(true)?.let { showEventPane(it) }
            }
        }.onFailure {
            it.printStackTrace()
            return@withIOContext Result.retry()
        }
        Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showEventPane(html: String) {
        if (Settings.hideHvEvents && html.contains("You have encountered a monster!")) {
            return
        }
        val notificationManager = NotificationManagerCompat.from(context)
        val chan = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(CHANNEL_ID)
            .build()
        notificationManager.createNotificationChannel(chan)
        val msg = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY))
            .setStyle(NotificationCompat.BigTextStyle())
            .build()
        runCatching {
            notificationManager.notify(1, msg)
        }.onFailure {
            it.printStackTrace()
        }
    }
}

val schedHour
    get() = Settings.getInt(Settings.KEY_REQUEST_NEWS_TIMER_HOUR, -1).takeUnless { it == -1 }

val schedMinute
    get() = Settings.getInt(Settings.KEY_REQUEST_NEWS_TIMER_MINUTE, -1).takeUnless { it == -1 }

private val whenToWork
    get() = LocalDateTime.now()
        .run { withHour(schedHour ?: hour) }
        .run { withMinute(schedMinute ?: minute) }

private val initialDelay
    get() = Duration.between(LocalDateTime.now(), whenToWork)
        .run { if (isNegative) plusDays(1) else this }

private fun getDailyCheckWorkRequest(): PeriodicWorkRequest {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    return PeriodicWorkRequestBuilder<DailyCheckWork>(Duration.ofDays(1))
        .setConstraints(constraints)
        .setInitialDelay(initialDelay)
        .build()
}

private const val workName = "DailyCheckWork"

fun updateDailyCheckWork(context: Context) {
    if (Settings.requestNews) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            getDailyCheckWorkRequest()
        )
    } else {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}
