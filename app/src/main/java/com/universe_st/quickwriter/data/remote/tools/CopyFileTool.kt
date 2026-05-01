package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class CopyFileTool : ChatTool {

    override val definition = ToolDefinition(
        name = "copy_file",
        description = "Copy a file to a new path within the same project. Directories are not supported for copying.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "sourcePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the source file"
                ),
                "targetPath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path for the copy (including new filename)"
                )
            ),
            "required" to listOf("sourcePath", "targetPath")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
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

        if (sourceFile.isDirectory) {
            return """{"error": "Copying directories is not supported"}"""
        }

        if (targetFile.exists()) {
            return """{"error": "Target already exists: $targetPath"}"""
        }

        targetFile.parentFile?.mkdirs()
        sourceFile.copyTo(targetFile, overwrite = false)

        val result = JSONObject().apply {
            put("copied", true)
            put("sourcePath", sourcePath)
            put("targetPath", targetPath)
            put("size", targetFile.length())
        }
        return result.toString(2)
    }
}
