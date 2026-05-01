package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.ChapterFileHelper
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class GetChapterMetaTool : ChatTool {

    override val definition = ToolDefinition(
        name = "get_chapter_meta",
        description = "Read the YAML front matter metadata (title, order, volume, summary) of a chapter file under 正文/. " +
            "Returns line numbers for front matter boundaries and body start, so you know which lines are safe to edit with edit_file.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the chapter file (e.g. 正文/第一章.md)"
                )
            ),
            "required" to listOf("relativePath")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
        val relativePath = arguments.optString("relativePath", "")

        if (relativePath.isEmpty()) return """{"error": "relativePath is required."}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!filePath.exists() || !filePath.isFile) {
            return """{"error": "File not found: $relativePath"}"""
        }

        val rawContent = filePath.readText()
        val lines = rawContent.lines()
        val totalLines = lines.size
        val (meta, body) = ChapterFileHelper.parseChapterContent(rawContent)

        val fmRange = findFrontMatterRange(lines)
        val hasFrontMatter = meta.title.isNotBlank() || meta.order > 0

        val result = JSONObject().apply {
            put("path", relativePath)
            put("hasFrontMatter", hasFrontMatter)
            put("title", meta.title)
            put("order", meta.order)
            put("volume", meta.volume)
            put("summary", meta.summary)
            put("totalLines", totalLines)
            if (fmRange != null) {
                val (fmStart, fmEnd) = fmRange
                put("frontMatterStartLine", fmStart)
                put("frontMatterEndLine", fmEnd)
                put("bodyStartLine", fmEnd + 2)
            } else {
                put("frontMatterStartLine", 0)
                put("frontMatterEndLine", 0)
                put("bodyStartLine", 1)
            }
        }
        return result.toString(2)
    }

    private fun findFrontMatterRange(lines: List<String>): Pair<Int, Int>? {
        if (lines.isEmpty() || lines[0] != "---") return null
        val endIndex = lines.subList(1, lines.size).indexOf("---")
        if (endIndex == -1) return null
        return Pair(1, endIndex + 2)
    }
}
