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
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.MessageRole
import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.ToolCall
import com.universe_st.quickwriter.domain.model.ToolCallFunction
import com.universe_st.quickwriter.util.StreamChunk
import com.universe_st.quickwriter.util.StreamParser
import com.universe_st.quickwriter.util.TokenEstimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val toolExecutor: ToolExecutor
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val streamParser = StreamParser()
    private val gson = Gson()

    fun sendMessage(sessionId: String, content: String, attachedFiles: List<String> = emptyList()) {
        if (sessionManager.isGenerating(sessionId)) return

        val job = launch {
            try {
                performSendMessage(sessionId, content, attachedFiles)
            } catch (e: kotlinx.coroutines.CancellationException) {
                sessionManager.setSessionState(sessionId, SessionState.Idle)
            } catch (e: Exception) {
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
        if (toolRound > MAX_TOOL_CALL_ROUNDS) {
            sessionManager.setSessionState(sessionId, SessionState.Error(
                com.universe_st.quickwriter.util.UiText.DynamicString("Max tool call rounds exceeded")
            ))
            return
        }

        val context = sessionManager.getSessionContext(sessionId)
            ?: sessionManager.loadSessionContext(sessionId) ?: run {
                sessionManager.setSessionState(sessionId, SessionState.Error(
                    com.universe_st.quickwriter.util.UiText.DynamicString("Session not found")
                ))
                return
            }

        val modelConfig = aiModelConfigDao.getConfigById(context.modelConfigId)
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

        sessionManager.setSessionState(sessionId, SessionState.Generating(""))

        val tools = toolExecutor.getToolDefinitions()
        val messagesForApi = buildMessagesForApi(context)
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
                val body = response.body()
                if (body == null) {
                    sessionManager.setSessionState(sessionId, SessionState.Error(
                        com.universe_st.quickwriter.util.UiText.DynamicString("Empty response body")
                    ))
                    return
                }

                processStreamResponse(sessionId, body, context.projectId, context.title, toolRound)
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
        private const val MAX_TOOL_CALL_ROUNDS = 10
    }

    private fun buildUserContent(content: String, attachedFiles: List<String>): String {
        if (attachedFiles.isEmpty()) return content
        val fileContents = attachedFiles.joinToString("\n\n") { "[Attached: $it]" }
        return "$fileContents\n\n$content"
    }

    private fun buildMessagesForApi(context: com.universe_st.quickwriter.domain.model.SessionContext): List<ChatMessageDto> {
        val nonSystem = context.messages.filter { it.role != MessageRole.SYSTEM }
        val systemMsg = ChatMessageDto(role = "system", content = context.systemPrompt)
        val truncated = truncateMessages(nonSystem, MAX_CONTEXT_TOKENS)
        val dtoList = mutableListOf(systemMsg)
        dtoList.addAll(truncated.map { it.toDto() })
        return dtoList
    }

    private fun truncateMessages(messages: List<ChatMessage>, maxTokens: Int): List<ChatMessage> {
        var totalTokens = 0
        val result = mutableListOf<ChatMessage>()
        for (msg in messages.reversed()) {
            val msgTokens = msg.tokenCount.coerceAtLeast(msg.content.length / 4)
            if (totalTokens + msgTokens > maxTokens && result.isNotEmpty()) break
            totalTokens += msgTokens
            result.add(0, msg)
        }
        return result
    }

    private suspend fun processStreamResponse(
        sessionId: String,
        body: okhttp3.ResponseBody,
        projectId: String,
        sessionTitle: String,
        toolRound: Int
    ) {
        val reader = BufferedReader(InputStreamReader(body.byteStream()))
        val fullContent = StringBuilder()
        var usageTokens: Int? = null
        val toolCallAccumulators = mutableMapOf<Int, ToolCallAccumulator>()
        var finishReason: String? = null

        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (!isActive) return@useLines

                    val chunk = streamParser.parseLine(line) ?: return@forEach

                    when (chunk) {
                        is StreamChunk.Content -> {
                            fullContent.append(chunk.text)
                            if (isActive) {
                                sessionManager.setSessionState(sessionId, SessionState.Generating(fullContent.toString()))
                            }
                        }
                        is StreamChunk.ToolCallBegin -> {
                            val index = toolCallAccumulators.size
                            toolCallAccumulators[index] = ToolCallAccumulator(
                                id = chunk.id,
                                name = chunk.name
                            )
                        }
                        is StreamChunk.ToolCallArgs -> {
                            val lastAcc = toolCallAccumulators[toolCallAccumulators.size - 1]
                            if (lastAcc != null && chunk.id.isNotEmpty()) {
                                lastAcc.argsBuilder.append(chunk.argsDelta)
                            }
                        }
                        is StreamChunk.Done -> {
                            usageTokens = chunk.usage?.totalTokens
                            finishReason = "stop"
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
            savePartialResponse(sessionId, fullContent, usageTokens, projectId, sessionTitle, toolRound, toolCallAccumulators)
            sessionManager.setSessionState(sessionId, SessionState.Idle)
            sessionManager.refreshSessionList(projectId)
            return
        } catch (e: Exception) {
            savePartialResponse(sessionId, fullContent, usageTokens, projectId, sessionTitle, toolRound, toolCallAccumulators)
            sessionManager.setSessionState(sessionId, SessionState.Idle)
            sessionManager.refreshSessionList(projectId)
            return
        } finally {
            body.close()
        }

        if (toolCallAccumulators.isNotEmpty() && fullContent.isEmpty()) {
            handleToolCalls(sessionId, toolCallAccumulators, projectId, sessionTitle, toolRound)
            return
        }

        if (!fullContent.isEmpty()) {
            persistAssistantMessage(sessionId, fullContent.toString(), usageTokens, sessionTitle, projectId)
        }

        sessionManager.setSessionState(sessionId, SessionState.Idle)
        sessionManager.refreshSessionList(projectId)
    }

    private suspend fun handleToolCalls(
        sessionId: String,
        accumulators: Map<Int, ToolCallAccumulator>,
        projectId: String,
        sessionTitle: String,
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

        val contentSummary = toolCalls.joinToString("; ") { "${it.function.name}(${it.function.arguments.take(50)})" }
        val assistantMsg = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "[Tool calls: $contentSummary]",
            toolCalls = toolCalls,
            tokenCount = TokenEstimator.estimateTokenCount(contentSummary),
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
            createdAt = assistantMsg.timestamp
        )
        aiMessageDao.insertMessage(assistantEntity)
        sessionManager.appendMessageToContext(sessionId, assistantMsg)

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

            val toolMsg = ChatMessage(
                role = MessageRole.TOOL,
                content = result,
                toolCallId = toolCall.id,
                timestamp = System.currentTimeMillis()
            )

            val toolOrder = aiMessageDao.getMessagesBySession(sessionId).size
            val toolEntity = AiMessageEntity(
                sessionId = sessionId,
                messageOrder = toolOrder,
                role = "tool",
                content = result,
                toolCallId = toolCall.id,
                createdAt = toolMsg.timestamp
            )
            aiMessageDao.insertMessage(toolEntity)
            sessionManager.appendMessageToContext(sessionId, toolMsg)
        }

        performSendMessage(sessionId, "", emptyList(), toolRound + 1)
    }

    private suspend fun savePartialResponse(
        sessionId: String,
        fullContent: StringBuilder,
        usageTokens: Int?,
        projectId: String,
        sessionTitle: String,
        toolRound: Int,
        accumulators: Map<Int, ToolCallAccumulator>
    ) {
        if (fullContent.isNotEmpty()) {
            persistAssistantMessage(sessionId, fullContent.toString(), usageTokens, sessionTitle, projectId)
        }
    }

    private suspend fun persistAssistantMessage(
        sessionId: String,
        content: String,
        usageTokens: Int?,
        sessionTitle: String,
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
            createdAt = System.currentTimeMillis()
        )
        aiMessageDao.insertMessage(entity)

        val assistantMsg = ChatMessage(
            id = entity.id,
            role = MessageRole.ASSISTANT,
            content = content,
            tokenCount = tokenCount,
            timestamp = entity.createdAt
        )
        sessionManager.appendMessageToContext(sessionId, assistantMsg)

        triggerAutoTitleIfNeeded(sessionId, sessionTitle, projectId)
    }

    private fun triggerAutoTitleIfNeeded(sessionId: String, currentTitle: String, projectId: String) {
        if (currentTitle.isNotEmpty()) return

        launch {
            val userMsgCount = aiMessageDao.getUserMessageCount(sessionId)
            if (userMsgCount != 1) return@launch

            val firstUserMsg = aiMessageDao.getMessagesBySession(sessionId)
                .firstOrNull { it.role == "user" && !it.isSilent }
                ?: return@launch

            val titleRequest = ChatMessageDto(
                role = "user",
                content = "Please generate a title (no more than 10 words) for the following conversation. Only output the title text, no quotes or explanations.\n\nConversation start:\n${firstUserMsg.content}"
            )

            val context = sessionManager.getSessionContext(sessionId) ?: return@launch
            val modelConfig = aiModelConfigDao.getConfigById(context.modelConfigId) ?: return@launch

            val request = ChatCompletionRequest(
                model = modelConfig.modelName,
                messages = listOf(
                    ChatMessageDto(role = "system", content = "You are a title generator. Output only the title text."),
                    titleRequest
                ),
                temperature = 0.3f,
                maxTokens = 50,
                stream = false
            )

            val result = aiServiceRepository.chatCompletion(configId = context.modelConfigId, request = request)

            result.onSuccess { response ->
                val title = response.choices.firstOrNull()?.message?.content
                    ?.trim()?.replace("\"", "")?.take(50)
                    ?: firstUserMsg.content.take(30)
                sessionManager.renameSession(sessionId, title)
            }.onFailure {
                val fallbackTitle = firstUserMsg.content.take(30)
                if (fallbackTitle.isNotEmpty()) sessionManager.renameSession(sessionId, fallbackTitle)
            }
        }
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
        toolCallId = toolCallId
    )
}
