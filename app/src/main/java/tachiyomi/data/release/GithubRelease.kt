package tachiyomi.data.release

import android.os.Build
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contains information about the latest release from GitHub.
 */
@Serializable
data class GithubRelease(
    @SerialName("tag_name") val version: String,
    @SerialName("body") val info: String,
    @SerialName("html_url") val releaseLink: String,
    @SerialName("assets") val assets: List<GitHubAssets>,
) {
    fun getDownloadLink(): String {
        val apkVariant = Build.SUPPORTED_ABIS[0]
        return (assets.find { it.downloadLink.contains(apkVariant) } ?: assets.find { it.downloadLink.contains("universal") } ?: assets[0]).downloadLink
    }
}

/**
 * Assets class containing download url.
 */
@Serializable
data class GitHubAssets(@SerialName("browser_download_url") val downloadLink: String)
