package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.ChapterFileHelper
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class CreateFileTool : ChatTool {

    companion object {
        private const val CHAPTER_DIR = "正文"
        private const val CHAPTER_FORMAT_HELP =
            "For files under \"正文/\" (chapter files), content MUST start with YAML front matter:\\n" +
            "---\\n" +
            "title: \\\"Chapter Title\\\"\\n" +
            "order: N\\n" +
            "summary: \\\"Brief description\\\"\\n" +
            "---\\n" +
            "\\n" +
            "# Chapter Title\\n" +
            "\\n" +
            "(body content...)\\n" +
            "\\n" +
            "Required fields: title, order. Optional: volume, summary. " +
            "Each chapter must have a unique, ordered integer 'order'. " +
            "Check existing chapters with view_file first to avoid duplicate order values."
    }

    override val definition = ToolDefinition(
        name = "create_file",
        description = "Create a new file at a specified path within a project, with optional initial content. " +
            "Parent directories are auto-created. " +
            "IMPORTANT: Files under \"正文/\" are CHAPTER files and MUST include YAML front matter " +
            "(---\\ntitle: \"...\"\\norder: N\\n---\\n) at the top for ordering and summary metadata. " +
            "Files in other directories (设定/, 时间线/, 记录/, 配置/) do NOT require front matter.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path for the new file"
                ),
                "content" to mapOf(
                    "type" to "string",
                    "description" to "Initial file content (optional). For 正文/ chapter files, MUST include YAML front matter with title and order."
                )
            ),
            "required" to listOf("relativePath")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
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

        if (content.isNotEmpty() && relativePath.startsWith("$CHAPTER_DIR/")) {
            val (meta, _) = ChapterFileHelper.parseChapterContent(content)
            if (meta.title.isBlank() || meta.order <= 0) {
                return JSONObject().apply {
                    put("error", "Chapter file under \"正文/\" MUST include YAML front matter with at least title and order. " +
                        "Format:\n---\ntitle: \"Title\"\norder: 1\n---\n\n# Title\n\nBody...\n" +
                        "Required: title (non-empty), order (positive integer). Optional: volume, summary.")
                    put("help", CHAPTER_FORMAT_HELP)
                }.toString(2)
            }
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
