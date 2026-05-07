package com.universe_st.quickwriter.data.remote

import com.universe_st.quickwriter.data.local.dao.AiMessageDao
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.dao.AiOperationDao
import com.universe_st.quickwriter.data.local.dao.AiSessionDao
import com.universe_st.quickwriter.data.local.dao.ProjectDao
import com.universe_st.quickwriter.data.local.entity.AiMessageEntity
import com.universe_st.quickwriter.data.local.entity.AiSessionEntity
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.MessageRole
import com.universe_st.quickwriter.domain.model.SessionContext
import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.SessionSummary
import com.universe_st.quickwriter.domain.model.ToolCall
import com.universe_st.quickwriter.domain.model.ToolCallFunction
import com.universe_st.quickwriter.util.PromptManager
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SessionManager(
    private val aiSessionDao: AiSessionDao,
    private val aiMessageDao: AiMessageDao,
    private val aiOperationDao: AiOperationDao,
    private val projectDao: ProjectDao,
    private val aiModelConfigDao: AiModelConfigDao,
    private val promptManager: PromptManager
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val _sessionStates = ConcurrentHashMap<String, MutableStateFlow<SessionState>>()
    private val _sessionListState = MutableStateFlow<List<SessionSummary>>(emptyList())
    private val _sessionCache = ConcurrentHashMap<String, AiSessionEntity>()
    private val _sessionContexts = ConcurrentHashMap<String, SessionContext>()

    private var _activeProjectId: String? = null
    private var idleRecycleJob: Job? = null

    fun setActiveProject(projectId: String) {
        if (_activeProjectId != projectId) {
            _activeProjectId = projectId
            launch {
                refreshSessionList(projectId)
            }
        }
    }

    fun createSession(projectId: String, systemPrompt: String?, modelConfigId: Int?): String {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val resolvedModelConfigId = modelConfigId ?: 0
        val resolvedSystemPrompt = systemPrompt ?: promptManager.getDefaultAssistantPrompt()

        val entity = AiSessionEntity(
            sessionId = sessionId,
            projectId = projectId,
            title = "",
            modelConfigId = resolvedModelConfigId,
            systemPrompt = resolvedSystemPrompt,
            createdAt = now,
            updatedAt = now
        )

        _sessionCache[sessionId] = entity
        _sessionStates[sessionId] = MutableStateFlow(SessionState.Idle)

        val context = SessionContext(
            sessionId = sessionId,
            projectId = projectId,
            title = "",
            systemPrompt = resolvedSystemPrompt,
            modelConfigId = resolvedModelConfigId,
            messages = emptyList(),
            createdAt = now,
            updatedAt = now
        )
        _sessionContexts[sessionId] = context

        launch {
            aiSessionDao.insertSession(entity)
            refreshSessionList(projectId)
        }

        return sessionId
    }

    fun createSessionWithProjectInfo(
        projectId: String,
        projectTitle: String,
        projectAuthor: String,
        projectGenre: String,
        storagePath: String,
        modelConfigId: Int?
    ): String {
        val systemPrompt = buildSystemPrompt(projectTitle, projectAuthor, projectGenre, storagePath)
        val resolvedModelConfigId = modelConfigId ?: 0

        val sessionId = createSession(projectId, systemPrompt, resolvedModelConfigId)

        launch {
            val defaultConfig = aiModelConfigDao.getDefaultConfig()
            if (defaultConfig != null && modelConfigId == null) {
                val entity = _sessionCache[sessionId]
                if (entity != null) {
                    val updated = entity.copy(modelConfigId = defaultConfig.id)
                    _sessionCache[sessionId] = updated
                    _sessionContexts[sessionId]?.let { ctx ->
                        _sessionContexts[sessionId] = ctx.copy(modelConfigId = defaultConfig.id)
                    }
                    aiSessionDao.updateSession(updated)
                }
            }
        }

        return sessionId
    }

    fun deleteSession(sessionId: String) {
        _sessionCache.remove(sessionId)
        _sessionStates.remove(sessionId)
        _sessionContexts.remove(sessionId)

        launch {
            val entity = aiSessionDao.getSessionById(sessionId)
            aiSessionDao.deleteSession(sessionId)
            entity?.let { refreshSessionList(it.projectId) }
        }
    }

    fun switchToSession(sessionId: String) {
        if (!_sessionStates.containsKey(sessionId)) {
            _sessionStates[sessionId] = MutableStateFlow(SessionState.Idle)
        }
        cancelIdleRecycle()
    }

    fun getSessionList(): List<SessionSummary> {
        return _sessionListState.value
    }

    fun getSessionDetail(sessionId: String): SessionDetail? {
        val entity = _sessionCache[sessionId] ?: return null
        val detail = entity.toSessionDetail()
        val state = _sessionStates[sessionId]?.value
        return detail.copy(isGenerating = state is SessionState.Generating)
    }

    fun getSessionContext(sessionId: String): SessionContext? {
        return _sessionContexts[sessionId]
    }

    suspend fun loadSessionContext(sessionId: String): SessionContext? {
        val entity = _sessionCache[sessionId] ?: aiSessionDao.getSessionById(sessionId) ?: return null
        _sessionCache[sessionId] = entity

        val messages = aiMessageDao.getMessagesBySession(sessionId).map { entity ->
            entity.toChatMessage()
        }

        val context = SessionContext(
            sessionId = entity.sessionId,
            projectId = entity.projectId,
            title = entity.title,
            systemPrompt = entity.systemPrompt,
            modelConfigId = entity.modelConfigId,
            messages = messages,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
        _sessionContexts[sessionId] = context
        _sessionStates.getOrPut(sessionId) { MutableStateFlow(SessionState.Idle) }
        return context
    }

    fun updateSessionContext(sessionId: String, messages: List<ChatMessage>) {
        _sessionContexts[sessionId]?.let { context ->
            _sessionContexts[sessionId] = context.copy(
                messages = messages,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun rebuildSessionContext(sessionId: String, newMessages: List<ChatMessage>) {
        _sessionContexts[sessionId]?.let { context ->
            val systemMsg = ChatMessage(
                role = MessageRole.SYSTEM,
                content = context.systemPrompt,
                timestamp = context.createdAt
            )
            val allMessages = listOf(systemMsg) + newMessages.filter { it.role != MessageRole.SYSTEM }
            _sessionContexts[sessionId] = context.copy(
                messages = allMessages,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun appendMessageToContext(sessionId: String, message: ChatMessage) {
        _sessionContexts[sessionId]?.let { context ->
            _sessionContexts[sessionId] = context.copy(
                messages = context.messages + message,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    fun renameSession(sessionId: String, title: String) {
        _sessionCache[sessionId]?.let { cached ->
            _sessionCache[sessionId] = cached.copy(title = title, updatedAt = System.currentTimeMillis())
        }
        _sessionContexts[sessionId]?.let { ctx ->
            _sessionContexts[sessionId] = ctx.copy(title = title, updatedAt = System.currentTimeMillis())
        }

        launch {
            val now = System.currentTimeMillis()
            aiSessionDao.updateSessionTitle(sessionId, title, now)
        }
    }

    fun deleteMessage(sessionId: String, messageIndex: Int) {
        launch {
            aiMessageDao.deleteMessagesFrom(sessionId, messageIndex)
        }
    }

    fun setSessionState(sessionId: String, state: SessionState) {
        _sessionStates[sessionId]?.value = state
    }

    fun getSessionState(sessionId: String): MutableStateFlow<SessionState> {
        return _sessionStates.getOrPut(sessionId) {
            MutableStateFlow(SessionState.Idle)
        }
    }

    fun observeSessionState(sessionId: String): StateFlowWrapper<SessionState> {
        return StateFlowWrapper(getSessionState(sessionId).asStateFlow())
    }

    fun observeSessionList(): StateFlowWrapper<List<SessionSummary>> {
        return StateFlowWrapper(_sessionListState.asStateFlow())
    }

    fun refreshSessionList(projectId: String) {
        launch {
            val sessions = aiSessionDao.getSessionsByProjectDirect(projectId)
            val summaries = sessions.map { entity ->
                val messageCount = try {
                    aiMessageDao.getMessagesBySession(entity.sessionId).size
                } catch (e: Exception) {
                    0
                }
                val lastMessage = try {
                    val messages = aiMessageDao.getMessagesBySession(entity.sessionId)
                    messages.lastOrNull { !it.isSilent }?.content?.take(50) ?: ""
                } catch (e: Exception) {
                    ""
                }

                SessionSummary(
                    sessionId = entity.sessionId,
                    title = entity.title.ifEmpty { "Untitled" },
                    lastMessage = lastMessage,
                    messageCount = messageCount,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    isActive = isGenerating(entity.sessionId)
                )
            }
            _sessionListState.value = summaries
        }
    }

    fun loadSessionFromDb(sessionId: String) {
        launch {
            val entity = aiSessionDao.getSessionById(sessionId)
            if (entity != null) {
                _sessionCache[sessionId] = entity
                if (!_sessionStates.containsKey(sessionId)) {
                    _sessionStates[sessionId] = MutableStateFlow(SessionState.Idle)
                }
            }
        }
    }

    fun scheduleIdleRecycle() {
        cancelIdleRecycle()
        idleRecycleJob = launch {
            delay(IDLE_RECYCLE_DELAY_MS)
            clear()
        }
    }

    private fun cancelIdleRecycle() {
        idleRecycleJob?.cancel()
        idleRecycleJob = null
    }

    fun clear() {
        _sessionCache.clear()
        _sessionStates.clear()
        _sessionContexts.clear()
    }

    fun isGenerating(sessionId: String): Boolean {
        return _sessionStates[sessionId]?.value is SessionState.Generating
    }

    fun buildSystemPrompt(title: String, author: String, genre: String, storagePath: String): String {
        return promptManager.getNovelWritingAssistantPrompt(title, author, genre, storagePath)
    }

    companion object {
        private const val IDLE_RECYCLE_DELAY_MS = 5 * 60 * 1000L
    }
}

private fun AiSessionEntity.toSessionDetail() = SessionDetail(
    sessionId = sessionId,
    projectId = projectId,
    title = title,
    systemPrompt = systemPrompt,
    modelConfigId = modelConfigId,
    messageCount = 0,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isGenerating = false
)

private fun AiMessageEntity.toChatMessage(): ChatMessage {
    val toolCalls: List<ToolCall>? = toolCallsJson?.takeIf { it.isNotBlank() }?.let { json: String ->
        try {
            val array = com.google.gson.JsonParser.parseString(json).asJsonArray
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
            Timber.w(e, "toChatMessage: failed to parse toolCallsJson for message id=%d", id)
            null
        }
    }

    return ChatMessage(
        id = id,
        role = try { MessageRole.valueOf(role.uppercase()) } catch (e: Exception) { MessageRole.USER },
        content = content,
        tokenCount = tokenCount,
        toolCalls = toolCalls,
        toolCallId = toolCallId,
        silent = isSilent,
        reasoningContent = reasoningContent,
        timestamp = createdAt
    )
}
