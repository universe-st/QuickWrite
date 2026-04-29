package com.universe_st.quickwriter.util

object TokenEstimator {

    private const val CHARS_PER_TOKEN = 4

    fun estimateTokenCount(text: String): Int {
        return text.length / CHARS_PER_TOKEN
    }

    fun estimateTokenCount(messages: List<com.universe_st.quickwriter.domain.model.ChatMessage>): Int {
        return messages.sumOf { estimateTokenCount(it.content) }
    }
}
