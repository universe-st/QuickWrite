package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class GetProjectInfoTool : ChatTool {

    override val definition = ToolDefinition(
        name = "get_project_info",
        description = "Get detailed information about a specific project, including metadata and directory statistics",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf<String, Any>(
            ),
            "required" to emptyList<String>()
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val projectDir = File(project.storagePath)
        val dirStats = JSONObject()
        val dirs = listOf("正文", "设定", "时间线", "记录", "配置")
        dirs.forEach { dirName ->
            val dir = File(projectDir, dirName)
            if (dir.exists() && dir.isDirectory) {
                val files = dir.walkTopDown().filter { it.isFile }.toList()
                dirStats.put(dirName, JSONObject().apply {
                    put("fileCount", files.size)
                    put("totalSize", files.sumOf { it.length() })
                })
            }
        }

        val infoJson = File(projectDir, "info.json")
        val infoContent = if (infoJson.exists()) infoJson.readText() else "{}"

        val result = JSONObject().apply {
            put("id", project.id)
            put("title", project.title)
            put("author", project.author)
            put("genre", project.genre)
            put("description", project.description ?: "")
            put("status", project.status)
            put("createdTime", project.createdTime)
            put("modifiedTime", project.modifiedTime)
            put("wordCount", project.wordCount)
            put("chapterCount", project.chapterCount)
            put("directoryStats", dirStats)
            put("infoJson", infoContent)
        }
        return result.toString(2)
    }
}
