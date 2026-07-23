@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.omer.mesuper.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.omer.mesuper.R

private fun wght(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))

/** Gövde metni: yuvarlak, sıcak, okunaklı. Değişken font (wght ekseni). */
val Nunito = FontFamily(
    Font(R.font.nunito_variable, FontWeight.Normal, variationSettings = wght(400)),
    Font(R.font.nunito_variable, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.nunito_variable, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.nunito_variable, FontWeight.Bold, variationSettings = wght(700)),
)

/** Başlıklar ve dev rakamlar: tombul, oyunsu. Değişken font (wdth+wght; wdth varsayılan 100). */
val Fredoka = FontFamily(
    Font(R.font.fredoka_variable, FontWeight.Medium, variationSettings = wght(500)),
    Font(R.font.fredoka_variable, FontWeight.SemiBold, variationSettings = wght(600)),
    Font(R.font.fredoka_variable, FontWeight.Bold, variationSettings = wght(700)),
)

private val d = Typography()

/** display/headline/title → Fredoka; body/label → Nunito. */
val AppTypography = Typography(
    displayLarge = d.displayLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    displayMedium = d.displayMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.Bold),
    displaySmall = d.displaySmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    headlineLarge = d.headlineLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    headlineMedium = d.headlineMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    headlineSmall = d.headlineSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    titleLarge = d.titleLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    titleMedium = d.titleMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
    titleSmall = d.titleSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.Medium),
    bodyLarge = d.bodyLarge.copy(fontFamily = Nunito),
    bodyMedium = d.bodyMedium.copy(fontFamily = Nunito),
    bodySmall = d.bodySmall.copy(fontFamily = Nunito),
    labelLarge = d.labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
    labelMedium = d.labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Medium),
    labelSmall = d.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Medium),
)
