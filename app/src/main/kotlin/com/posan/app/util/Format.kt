package com.posan.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Format {
    private val idLocale = Locale("in", "ID")
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(idLocale)

    fun number(value: Double): String = numberFormat.format(value)

    fun money(value: Double, symbol: String = "Rp"): String =
        "$symbol ${numberFormat.format(value)}"

    fun moneyShort(value: Double, symbol: String = "Rp"): String =
        "$symbol${numberFormat.format(value)}"

    fun datetime(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy HH:mm", idLocale).format(Date(timestamp))

    fun date(timestamp: Long): String =
        SimpleDateFormat("dd MMM yyyy", idLocale).format(Date(timestamp))

    fun timeOnly(timestamp: Long): String =
        SimpleDateFormat("HH:mm", idLocale).format(Date(timestamp))

    fun startOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun generateTransactionCode(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
        return "TRX-${sdf.format(Date(timestamp))}"
    }
}
