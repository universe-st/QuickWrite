package com.universe_st.quickwriter.util

import android.content.Context
import timber.log.Timber

class PromptManager(context: Context) {

    companion object {
        private const val PROMPTS_DIR = "prompts"
        private const val PLACEHOLDER_PREFIX = "{{"
        private const val PLACEHOLDER_SUFFIX = "}}"
    }

    private val templates = mutableMapOf<String, String>()

    init {
        try {
            val assetFiles = context.assets.list(PROMPTS_DIR) ?: emptyArray()
            for (fileName in assetFiles) {
                if (!fileName.endsWith(".md")) continue
                val key = fileName.removeSuffix(".md")
                val content = context.assets.open("$PROMPTS_DIR/$fileName")
                    .bufferedReader()
                    .use { it.readText() }
                templates[key] = content
                Timber.d("PromptManager: loaded template \"%s\" (%d chars)", key, content.length)
            }
        } catch (e: Exception) {
            Timber.e(e, "PromptManager: failed to load prompt templates")
        }
    }

    fun resolve(templateKey: String, variables: Map<String, String> = emptyMap()): String {
        val template = templates[templateKey] ?: run {
            Timber.w("PromptManager: template \"%s\" not found, returning empty", templateKey)
            return ""
        }
        var result = template
        for ((key, value) in variables) {
            result = result.replace("$PLACEHOLDER_PREFIX$key$PLACEHOLDER_SUFFIX", value)
        }
        return result.trimEnd()
    }

    fun getDefaultAssistantPrompt(): String {
        return resolve("default_assistant")
    }

    fun getNovelWritingAssistantPrompt(
        title: String,
        author: String,
        genre: String,
        storagePath: String,
        description: String = "",
        writingRules: String = ""
    ): String {
        return resolve("novel_writing_assistant", mapOf(
            "title" to title,
            "author" to author,
            "genre" to genre,
            "description" to description,
            "storagePath" to storagePath,
            "writingRulesContent" to writingRules
        ))
    }

    fun getNoProjectAssistantPrompt(): String {
        return resolve("no_project_assistant").ifEmpty { getDefaultAssistantPrompt() }
    }
}
