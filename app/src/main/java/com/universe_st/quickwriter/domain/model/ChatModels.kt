package com.universe_st.quickwriter.domain.model

import com.universe_st.quickwriter.util.UiText

data class SessionContext(
    val sessionId: String,
    val projectId: String,
    val title: String,
    val systemPrompt: String,
    val modelConfigId: Int,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val updatedAt: Long
)

enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }

data class ChatMessage(
    val id: Long = 0,
    val role: MessageRole,
    val content: String,
    val tokenCount: Int = 0,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val silent: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

sealed class SessionState {
    object Idle : SessionState()
    data class Generating(val partialContent: String) : SessionState()
    data class Error(val message: UiText) : SessionState()
}

data class SessionSummary(
    val sessionId: String,
    val title: String,
    val lastMessage: String,
    val messageCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = false
)
