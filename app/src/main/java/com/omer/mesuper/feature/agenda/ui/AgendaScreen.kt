package com.omer.mesuper.feature.agenda.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omer.mesuper.core.pomodoro.PomodoroPhase
import com.omer.mesuper.core.pomodoro.PomodoroState
import com.omer.mesuper.feature.agenda.data.TaskEntity

private fun parseHex(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

private val habitEmojis = listOf("💧", "📖", "🏃", "🧘", "🦷", "🥗", "😴", "✍️")
private val habitColors = listOf("#1976D2", "#388E3C", "#7B1FA2", "#EF6C00", "#C2185B", "#455A64")

@Composable
fun AgendaScreen(
    vm: AgendaViewModel = hiltViewModel(),
    pomodoroVm: PomodoroViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val pomodoroState by pomodoroVm.state.collectAsStateWithLifecycle()

    var showHabitDialog by rememberSaveable { mutableStateOf(false) }
    var showTaskDialog by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        state.error?.let { error ->
            item {
                Card {
                    Text(
                        "Analiz hatası: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        item {
            HabitChainSection(
                habits = state.habits,
                onAdd = { showHabitDialog = true },
                onToggle = vm::toggleTick,
                onDelete = vm::deleteHabit,
            )
        }

        item {
            PomodoroSection(
                state = pomodoroState,
                stats = state.pomodoroStats,
                openTasks = state.tasks.filter { !it.isDone },
                onStart = pomodoroVm::start,
                onPause = pomodoroVm::pause,
                onResume = pomodoroVm::resume,
                onStop = pomodoroVm::stop,
            )
        }

        item {
            TaskListSection(
                tasks = state.tasks,
                onAdd = { showTaskDialog = true },
                onToggleDone = { task, done -> vm.setTaskDone(task, done) },
                onDelete = vm::deleteTask,
            )
        }

        item { GithubStreakSection(state.githubStreak) }
    }

    if (showHabitDialog) {
        AddHabitDialog(onConfirm = vm::addHabit, onDismiss = { showHabitDialog = false })
    }
    if (showTaskDialog) {
        AddTaskDialog(onConfirm = vm::addTask, onDismiss = { showTaskDialog = false })
    }
}

@Composable
private fun HabitChainSection(
    habits: List<HabitRowUi>,
    onAdd: () -> Unit,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Alışkanlık Zinciri", onAdd)
            if (habits.isEmpty()) {
                Text("Henüz alışkanlık yok. Zincirini kırma!", style = MaterialTheme.typography.bodySmall)
            }
            habits.forEach { habit ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${habit.emoji} ${habit.name}")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "🔥 ${habit.currentStreak} (en uzun ${habit.longestStreak})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            DeleteButton { onDelete(habit.id) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        habit.last7Days.forEachIndexed { index, ticked ->
                            val isToday = index == habit.last7Days.lastIndex
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .then(
                                        if (ticked) {
                                            Modifier.background(parseHex(habit.colorHex), CircleShape)
                                        } else Modifier,
                                    )
                                    .then(if (isToday) Modifier.clickable { onToggle(habit.id) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (ticked) Text("✓", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PomodoroSection(
    state: PomodoroState,
    stats: PomodoroStats?,
    openTasks: List<TaskEntity>,
    onStart: (minutes: Int, taskId: Long?, taskTitle: String?) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    var selectedMinutes by rememberSaveable { mutableStateOf(25) }
    var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Pomodoro", style = MaterialTheme.typography.titleMedium)

            val minutes = state.remainingSeconds / 60
            val seconds = state.remainingSeconds % 60
            val display = if (state.phase == PomodoroPhase.IDLE) "%02d:00".format(selectedMinutes) else "%02d:%02d".format(minutes, seconds)
            Text(display, style = MaterialTheme.typography.displaySmall)

            if (state.phase == PomodoroPhase.IDLE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 25, 45).forEach { m ->
                        FilterChip(
                            selected = selectedMinutes == m,
                            onClick = { selectedMinutes = m },
                            label = { Text("$m dk") },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val selectedTitle = openTasks.firstOrNull { it.id == selectedTaskId }?.title ?: "Görev seçme (isteğe bağlı)"
                    TextButton(onClick = { taskMenuExpanded = true }) { Text("📌 $selectedTitle") }
                    DropdownMenu(expanded = taskMenuExpanded, onDismissRequest = { taskMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Görev yok") },
                            onClick = { selectedTaskId = null; taskMenuExpanded = false },
                        )
                        openTasks.forEach { task ->
                            DropdownMenuItem(
                                text = { Text(task.title) },
                                onClick = { selectedTaskId = task.id; taskMenuExpanded = false },
                            )
                        }
                    }
                }

                TextButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val title = openTasks.firstOrNull { it.id == selectedTaskId }?.title
                        onStart(selectedMinutes, selectedTaskId, title)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("▶ Başlat") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.phase == PomodoroPhase.RUNNING) {
                        TextButton(onClick = onPause) { Text("⏸ Duraklat") }
                    } else if (state.phase == PomodoroPhase.PAUSED) {
                        TextButton(onClick = onResume) { Text("▶ Devam") }
                    }
                    TextButton(onClick = onStop) { Text("⏹ Bitir") }
                }
                state.taskTitle?.let {
                    Text("Görev: $it", style = MaterialTheme.typography.bodySmall)
                }
            }

            stats?.let {
                Text(
                    "Bugün ${it.todayMinutes} dk (${it.todaySessions}) • Bu hafta ${it.weekMinutes} dk • " +
                        "Tamamlanma %${"%.0f".format(it.completionRate * 100)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TaskListSection(
    tasks: List<TaskEntity>,
    onAdd: () -> Unit,
    onToggleDone: (TaskEntity, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionHeader("Yapılacaklar", onAdd)
            if (tasks.isEmpty()) {
                Text("Görev yok.", style = MaterialTheme.typography.bodySmall)
            }
            tasks.forEach { task ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.isDone, onCheckedChange = { onToggleDone(task, it) })
                        Text(
                            task.title,
                            style = if (task.isDone) {
                                MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else MaterialTheme.typography.bodyMedium,
                        )
                    }
                    DeleteButton { onDelete(task.id) }
                }
            }
        }
    }
}

@Composable
private fun GithubStreakSection(streak: GithubStreak?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("GitHub Streak", style = MaterialTheme.typography.titleMedium)
            if (streak == null) {
                Text(
                    "Ayarlar'dan GitHub kullanıcı adı ve token girip senkronize et.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text("🔥 ${streak.currentStreak} günlük streak (en uzun ${streak.longestStreak})")
                Text(
                    "Bu yıl ${streak.totalThisYear} commit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddHabitDialog(onConfirm: (name: String, emoji: String, colorHex: String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var emoji by rememberSaveable { mutableStateOf(habitEmojis.first()) }
    var colorHex by rememberSaveable { mutableStateOf(habitColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Alışkanlık") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("İsim") }, singleLine = true)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    habitEmojis.forEach { e ->
                        FilterChip(selected = emoji == e, onClick = { emoji = e }, label = { Text(e) })
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    habitColors.forEach { c ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(parseHex(c), CircleShape)
                                .clickable { colorHex = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (colorHex == c) Text("✓", color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), emoji, colorHex); onDismiss() },
                enabled = name.isNotBlank(),
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun AddTaskDialog(onConfirm: (title: String, dueDate: java.time.LocalDate?) -> Unit, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Görev") },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Başlık") }, singleLine = true)
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), null); onDismiss() },
                enabled = title.isNotBlank(),
            ) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onAdd) { Text("+ Ekle") }
    }
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
