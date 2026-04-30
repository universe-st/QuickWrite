package com.universe_st.quickwriter.presentation.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.AiSessionEntity
import com.universe_st.quickwriter.data.remote.AIChatService
import com.universe_st.quickwriter.data.remote.IChatService
import com.universe_st.quickwriter.data.remote.SessionDetail
import com.universe_st.quickwriter.data.repository.AiConversationRepository
import com.universe_st.quickwriter.data.repository.AiModelConfigRepository
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.SessionSummary
import kotlinx.coroutines.Job
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AiChatViewModel(
    application: Application,
    private val conversationRepository: AiConversationRepository,
    private val aiModelConfigRepository: AiModelConfigRepository
) : AndroidViewModel(application) {

    private var chatService: IChatService? = null
    var isServiceBound by mutableStateOf(false)
        private set

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            chatService = (binder as? AIChatService.ChatServiceBinder)?.getService()
            isServiceBound = true
            selectedProjectId?.let { startObservingSessions(it) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            chatService = null
            isServiceBound = false
        }
    }

    private val _sessions = MutableStateFlow<List<SessionSummary>>(emptyList())
    val sessions: StateFlow<List<SessionSummary>> = _sessions.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    var currentSessionId: String? by mutableStateOf(null)
        private set
    var currentSessionDetail: SessionDetail? by mutableStateOf(null)
        private set
    var selectedProjectId: String? by mutableStateOf(null)
        private set
    var inputText by mutableStateOf("")
    var showSidebar by mutableStateOf(false)
    var hasModelConfig by mutableStateOf(false)
        private set

    private var sessionListJob: Job? = null
    private var messagesJob: Job? = null
    private var sessionStateJob: Job? = null

    init {
        bindToService()
        viewModelScope.launch {
            hasModelConfig = aiModelConfigRepository.hasAnyConfig()
        }
    }

    private fun bindToService() {
        val intent = Intent(getApplication(), AIChatService::class.java)
        getApplication<Application>().startService(intent)
        getApplication<Application>().bindService(
            intent, serviceConnection, Context.BIND_AUTO_CREATE
        )
    }

    fun loadSessions(projectId: String) {
        if (selectedProjectId == projectId) return
        selectedProjectId = projectId
        viewModelScope.launch {
            hasModelConfig = aiModelConfigRepository.hasAnyConfig()
        }
        if (isServiceBound) {
            startObservingSessions(projectId)
        }
    }

    private fun startObservingSessions(projectId: String) {
        sessionListJob?.cancel()
        sessionListJob = viewModelScope.launch {
            conversationRepository.getSessionsByProject(projectId).collect { entities ->
                val summaries = entities.mapNotNull { entity ->
                    createSessionSummary(entity)
                }.sortedByDescending { it.updatedAt }
                _sessions.value = summaries

                if (currentSessionId == null && summaries.isNotEmpty()) {
                    selectSession(summaries.first().sessionId)
                }
            }
        }
    }

    private suspend fun createSessionSummary(entity: AiSessionEntity): SessionSummary {
        val messageCount = conversationRepository.getUserMessageCount(entity.sessionId)
        val lastMsg = conversationRepository.getLastMessage(entity.sessionId)
        return SessionSummary(
            sessionId = entity.sessionId,
            title = entity.title,
            lastMessage = lastMsg?.content?.take(80) ?: "",
            messageCount = messageCount,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            isActive = entity.sessionId == currentSessionId
        )
    }

    fun createSession(
        projectId: String,
        systemPrompt: String? = null,
        modelConfigId: Int? = null
    ) {
        if (modelConfigId != null) {
            val service = chatService ?: return
            try {
                val sessionId = service.createSession(projectId, systemPrompt, modelConfigId)
                selectSession(sessionId)
            } catch (e: Exception) { Timber.e(e, "AiChatViewModel.createSession(1) failed") }
        } else {
            viewModelScope.launch {
                val config = aiModelConfigRepository.getDefaultConfig()
                    ?: aiModelConfigRepository.getAllConfigs().first().firstOrNull()
                    ?: return@launch
                val service = chatService ?: return@launch
                try {
                    val sessionId = service.createSession(projectId, systemPrompt, config.id)
                    selectSession(sessionId)
                } catch (e: Exception) { Timber.e(e, "AiChatViewModel.createSession(2) failed") }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val service = chatService ?: return
        try {
            service.deleteSession(sessionId)
            if (currentSessionId == sessionId) {
                clearCurrentSession()
                val remaining = _sessions.value.filter { it.sessionId != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().sessionId)
                }
            }
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.deleteSession failed") }
    }

    fun selectSession(sessionId: String) {
        if (currentSessionId == sessionId) return
        currentSessionId = sessionId
        chatService?.switchToSession(sessionId)

        viewModelScope.launch {
            val detail = chatService?.getSessionDetail(sessionId)
            currentSessionDetail = detail
        }

        observeMessages(sessionId)
        observeSessionState(sessionId)
    }

    private fun clearCurrentSession() {
        currentSessionId = null
        currentSessionDetail = null
        _messages.value = emptyList()
        _sessionState.value = SessionState.Idle
        messagesJob?.cancel()
        sessionStateJob?.cancel()
    }

    private fun observeMessages(sessionId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            conversationRepository.getVisibleMessages(sessionId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    private fun observeSessionState(sessionId: String) {
        sessionStateJob?.cancel()
        sessionStateJob = viewModelScope.launch {
            val service = chatService ?: return@launch
            service.observeSessionState(sessionId).asStateFlow().collect { state ->
                _sessionState.value = state
            }
        }
    }

    fun sendMessage() {
        val content = inputText.trim()
        if (content.isEmpty()) return
        if (!hasModelConfig) return
        val service = chatService ?: return
        val sessionId = currentSessionId ?: return

        inputText = ""
        try {
            service.sendMessage(sessionId, content)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.sendMessage failed — message dropped") }
    }

    fun stopGeneration() {
        val sessionId = currentSessionId ?: return
        try {
            chatService?.stopGeneration(sessionId)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.stopGeneration failed") }
    }

    fun retryLastMessage() {
        val sessionId = currentSessionId ?: return
        try {
            chatService?.retryLastMessage(sessionId)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.retryLastMessage failed") }
    }

    fun deleteMessage(messageIndex: Int) {
        val sessionId = currentSessionId ?: return
        try {
            chatService?.deleteMessage(sessionId, messageIndex)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.deleteMessage failed") }
    }

    fun renameSession(sessionId: String, title: String) {
        try {
            chatService?.renameSession(sessionId, title)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.renameSession failed") }
    }

    override fun onCleared() {
        super.onCleared()
        sessionListJob?.cancel()
        messagesJob?.cancel()
        sessionStateJob?.cancel()
        try {
            getApplication<Application>().unbindService(serviceConnection)
        } catch (e: Exception) { Timber.e(e, "AiChatViewModel.onCleared unbind failed") }
    }
}

class AiChatViewModelFactory(
    private val application: Application,
    private val conversationRepository: AiConversationRepository,
    private val aiModelConfigRepository: AiModelConfigRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiChatViewModel::class.java)) {
            return AiChatViewModel(application, conversationRepository, aiModelConfigRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
