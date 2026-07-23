package com.omer.mesuper.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bütünleşik akış tasarım dili (Faz 6): ekranlar artık bölüm-başına kart yerine
 * "zeminde akan içerik" olarak kurulur. Hiyerarşi kutu yerine renk + tipografi +
 * boşlukla verilir. Üç yapı taşı:
 *   - [ScreenTitle]   : içerikte büyük başlık (her ekranın ilk öğesi)
 *   - [HeroPanel]     : ekran başına TEK dolu-renkli çıpa (odak istatistik, gradyansız)
 *   - [SectionHeader] : modül renkli ikon çipli, kartsız bölüm ayıracı
 *   - [RowDivider]    : liste satırları arası ince ayraç
 * [SoftPanel] yalnızca gerçek vurgu gereken tek-tük yerde (kutu dilini geri getirmez).
 */

/** İçerikte büyük başlık: opsiyonel üst satır (tarih/selam/ay) + Fredoka başlık + opsiyonel sağ aksiyon. */
@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        trailing?.invoke()
    }
}

/**
 * Ekran başına tek dolu-renkli hero çıpası — odak istatistiği barındırır.
 * **Gradyan yok**: solid [containerColor]. Modül aksanı gibi şema-dışı bir renk
 * verildiğinde [contentColor] elle geçilmeli (contentColorFor yalnızca şema rollerini bilir).
 */
@Composable
fun HeroPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: Dp = 20.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/**
 * Seyrek kullanılan yumuşak tonal blok — gerçek vurgu gereken *tek-tük* yerde
 * (içgörü callout'u, pomodoro odağı). Elevation yok, kenarlık yok; her bölüm için değil.
 */
@Composable
fun SoftPanel(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: Dp = 16.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** Liste satırları arası ince ayraç (hairline). Son satırdan sonra çizilmez. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

/** Modül renginde yumuşak daire içinde bir ikon — emoji başlık göstergelerinin yerine. */
@Composable
fun ModuleIcon(
    icon: ImageVector,
    contentDescription: String?,
    module: ModuleColor,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(module.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = module.accent,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

/**
 * Kartsız bölüm ayıracı: modül renkli ikon çipi + Fredoka başlık + opsiyonel sağ aksiyon.
 * Doğrudan `LazyColumn` öğesi olarak kullanılır; [topPadding] bölümler arası nefes boşluğunu taşır.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    module: ModuleColor? = null,
    topPadding: Dp = 12.dp,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null && module != null) {
            ModuleIcon(icon = icon, contentDescription = null, module = module, size = 32.dp)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** Küçük, sessiz sil butonu — liste satırlarının sonunda. */
@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Sil",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Etiket + dev Fredoka rakam — özet/hero istatistikler için. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = valueColor)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
