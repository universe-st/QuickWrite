package com.universe_st.quickwriter.data.remote.tools

import com.universe_st.quickwriter.data.remote.ViewTracker
import com.universe_st.quickwriter.data.repository.ProjectRepository
import com.universe_st.quickwriter.domain.model.ChatTool
import com.universe_st.quickwriter.domain.model.ToolContext
import com.universe_st.quickwriter.domain.model.ToolDefinition
import com.universe_st.quickwriter.util.ChapterFileHelper
import com.universe_st.quickwriter.util.FileManager
import org.json.JSONObject
import java.io.File

class EditFileTool : ChatTool {

    companion object {
        private const val CHAPTER_DIR = "正文"
    }

    override val definition = ToolDefinition(
        name = "edit_file",
        description = "Replace content at a specific line range in a project file. Supports precise line-level editing. " +
            "CRITICAL: You MUST call view_file on the same file first with a line range that covers the edit range. " +
            "Editing a file not yet viewed will result in an error. After a successful edit, the view is cleared — you must view_file again before the next edit. " +
            "IMPORTANT: For chapter files under \"正文/\", editing the YAML front matter (lines between --- and ---) is BLOCKED. " +
            "Use get_chapter_meta to read metadata and update_chapter_meta to modify it. " +
            "You may only edit body content after the closing ---.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "relativePath" to mapOf(
                    "type" to "string",
                    "description" to "Relative path of the file from project root"
                ),
                "startLine" to mapOf(
                    "type" to "integer",
                    "description" to "Starting line to replace (1-indexed)"
                ),
                "endLine" to mapOf(
                    "type" to "integer",
                    "description" to "Ending line to replace (1-indexed, -1 = replace to end of file)"
                ),
                "newContent" to mapOf(
                    "type" to "string",
                    "description" to "New content to insert (multi-line text)"
                )
            ),
            "required" to listOf("relativePath", "startLine", "endLine", "newContent")
        )
    )

    override suspend fun execute(arguments: JSONObject, context: ToolContext): String {
        val projectId = context.projectId
        val relativePath = arguments.optString("relativePath", "")
        val startLine = arguments.optInt("startLine", 1)
        val endLine = arguments.optInt("endLine", -1)
        val newContent = arguments.optString("newContent", "")

        if (relativePath.isEmpty()) return """{"error": "relativePath is required. Call get_folder_structure first to discover available file paths."}"""

        val project = context.projectRepository.getProjectById(projectId)
            ?: return """{"error": "Project not found: $projectId"}"""

        val filePath = File(project.storagePath, relativePath)
        if (!context.fileManager.isPathSafe(filePath.canonicalPath)) {
            return """{"error": "Path is not within project scope"}"""
        }

        if (!filePath.exists()) {
            return """{"error": "File not found: $relativePath"}"""
        }

        val rawContent = filePath.readText()
        val lines = rawContent.lines().toMutableList()
        val totalLines = lines.size
        val actualStartLine = startLine.coerceIn(1, totalLines.coerceAtLeast(1))
        val actualEndLine = if (endLine == -1) totalLines else endLine.coerceIn(actualStartLine, totalLines)

        val tracker = context.viewTracker
        if (tracker != null && context.sessionId.isNotEmpty()) {
            val viewRecord = tracker.getViewRecord(context.sessionId, relativePath)
            if (viewRecord == null) {
                return JSONObject().apply {
                    put("error", "File not viewed yet. You MUST call view_file(\"$relativePath\") first to read the file content before calling edit_file. Editing a file without viewing it first is forbidden.")
                    put("requiredAction", "view_file")
                    put("requiredPath", relativePath)
                }.toString(2)
            }
            if (actualStartLine < viewRecord.startLine || actualEndLine > viewRecord.endLine) {
                return JSONObject().apply {
                    put("error", "View range does not cover edit range. You viewed lines ${viewRecord.startLine}-${viewRecord.endLine} but tried to edit lines $actualStartLine-$actualEndLine. Call view_file(\"$relativePath\", startLine=$actualStartLine, endLine=$actualEndLine) first to view the lines you intend to edit, then call edit_file again.")
                    put("viewedStartLine", viewRecord.startLine)
                    put("viewedEndLine", viewRecord.endLine)
                    put("editStartLine", actualStartLine)
                    put("editEndLine", actualEndLine)
                    put("requiredAction", "view_file")
                    put("requiredPath", relativePath)
                }.toString(2)
            }
        }

        if (relativePath.startsWith("$CHAPTER_DIR/")) {
            val fmRange = findFrontMatterRange(lines)
            if (fmRange != null) {
                val (fmStart, fmEnd) = fmRange
                if (actualStartLine <= fmEnd) {
                    return JSONObject().apply {
                        put("error", "Cannot edit YAML front matter (lines $fmStart-$fmEnd) with edit_file. " +
                            "Use get_chapter_meta to read metadata and update_chapter_meta to modify title, order, volume, or summary.")
                        put("frontMatterStartLine", fmStart)
                        put("frontMatterEndLine", fmEnd)
                        put("bodyStartLine", fmEnd + 2)
                        put("suggestion", "Set startLine to $fmEnd+2 or higher to edit body content only.")
                    }.toString(2)
                }
            }
        }

        val newLines = newContent.lines()

        while (lines.size < actualStartLine - 1) lines.add("")
        while (lines.size < actualEndLine) lines.add("")

        lines.subList(actualStartLine - 1, actualEndLine).clear()
        lines.addAll(actualStartLine - 1, newLines)

        filePath.writeText(lines.joinToString("\n"))

        context.viewTracker?.clearFileView(context.sessionId, relativePath)

        val result = JSONObject().apply {
            put("filePath", relativePath)
            put("replacedRange", "$actualStartLine-$actualEndLine")
            put("newContent", newContent)
            put("lineCountAfter", lines.size)
        }
        return result.toString(2)
    }

    private fun findFrontMatterRange(lines: List<String>): Pair<Int, Int>? {
        if (lines.isEmpty() || lines[0] != "---") return null
        val endIndex = lines.subList(1, lines.size).indexOf("---")
        if (endIndex == -1) return null
        return Pair(1, endIndex + 2) // 1-indexed, inclusive
    }
}
