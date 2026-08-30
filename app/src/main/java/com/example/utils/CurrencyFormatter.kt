package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indonesianLocale = Locale("id", "ID")
    private val format = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
        maximumFractionDigits = 0
    }

    fun formatRupiah(amount: Double): String {
        return try {
            format.format(amount).replace("Rp", "Rp ").trim()
        } catch (e: Exception) {
            "Rp ${amount.toLong()}"
        }
    }

    fun formatNumber(amount: Double): String {
        return try {
            NumberFormat.getNumberInstance(indonesianLocale).format(amount)
        } catch (e: Exception) {
            amount.toLong().toString()
        }
    }
}
