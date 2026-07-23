package com.omer.mesuper.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Bir modülün vurgu rengi üçlüsü: aksan + üzerine bir şey konabilecek yumuşak kap + kap üzeri metin. */
data class ModuleColor(
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

/** Oyunsu kimliğin temeli: her modülün kendi rengi. */
data class ModuleColors(
    val dashboard: ModuleColor,
    val finance: ModuleColor,
    val agenda: ModuleColor,
    val activity: ModuleColor,
)

val LightModuleColors = ModuleColors(
    dashboard = ModuleColor(Color(0xFF2B4C7E), Color(0xFFD6E2F5), Color(0xFF0A1526)),
    finance = ModuleColor(Color(0xFF2E7D32), Color(0xFFC6EFC8), Color(0xFF0A2E0C)),
    agenda = ModuleColor(Color(0xFF6A3DB8), Color(0xFFE7DDFF), Color(0xFF21005D)),
    activity = ModuleColor(Color(0xFFE8590C), Color(0xFFFFDBC7), Color(0xFF351000)),
)

val DarkModuleColors = ModuleColors(
    dashboard = ModuleColor(Color(0xFFAEC6FF), Color(0xFF274777), Color(0xFFD7E2FF)),
    finance = ModuleColor(Color(0xFF7DD180), Color(0xFF1F5423), Color(0xFF99EE9B)),
    agenda = ModuleColor(Color(0xFFCFBCFF), Color(0xFF4F378B), Color(0xFFE9DDFF)),
    activity = ModuleColor(Color(0xFFFFB68F), Color(0xFF7A3D14), Color(0xFFFFDBC7)),
)

/** Composable ağacına aktif modül paletini taşır (aydınlık/karanlığa göre MeSuperTheme sağlar). */
val LocalModuleColors = staticCompositionLocalOf { LightModuleColors }
