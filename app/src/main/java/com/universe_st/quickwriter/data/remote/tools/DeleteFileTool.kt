package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class DeleteFileTool : ChatTool {

    override val definition = ToolDefinition(
        name = "delete_file",
        description = "Delete a specified file or empty directory within a project. Core project directories (正文/, 设定/, 时间线/ etc.) at root level are protected.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project"
                ),
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the file/directory to delete"
                )
            ),
            "required" to listOf("projectId", "relativePath")
        )
    )

    companion object {
        private val protectedDirs = setOf("正文", "设定", "时间线", "记录", "配置")
    }

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val relativePath = arguments.optString("relativePath", "")

        if (relativePath.isEmpty()) return """{"error": "relativePath is required. Call get_folder_structure first to discover available file paths."}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!filePath.exists()) {
            return """{"error": "File/directory not found: $relativePath"}"""
        }

        val normalizedPath = relativePath.trimStart('/').split("/").firstOrNull() ?: ""
        if (normalizedPath in protectedDirs && relativePath.split("/").size == 1) {
            return """{"error": "Cannot delete core directory: $normalizedPath"}"""
        }

        val wasDeleted = filePath.deleteRecursively()

        val result = JSONObject().apply {
            put("deleted", wasDeleted)
            put("path", relativePath)
        }
        return result.toString(2)
    }
}
