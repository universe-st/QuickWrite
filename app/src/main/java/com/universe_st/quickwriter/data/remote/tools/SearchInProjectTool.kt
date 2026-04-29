package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SearchInProjectTool : ChatTool {

    override val definition = ToolDefinition(
        name = "search_in_project",
        description = "Search file contents recursively within a project directory, supporting keyword and regex matching",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project"
                ),
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Starting directory for search (default: project root)"
                ),
                "query" to mapOf(
                    "type" to "string",
                    "description" to "Keyword or regex pattern to search for"
                ),
                "useRegex" to mapOf(
                    "type" to "boolean",
                    "description" to "Whether to treat query as regex (default: false)"
                ),
                "maxResults" to mapOf(
                    "type" to "integer",
                    "description" to "Maximum number of results to return (default: 20)"
                )
            ),
            "required" to listOf("projectId", "query")
        )
    )

    companion object {
        private const val DEFAULT_MAX_RESULTS = 20
        private const val MAX_FILE_SIZE = 1024L * 1024L
    }

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val relativePath = arguments.optString("relativePath", "")
        val query = arguments.optString("query", "")
        val useRegex = arguments.optBoolean("useRegex", false)
        val maxResults = arguments.optInt("maxResults", DEFAULT_MAX_RESULTS)

        if (query.isEmpty()) return """{"error": "query is required"}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val searchDir = if (relativePath.isEmpty()) {
            File(project.storagePath)
        } else {
            File(project.storagePath, relativePath)
        }

        if (!context.fileManager.isPathSafe(searchDir.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        val regex = try {
            if (useRegex) Regex(query, RegexOption.IGNORE_CASE) else null
        } catch (e: Exception) {
            return """{"error": "Invalid regex: ${e.message}"}"""
        }

        val results = JSONArray()
        val searchExtensions = setOf("md", "txt", "json")
        var matchCount = 0

        searchDir.walkTopDown().forEach { file ->
            if (matchCount >= maxResults) return@forEach
            if (!file.isFile) return@forEach
            if (file.length() > MAX_FILE_SIZE) return@forEach
            if (file.extension !in searchExtensions) return@forEach

            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    if (matchCount >= maxResults) return@useLines
                    val matches = if (regex != null) {
                        regex.containsMatchIn(line)
                    } else {
                        line.contains(query, ignoreCase = true)
                    }
                    if (matches) {
                        val relative = file.absolutePath.removePrefix(
                            File(project.storagePath).absolutePath
                        ).removePrefix(File.separator)
                        val result = JSONObject().apply {
                            put("filePath", relative)
                            put("lineNumber", index + 1)
                            put("lineContent", line.trim().take(200))
                            put("matchText", query)
                        }
                        results.put(result)
                        matchCount++
                    }
                }
            }
        }

        val result = JSONObject().apply {
            put("query", query)
            put("useRegex", useRegex)
            put("results", results)
            put("resultCount", results.length())
            if (matchCount >= maxResults) {
                put("truncated", true)
                put("message", "Results truncated at $maxResults matches")
            }
        }
        return result.toString(2)
    }
}
