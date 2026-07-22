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

data class RawgGameResult(val rawgId: Long, val name: String, val genres: String, val coverUrl: String?)

class RawgApiException(message: String) : Exception(message)

@Serializable
private data class RawgSearchResponse(val results: List<RawgGameDto> = emptyList())

@Serializable
private data class RawgGameDto(
    val id: Long,
    val name: String,
    val genres: List<RawgGenreDto> = emptyList(),
    @SerialName("background_image") val backgroundImage: String? = null,
)

@Serializable
private data class RawgGenreDto(val name: String)

/** RAWG oyun veritabanı — elle eklenen oyunlara kapak görseli ve tür bilgisi getirir. */
@Singleton
class RawgApi @Inject constructor(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun searchGames(apiKey: String, query: String): List<RawgGameResult> =
        withContext(Dispatchers.IO) {
            val url = "https://api.rawg.io/api/games".toHttpUrl().newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("search", query)
                .addQueryParameter("page_size", "10")
                .build()

            val request = Request.Builder().url(url).get().build()
            val rawBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw RawgApiException("RAWG API hatası: ${response.code}")
                response.body.string()
            }

            val parsed = json.decodeFromString(RawgSearchResponse.serializer(), rawBody)
            parsed.results.map {
                RawgGameResult(
                    rawgId = it.id,
                    name = it.name,
                    genres = it.genres.joinToString(", ") { genre -> genre.name },
                    coverUrl = it.backgroundImage,
                )
            }
        }
}
