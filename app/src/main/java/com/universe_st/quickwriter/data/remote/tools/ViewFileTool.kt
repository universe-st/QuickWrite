package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class ViewFileTool : ChatTool {

    override val definition = ToolDefinition(
        name = "view_file",
        description = "Read the content of a specified file within a project, with optional line range to limit output size",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the file from project root"
                ),
                "startLine" to mapOf(
                    "type" to "integer",
                    "description" to "Starting line number (1-indexed, default: 1)"
                ),
                "endLine" to mapOf(
                    "type" to "integer",
                    "description" to "Ending line number (1-indexed, default: 0 = all lines)"
                )
            ),
            "required" to listOf("relativePath")
        )
    )

    companion object {
        private const val MAX_LINES = 500
    }

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
        val relativePath = arguments.optString("relativePath", "")
        val startLine = arguments.optInt("startLine", 1).coerceAtLeast(1)
        val endLine = arguments.optInt("endLine", 0)

        if (relativePath.isEmpty()) return """{"error": "relativePath is required. Call get_folder_structure first to discover available file paths."}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!filePath.exists() || !filePath.isFile) {
            return """{"error": "File not found: $relativePath"}"""
        }

        val allLines = filePath.readLines()
        val totalLines = allLines.size
        val actualEndLine = if (endLine <= 0) totalLines else endLine.coerceAtMost(totalLines)
        val actualStartLine = startLine.coerceAtMost(actualEndLine)

        val selectedLines = allLines.subList(actualStartLine - 1, actualEndLine)

        val effectiveEnd = if (selectedLines.size > MAX_LINES) {
            actualStartLine + MAX_LINES - 1
        } else {
            actualEndLine
        }

        val linesToShow = if (selectedLines.size > MAX_LINES) {
            selectedLines.take(MAX_LINES)
        } else {
            selectedLines
        }

        val content = linesToShow.mapIndexed { index, line ->
            "${actualStartLine + index}: $line"
        }.joinToString("\n")

        val extension = filePath.extension.ifEmpty { "txt" }

        val result = JSONObject().apply {
            put("filePath", relativePath)
            put("totalLines", totalLines)
            put("startLine", actualStartLine)
            put("endLine", effectiveEnd)
            put("language", extension)
            put("content", content)
            if (selectedLines.size > MAX_LINES) {
                put("truncated", true)
                put("message", "Content truncated: showing $MAX_LINES lines out of ${selectedLines.size} selected lines")
            }
        }
        return result.toString(2)
    }
}
