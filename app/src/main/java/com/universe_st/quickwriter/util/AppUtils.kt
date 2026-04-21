package com.universe_st.quickwriter.util

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

    fun formatRelativeTime(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - timestamp

        return when {
            diff < 60_000L -> "刚刚"
            diff < 3600_000L -> "${diff / 60_000L}分钟前"
            diff < 86400_000L -> "${diff / 3600_000L}小时前"
            diff < 604800_000L -> "${diff / 86400_000L}天前"
            diff < 2592000_000L -> "${diff / 604800_000L}周前"
            diff < 31536000_000L -> "${diff / 2592000_000L}个月前"
            else -> "${diff / 31536000_000L}年前"
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

    fun formatWordCount(wordCount: Int): String {
        return when {
            wordCount > 10000 -> String.format("%.1f万字", wordCount / 10000.0)
            wordCount > 1000 -> String.format("%.1f千字", wordCount / 1000.0)
            else -> "${wordCount}字"
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