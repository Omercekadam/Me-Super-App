package com.omer.mesuper.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

/** API anahtarları gibi kullanıcıya özel ayarlar — sadece cihazda, repo'ya asla girmez. */
@Singleton
class UserPrefsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val GITHUB_PAT = stringPreferencesKey("github_pat")
        val GITHUB_USERNAME = stringPreferencesKey("github_username")
        val STEAM_API_KEY = stringPreferencesKey("steam_api_key")
        val STEAM_ID = stringPreferencesKey("steam_id")
        val RAWG_API_KEY = stringPreferencesKey("rawg_api_key")
        val TMDB_API_KEY = stringPreferencesKey("tmdb_api_key")
    }

    val githubPat: Flow<String?> = context.dataStore.data.map { it[Keys.GITHUB_PAT] }
    val githubUsername: Flow<String?> = context.dataStore.data.map { it[Keys.GITHUB_USERNAME] }
    val steamApiKey: Flow<String?> = context.dataStore.data.map { it[Keys.STEAM_API_KEY] }
    val steamId: Flow<String?> = context.dataStore.data.map { it[Keys.STEAM_ID] }
    val rawgApiKey: Flow<String?> = context.dataStore.data.map { it[Keys.RAWG_API_KEY] }
    val tmdbApiKey: Flow<String?> = context.dataStore.data.map { it[Keys.TMDB_API_KEY] }

    suspend fun setGithubCredentials(pat: String, username: String) {
        context.dataStore.edit {
            it[Keys.GITHUB_PAT] = pat
            it[Keys.GITHUB_USERNAME] = username
        }
    }

    suspend fun clearGithubCredentials() {
        context.dataStore.edit {
            it.remove(Keys.GITHUB_PAT)
            it.remove(Keys.GITHUB_USERNAME)
        }
    }

    suspend fun setSteamCredentials(apiKey: String, steamId: String) {
        context.dataStore.edit {
            it[Keys.STEAM_API_KEY] = apiKey
            it[Keys.STEAM_ID] = steamId
        }
    }

    suspend fun setRawgApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.RAWG_API_KEY] = apiKey }
    }

    suspend fun setTmdbApiKey(apiKey: String) {
        context.dataStore.edit { it[Keys.TMDB_API_KEY] = apiKey }
    }
}
