package com.example.reservas.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun formatDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return "Sin fecha"
    return try {
        // Handle ISO formats like 2026-06-26T15:56:15.136940Z or 2026-06-26T15:56:15
        val cleanedDate = isoDate.split(".")[0].replace("Z", "")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(cleanedDate)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        date?.let { outputFormat.format(it) } ?: "Fecha inválida"
    } catch (e: Exception) {
        Log.e("Utils", "Error formatting date: $isoDate", e)
        isoDate ?: "Error"
    }
}

fun isInactive(lastUpdate: String?): Boolean {
    if (lastUpdate.isNullOrBlank()) return true
    return try {
        val cleanedDate = lastUpdate.split(".")[0].replace("Z", "")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(cleanedDate)
        val now = Calendar.getInstance().time
        val diff = now.time - (date?.time ?: 0)
        diff > 30 * 60 * 1000 // 30 minutes in ms
    } catch (e: Exception) {
        true
    }
}
