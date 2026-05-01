package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.ChapterFileHelper
import com.universe_st.quickwriter.util.ChapterMeta
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class UpdateChapterMetaTool : ChatTool {

    override val definition = ToolDefinition(
        name = "update_chapter_meta",
        description = "Update the YAML front matter metadata of a chapter file under 正文/. " +
            "Only the provided fields will be updated; omitted fields keep their current values. " +
            "Use this instead of edit_file to modify title, order, volume, or summary.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the chapter file (e.g. 正文/第一章.md)"
                ),
                "title" to mapOf(
                    "type" to "string",
                    "description" to "New chapter title (optional, omit to keep current)"
                ),
                "order" to mapOf(
                    "type" to "integer",
                    "description" to "New sort order number (optional, omit to keep current)"
                ),
                "volume" to mapOf(
                    "type" to "string",
                    "description" to "New volume name (optional, omit to keep current)"
                ),
                "summary" to mapOf(
                    "type" to "string",
                    "description" to "New content summary (optional, omit to keep current)"
                )
            ),
            "required" to listOf("relativePath")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
        val relativePath = arguments.optString("relativePath", "")

        if (relativePath.isEmpty()) return """{"error": "relativePath is required."}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!filePath.exists() || !filePath.isFile) {
            return """{"error": "File not found: $relativePath"}"""
        }

        val rawContent = filePath.readText()
        val (currentMeta, body) = ChapterFileHelper.parseChapterContent(rawContent)

        val newTitle = if (arguments.has("title")) arguments.optString("title", "") else currentMeta.title
        val newOrder = if (arguments.has("order")) arguments.optInt("order", 0) else currentMeta.order
        val newVolume = if (arguments.has("volume")) arguments.optString("volume", "") else currentMeta.volume
        val newSummary = if (arguments.has("summary")) arguments.optString("summary", "") else currentMeta.summary

        if (newTitle.isBlank() && newOrder <= 0) {
            return JSONObject().apply {
                put("error", "At least one of title or order must be non-empty/positive.")
                put("current", JSONObject().apply {
                    put("title", currentMeta.title)
                    put("order", currentMeta.order)
                    put("volume", currentMeta.volume)
                    put("summary", currentMeta.summary)
                })
            }.toString(2)
        }

        val updatedMeta = ChapterMeta(
            title = newTitle,
            order = newOrder,
            volume = newVolume,
            summary = newSummary
        )

        val newContent = ChapterFileHelper.buildChapterContent(updatedMeta, body)
        filePath.writeText(newContent)

        val result = JSONObject().apply {
            put("updated", true)
            put("path", relativePath)
            put("title", updatedMeta.title)
            put("order", updatedMeta.order)
            put("volume", updatedMeta.volume)
            put("summary", updatedMeta.summary)
        }
        return result.toString(2)
    }
}
