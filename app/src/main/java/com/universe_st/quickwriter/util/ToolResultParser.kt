package com.universe_st.quickwriter.util

import com.universe_st.quickwriter.domain.model.ExpandableItem
import com.universe_st.quickwriter.domain.model.StatItem
import com.universe_st.quickwriter.domain.model.ToolResultParsed
import org.json.JSONArray
import org.json.JSONObject

object ToolResultParser {

    fun parse(
        toolName: String,
        resultJson: String,
        argumentsJson: String?
    ): ToolResultParsed {
        val hasError = resultJson.trimStart().startsWith("{") && resultJson.contains("\"error\"")

        return try {
            val json = JSONObject(resultJson)
            if (json.has("error")) {
                ToolResultParsed(
                    toolName = toolName,
                    success = false,
                    errorMessage = json.optString("error", "Unknown error"),
                    summary = json.optString("error", "Unknown error")
                )
            } else {
                parseSuccess(toolName, json, argumentsJson)
            }
        } catch (e: Exception) {
            if (hasError) {
                val errorMsg = resultJson.substringAfter("\"error\":").trim().trim('"', '}', '{')
                    .take(200)
                ToolResultParsed(
                    toolName = toolName,
                    success = false,
                    errorMessage = errorMsg,
                    summary = errorMsg
                )
            } else {
                ToolResultParsed(
                    toolName = toolName,
                    success = true,
                    errorMessage = null,
                    summary = resultJson.take(200)
                )
            }
        }
    }

    private fun parseSuccess(toolName: String, json: JSONObject, argumentsJson: String?): ToolResultParsed {
        return when (toolName) {
            "create_file" -> parseCreateFile(json, argumentsJson)
            "edit_file" -> parseEditFile(json)
            "delete_file" -> parseDeleteFile(json)
            "move_file" -> parseMoveFile(json)
            "copy_file" -> parseCopyFile(json)
            "create_project" -> parseCreateProject(json)
            "delete_project" -> parseDeleteProject(json)
            "update_project_info" -> parseUpdateProjectInfo(json)
            "view_file" -> parseViewFile(json)
            "search_in_project" -> parseSearchInProject(json)
            "get_project_list" -> parseGetProjectList(json)
            "get_project_info" -> parseGetProjectInfo(json)
            "get_folder_structure" -> parseGetFolderStructure(json)
            else -> ToolResultParsed(
                toolName = toolName,
                success = true,
                errorMessage = null,
                summary = "",
                expandableContent = json.toString(2)
            )
        }
    }

    private fun parseCreateFile(json: JSONObject, argumentsJson: String?): ToolResultParsed {
        val path = json.optString("path", "")
        val size = json.optLong("size", 0)
        val statItems = mutableListOf<StatItem>()
        if (size > 0) statItems.add(StatItem("size", formatBytes(size)))

        var lineCount = 0
        var charCount = 0
        val content = extractContentFromArgs(argumentsJson)
        if (content != null) {
            lineCount = content.lines().size
            charCount = content.length
        }
        if (lineCount > 0) statItems.add(StatItem("lines", lineCount.toString()))
        if (charCount > 0) statItems.add(StatItem("chars", charCount.toString()))

        return ToolResultParsed(
            toolName = "create_file",
            success = true,
            errorMessage = null,
            summary = path,
            detailLines = if (statItems.isEmpty()) listOf(path) else emptyList(),
            expandableContent = content,
            statItems = statItems,
            extra = mapOf("path" to path)
        )
    }

    private fun parseEditFile(json: JSONObject): ToolResultParsed {
        val filePath = json.optString("filePath", "")
        val replacedRange = json.optString("replacedRange", "")
        val newContent = json.optString("newContent", "")
        val lineCountAfter = json.optInt("lineCountAfter", 0)
        val statItems = mutableListOf<StatItem>()
        if (replacedRange.isNotEmpty()) statItems.add(StatItem("range", replacedRange))
        if (lineCountAfter > 0) statItems.add(StatItem("linesAfter", lineCountAfter.toString()))
        val newLines = newContent.lines().size
        if (newLines > 0) statItems.add(StatItem("newLines", newLines.toString()))

        return ToolResultParsed(
            toolName = "edit_file",
            success = true,
            errorMessage = null,
            summary = filePath,
            expandableContent = newContent,
            statItems = statItems,
            extra = mapOf("path" to filePath, "range" to replacedRange)
        )
    }

    private fun parseDeleteFile(json: JSONObject): ToolResultParsed {
        val path = json.optString("path", "")
        val deleted = json.optBoolean("deleted", false)
        return ToolResultParsed(
            toolName = "delete_file",
            success = deleted,
            errorMessage = if (deleted) null else "Failed to delete",
            summary = path,
            extra = mapOf("path" to path)
        )
    }

    private fun parseMoveFile(json: JSONObject): ToolResultParsed {
        val sourcePath = json.optString("sourcePath", "")
        val targetPath = json.optString("targetPath", "")
        val moved = json.optBoolean("moved", false)
        return ToolResultParsed(
            toolName = "move_file",
            success = moved,
            errorMessage = if (moved) null else "Failed to move",
            summary = "$sourcePath → $targetPath",
            statItems = listOf(StatItem("from", sourcePath), StatItem("to", targetPath)),
            extra = mapOf("source" to sourcePath, "target" to targetPath)
        )
    }

    private fun parseCopyFile(json: JSONObject): ToolResultParsed {
        val sourcePath = json.optString("sourcePath", "")
        val targetPath = json.optString("targetPath", "")
        val size = json.optLong("size", 0)
        val statItems = mutableListOf(StatItem("to", targetPath))
        if (size > 0) statItems.add(StatItem("size", formatBytes(size)))

        return ToolResultParsed(
            toolName = "copy_file",
            success = true,
            errorMessage = null,
            summary = "$sourcePath → $targetPath",
            statItems = statItems,
            extra = mapOf("source" to sourcePath, "target" to targetPath)
        )
    }

    private fun parseCreateProject(json: JSONObject): ToolResultParsed {
        val title = json.optString("title", "")
        val projectId = json.optString("projectId", "")
        return ToolResultParsed(
            toolName = "create_project",
            success = true,
            errorMessage = null,
            summary = title,
            statItems = listOf(StatItem("id", projectId)),
            extra = mapOf("title" to title, "projectId" to projectId)
        )
    }

    private fun parseDeleteProject(json: JSONObject): ToolResultParsed {
        val title = json.optString("title", "")
        val projectId = json.optString("projectId", "")
        val deleted = json.optBoolean("deleted", false)
        return ToolResultParsed(
            toolName = "delete_project",
            success = deleted,
            errorMessage = if (deleted) null else "Failed to delete",
            summary = title,
            extra = mapOf("title" to title, "projectId" to projectId)
        )
    }

    private fun parseUpdateProjectInfo(json: JSONObject): ToolResultParsed {
        val fields = json.optJSONObject("changedFields") ?: JSONObject()
        val fieldNames = fields.keys().asSequence().toList()
        val detailLines = fieldNames.map { "$it: ${fields.optString(it)}" }
        val updated = json.optBoolean("updated", false)

        return ToolResultParsed(
            toolName = "update_project_info",
            success = updated,
            errorMessage = null,
            summary = if (fieldNames.isEmpty()) "No fields changed" else fieldNames.joinToString(", "),
            detailLines = detailLines,
            extra = mapOf("fields" to fieldNames.joinToString(", "))
        )
    }

    private fun parseViewFile(json: JSONObject): ToolResultParsed {
        val filePath = json.optString("filePath", "")
        val totalLines = json.optInt("totalLines", 0)
        val content = json.optString("content", "")
        val truncated = json.optBoolean("truncated", false)
        val message = json.optString("message", "")
        val statItems = mutableListOf(StatItem("lines", totalLines.toString()))
        if (truncated) statItems.add(StatItem("truncated", "true"))

        return ToolResultParsed(
            toolName = "view_file",
            success = true,
            errorMessage = null,
            summary = filePath,
            expandableContent = content,
            statItems = statItems,
            truncated = truncated,
            truncatedMessage = if (truncated) message else null,
            extra = mapOf("path" to filePath, "totalLines" to totalLines.toString())
        )
    }

    private fun parseSearchInProject(json: JSONObject): ToolResultParsed {
        val query = json.optString("query", "")
        val resultCount = json.optInt("resultCount", 0)
        val truncated = json.optBoolean("truncated", false)
        val results = json.optJSONArray("results")

        val items = mutableListOf<ExpandableItem>()
        if (results != null) {
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)
                items.add(
                    ExpandableItem(
                        title = r.optString("filePath", ""),
                        subtitle = "Line ${r.optInt("lineNumber", 0)}",
                        content = r.optString("lineContent", "")
                    )
                )
            }
        }

        return ToolResultParsed(
            toolName = "search_in_project",
            success = true,
            errorMessage = null,
            summary = query,
            expandableItems = if (items.isEmpty()) null else items,
            statItems = listOf(StatItem("results", resultCount.toString())),
            truncated = truncated,
            truncatedMessage = if (truncated) json.optString("message", "") else null,
            extra = mapOf("query" to query, "count" to resultCount.toString())
        )
    }

    private fun parseGetProjectList(json: JSONObject): ToolResultParsed {
        val count = json.optInt("count", 0)
        val projects = json.optJSONArray("projects")

        val items = mutableListOf<ExpandableItem>()
        if (projects != null) {
            for (i in 0 until projects.length()) {
                val p = projects.getJSONObject(i)
                val line = "${p.optString("title", "")} · ${p.optString("author", "")} · ${p.optString("genre", "")} · ${p.optString("status", "")}"
                items.add(
                    ExpandableItem(
                        title = p.optString("title", ""),
                        subtitle = p.optString("id", ""),
                        content = line
                    )
                )
            }
        }

        return ToolResultParsed(
            toolName = "get_project_list",
            success = true,
            errorMessage = null,
            summary = "$count projects",
            expandableItems = if (items.isEmpty()) null else items,
            statItems = listOf(StatItem("count", count.toString())),
            extra = mapOf("count" to count.toString())
        )
    }

    private fun parseGetProjectInfo(json: JSONObject): ToolResultParsed {
        val title = json.optString("title", "")
        val author = json.optString("author", "")
        val genre = json.optString("genre", "")
        val wordCount = json.optInt("wordCount", 0)
        val chapterCount = json.optInt("chapterCount", 0)
        val directoryStats = json.optJSONObject("directoryStats")

        val statItems = mutableListOf<StatItem>()
        if (chapterCount > 0) statItems.add(StatItem("chapters", chapterCount.toString()))
        if (wordCount > 0) statItems.add(StatItem("words", wordCount.toString()))

        val detailLines = mutableListOf<String>()
        detailLines.add("$title · $author · $genre")
        if (directoryStats != null) {
            val keys = directoryStats.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val stat = directoryStats.getJSONObject(key)
                val files = stat.optInt("fileCount", 0)
                val size = stat.optLong("totalSize", 0)
                detailLines.add("$key: $files files, ${formatBytes(size)}")
            }
        }

        return ToolResultParsed(
            toolName = "get_project_info",
            success = true,
            errorMessage = null,
            summary = "$title · $author",
            detailLines = detailLines,
            statItems = statItems,
            expandableContent = json.toString(2),
            extra = mapOf(
                "title" to title, "author" to author, "genre" to genre,
                "wordCount" to wordCount.toString(), "chapterCount" to chapterCount.toString()
            )
        )
    }

    private fun parseGetFolderStructure(json: JSONObject): ToolResultParsed {
        val path = json.optString("path", "/")
        val entries = json.optJSONArray("entries") ?: JSONArray()
        val totalItems = countEntries(entries)
        val treeLines = buildTreeLines(entries, "")

        return ToolResultParsed(
            toolName = "get_folder_structure",
            success = true,
            errorMessage = null,
            summary = path,
            expandableContent = treeLines,
            statItems = listOf(StatItem("items", totalItems.toString())),
            extra = mapOf("path" to path, "items" to totalItems.toString())
        )
    }

    private fun countEntries(entries: JSONArray): Int {
        var count = 0
        for (i in 0 until entries.length()) {
            count++
            val children = entries.getJSONObject(i).optJSONArray("children")
            if (children != null) count += countEntries(children)
        }
        return count
    }

    private fun buildTreeLines(entries: JSONArray, indent: String): String {
        val sb = StringBuilder()
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val name = entry.optString("name", "")
            val type = entry.optString("type", "")
            val size = entry.optLong("size", 0)
            val prefix = if (type == "directory") "📁" else "📄"
            val sizeStr = if (type == "file" && size > 0) " (${formatBytes(size)})" else ""
            sb.appendLine("$indent$prefix $name$sizeStr")
            val children = entry.optJSONArray("children")
            if (children != null) {
                sb.append(buildTreeLines(children, "$indent  "))
            }
        }
        return sb.toString()
    }

    private fun extractContentFromArgs(argumentsJson: String?): String? {
        if (argumentsJson.isNullOrBlank()) return null
        return try {
            val args = JSONObject(argumentsJson)
            args.optString("content", null)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        if (bytes < 1024 * 1024) return "${bytes / 1024} KB"
        return String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
    }
}
