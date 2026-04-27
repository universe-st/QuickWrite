package com.universe_st.quickwriter.util

import android.content.Context
import com.universe_st.quickwriter.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AppUtils {

    fun generateProjectId(): String {
        return UUID.randomUUID().toString()
    }

    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    fun formatTimestamp(timestamp: Long, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatRelativeTime(context: Context, timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp

        return when {
            diff < 60_000L -> context.getString(R.string.time_just_now)
            diff < 3600_000L -> context.getString(R.string.time_minutes_ago, diff / 60_000L)
            diff < 86400_000L -> context.getString(R.string.time_hours_ago, diff / 3600_000L)
            diff < 604800_000L -> context.getString(R.string.time_days_ago, diff / 86400_000L)
            diff < 2592000_000L -> context.getString(R.string.time_weeks_ago, diff / 604800_000L)
            diff < 31536000_000L -> context.getString(R.string.time_months_ago, diff / 2592000_000L)
            else -> context.getString(R.string.time_years_ago, diff / 31536000_000L)
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    fun formatWordCount(context: Context, wordCount: Int): String {
        return when {
            wordCount > 10000 -> context.getString(R.string.word_count_ten_k, wordCount / 10000.0)
            wordCount > 1000 -> context.getString(R.string.word_count_k, wordCount / 1000.0)
            else -> context.getString(R.string.word_count_single, wordCount)
        }
    }

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidUrl(url: String): Boolean {
        return android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    fun sanitizeFileName(fileName: String): String {
        val invalidChars = Regex("[\\\\/:*?\"<>|]")
        return fileName.replace(invalidChars, "_")
    }

    fun truncateText(text: String, maxLength: Int = 100): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.substring(0, maxLength) + "..."
        }
    }
}