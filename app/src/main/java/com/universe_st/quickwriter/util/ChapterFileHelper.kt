package com.universe_st.quickwriter.util

data class ChapterMeta(
    val title: String = "",
    val order: Int = 0,
    val volume: String = "",
    val summary: String = ""
)

object ChapterFileHelper {

    fun parseChapterContent(content: String): Pair<ChapterMeta, String> {
        val lines = content.lines()
        if (lines.size < 2 || lines.first() != "---") {
            return Pair(ChapterMeta(), content)
        }

        val endIndex = lines.subList(1, lines.size).indexOf("---")
        if (endIndex == -1) return Pair(ChapterMeta(), content)
        val actualEnd = endIndex + 1

        val frontMatterLines = lines.subList(1, actualEnd)
        val body = lines.drop(actualEnd + 1).joinToString("\n").trimStart('\n')

        var title = ""
        var order = 0
        var volume = ""
        var summary = ""

        for (line in frontMatterLines) {
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) continue
            val key = line.substring(0, colonIndex).trim()
            val rawValue = line.substring(colonIndex + 1).trim()
            val value = rawValue.removeSurrounding("\"").removeSurrounding("'").trim()
            when (key) {
                "title" -> title = value
                "order" -> order = value.toIntOrNull() ?: 0
                "volume" -> volume = value
                "summary" -> summary = value
            }
        }

        return Pair(ChapterMeta(title, order, volume, summary), body)
    }

    fun buildChapterContent(meta: ChapterMeta, body: String): String {
        val sb = StringBuilder()
        sb.appendLine("---")
        if (meta.title.isNotBlank()) sb.appendLine("title: \"${meta.title}\"")
        if (meta.order > 0) sb.appendLine("order: ${meta.order}")
        if (meta.volume.isNotBlank()) sb.appendLine("volume: \"${meta.volume}\"")
        if (meta.summary.isNotBlank()) sb.appendLine("summary: \"${meta.summary}\"")
        sb.appendLine("---")
        sb.appendLine()
        sb.append(body.trimStart())
        return sb.toString()
    }

    fun extractTitleFromBody(body: String): String {
        for (line in body.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("# ")) {
                return trimmed.removePrefix("# ").trim()
            }
        }
        return ""
    }
}
