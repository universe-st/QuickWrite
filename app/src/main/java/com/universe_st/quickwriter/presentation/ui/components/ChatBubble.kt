package com.universe_st.quickwriter.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.MessageRole
import dev.jeziellago.compose.markdowntext.MarkdownText

@Composable
fun MessageBubble(
    message: ChatMessage,
    showActions: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (message.role) {
        MessageRole.USER -> UserMessageBubble(
            content = message.content,
            showActions = showActions,
            onRetry = onRetry,
            onDelete = onDelete,
            modifier = modifier
        )
        MessageRole.ASSISTANT -> AssistantMessageBubble(
            content = message.content,
            modifier = modifier
        )
        MessageRole.TOOL -> { /* Tool results rendered as ToolExecutionCard in ChatTab */ }
        MessageRole.SYSTEM -> SystemMessageBubble(
            content = message.content,
            modifier = modifier
        )
    }
}

@Composable
fun UserMessageBubble(
    content: String,
    showActions: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onRetry != null) {
                    SmallIconButton(
                        icon = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.chat_retry),
                        onClick = onRetry
                    )
                }
                SmallIconButton(
                    icon = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.chat_copy),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(content))
                        showCopied = true
                    }
                )
                if (onDelete != null) {
                    SmallIconButton(
                        icon = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.common_delete),
                        onClick = onDelete
                    )
                }
            }
        }

        if (showCopied) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1500)
                showCopied = false
            }
            Text(
                text = stringResource(R.string.chat_copied),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun AssistantMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            MarkdownText(markdown = content)
        }
    }
}

@Composable
fun GeneratingBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                if (content.isNotEmpty()) {
                    MarkdownText(markdown = content)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LoadingPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingPlaceholder")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loadingAlpha"
    )

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.chat_typing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { index ->
                val dotAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, delayMillis = index * 150),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "loadingDot$index"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dotAlpha)
                        )
                )
            }
        }
    }
}

@Composable
fun SystemMessageBubble(
    content: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
