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

data class SteamOwnedGame(val appId: Long, val name: String, val playtimeForeverMin: Long)

class SteamApiException(message: String) : Exception(message)

@Serializable
private data class OwnedGamesResponse(val response: OwnedGamesBody = OwnedGamesBody())

@Serializable
private data class OwnedGamesBody(val games: List<OwnedGameDto> = emptyList())

@Serializable
private data class OwnedGameDto(
    val appid: Long,
    val name: String = "",
    @SerialName("playtime_forever") val playtimeForeverMin: Long = 0,
)

/** Steam'in resmi Web API'si. Profilin "oyun detayları" gizlilik ayarı herkese açık olmalı, yoksa liste boş döner. */
@Singleton
class SteamApi @Inject constructor(private val client: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchOwnedGames(apiKey: String, steamId: String): List<SteamOwnedGame> =
        withContext(Dispatchers.IO) {
            val url = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v1/".toHttpUrl().newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("steamid", steamId)
                .addQueryParameter("include_appinfo", "true")
                .addQueryParameter("include_played_free_games", "true")
                .addQueryParameter("format", "json")
                .build()

            val request = Request.Builder().url(url).get().build()
            val rawBody = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw SteamApiException("Steam API hatası: ${response.code}")
                response.body.string()
            }

            val parsed = json.decodeFromString(OwnedGamesResponse.serializer(), rawBody)
            parsed.response.games.map {
                SteamOwnedGame(appId = it.appid, name = it.name, playtimeForeverMin = it.playtimeForeverMin)
            }
        }
}
