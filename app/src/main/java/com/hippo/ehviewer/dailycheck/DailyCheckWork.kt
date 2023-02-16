package com.hippo.ehviewer.dailycheck

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.R
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.EhRequestBuilder
import com.hippo.ehviewer.client.EhUrl
import com.hippo.ehviewer.client.parser.EventPaneParser
import java.time.Duration
import java.time.Instant

private val signedIn
    get() = EhApplication.ehCookieStore.hasSignedIn()

private const val CHANNEL_ID = "DailyCheckNotification"

class DailyCheckWork(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        runCatching {
            if (signedIn) {
                val referer = EhUrl.REFERER_E
                val request = EhRequestBuilder(EhUrl.HOST_E + "news.php", referer).build()
                val call = EhApplication.okHttpClient.newCall(request)
                call.execute().use { response ->
                    val responseBody = response.body
                    val body = responseBody.string()
                    EventPaneParser.parse(body)?.let { showEventPane(it) }
                }
            }
        }.onFailure {
            it.printStackTrace()
            return Result.retry()
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun showEventPane(html: String) {
        if (Settings.getHideHvEvents() && html.contains("You have encountered a monster!")) {
            return
        }
        val notificationManager = NotificationManagerCompat.from(context)
        val chan =
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName("a")
                .build()
        notificationManager.createNotificationChannel(chan)
        val msg = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentText(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY))
            .setStyle(NotificationCompat.BigTextStyle())
            .build()
        notificationManager.notify(1, msg)
    }
}

private val whenToWork
    get() = Instant.now().plusSeconds(10)

private val initialDelay
    get() = Duration.between(Instant.now(), whenToWork)

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

fun initializeDailyCheckWork(context: Context) {
    if (Settings.getRequestNews()) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            getDailyCheckWorkRequest()
        )
    } else {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}
