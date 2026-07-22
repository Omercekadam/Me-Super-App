package com.omer.mesuper.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

enum class TmdbMediaKind { MOVIE, TV }

data class TmdbSearchResult(val tmdbId: Long, val title: String, val posterUrl: String?)

class TmdbApiException(message: String) : Exception(message)

@Serializable
private data class TmdbSearchResponse(val results: List<TmdbResultDto> = emptyList())

@Serializable
private data class TmdbResultDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

private const val POSTER_BASE = "https://image.tmdb.org/t/p/w342"

/** TMDB film/dizi arama — kişisel kullanım için ücretsiz anahtar yeterli. */
@Singleton
class TmdbApi @Inject constructor(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun search(apiKey: String, kind: TmdbMediaKind, query: String): List<TmdbSearchResult> =
        withContext(Dispatchers.IO) {
            val path = if (kind == TmdbMediaKind.MOVIE) "movie" else "tv"
            val url = "https://api.themoviedb.org/3/search/$path".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", apiKey)
                .addQueryParameter("query", query)
                .addQueryParameter("language", "tr-TR")
                .build()

            val request = Request.Builder().url(url).get().build()
            val rawBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw TmdbApiException("TMDB API hatası: ${response.code}")
                response.body.string()
            }

            val parsed = json.decodeFromString(TmdbSearchResponse.serializer(), rawBody)
            parsed.results.map {
                TmdbSearchResult(
                    tmdbId = it.id,
                    title = it.title ?: it.name ?: "?",
                    posterUrl = it.posterPath?.let { path -> "$POSTER_BASE$path" },
                )
            }
        }
}
