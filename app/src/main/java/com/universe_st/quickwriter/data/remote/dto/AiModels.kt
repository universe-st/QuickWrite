package com.universe_st.quickwriter.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens")
    val maxTokens: Int = 50000,
    val stream: Boolean = true,
    val tools: List<ToolDefinitionDto>? = null,
    @SerializedName("tool_choice")
    val toolChoice: String? = "auto"
)

data class ChatMessageDto(
    val role: String,
    val content: String?,
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCallDto>? = null,
    @SerializedName("tool_call_id")
    val toolCallId: String? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class ToolDefinitionDto(
    val type: String = "function",
    val function: ToolFunctionDto
)

data class ToolFunctionDto(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunctionDto
)

data class ToolCallFunctionDto(
    val name: String,
    val arguments: String
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<ChoiceDto>,
    val usage: UsageDto? = null
)

data class ChoiceDto(
    val index: Int,
    val message: ChatMessageDto? = null,
    val delta: DeltaDto? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class DeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerializedName("tool_calls")
    val toolCalls: List<ToolCallChunkDto>? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class ToolCallChunkDto(
    val index: Int = 0,
    val id: String? = null,
    val type: String? = "function",
    val function: ToolCallFunctionChunkDto? = null
)

data class ToolCallFunctionChunkDto(
    val name: String? = null,
    val arguments: String? = null
)

data class UsageDto(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)
