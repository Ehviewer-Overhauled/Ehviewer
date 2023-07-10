package tachiyomi.data.release

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubCommitComparison(val commits: List<GithubCommit>)

@Serializable
data class GithubCommit(val commit: GithubCommitDetail, val author: GithubCommitAuthor)

@Serializable
data class GithubCommitDetail(val message: String)

@Serializable
data class GithubCommitAuthor(@SerialName("login") val name: String)
