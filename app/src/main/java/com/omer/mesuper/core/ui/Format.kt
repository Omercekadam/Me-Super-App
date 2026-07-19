package com.omer.mesuper.core.ui

import java.text.NumberFormat
import java.util.Locale

private val trCurrency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

/** Kuruş cinsinden tutarı "₺1.234,56" biçiminde gösterir. Para hesabı her zaman Long kuruş; Double yalnızca görüntüleme için. */
fun Long.formatKurusAsTl(): String = trCurrency.format(this / 100.0)

/**
 * Kullanıcı girişini kuruşa çevirir: "1.234,56" -> 123456, "250" -> 25000, "99.90" -> 9990.
 * Hem virgül hem nokta ondalık ayracı kabul edilir; geçersiz girişte null döner.
 */
fun parseTlToKurus(input: String): Long? {
    val cleaned = input.trim().replace("₺", "").replace(" ", "")
    if (cleaned.isEmpty()) return null
    val lastComma = cleaned.lastIndexOf(',')
    val lastDot = cleaned.lastIndexOf('.')
    val decimalSep = when {
        lastComma > lastDot -> ','
        lastDot > lastComma && cleaned.length - lastDot <= 3 -> '.'
        else -> null
    }
    val normalized = if (decimalSep != null) {
        val intPart = cleaned.substring(0, cleaned.lastIndexOf(decimalSep)).filter(Char::isDigit)
        val fracPart = cleaned.substring(cleaned.lastIndexOf(decimalSep) + 1).filter(Char::isDigit)
        if (fracPart.length > 2) return null
        "$intPart.${fracPart.padEnd(2, '0')}"
    } else {
        val digits = cleaned.filter(Char::isDigit)
        if (digits.isEmpty()) return null
        "$digits.00"
    }
    return runCatching {
        val (lira, kurus) = normalized.split(".")
        lira.ifEmpty { "0" }.toLong() * 100 + kurus.toLong()
    }.getOrNull()
}
