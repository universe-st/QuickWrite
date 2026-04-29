package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class GetFolderStructureTool : ChatTool {

    override val definition = ToolDefinition(
        name = "get_folder_structure",
        description = "Get the complete folder structure under a specified path within a project, returning file and directory lists without file contents",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "projectId" to mapOf(
                    "type" to "string",
                    "description" to "The ID of the project"
                ),
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path from project root (default: empty = root)"
                )
            ),
            "required" to listOf("projectId")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = arguments.optString("projectId", context.projectId)
        val relativePath = arguments.optString("relativePath", "")

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val baseDir = if (relativePath.isEmpty()) {
            File(project.storagePath)
        } else {
            File(project.storagePath, relativePath)
        }

        val fullPath = baseDir.canonicalPath
        if (!context.fileManager.isPathSafe(fullPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!baseDir.exists()) {
            return """{"error": "Directory not found: $relativePath"}"""
        }

        val result = JSONObject()
        result.put("path", relativePath.ifEmpty { "/" })
        result.put("entries", buildTree(baseDir, project.storagePath))
        return result.toString(2)
    }

    private fun buildTree(dir: File, projectRoot: String): JSONArray {
        val entries = JSONArray()
        val files = dir.listFiles()?.sortedBy { it.name } ?: return entries

        files.forEach { file ->
            val entry = JSONObject()
            val relative = file.absolutePath.removePrefix(projectRoot).removePrefix(File.separator)
            entry.put("name", file.name)
            entry.put("path", relative)
            entry.put("type", if (file.isDirectory) "directory" else "file")
            entry.put("size", if (file.isFile) file.length() else 0)
            entry.put("lastModified", file.lastModified())

            if (file.isDirectory) {
                entry.put("children", buildTree(file, projectRoot))
            }

            entries.put(entry)
        }
        return entries
    }
}
