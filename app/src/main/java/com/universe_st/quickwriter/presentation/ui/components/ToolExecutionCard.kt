package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.domain.model.ExpandableItem
import com.universe_st.quickwriter.domain.model.StatItem
import com.universe_st.quickwriter.domain.model.ToolResultParsed
import com.universe_st.quickwriter.util.ToolDisplayHelper

@Composable
fun ToolExecutionCard(
    toolName: String,
    parsed: ToolResultParsed?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val displayStrings = ToolDisplayHelper.getDisplayStrings(toolName)
    val displayName = displayStrings.displayName
    val loadingText = displayStrings.loadingText
    val icon = ToolDisplayHelper.getIcon(toolName)

    var expanded by remember { mutableStateOf(false) }
    val canExpand = hasExpandableContent(parsed)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(4.dp, 12.dp, 12.dp, 12.dp),
            color = when {
                isLoading -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                parsed != null && !parsed.success -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
            border = if (isLoading) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            } else null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .then(
                        if (canExpand) Modifier.clickable { expanded = !expanded }
                        else Modifier
                    )
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = when {
                            parsed != null && !parsed.success -> MaterialTheme.colorScheme.error
                            isLoading -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (isLoading) loadingText else displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    if (isLoading) {
                        Spacer(modifier = Modifier.width(6.dp))
                        LoadingDots(dotSize = 5.dp, spacing = 3.dp, dotColor = MaterialTheme.colorScheme.tertiary)
                    } else if (parsed != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusBadge(success = parsed.success)
                        if (canExpand) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (expanded) "▲" else "▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                if (parsed != null && parsed.statItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    StatRow(parsed.statItems)
                } else if (parsed != null && parsed.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parsed.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }

                if (parsed != null && parsed.detailLines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    parsed.detailLines.take(3).forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                }

                if (parsed != null && parsed.truncated) {
                    Text(
                        text = stringResource(R.string.tool_truncated),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                AnimatedVisibility(
                    visible = expanded && canExpand,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        ExpandedContent(parsed)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(success: Boolean) {
    val (text, color) = if (success) {
        stringResource(R.string.tool_status_completed) to MaterialTheme.colorScheme.primary
    } else {
        stringResource(R.string.tool_status_failed) to MaterialTheme.colorScheme.error
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun StatRow(statItems: List<StatItem>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        statItems.forEach { stat ->
            Text(
                text = "${statLabel(stat.label)}: ${stat.value}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun ExpandedContent(parsed: ToolResultParsed?) {
    if (parsed == null) return

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .heightIn(max = 300.dp)
            .verticalScroll(scrollState)
    ) {
        if (parsed.expandableContent != null) {
            Text(
                text = parsed.expandableContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        } else if (parsed.expandableItems != null) {
            parsed.expandableItems.forEach { item ->
                ExpandableItemRow(item)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ExpandableItemRow(item: ExpandableItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            if (item.subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
        Text(
            text = item.content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 3
        )
    }
}

private fun hasExpandableContent(parsed: ToolResultParsed?): Boolean {
    if (parsed == null) return false
    return parsed.expandableContent != null || !parsed.expandableItems.isNullOrEmpty()
}

@Composable
private fun statLabel(key: String): String {
    return when (key) {
        "lines" -> stringResource(R.string.tool_stat_lines).replace(Regex("%1\\\$d"), "").trim()
        "chars" -> stringResource(R.string.tool_stat_chars).replace(Regex("%1\\\$d"), "").trim()
        "size" -> stringResource(R.string.tool_size)
        "results" -> stringResource(R.string.tool_stat_results).replace(Regex("%1\\\$d"), "").trim()
        "items" -> stringResource(R.string.tool_stat_items).replace(Regex("%1\\\$d"), "").trim()
        "count" -> stringResource(R.string.tool_count)
        "chapters" -> stringResource(R.string.tool_chapters)
        "words" -> stringResource(R.string.tool_words)
        "range" -> stringResource(R.string.tool_stat_range).replace(Regex("%1\\\$s"), "").trim()
        "linesAfter" -> stringResource(R.string.tool_stat_lines_after).replace(Regex("%1\\\$d"), "").trim()
        "newLines" -> "new lines"
        "from" -> stringResource(R.string.tool_stat_from).replace(Regex("%1\\\$s"), "").trim()
        "to" -> stringResource(R.string.tool_stat_to).replace(Regex("%1\\\$s"), "").trim()
        "fields" -> stringResource(R.string.tool_fields)
        "id" -> "ID"
        else -> key
    }
}


