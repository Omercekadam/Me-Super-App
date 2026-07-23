package com.omer.mesuper.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omer.mesuper.feature.activity.ui.ActivityScreen
import com.omer.mesuper.feature.agenda.ui.AgendaScreen
import com.omer.mesuper.feature.dashboard.ui.DashboardScreen
import com.omer.mesuper.feature.finance.data.CategoryEntity
import com.omer.mesuper.feature.finance.data.FinanceRepository
import com.omer.mesuper.feature.finance.data.TxType
import com.omer.mesuper.feature.finance.ui.AddTransactionForm
import com.omer.mesuper.feature.finance.ui.FinanceScreen
import com.omer.mesuper.feature.settings.ui.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Alt navigasyon hedefleri. */
enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Ana Sayfa", Icons.Default.Home),
    FINANCE("finance", "Finans", Icons.Default.AccountBalanceWallet),
    AGENDA("agenda", "Ajanda", Icons.Default.Checklist),
    ACTIVITY("activity", "Aktivite", Icons.Default.SportsEsports),
}

/** Hızlı-giriş barının ihtiyaç duyduğu minimal VM (kategoriler + işlem ekleme). */
@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repo: FinanceRepository,
) : ViewModel() {
    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(amountKurus: Long, type: TxType, categoryId: Long, date: LocalDate, note: String) {
        viewModelScope.launch {
            repo.addTransaction(amountKurus, type, categoryId, date, note)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(quickAdd: QuickAddViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.DASHBOARD.route
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            // Faz 6: başlık metni yok — her ekran kendi büyük ScreenTitle'ını çiziyor.
            // Üst çubuk yalnızca status bar inset'i + ayarlar dişlisini taşıyan ince bir şerit.
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = {
                        navController.navigate("settings") { launchSingleTop = true }
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showQuickAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Hızlı işlem ekle")
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.DASHBOARD.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.DASHBOARD.route) { DashboardScreen() }
            composable(Destination.FINANCE.route) { FinanceScreen() }
            composable(Destination.AGENDA.route) { AgendaScreen() }
            composable(Destination.ACTIVITY.route) { ActivityScreen() }
            composable("settings") { SettingsScreen() }
        }
    }

    if (showQuickAdd) {
        val categories by quickAdd.categories.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showQuickAdd = false },
            sheetState = sheetState,
        ) {
            AddTransactionForm(
                categories = categories,
                onSave = quickAdd::add,
                onSaved = { showQuickAdd = false },
            )
        }
    }
}
