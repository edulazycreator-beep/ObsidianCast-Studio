package com.obsidiancast.studio.utils

import java.util.Locale

object TimecodeFormatter {
    fun formatDurationHhMmSs(totalSeconds: Long): String {
        val safeSec = totalSeconds.coerceAtLeast(0L)
        val hours = safeSec / 3600L
        val minutes = (safeSec % 3600L) / 60L
        val seconds = safeSec % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatByteSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
