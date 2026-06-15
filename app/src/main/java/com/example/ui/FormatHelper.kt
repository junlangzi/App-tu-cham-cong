package com.example.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object FormatHelper {
    private val decimalFormatter = DecimalFormat("#,###").apply {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        decimalFormatSymbols = symbols
    }

    fun formatVnd(amount: Double): String {
        return "${decimalFormatter.format(amount)} đ"
    }

    fun formatVndNoSymbol(amount: Double): String {
        return decimalFormatter.format(amount)
    }

    fun formatRatio(ratio: Double): String {
        return if (ratio % 1.0 == 0.0) {
            ratio.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", ratio)
        }
    }
}
