package com.universe_st.quickwriter.util

import android.content.Context
import android.net.Uri
import com.universe_st.quickwriter.R
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

enum class ChapterPattern(val displayNameKey: Int, val regex: String?) {
    CN_CHAPTER(R.string.txt_import_pattern_cn, """^第[\d零一二三四五六七八九十百千万]+[章回卷节].*$"""),
    EN_CHAPTER(R.string.txt_import_pattern_en, """^[Cc]hapter\s+\d+.*$"""),
    NUM_HEADING(R.string.txt_import_pattern_num, """^\d+[\.、．\s].*$"""),
    VOLUME(R.string.txt_import_pattern_vol, """^第[\d零一二三四五六七八九十百千万]+卷.*$"""),
    CUSTOM(R.string.txt_import_pattern_custom, null);

    fun buildRegex(customRegex: String?): Regex? {
        return when (this) {
            CUSTOM -> if (customRegex.isNullOrBlank()) null else try { Regex(customRegex) } catch (_: Exception) { null }
            else -> regex?.let { Regex(it) }
        }
    }
}

data class ChapterSlice(
    val index: Int,
    val title: String,
    val body: String,
    val isVolumeHeader: Boolean = false
)

data class TxtParseResult(
    val preludeBody: String?,
    val chapters: List<ChapterSlice>
)

object TxtChapterParser {

    fun parseChapters(
        context: Context,
        txtUri: Uri,
        charset: java.nio.charset.Charset,
        selectedPatterns: Set<ChapterPattern>,
        customRegex: String?
    ): TxtParseResult {
        val patterns = selectedPatterns.mapNotNull { it.buildRegex(customRegex) }
        val preludeLines = mutableListOf<String>()
        val chapterSlices = mutableListOf<ChapterSlice>()
        val currentBody = StringBuilder()

        var currentTitle: String? = null
        var chapterIndex = 0

        context.contentResolver.openInputStream(txtUri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, charset))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) {
                    currentBody.appendLine()
                    continue
                }

                val matchedPattern = patterns.find { it.matches(trimmed) }
                if (matchedPattern != null) {
                    if (currentTitle != null) {
                        chapterSlices.add(
                            ChapterSlice(
                                index = chapterIndex,
                                title = currentTitle,
                                body = currentBody.toString().trimEnd()
                            )
                        )
                        chapterIndex++
                    } else {
                        val preludeText = currentBody.toString().trimEnd()
                        if (preludeText.isNotEmpty()) {
                            preludeLines.add(preludeText)
                        }
                    }
                    currentTitle = trimmed.take(100).trim()
                    currentBody.clear()
                } else {
                    currentBody.appendLine(line)
                }
            }

            if (currentTitle != null) {
                chapterSlices.add(
                    ChapterSlice(
                        index = chapterIndex,
                        title = currentTitle,
                        body = currentBody.toString().trimEnd()
                    )
                )
            }
        } ?: throw IllegalStateException("Cannot open input stream for TXT file")

        val prelude = if (preludeLines.isNotEmpty()) preludeLines.joinToString("\n").trimEnd() else null

        val dedupedChapters = deduplicateTitles(chapterSlices)

        Timber.tag("TxtChapterParser").i(
            "Parsed %d chapters, prelude=%b",
            dedupedChapters.size,
            prelude != null
        )
        return TxtParseResult(prelude, dedupedChapters)
    }

    private fun deduplicateTitles(chapters: List<ChapterSlice>): List<ChapterSlice> {
        val titleCounts = mutableMapOf<String, Int>()
        return chapters.map { chapter ->
            val baseTitle = chapter.title
            val count = titleCounts.getOrDefault(baseTitle, 0)
            titleCounts[baseTitle] = count + 1
            if (count == 0) {
                chapter
            } else {
                chapter.copy(title = "$baseTitle (${count + 1})")
            }
        }
    }
}
