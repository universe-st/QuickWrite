package com.universe_st.quickwriter.data.remote

import com.google.gson.Gson
import com.universe_st.quickwriter.data.local.dao.AiMessageDao
import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.entity.AiMessageEntity
import com.universe_st.quickwriter.data.remote.dto.ChatCompletionRequest
import com.universe_st.quickwriter.data.remote.dto.ChatMessageDto
import com.universe_st.quickwriter.data.remote.dto.ToolCallDto
import com.universe_st.quickwriter.data.remote.dto.ToolCallFunctionDto
import com.universe_st.quickwriter.data.remote.dto.ToolDefinitionDto
import com.universe_st.quickwriter.data.repository.AiServiceRepository
import com.universe_st.quickwriter.data.repository.UserSettingsRepository
import com.universe_st.quickwriter.util.PromptManager
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.MessageRole
import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.ToolCall
import com.universe_st.quickwriter.domain.model.ToolCallFunction
import com.universe_st.quickwriter.util.StreamChunk
import com.universe_st.quickwriter.util.StreamParser
import com.universe_st.quickwriter.util.TokenEstimator
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

class ApiDispatcher(
    private val aiServiceRepository: AiServiceRepository,
    private val aiMessageDao: AiMessageDao,
    private val aiModelConfigDao: AiModelConfigDao,
    private val sessionManager: SessionManager,
    private val toolExecutor: ToolExecutor,
    private val userSettingsRepository: UserSettingsRepository,
    private val promptManager: PromptManager
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val streamParser = StreamParser()
    private val gson = Gson()

    fun sendMessage(sessionId: String, content: String, attachedFiles: List<String> = emptyList()) {
        if (sessionManager.isGenerating(sessionId)) {
            Timber.w("ApiDispatcher.sendMessage: session %s is already generating, ignoring", sessionId)
            return
        }

        Timber.d("ApiDispatcher.sendMessage: sessionId=%s content=\"%s\"", sessionId, content.take(100))
        val job = launch {
            try {
                performSendMessage(sessionId, content, attachedFiles)
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.d("ApiDispatcher.sendMessage: cancelled for session %s", sessionId)
                sessionManager.setSessionState(sessionId, SessionState.Idle)
            } catch (e: Exception) {
                Timber.e(e, "ApiDispatcher.sendMessage: unhandled exception for session %s", sessionId)
                sessionManager.setSessionState(sessionId, SessionState.Error(
                    com.universe_st.quickwriter.util.UiText.DynamicString(e.message ?: "Unknown error")
                ))
            }
        }

        activeJobs[sessionId] = job
    }

    private suspend fun performSendMessage(
        sessionId: String,
        content: String,
        attachedFiles: List<String>,
        toolRound: Int = 0
    ) {
        val maxRounds = getMaxToolCallRounds()
        if (toolRound > maxRounds) {
            Timber.e("ApiDispatcher.performSendMessage: MAX_TOOL_CALL_ROUNDS (%d) exceeded for session %s", maxRounds, sessionId)
            sessionManager.setSessionState(sessionId, SessionState.Error(
                com.universe_st.quickwriter.util.UiText.DynamicString("Max tool call rounds exceeded ($maxRounds). Adjust in writing settings.")
            ))
            return
        }

        Timber.d("ApiDispatcher.performSendMessage: sessionId=%s toolRound=%d", sessionId, toolRound)

        val context = sessionManager.getSessionContext(sessionId)
            ?: sessionManager.loadSessionContext(sessionId) ?: run {
                sessionManager.setSessionState(sessionId, SessionState.Error(
                    com.universe_st.quickwriter.util.UiText.DynamicString("Session not found")
                ))
                return
            }

        var modelConfig = aiModelConfigDao.getConfigById(context.modelConfigId)
        if (modelConfig == null) {
            modelConfig = aiModelConfigDao.getDefaultConfig()
        }
        if (modelConfig == null) {
            sessionManager.setSessionState(sessionId, SessionState.Error(
                com.universe_st.quickwriter.util.UiText.DynamicString("AI model config not found")
            ))
            return
        }

        val isNewUserMessage = toolRound == 0
        if (isNewUserMessage) {
            val userMessage = ChatMessage(
                role = MessageRole.USER,
                content = buildUserContent(content, attachedFiles),
                tokenCount = TokenEstimator.estimateTokenCount(content),
                timestamp = System.currentTimeMillis()
            )

            val messageOrder = aiMessageDao.getMessagesBySession(sessionId).size
            val userEntity = AiMessageEntity(
                sessionId = sessionId,
                messageOrder = messageOrder,
                role = "user",
                content = userMessage.content,
                tokenCount = userMessage.tokenCount,
                createdAt = userMessage.timestamp
            )
            aiMessageDao.insertMessage(userEntity)
            sessionManager.appendMessageToContext(sessionId, userMessage)
        }

        val apiContext = sessionManager.getSessionContext(sessionId) ?: context

        sessionManager.setSessionState(sessionId, SessionState.Generating(""))

        val tools = toolExecutor.getToolDefinitions().map { tool ->
            when (tool.function.name) {
                "create_file", "edit_file" -> {
                    val charLimit = (modelConfig.maxTokens / 2.5).toInt()
                    tool.copy(
                        function = tool.function.copy(
                            description = tool.function.description +
                                " IMPORTANT: Each call's content MUST not exceed ~$charLimit characters (max_tokens=${modelConfig.maxTokens}/2.5)." +
                                " For larger content, split across multiple calls."
                        )
                    )
                }
                else -> tool
            }
        }
        val messagesForApi = buildMessagesForApi(apiContext)
        val request = ChatCompletionRequest(
            model = modelConfig.modelName,
            messages = messagesForApi,
            temperature = modelConfig.temperature,
            maxTokens = modelConfig.maxTokens,
            stream = true,
            tools = tools.ifEmpty { null },
            toolChoice = if (tools.isNotEmpty()) "auto" else null
        )

        val result = aiServiceRepository.chatCompletionStream(
            configId = context.modelConfigId,
            request = request
        )

        result.fold(
            onSuccess = { response ->
                if (!response.isSuccessful) {
                    val errorDetail = try {
                        response.errorBody()?.string() ?: "HTTP ${response.code()}"
                    } catch (_: Exception) {
                        "HTTP ${response.code()}"
                    }
                    sessionManager.setSessionState(sessionId, SessionState.Error(
                        com.universe_st.quickwriter.util.UiText.DynamicString(
                            "API error (${response.code()}): ${errorDetail.take(200)}"
                        )
                    ))
                    return
                }
                val body = response.body()
                if (body == null) {
                    sessionManager.setSessionState(sessionId, SessionState.Error(
                        com.universe_st.quickwriter.util.UiText.DynamicString("Empty response body")
                    ))
                    return
                }

                processStreamResponse(sessionId, body, context.projectId, toolRound)
            },
            onFailure = { error ->
                sessionManager.setSessionState(sessionId, SessionState.Error(
                    com.universe_st.quickwriter.util.UiText.DynamicString(error.message ?: "Unknown error")
                ))
            }
        )
    }

    fun stopGeneration(sessionId: String) {
        activeJobs[sessionId]?.cancel()
        activeJobs.remove(sessionId)
        sessionManager.setSessionState(sessionId, SessionState.Idle)
    }

    fun retryLastMessage(sessionId: String) {
        launch {
            val lastUserMsg = aiMessageDao.getMessagesBySession(sessionId)
                .lastOrNull { it.role == "user" && !it.isSilent }
                ?: return@launch

            aiMessageDao.deleteMessagesFrom(sessionId, lastUserMsg.messageOrder + 1)

            sendMessage(sessionId, lastUserMsg.content)
        }
    }

    fun cancel() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        coroutineContext.cancel()
    }

    companion object {
        private const val MAX_CONTEXT_TOKENS = 6000
        private const val DEFAULT_MAX_TOOL_CALL_ROUNDS = 30
    }

    private suspend fun getMaxToolCallRounds(): Int {
        return userSettingsRepository.getMaxToolCallRounds()
    }

    private fun buildUserContent(content: String, attachedFiles: List<String>): String {
        if (attachedFiles.isEmpty()) return content
        val fileContents = attachedFiles.joinToString("\n\n") { "[Attached: $it]" }
        return "$fileContents\n\n$content"
    }

    private fun buildMessagesForApi(context: com.universe_st.quickwriter.domain.model.SessionContext): List<ChatMessageDto> {
        val nonSystem = context.messages.filter { it.role != MessageRole.SYSTEM }
        val systemMsg = ChatMessageDto(role = "system", content = context.systemPrompt)
        val truncated = truncateMessagesAtomic(nonSystem, MAX_CONTEXT_TOKENS)

        val seenToolCallIds = mutableSetOf<String>()
        for (msg in truncated) {
            if (msg.role == MessageRole.ASSISTANT && msg.toolCalls != null) {
                seenToolCallIds.addAll(msg.toolCalls.map { it.id })
            }
            if (msg.role == MessageRole.TOOL && msg.toolCallId != null && msg.toolCallId !in seenToolCallIds) {
                Timber.e("buildMessagesForApi: orphaned TOOL message — toolCallId=%s has no preceding ASSISTANT with matching tool_calls", msg.toolCallId)
            }
        }

        val dtoList = mutableListOf(systemMsg)
        dtoList.addAll(truncated.map { it.toDto() })
        return dtoList
    }

    private fun truncateMessagesAtomic(messages: List<ChatMessage>, maxTokens: Int): List<ChatMessage> {
        val segments = mutableListOf<MutableList<ChatMessage>>()
        var i = 0
        while (i < messages.size) {
            val segment = mutableListOf<ChatMessage>()
            val msg = messages[i]
            segment.add(msg)
            if (msg.role == MessageRole.ASSISTANT && msg.toolCalls != null) {
                i++
                while (i < messages.size && messages[i].role == MessageRole.TOOL) {
                    segment.add(messages[i])
                    i++
                }
            } else {
                i++
            }
            segments.add(segment)
        }

        var totalTokens = 0
        val resultSegments = mutableListOf<MutableList<ChatMessage>>()
        for (segment in segments.reversed()) {
            val segmentTokens = segment.sumOf { it.tokenCount.coerceAtLeast(it.content.length / 4) }
            if (totalTokens + segmentTokens > maxTokens && resultSegments.isNotEmpty()) break
            totalTokens += segmentTokens
            resultSegments.add(0, segment)
        }
        return resultSegments.flatten()
    }

    private suspend fun processStreamResponse(
        sessionId: String,
        body: okhttp3.ResponseBody,
        projectId: String,
        toolRound: Int
    ) {
        val reader = BufferedReader(InputStreamReader(body.byteStream()))
        val fullContent = StringBuilder()
        val reasoningContent = StringBuilder()
        var usageTokens: Int? = null
        val toolCallAccumulators = mutableMapOf<Int, ToolCallAccumulator>()
        var finishReason: String? = null
        var lineCount = 0

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (!isActive) return@useLines
                    lineCount++
                    if (lineCount <= 3) {
                        Timber.d("SSE[%d]: %s", lineCount, line.take(200))
                    }

                    val chunk = streamParser.parseLine(line) ?: return@forEach

                    when (chunk) {
                        is StreamChunk.ReasoningContent -> {
                            reasoningContent.append(chunk.text)
                        }
                        is StreamChunk.Content -> {
                            fullContent.append(chunk.text)
                            if (isActive) {
                                sessionManager.setSessionState(sessionId, SessionState.Generating(fullContent.toString()))
                            }
                        }
                        is StreamChunk.ToolCallBegin -> {
                            toolCallAccumulators[chunk.index] = ToolCallAccumulator(
                                id = chunk.id,
                                name = chunk.name,
                                argsBuilder = StringBuilder(chunk.initialArgsDelta)
                            )
                        }
                        is StreamChunk.ToolCallArgs -> {
                            val acc = toolCallAccumulators[chunk.index]
                                ?: toolCallAccumulators.keys.maxOrNull()?.let { fallbackIndex ->
                                    toolCallAccumulators[fallbackIndex]
                                }
                            if (acc != null) {
                                acc.argsBuilder.append(chunk.argsDelta)
                            } else {
                                Timber.w("ApiDispatcher: ToolCallArgs dropped — no accumulator for index=%d argsDelta=\"%s\"",
                                    chunk.index, chunk.argsDelta.take(80))
                            }
                        }
                        is StreamChunk.Done -> {
                            usageTokens = chunk.usage?.totalTokens
                            finishReason = "stop"
                            Timber.d("ApiDispatcher: StreamChunk.Done — toolCallAccumulators.size=%d, fullContent.length=%d, totalLines=%d",
                                toolCallAccumulators.size, fullContent.length, lineCount)
                        }
                        is StreamChunk.Error -> {
                            if (isActive && fullContent.isEmpty()) {
                                sessionManager.setSessionState(sessionId, SessionState.Error(
                                    com.universe_st.quickwriter.util.UiText.DynamicString(chunk.message)
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            savePartialResponse(sessionId, fullContent, reasoningContent, usageTokens, projectId, toolRound, toolCallAccumulators)
            sessionManager.setSessionState(sessionId, SessionState.Idle)
            sessionManager.refreshSessionList(projectId)
            return
        } catch (e: Exception) {
            savePartialResponse(sessionId, fullContent, reasoningContent, usageTokens, projectId, toolRound, toolCallAccumulators)
            sessionManager.setSessionState(sessionId, SessionState.Idle)
            sessionManager.refreshSessionList(projectId)
            return
        } finally {
            body.close()
        }

        if (toolCallAccumulators.isNotEmpty()) {
            val textContent = fullContent.toString()
            Timber.d("ApiDispatcher: BRANCH tool_calls — %d accumulators, textContent=\"%s\", calling handleToolCalls",
                toolCallAccumulators.size, textContent.take(100))
            handleToolCalls(sessionId, toolCallAccumulators, textContent, reasoningContent.toString(), projectId, toolRound)
            return
        }

        if (fullContent.isNotEmpty()) {
            Timber.d("ApiDispatcher: BRANCH text_only — persisting %d chars, setting Idle", fullContent.length)
            sessionManager.setSessionState(sessionId, SessionState.Idle)
            persistAssistantMessage(sessionId, fullContent.toString(), reasoningContent.toString(), usageTokens, projectId)
        } else {
            Timber.e("ApiDispatcher: BRANCH empty_response_ERROR — fullContent is empty, toolCallAccumulators is empty, setting Error state")
            sessionManager.setSessionState(sessionId, SessionState.Error(
                com.universe_st.quickwriter.util.UiText.DynamicString(
                    "AI model returned empty response. Check your API key and model configuration."
                )
            ))
            return
        }
        sessionManager.refreshSessionList(projectId)
    }

    private suspend fun handleToolCalls(
        sessionId: String,
        accumulators: Map<Int, ToolCallAccumulator>,
        textContent: String,
        reasoningContent: String,
        projectId: String,
        toolRound: Int
    ) {
        val toolCalls = accumulators.values.map { acc ->
            ToolCall(
                id = acc.id,
                function = ToolCallFunction(
                    name = acc.name,
                    arguments = acc.argsBuilder.toString()
                )
            )
        }

        Timber.d("ApiDispatcher: handleToolCalls — toolRound=%d, toolCount=%d, tools=[%s]",
            toolRound, toolCalls.size, toolCalls.joinToString { "${it.function.name}(${it.function.arguments.take(80)})" })

        val toolCallsJson = gson.toJson(toolCalls.map { tc ->
            mapOf(
                "id" to tc.id,
                "type" to "function",
                "function" to mapOf(
                    "name" to tc.function.name,
                    "arguments" to tc.function.arguments
                )
            )
        })

        val allSilent = toolCalls.all { it.function.name == "rename_session" }

        val displayContent = textContent.ifBlank { "" }
        val assistantMsg = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = displayContent,
            toolCalls = toolCalls,
            reasoningContent = reasoningContent.ifEmpty { null },
            tokenCount = TokenEstimator.estimateTokenCount(displayContent),
            silent = allSilent,
            timestamp = System.currentTimeMillis()
        )

        val messageOrder = aiMessageDao.getMessagesBySession(sessionId).size
        val assistantEntity = AiMessageEntity(
            sessionId = sessionId,
            messageOrder = messageOrder,
            role = "assistant",
            content = assistantMsg.content,
            tokenCount = assistantMsg.tokenCount,
            toolCallsJson = toolCallsJson,
            reasoningContent = reasoningContent.ifEmpty { null },
            isSilent = allSilent,
            createdAt = assistantMsg.timestamp
        )
        aiMessageDao.insertMessage(assistantEntity)
        sessionManager.appendMessageToContext(sessionId, assistantMsg)

        delay(150) // Let UI observe tool cards via Room Flow before executing tools

        val context = sessionManager.getSessionContext(sessionId) ?: return

        for (toolCall in toolCalls) {
            sessionManager.setSessionState(sessionId, SessionState.Generating("Executing tool: ${toolCall.function.name}..."))

            val result = toolExecutor.executeToolCall(
                toolCallId = toolCall.id,
                functionName = toolCall.function.name,
                argumentsJson = toolCall.function.arguments,
                projectId = projectId,
                sessionId = sessionId
            )

            Timber.d("ApiDispatcher: tool result — name=\"${toolCall.function.name}\" result=\"${result.take(200)}\"")

            val toolMsg = ChatMessage(
                role = MessageRole.TOOL,
                content = result,
                toolCallId = toolCall.id,
                silent = toolCall.function.name == "rename_session",
                timestamp = System.currentTimeMillis()
            )

            val toolOrder = aiMessageDao.getMessagesBySession(sessionId).size
            val toolEntity = AiMessageEntity(
                sessionId = sessionId,
                messageOrder = toolOrder,
                role = "tool",
                content = result,
                toolCallId = toolCall.id,
                isSilent = toolCall.function.name == "rename_session",
                createdAt = toolMsg.timestamp
            )
            aiMessageDao.insertMessage(toolEntity)
            sessionManager.appendMessageToContext(sessionId, toolMsg)

            delay(100) // Let UI observe each tool result via Room Flow
        }

        Timber.d("ApiDispatcher: handleToolCalls — all %d tools done, calling performSendMessage (toolRound=%d)",
            toolCalls.size, toolRound + 1)
        performSendMessage(sessionId, "", emptyList(), toolRound + 1)
    }

    private suspend fun savePartialResponse(
        sessionId: String,
        fullContent: StringBuilder,
        reasoningContent: StringBuilder,
        usageTokens: Int?,
        projectId: String,
        toolRound: Int,
        accumulators: Map<Int, ToolCallAccumulator>
    ) {
        if (fullContent.isNotEmpty()) {
            persistAssistantMessage(sessionId, fullContent.toString(), reasoningContent.toString(), usageTokens, projectId)
        }
    }

    private suspend fun persistAssistantMessage(
        sessionId: String,
        content: String,
        reasoningContent: String,
        usageTokens: Int?,
        projectId: String
    ) {
        val messageOrder = aiMessageDao.getMessagesBySession(sessionId).size
        val tokenCount = usageTokens ?: TokenEstimator.estimateTokenCount(content)

        val entity = AiMessageEntity(
            sessionId = sessionId,
            messageOrder = messageOrder,
            role = "assistant",
            content = content,
            tokenCount = tokenCount,
            reasoningContent = reasoningContent.ifEmpty { null },
            createdAt = System.currentTimeMillis()
        )
        aiMessageDao.insertMessage(entity)

        val assistantMsg = ChatMessage(
            id = entity.id,
            role = MessageRole.ASSISTANT,
            content = content,
            tokenCount = tokenCount,
            reasoningContent = reasoningContent.ifEmpty { null },
            timestamp = entity.createdAt
        )
        sessionManager.appendMessageToContext(sessionId, assistantMsg)
    }

    private data class ToolCallAccumulator(
        val id: String,
        val name: String,
        val argsBuilder: StringBuilder = StringBuilder()
    )
}

private fun ChatMessage.toDto(): ChatMessageDto {
    val toolCallsDto = toolCalls?.map { tc ->
        ToolCallDto(
            id = tc.id,
            function = ToolCallFunctionDto(
                name = tc.function.name,
                arguments = tc.function.arguments
            )
        )
    }

    return ChatMessageDto(
        role = role.name.lowercase(),
        content = content,
        toolCalls = toolCallsDto,
        toolCallId = toolCallId,
        reasoningContent = reasoningContent
    )
}
