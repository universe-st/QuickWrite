package com.universe_st.quickwriter.data.remote

import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.SessionSummary

interface IChatService {

    fun createSession(projectId: String, systemPrompt: String?, modelConfigId: Int?): String

    fun deleteSession(sessionId: String)

    fun switchToSession(sessionId: String)

    fun getSessionList(): List<SessionSummary>

    fun getSessionDetail(sessionId: String): SessionDetail?

    fun renameSession(sessionId: String, title: String)

    fun sendMessage(sessionId: String, content: String, attachedFiles: List<String> = emptyList())

    fun stopGeneration(sessionId: String)

    fun retryLastMessage(sessionId: String)

    fun deleteMessage(sessionId: String, messageIndex: Int)

    fun observeSessionState(sessionId: String): StateFlowWrapper<SessionState>

    fun observeSessionList(): StateFlowWrapper<List<SessionSummary>>
}

data class SessionDetail(
    val sessionId: String,
    val projectId: String,
    val title: String,
    val systemPrompt: String,
    val modelConfigId: Int,
    val messageCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isGenerating: Boolean
)
