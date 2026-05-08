package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import org.json.JSONObject

class RenameSessionTool : ChatTool {

    override val definition = ToolDefinition(
        name = "rename_session",
        description = "Rename the current conversation session title. Call this to set a meaningful title for the session based on the conversation topic.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "title" to mapOf(
                    "type" to "string",
                    "description" to "The new title for this conversation session (e.g., 'Character Profile Development', 'Chapter 3 Plot Brainstorm')"
                )
            ),
            "required" to listOf("title")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val title = arguments.optString("title", "").trim()
        if (title.isEmpty()) {
            return """{"error": "Title cannot be empty"}"""
        }
        if (title.length > 100) {
            return """{"error": "Title must be 100 characters or less"}"""
        }

        val renameFn = context.renameSession
            ?: return """{"error": "Session rename not available"}"""

        val sessionId = context.sessionId
        if (sessionId.isEmpty()) {
            return """{"error": "Session ID not available"}"""
        }

        try {
            renameFn(sessionId, title)
            return JSONObject().apply {
                put("renamed", true)
                put("title", title)
            }.toString(2)
        } catch (e: Exception) {
            return """{"error": "Failed to rename session: ${e.message}"}"""
        }
    }
}
