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
    }

    val githubPat: Flow<String?> = context.dataStore.data.map { it[Keys.GITHUB_PAT] }
    val githubUsername: Flow<String?> = context.dataStore.data.map { it[Keys.GITHUB_USERNAME] }

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
}
