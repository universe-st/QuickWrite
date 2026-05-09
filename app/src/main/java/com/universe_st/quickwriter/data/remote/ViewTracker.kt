package com.universe_st.quickwriter.data.remote

import java.util.concurrent.ConcurrentHashMap

class ViewTracker {

    data class ViewRecord(
        val startLine: Int,
        val endLine: Int,
        val timestamp: Long
    )

    private val records = ConcurrentHashMap<String, ConcurrentHashMap<String, ViewRecord>>()

    fun recordView(sessionId: String, filePath: String, startLine: Int, endLine: Int) {
        val sessionRecords = records.getOrPut(sessionId) { ConcurrentHashMap() }
        sessionRecords[filePath] = ViewRecord(startLine, endLine, System.currentTimeMillis())
    }

    fun getViewRecord(sessionId: String, filePath: String): ViewRecord? {
        return records[sessionId]?.get(filePath)
    }

    fun clearSession(sessionId: String) {
        records.remove(sessionId)
    }

    fun clearFileView(sessionId: String, filePath: String) {
        records[sessionId]?.remove(filePath)
    }

    fun clearAll() {
        records.clear()
    }

    fun getFileViewHistory(sessionId: String): Map<String, ViewRecord> {
        return records[sessionId]?.toMap() ?: emptyMap()
    }
}
