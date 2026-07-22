package com.omer.mesuper.feature.activity.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omer.mesuper.core.network.RawgGameResult
import com.omer.mesuper.core.network.TmdbMediaKind
import com.omer.mesuper.core.network.TmdbSearchResult
import com.omer.mesuper.feature.activity.data.GameSource
import com.omer.mesuper.feature.activity.data.GameStatus
import com.omer.mesuper.feature.activity.data.MediaEntity
import com.omer.mesuper.feature.activity.data.MediaStatus
import com.omer.mesuper.feature.activity.data.MediaType
import com.omer.mesuper.feature.activity.data.RaceNoteEntity
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun GameStatus.label() = when (this) {
    GameStatus.PLAYING -> "Oynuyorum"
    GameStatus.WISHLIST -> "İstek Listesi"
    GameStatus.DONE -> "Bitirdim"
}

private fun formatLapTime(ms: Long): String {
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val millis = ms % 1000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}

private val tabTitles = listOf("Oyunlar", "Film/Dizi", "Yarış")

@Composable
fun ActivityScreen(vm: ActivityViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val actionStatus by vm.actionStatus.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    var showGameDialog by rememberSaveable { mutableStateOf(false) }
    var showMediaDialog by rememberSaveable { mutableStateOf(false) }
    var showRaceDialog by rememberSaveable { mutableStateOf(false) }
    var ratingGameId by remember { mutableStateOf<Long?>(null) }
    var ratingMedia by remember { mutableStateOf<MediaEntity?>(null) }
    var playtimeGameId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let { error ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("Analiz hatası: $error", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
            }
        }

        item { WeeklyBalanceSection(state.weeklyBalance, state.genreBreakdown) }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
                }
            }
        }

        when (selectedTab) {
            0 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = vm::refreshSteamLibrary) { Text("🔄 Steam") }
                        TextButton(onClick = { showGameDialog = true }) { Text("+ Ekle") }
                    }
                }
                actionStatus?.let { msg -> item { Text(msg, style = MaterialTheme.typography.bodySmall) } }
                if (state.games.isEmpty()) {
                    item { Text("Henüz oyun yok.", style = MaterialTheme.typography.bodySmall) }
                }
                items(state.games, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onRate = { ratingGameId = game.id },
                        onStatusChange = { vm.setGameStatus(game.id, it) },
                        onLogPlaytime = { playtimeGameId = game.id },
                        onDelete = { vm.deleteGame(game.id) },
                    )
                }
            }
            1 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showMediaDialog = true }) { Text("+ Ekle") }
                    }
                }
                if (state.media.isEmpty()) {
                    item { Text("Henüz film/dizi yok.", style = MaterialTheme.typography.bodySmall) }
                }
                items(state.media, key = { it.id }) { media ->
                    MediaCard(media = media, onMarkWatched = { ratingMedia = media }, onDelete = { vm.deleteMedia(media.id) })
                }
            }
            2 -> {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showRaceDialog = true }) { Text("+ Ekle") }
                    }
                }
                if (state.raceNotes.isEmpty()) {
                    item { Text("Henüz yarış notu yok.", style = MaterialTheme.typography.bodySmall) }
                }
                items(state.raceNotes, key = { it.id }) { note ->
                    RaceNoteCard(note = note, onDelete = { vm.deleteRaceNote(note.id) })
                }
            }
        }
    }

    if (showGameDialog) {
        AddGameDialog(
            onSearchRawg = vm::searchRawgGames,
            onAddManual = vm::addManualGame,
            onAddRawg = vm::addRawgGame,
            onDismiss = { showGameDialog = false },
        )
    }
    if (showMediaDialog) {
        AddMediaDialog(onSearch = vm::searchTmdb, onAdd = vm::addMedia, onDismiss = { showMediaDialog = false })
    }
    if (showRaceDialog) {
        AddRaceNoteDialog(onConfirm = vm::addRaceNote, onDismiss = { showRaceDialog = false })
    }
    ratingGameId?.let { id ->
        val current = state.games.firstOrNull { it.id == id }?.rating10
        RateDialog(current = current, onConfirm = { vm.rateGame(id, it) }, onDismiss = { ratingGameId = null })
    }
    ratingMedia?.let { media ->
        RateDialog(current = media.rating10, onConfirm = { vm.markMediaWatched(media, it) }, onDismiss = { ratingMedia = null })
    }
    playtimeGameId?.let { id ->
        AddPlaytimeDialog(onConfirm = { date, minutes -> vm.addPlayLog(id, date, minutes) }, onDismiss = { playtimeGameId = null })
    }
}

@Composable
private fun WeeklyBalanceSection(balance: WeeklyBalance?, genres: List<GenreMinutes>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Haftalık Denge", style = MaterialTheme.typography.titleMedium)
            if (balance == null || (balance.gameMinutes == 0 && balance.workMinutes == 0)) {
                Text(
                    "Bu hafta henüz oyun/pomodoro verisi yok.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LinearProgressIndicator(
                    progress = { balance.gameRatio.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    "🎮 ${balance.gameMinutes} dk (%${"%.0f".format(balance.gameRatio * 100)}) • " +
                        "🎯 ${balance.workMinutes} dk (%${"%.0f".format(balance.workRatio * 100)})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (genres.isNotEmpty()) {
                Text("Tür kırılımı (son 7 gün)", style = MaterialTheme.typography.labelMedium)
                genres.take(5).forEach { g ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(g.genre, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${g.minutes} dk (%${"%.0f".format(g.ratio * 100)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameRowUi,
    onRate: () -> Unit,
    onStatusChange: (GameStatus) -> Unit,
    onLogPlaytime: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    game.coverUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(game.name, style = MaterialTheme.typography.titleSmall)
                        if (game.genres.isNotBlank()) {
                            Text(game.genres, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                DeleteButton(onDelete)
            }
            Text(
                "Son 7 gün: ${game.minutesLast7Days} dk" + (game.rating10?.let { " • ⭐ $it/10" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GameStatus.entries.forEach { s ->
                    FilterChip(selected = game.status == s, onClick = { onStatusChange(s) }, label = { Text(s.label()) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRate) { Text("⭐ Puanla") }
                if (game.source == GameSource.MANUAL) {
                    TextButton(onClick = onLogPlaytime) { Text("⏱ Süre Ekle") }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(media: MediaEntity, onMarkWatched: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            media.posterUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(media.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    (if (media.type == MediaType.MOVIE) "Film" else "Dizi") +
                        (media.rating10?.let { " • ⭐ $it/10" } ?: "") +
                        if (media.status == MediaStatus.WATCHED) " • İzlendi" else " • İzleme listesi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (media.status == com.omer.mesuper.feature.activity.data.MediaStatus.WATCHLIST) {
                TextButton(onClick = onMarkWatched) { Text("İzledim") }
            }
            DeleteButton(onDelete)
        }
    }
}

@Composable
private fun RaceNoteCard(note: RaceNoteEntity, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${note.sim} • ${note.track}", style = MaterialTheme.typography.titleSmall)
                DeleteButton(onDelete)
            }
            Text(
                "${note.car} • ${note.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            note.bestLapMs?.let { Text("⏱ ${formatLapTime(it)}", style = MaterialTheme.typography.bodyMedium) }
            if (note.setupNotes.isNotBlank()) Text(note.setupNotes, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RateDialog(current: Int?, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(current ?: 5) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Puanla") },
        text = {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..10).forEach { n -> FilterChip(selected = selected == n, onClick = { selected = n }, label = { Text("$n") }) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected); onDismiss() }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun AddPlaytimeDialog(onConfirm: (LocalDate, Int) -> Unit, onDismiss: () -> Unit) {
    var minutesInput by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oynama Süresi Ekle") },
        text = {
            OutlinedTextField(
                value = minutesInput,
                onValueChange = { minutesInput = it.filter(Char::isDigit) },
                label = { Text("Bugün için dakika") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { minutesInput.toIntOrNull()?.let { onConfirm(LocalDate.now(), it) }; onDismiss() },
                enabled = (minutesInput.toIntOrNull() ?: 0) > 0,
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun AddGameDialog(
    onSearchRawg: suspend (String) -> Result<List<RawgGameResult>>,
    onAddManual: (name: String, genres: String, status: GameStatus) -> Unit,
    onAddRawg: (RawgGameResult, GameStatus) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchMode by rememberSaveable { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var manualName by rememberSaveable { mutableStateOf("") }
    var manualGenres by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(GameStatus.PLAYING) }
    var results by remember { mutableStateOf<List<RawgGameResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<RawgGameResult?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oyun Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = searchMode, onClick = { searchMode = true }, label = { Text("RAWG'da ara") })
                    FilterChip(selected = !searchMode, onClick = { searchMode = false }, label = { Text("Elle ekle") })
                }
                if (searchMode) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Oyun adı") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = {
                                searching = true; searchError = null
                                scope.launch {
                                    onSearchRawg(query).fold(
                                        onSuccess = { results = it; searching = false },
                                        onFailure = { searchError = it.message; searching = false },
                                    )
                                }
                            },
                            enabled = query.isNotBlank() && !searching,
                        ) { Text("Ara") }
                    }
                    if (searching) Text("Aranıyor…", style = MaterialTheme.typography.bodySmall)
                    searchError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Column(
                        modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        results.forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selected = r }
                                    .background(if (selected == r) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                    .padding(6.dp),
                            ) {
                                Column {
                                    Text(r.name, style = MaterialTheme.typography.bodyMedium)
                                    if (r.genres.isNotBlank()) {
                                        Text(r.genres, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text("İsim") }, singleLine = true)
                    OutlinedTextField(
                        value = manualGenres,
                        onValueChange = { manualGenres = it },
                        label = { Text("Türler (virgülle)") },
                        singleLine = true,
                    )
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GameStatus.entries.forEach { s -> FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label()) }) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (searchMode) selected?.let { onAddRawg(it, status) } else onAddManual(manualName.trim(), manualGenres.trim(), status)
                    onDismiss()
                },
                enabled = if (searchMode) selected != null else manualName.isNotBlank(),
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun AddMediaDialog(
    onSearch: suspend (TmdbMediaKind, String) -> Result<List<TmdbSearchResult>>,
    onAdd: (TmdbSearchResult, MediaType) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(TmdbMediaKind.MOVIE) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<TmdbSearchResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<TmdbSearchResult?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Film/Dizi Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = kind == TmdbMediaKind.MOVIE, onClick = { kind = TmdbMediaKind.MOVIE }, label = { Text("Film") })
                    FilterChip(selected = kind == TmdbMediaKind.TV, onClick = { kind = TmdbMediaKind.TV }, label = { Text("Dizi") })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Ara") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            searching = true; searchError = null
                            scope.launch {
                                onSearch(kind, query).fold(
                                    onSuccess = { results = it; searching = false },
                                    onFailure = { searchError = it.message; searching = false },
                                )
                            }
                        },
                        enabled = query.isNotBlank() && !searching,
                    ) { Text("Ara") }
                }
                if (searching) Text("Aranıyor…", style = MaterialTheme.typography.bodySmall)
                searchError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Column(
                    modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    results.forEach { r ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selected = r }
                                .background(if (selected == r) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .padding(6.dp),
                        ) { Text(r.title, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selected?.let { onAdd(it, if (kind == TmdbMediaKind.MOVIE) MediaType.MOVIE else MediaType.TV) }
                    onDismiss()
                },
                enabled = selected != null,
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun AddRaceNoteDialog(
    onConfirm: (date: LocalDate, sim: String, track: String, car: String, bestLapMs: Long?, setupNotes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var sim by rememberSaveable { mutableStateOf("") }
    var track by rememberSaveable { mutableStateOf("") }
    var car by rememberSaveable { mutableStateOf("") }
    var lapInput by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Yarış Notu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = sim, onValueChange = { sim = it }, label = { Text("Simülatör") }, singleLine = true)
                OutlinedTextField(value = track, onValueChange = { track = it }, label = { Text("Pist") }, singleLine = true)
                OutlinedTextField(value = car, onValueChange = { car = it }, label = { Text("Araç") }, singleLine = true)
                OutlinedTextField(
                    value = lapInput,
                    onValueChange = { lapInput = it },
                    label = { Text("En iyi tur (saniye, örn 83.456)") },
                    singleLine = true,
                )
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Setup notları") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bestLapMs = lapInput.toDoubleOrNull()?.let { (it * 1000).toLong() }
                    onConfirm(LocalDate.now(), sim.trim(), track.trim(), car.trim(), bestLapMs, notes.trim())
                    onDismiss()
                },
                enabled = sim.isNotBlank() && track.isNotBlank() && car.isNotBlank(),
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Sil",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
        )
    }
}
