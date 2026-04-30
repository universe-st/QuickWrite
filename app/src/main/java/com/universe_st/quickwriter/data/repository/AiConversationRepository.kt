package com.universe_st.quickwriter.data.repository

import com.universe_st.quickwriter.data.local.dao.AiMessageDao
import com.universe_st.quickwriter.data.local.dao.AiSessionDao
import com.universe_st.quickwriter.data.local.entity.AiMessageEntity
import com.universe_st.quickwriter.data.local.entity.AiSessionEntity
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.MessageRole
import com.universe_st.quickwriter.domain.model.ToolCall
import com.universe_st.quickwriter.domain.model.ToolCallFunction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AiConversationRepository(
    private val aiSessionDao: AiSessionDao,
    private val aiMessageDao: AiMessageDao
) {

    fun getSessionsByProject(projectId: String): Flow<List<AiSessionEntity>> {
        return aiSessionDao.getSessionsByProject(projectId)
    }

    suspend fun getSessionById(sessionId: String): AiSessionEntity? {
        return aiSessionDao.getSessionById(sessionId)
    }

    suspend fun insertSession(session: AiSessionEntity): Long {
        return aiSessionDao.insertSession(session)
    }

    suspend fun updateSession(session: AiSessionEntity) {
        aiSessionDao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: String) {
        aiSessionDao.deleteSession(sessionId)
    }

    suspend fun updateSessionTitle(sessionId: String, title: String, updatedAt: Long) {
        aiSessionDao.updateSessionTitle(sessionId, title, updatedAt)
    }

    suspend fun getMessagesBySession(sessionId: String): List<ChatMessage> {
        return aiMessageDao.getMessagesBySession(sessionId).map { it.toDomain() }
    }

    fun getVisibleMessages(sessionId: String): Flow<List<ChatMessage>> {
        return aiMessageDao.getVisibleMessagesBySession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertMessage(message: AiMessageEntity): Long {
        return aiMessageDao.insertMessage(message)
    }

    suspend fun deleteMessagesFrom(sessionId: String, fromOrder: Int) {
        aiMessageDao.deleteMessagesFrom(sessionId, fromOrder)
    }

    suspend fun getUserMessageCount(sessionId: String): Int {
        return aiMessageDao.getUserMessageCount(sessionId)
    }

    suspend fun getLastMessage(sessionId: String): AiMessageEntity? {
        val messages = aiMessageDao.getMessagesBySession(sessionId)
        return messages.lastOrNull { !it.isSilent }
    }

    suspend fun getLastUserMessage(sessionId: String): AiMessageEntity? {
        val messages = aiMessageDao.getMessagesBySession(sessionId)
        return messages.lastOrNull { it.role == "user" && !it.isSilent }
    }

    suspend fun getNextMessageOrder(sessionId: String): Int {
        val messages = aiMessageDao.getMessagesBySession(sessionId)
        return messages.size
    }
}

fun AiMessageEntity.toDomain(): ChatMessage {
    val toolCalls: List<ToolCall>? = toolCallsJson?.let {
        try {
            val gson = com.google.gson.Gson()
            val array = com.google.gson.JsonParser.parseString(it).asJsonArray
            array.map { element ->
                val obj = element.asJsonObject
                val function = obj.getAsJsonObject("function")
                ToolCall(
                    id = obj.get("id")?.asString ?: "",
                    function = ToolCallFunction(
                        name = function.get("name")?.asString ?: "",
                        arguments = function.get("arguments")?.asString ?: ""
                    )
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    return ChatMessage(
        id = id,
        role = MessageRole.valueOf(role.uppercase()),
        content = content,
        tokenCount = tokenCount,
        toolCalls = toolCalls,
        toolCallId = toolCallId,
        silent = isSilent,
        reasoningContent = reasoningContent,
        timestamp = createdAt
    )
}
