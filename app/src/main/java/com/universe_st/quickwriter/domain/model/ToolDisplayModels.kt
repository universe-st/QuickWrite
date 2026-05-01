package com.universe_st.quickwriter.domain.model

data class ExpandableItem(
    val title: String,
    val subtitle: String?,
    val content: String
)

data class StatItem(
    val label: String,
    val value: String
)

data class ToolResultParsed(
    val toolName: String,
    val success: Boolean,
    val errorMessage: String?,
    val summary: String,
    val detailLines: List<String> = emptyList(),
    val expandableContent: String? = null,
    val expandableItems: List<ExpandableItem>? = null,
    val statItems: List<StatItem> = emptyList(),
    val truncated: Boolean = false,
    val truncatedMessage: String? = null,
    val extra: Map<String, String> = emptyMap()
)
