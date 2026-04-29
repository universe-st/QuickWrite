package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class EditFileTool : ChatTool {

    override val definition = ToolDefinition(
        name = "edit_file",
        description = "Replace content at a specific line range in a project file. Supports precise line-level editing.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project"
                ),
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the file from project root"
                ),
                "startLine" to mapOf(
                    "type" to "integer",
                    "description" to "Starting line to replace (1-indexed)"
                ),
                "endLine" to mapOf(
                    "type" to "integer",
                    "description" to "Ending line to replace (1-indexed, -1 = replace to end of file)"
                ),
                "newContent" to mapOf(
                    "type" to "string",
                    "description" to "New content to insert (multi-line text)"
                )
            ),
            "required" to listOf("projectId", "relativePath", "startLine", "endLine", "newContent")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val relativePath = arguments.optString("relativePath", "")
        val startLine = arguments.optInt("startLine", 1)
        val endLine = arguments.optInt("endLine", -1)
        val newContent = arguments.optString("newContent", "")

        if (relativePath.isEmpty()) return """{"error": "relativePath is required"}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!filePath.exists()) {
            return """{"error": "File not found: $relativePath"}"""
        }

        val lines = filePath.readLines().toMutableList()
        val totalLines = lines.size
        val actualStartLine = startLine.coerceIn(1, totalLines.coerceAtLeast(1))
        val actualEndLine = if (endLine == -1) totalLines else endLine.coerceIn(actualStartLine, totalLines)

        val newLines = newContent.lines()

        while (lines.size < actualStartLine - 1) lines.add("")
        while (lines.size < actualEndLine) lines.add("")

        lines.subList(actualStartLine - 1, actualEndLine).clear()
        lines.addAll(actualStartLine - 1, newLines)

        filePath.writeText(lines.joinToString("\n"))

        val result = JSONObject().apply {
            put("filePath", relativePath)
            put("replacedRange", "$actualStartLine-$actualEndLine")
            put("newContent", newContent)
            put("lineCountAfter", lines.size)
        }
        return result.toString(2)
    }
}
