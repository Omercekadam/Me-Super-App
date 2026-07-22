package com.omer.mesuper.feature.settings.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.omer.mesuper.core.backup.BackupManager
import com.omer.mesuper.core.datastore.UserPrefsStore
import com.omer.mesuper.feature.activity.data.ActivityRepository
import com.omer.mesuper.feature.agenda.data.AgendaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val userPrefsStore: UserPrefsStore,
    private val agendaRepository: AgendaRepository,
    private val activityRepository: ActivityRepository,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status

    private val _githubStatus = MutableStateFlow<String?>(null)
    val githubStatus: StateFlow<String?> = _githubStatus

    private val _steamStatus = MutableStateFlow<String?>(null)
    val steamStatus: StateFlow<String?> = _steamStatus

    private val _rawgStatus = MutableStateFlow<String?>(null)
    val rawgStatus: StateFlow<String?> = _rawgStatus

    private val _tmdbStatus = MutableStateFlow<String?>(null)
    val tmdbStatus: StateFlow<String?> = _tmdbStatus

    val githubUsername: StateFlow<String?> =
        userPrefsStore.githubUsername.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val steamId: StateFlow<String?> =
        userPrefsStore.steamId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _status.value = runCatching {
                val payload = backupManager.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Dosya açılamadı")
                }
                "✅ Yedek dışa aktarıldı"
            }.getOrElse { "❌ Dışa aktarma hatası: ${it.message}" }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            _status.value = runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Dosya okunamadı")
                }
                backupManager.importJson(raw)
                "✅ Yedek içe aktarıldı"
            }.getOrElse { "❌ İçe aktarma hatası: ${it.message}" }
        }
    }

    fun saveGithubCredentials(pat: String, username: String) {
        viewModelScope.launch {
            userPrefsStore.setGithubCredentials(pat, username)
            _githubStatus.value = runCatching {
                agendaRepository.syncGithubContributions()
                "✅ GitHub senkronize edildi"
            }.getOrElse { "❌ Senkronizasyon hatası: ${it.message}" }
        }
    }

    fun saveSteamCredentials(apiKey: String, steamId: String) {
        viewModelScope.launch {
            userPrefsStore.setSteamCredentials(apiKey, steamId)
            _steamStatus.value = runCatching {
                activityRepository.syncSteamLibrary()
                "✅ Steam kütüphanesi senkronize edildi"
            }.getOrElse { "❌ Senkronizasyon hatası: ${it.message}" }
        }
    }

    fun saveRawgApiKey(apiKey: String) {
        viewModelScope.launch {
            userPrefsStore.setRawgApiKey(apiKey)
            _rawgStatus.value = "✅ Kaydedildi"
        }
    }

    fun saveTmdbApiKey(apiKey: String) {
        viewModelScope.launch {
            userPrefsStore.setTmdbApiKey(apiKey)
            _tmdbStatus.value = "✅ Kaydedildi"
        }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val status by vm.status.collectAsStateWithLifecycle()
    val githubStatus by vm.githubStatus.collectAsStateWithLifecycle()
    val savedUsername by vm.githubUsername.collectAsStateWithLifecycle()
    val steamStatus by vm.steamStatus.collectAsStateWithLifecycle()
    val rawgStatus by vm.rawgStatus.collectAsStateWithLifecycle()
    val tmdbStatus by vm.tmdbStatus.collectAsStateWithLifecycle()
    val savedSteamId by vm.steamId.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(vm::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(vm::importFrom) }

    var patInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable(savedUsername) { mutableStateOf(savedUsername ?: "") }
    var steamKeyInput by rememberSaveable { mutableStateOf("") }
    var steamIdInput by rememberSaveable(savedSteamId) { mutableStateOf(savedSteamId ?: "") }
    var rawgKeyInput by rememberSaveable { mutableStateOf("") }
    var tmdbKeyInput by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("GitHub Streak", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Ajanda'da günlük commit sayısı ve streak için bir Personal Access Token " +
                        "(read:user, repo kapsamı yeterli) gerekir. Anahtar sadece cihazda saklanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("GitHub kullanıcı adı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = patInput,
                    onValueChange = { patInput = it },
                    label = { Text("Personal Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = { vm.saveGithubCredentials(patInput, usernameInput) },
                    enabled = patInput.isNotBlank() && usernameInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kaydet ve Senkronize Et") }
                githubStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Steam Kütüphanesi", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Oynama süresi senkronu için Steam Web API anahtarı ve 64-bit Steam ID gerekir. " +
                        "Profilin \"oyun detayları\" gizlilik ayarı herkese açık olmalı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = steamKeyInput,
                    onValueChange = { steamKeyInput = it },
                    label = { Text("Steam Web API anahtarı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = steamIdInput,
                    onValueChange = { steamIdInput = it },
                    label = { Text("Steam ID (64-bit)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = { vm.saveSteamCredentials(steamKeyInput, steamIdInput) },
                    enabled = steamKeyInput.isNotBlank() && steamIdInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kaydet ve Senkronize Et") }
                steamStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RAWG (Oyun Metadata)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Elle eklenen oyunlara kapak görseli ve tür bilgisi getirmek için ücretsiz RAWG API anahtarı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = rawgKeyInput,
                    onValueChange = { rawgKeyInput = it },
                    label = { Text("RAWG API anahtarı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = { vm.saveRawgApiKey(rawgKeyInput) },
                    enabled = rawgKeyInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kaydet") }
                rawgStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TMDB (Film/Dizi)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Film/dizi arama ve loglama için kişisel kullanım amaçlı ücretsiz TMDB API anahtarı.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = tmdbKeyInput,
                    onValueChange = { tmdbKeyInput = it },
                    label = { Text("TMDB API anahtarı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Button(
                    onClick = { vm.saveTmdbApiKey(tmdbKeyInput) },
                    enabled = tmdbKeyInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kaydet") }
                tmdbStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Yedekleme", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Tüm veriler cihazında tutulur. JSON yedeği alıp istediğin yerde saklayabilir, " +
                        "yeni cihazda içe aktarabilirsin. İçe aktarma mevcut verilerin ÜZERİNE yazar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { exportLauncher.launch("mesuper-yedek-${LocalDate.now()}.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Dışa Aktar (JSON)") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("İçe Aktar") }
                status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Hakkında", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Me SuperApp 0.1.0 — Kotlin + Compose arayüz, gömülü Python/pandas analiz motoru (Chaquopy).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
