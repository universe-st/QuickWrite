package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class CreateFileTool : ChatTool {

    override val definition = ToolDefinition(
        name = "create_file",
        description = "Create a new file at a specified path within a project, with optional initial content. Parent directories are auto-created.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project"
                ),
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path for the new file"
                ),
                "content" to mapOf(
                    "type" to "string",
                    "description" to "Initial file content (optional)"
                )
            ),
            "required" to listOf("projectId", "relativePath")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val relativePath = arguments.optString("relativePath", "")
        val content = arguments.optString("content", "")

        if (relativePath.isEmpty()) return """{"error": "relativePath is required. Call get_folder_structure first to see the directory layout, then use a valid relative path."}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (filePath.exists()) {
            return """{"error": "File already exists: $relativePath"}"""
        }

        filePath.parentFile?.mkdirs()
        filePath.createNewFile()
        if (content.isNotEmpty()) {
            filePath.writeText(content)
        }

        val result = JSONObject().apply {
            put("created", true)
            put("path", relativePath)
            put("size", filePath.length())
        }
        return result.toString(2)
    }
}
