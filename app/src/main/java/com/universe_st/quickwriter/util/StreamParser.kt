package com.universe_st.quickwriter.util

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.universe_st.quickwriter.data.remote.dto.DeltaDto
import com.universe_st.quickwriter.data.remote.dto.ToolCallChunkDto
import com.universe_st.quickwriter.data.remote.dto.UsageDto
import timber.log.Timber

sealed class StreamChunk {
    data class Content(val text: String) : StreamChunk()
    data class ToolCallBegin(val id: String, val name: String) : StreamChunk()
    data class ToolCallArgs(val id: String, val argsDelta: String) : StreamChunk()
    data class Done(val usage: UsageDto?) : StreamChunk()
    data class Error(val message: String) : StreamChunk()
}

class StreamParser {

    private val gson = Gson()

    fun parseLine(line: String): StreamChunk? {
        if (line.isBlank()) return null

        if (!line.startsWith("data:")) return null

        val data = line.removePrefix("data:").trim()

        if (data == "[DONE]") {
            return StreamChunk.Done(null)
        }

        return try {
            val json = JsonParser.parseString(data).asJsonObject
            parseJsonChunk(json)
        } catch (e: Exception) {
            Timber.e(e, "StreamParser: JSON parse error, data starts with: %s", data.take(100))
            StreamChunk.Error("Failed to parse stream data: ${e.message}")
        }
    }

    private fun JsonObject.optString(key: String): String? {
        val el = get(key)
        return if (el != null && !el.isJsonNull) el.asString else null
    }

    private fun parseJsonChunk(json: JsonObject): StreamChunk? {
        val choices = json.getAsJsonArray("choices")
        if (choices != null && choices.size() > 0) {
            return parseChoiceFormat(json, choices[0].asJsonObject)
        }

        val contentRaw = json.optString("content")
        if (!contentRaw.isNullOrEmpty()) {
            return StreamChunk.Content(contentRaw)
        }

        return null
    }

    private fun parseChoiceFormat(json: JsonObject, choice: JsonObject): StreamChunk? {
        val finishReason = choice.optString("finish_reason")
        if (finishReason == "stop" || finishReason == "length" || finishReason == "tool_calls") {
            val usageJson = json.getAsJsonObject("usage")
            val usage = if (usageJson != null) gson.fromJson(usageJson, UsageDto::class.java) else null
            return StreamChunk.Done(usage)
        }

        val delta = choice.getAsJsonObject("delta")
        if (delta != null) {
            val content = delta.optString("content")
            if (!content.isNullOrEmpty()) {
                return StreamChunk.Content(content)
            }

            val toolCalls = delta.getAsJsonArray("tool_calls")
            if (toolCalls != null && toolCalls.size() > 0) {
                return parseToolCallChunk(toolCalls)
            }

            return null
        }

        val content = choice.optString("content")
        if (!content.isNullOrEmpty()) {
            return StreamChunk.Content(content)
        }

        val text = choice.optString("text")
        if (!text.isNullOrEmpty()) {
            return StreamChunk.Content(text)
        }

        return null
    }

    fun parseToolCallDelta(delta: DeltaDto): ToolCallChunkDto? {
        return delta.toolCalls?.firstOrNull()
    }

    private fun parseToolCallChunk(toolCalls: com.google.gson.JsonArray): StreamChunk? {
        val first = toolCalls[0].asJsonObject
        val id = first.optString("id")
        val function = first.getAsJsonObject("function")

        val name = function?.optString("name")
        val arguments = function?.optString("arguments")

        return when {
            !name.isNullOrEmpty() && id != null -> StreamChunk.ToolCallBegin(id, name)
            !arguments.isNullOrEmpty() && id != null -> StreamChunk.ToolCallArgs(id, arguments)
            else -> null
        }
    }
}
