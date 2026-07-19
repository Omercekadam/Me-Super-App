package com.omer.mesuper.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** GitHub commit günü: tarih + o güne ait commit sayısı (contributionsCollection'dan). */
data class GithubContributionDay(val date: LocalDate, val count: Int)

class GitHubAuthException(message: String) : Exception(message)

@Serializable
private data class GraphQlRequest(val query: String, val variables: Variables)

@Serializable
private data class Variables(val login: String, val from: String, val to: String)

@Serializable
private data class GraphQlResponse(
    val data: DataWrapper? = null,
    val errors: List<GraphQlError>? = null,
)

@Serializable
private data class GraphQlError(val message: String)

@Serializable
private data class DataWrapper(val user: UserWrapper? = null)

@Serializable
private data class UserWrapper(val contributionsCollection: ContributionsCollection)

@Serializable
private data class ContributionsCollection(val contributionCalendar: ContributionCalendar)

@Serializable
private data class ContributionCalendar(val weeks: List<Week>)

@Serializable
private data class Week(val contributionDays: List<ContributionDay>)

@Serializable
private data class ContributionDay(val date: String, @SerialName("contributionCount") val count: Int)

private const val QUERY = """
query(${'$'}login: String!, ${'$'}from: DateTime!, ${'$'}to: DateTime!) {
  user(login: ${'$'}login) {
    contributionsCollection(from: ${'$'}from, to: ${'$'}to) {
      contributionCalendar {
        weeks { contributionDays { date, contributionCount } }
      }
    }
  }
}
"""

/** GitHub GraphQL API'sinden commit takvimini çeker. Kotlin↔dış servis köprüsü — Python'a girmez. */
@Singleton
class GitHubApi @Inject constructor(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Son 365 günün commit takvimini döndürür. PAT'in en az `read:user` kapsamı olmalı. */
    suspend fun fetchContributionCalendar(pat: String, username: String): List<GithubContributionDay> =
        withContext(Dispatchers.IO) {
            val to = Instant.now()
            val from = to.minus(365, ChronoUnit.DAYS)
            val requestBody = json.encodeToString(
                GraphQlRequest.serializer(),
                GraphQlRequest(QUERY, Variables(username, from.toString(), to.toString())),
            ).toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://api.github.com/graphql")
                .addHeader("Authorization", "Bearer $pat")
                .post(requestBody)
                .build()

            val rawBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw GitHubAuthException("GitHub API hatası: ${response.code}")
                }
                response.body.string()
            }

            val parsed = json.decodeFromString(GraphQlResponse.serializer(), rawBody)
            parsed.errors?.firstOrNull()?.let { throw GitHubAuthException(it.message) }
            val user = parsed.data?.user ?: throw GitHubAuthException("Kullanıcı bulunamadı: $username")

            user.contributionsCollection.contributionCalendar.weeks
                .flatMap { it.contributionDays }
                .map { day -> GithubContributionDay(date = LocalDate.parse(day.date), count = day.count) }
        }
}
