package com.omer.mesuper.core.ui

import java.text.NumberFormat
import java.util.Locale

private val trCurrency: NumberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

/** Kuruş cinsinden tutarı "₺1.234,56" biçiminde gösterir. Para hesabı her zaman Long kuruş; Double yalnızca görüntüleme için. */
fun Long.formatKurusAsTl(): String = trCurrency.format(this / 100.0)
