package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class MoveFileTool : ChatTool {

    override val definition = ToolDefinition(
        name = "move_file",
        description = "Move or rename a file/directory within a project. Source and target must be within the same project. Core root directories are protected.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project"
                ),
                "sourcePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the source file/directory"
                ),
                "targetPath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the target (including new name)"
                )
            ),
            "required" to listOf("projectId", "sourcePath", "targetPath")
        )
    )

    companion object {
        private val protectedDirs = setOf("正文", "设定", "时间线", "记录", "配置")
    }

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val sourcePath = arguments.optString("sourcePath", "")
        val targetPath = arguments.optString("targetPath", "")

        if (sourcePath.isEmpty()) return """{"error": "sourcePath is required"}"""
        if (targetPath.isEmpty()) return """{"error": "targetPath is required"}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val sourceFile = File(project.storagePath, sourcePath)
        val targetFile = File(project.storagePath, targetPath)

        if (!context.fileManager.isPathSafe(sourceFile.canonicalPath)) {
            return """{"error": "Source path is not within project scope"}"""
        }
        if (!context.fileManager.isPathSafe(targetFile.canonicalPath)) {
            return """{"error": "Target path is not within project scope"}"""
        }

        if (!sourceFile.exists()) {
            return """{"error": "Source not found: $sourcePath"}"""
        }

        val normalizedSource = sourcePath.trimStart('/').split("/").firstOrNull() ?: ""
        if (normalizedSource in protectedDirs && sourcePath.split("/").size == 1) {
            return """{"error": "Cannot move core directory: $normalizedSource"}"""
        }

        if (targetFile.exists()) {
            return """{"error": "Target already exists: $targetPath"}"""
        }

        targetFile.parentFile?.mkdirs()
        val moved = sourceFile.renameTo(targetFile)

        val result = JSONObject().apply {
            put("moved", moved)
            put("sourcePath", sourcePath)
            put("targetPath", targetPath)
        }
        return result.toString(2)
    }
}
