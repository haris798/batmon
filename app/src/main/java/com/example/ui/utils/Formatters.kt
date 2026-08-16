package com.example.ui.utils

import java.util.Locale

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
    } else {
        String.format(Locale.getDefault(), "%dm", minutes)
    }
}

fun formatFloat(value: Float, decimals: Int = 1): String {
    return String.format(Locale.getDefault(), "%.${decimals}f", value)
}
