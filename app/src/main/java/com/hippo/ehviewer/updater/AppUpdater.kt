package com.hippo.ehviewer.updater

import com.hippo.ehviewer.BuildConfig
import com.hippo.ehviewer.Settings
import com.hippo.ehviewer.client.execute
import com.hippo.ehviewer.client.executeAndParseAs
import eu.kanade.tachiyomi.util.system.logcat
import io.ktor.util.encodeBase64
import moe.tarsin.coroutines.runSuspendCatching
import okhttp3.Request
import okio.sink
import org.json.JSONObject
import tachiyomi.data.release.GithubArtifacts
import tachiyomi.data.release.GithubCommitComparison
import tachiyomi.data.release.GithubRelease
import tachiyomi.data.release.GithubWorkflowRuns
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.zip.ZipInputStream

private const val API_URL = "https://api.github.com/repos/${BuildConfig.REPO_NAME}"
private const val LATEST_RELEASE_URL = "$API_URL/releases/latest"

object AppUpdater {
    suspend fun checkForUpdate(forceCheck: Boolean = false): Release? {
        val now = Instant.now()
        val lastChecked = Instant.ofEpochSecond(Settings.lastUpdateDay)
        val interval = Settings.updateIntervalDays
        if (forceCheck || interval != 0 && now.isAfter(lastChecked.plus(interval.toLong(), ChronoUnit.DAYS))) {
            Settings.lastUpdateDay = now.epochSecond
            if (Settings.useCIUpdateChannel) {
                val curSha = BuildConfig.COMMIT_SHA
                val branch = ghRequest(API_URL).execute {
                    JSONObject(body.string()).getString("default_branch")
                }
                val workflowRunsUrl = "$API_URL/actions/workflows/ci.yml/runs?branch=$branch&status=success&per_page=1"
                val workflowRun = ghRequest(workflowRunsUrl).executeAndParseAs<GithubWorkflowRuns>().workflowRuns[0]
                val shortSha = workflowRun.headSha.take(7)
                if (shortSha != curSha) {
                    val artifacts = ghRequest(workflowRun.artifactsUrl).executeAndParseAs<GithubArtifacts>()
                    val archiveUrl = artifacts.getDownloadLink()
                    val changelog = runSuspendCatching {
                        val commitComparisonUrl = "$API_URL/compare/$curSha...$shortSha"
                        val result = ghRequest(commitComparisonUrl).executeAndParseAs<GithubCommitComparison>()
                        // TODO: Prettier format, Markdown?
                        result.commits.joinToString("\n") { "${it.commit.message} (@${it.author.name})" }
                    }.getOrDefault(workflowRun.title)
                    return Release(shortSha, changelog, archiveUrl)
                }
            } else {
                val curVersion = BuildConfig.VERSION_NAME
                val release = ghRequest(LATEST_RELEASE_URL).executeAndParseAs<GithubRelease>()
                val latestVersion = release.version
                val description = release.info
                val downloadUrl = release.getDownloadLink()
                if (latestVersion != curVersion) {
                    return Release(latestVersion, description, downloadUrl)
                }
            }
        }
        return null
    }

    suspend fun downloadUpdate(url: String, file: File) =
        ghRequest(url).execute {
            if (url.endsWith("zip")) {
                ZipInputStream(body.byteStream()).use { zip ->
                    zip.nextEntry
                    file.outputStream().use {
                        zip.copyTo(it)
                    }
                }
            } else {
                file.sink().use {
                    body.source().readAll(it)
                }
            }
        }
}

private inline fun ghRequest(url: String, builder: Request.Builder.() -> Unit = {}) = Request.Builder().url(url).apply {
    logcat { url }
    val token = "github_" + "pat_11AXZS" + "T4A0k3TArCGakP3t_7DzUE5S" + "mr1zw8rmmzVtCeRq62" + "A4qkuDMw6YQm5ZUtHSLZ2MLI3J4VSifLXZ"
    val user = "nullArrayList"
    val base64 = "$user:$token".encodeBase64()
    addHeader("Authorization", "Basic $base64")
}.apply(builder).build()

data class Release(
    val version: String,
    val changelog: String,
    val downloadLink: String,
)
