# 对话消息列表重构 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]` / `- [ ]`) syntax for tracking.

**Goal:** 用 `reverseLayout` 重构消息列表，彻底消除手动滚动逻辑；统一 AI 回复使用 MarkdownText；AI 生成时在气泡内显示进度条。

**Architecture:** LazyColumn(reverseLayout=true) + `baseItems.asReversed()`，索引 0 锚定底部；流式内容增长时自动扩张无需手动 scrollToItem；生成中气泡独立 item 不在 displayItems 中。

**Tech Stack:** Jetpack Compose, dev.jeziellago.compose.markdowntext.MarkdownText

**Spec:** `docs/superpowers/specs/2026-05-09-chat-message-list-refactor-design.md`

---

## File Structure

| 文件 | 变更 | 职责 |
|------|------|------|
| `ChatBubble.kt` | 重写 | AI 气泡组件：简化 `AssistantMessageBubble`，新增 `GeneratingBubble`，废弃旧组件 |
| `ChatTab.kt` | 重写 | `ChatContentArea`：reverseLayout + 简化滚动；`preprocessMessages` 保持原逻辑；清理 `ChatTab` 外层 |

---

### Task 1: Refactor ChatBubble.kt

**Files:** Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ChatBubble.kt`

- [ ] **Step 1: Replace ChatBubble.kt with simplified version**

Read the current file first, then write the full replacement. Removes `PulsingCursor`, `TypingIndicator`, `ToolCallBubble`, `ToolResultBubble`. `AssistantMessageBubble` simplified to always use `MarkdownText` (no `isGenerating`, `toolCalls` params). Adds `GeneratingBubble` with indeterminate `LinearProgressIndicator`.

```kotlin
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
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/components/ChatBubble.kt
git commit -m "refactor: simplify ChatBubble - always use MarkdownText for AI, add GeneratingBubble with progress bar"
```

---

### Task 2: Rewrite ChatTab.kt — ChatContentArea with reverseLayout

**Files:** Modify: `app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ChatTab.kt`

- [ ] **Step 1: Read current file to confirm state**

Read the full `ChatTab.kt` to have exact line numbers and current content for each edit.

- [ ] **Step 2: Replace DisplayItem + preprocessMessages (lines 219-274)**

The `preprocessMessages` logic stays the same — it still needs `isGenerating` for tool card loading states. Only the pseudo-message (id=Long.MAX_VALUE) is removed from ChatContentArea, not from this function.

No change needed to `DisplayItem` or `preprocessMessages`. Skip this step.

- [ ] **Step 3: Replace entire ChatContentArea function (lines 501-805)**

Replace the old `ChatContentArea` with the reverseLayout version. Key changes:
- `LazyColumn(reverseLayout = true)` with `displayItems.asReversed()`
- `GeneratingBubble` as a separate `item(key = "generating")` — not in displayItems
- Scroll-to-bottom FAB based on `listState.firstVisibleItemIndex > 0`
- Remove all old state: `followBottom`, `isProgrammaticScroll`, `isScrollingToBottom`, `isAtBottom`, `scrollToBottom()`, `snapshotFlow`, `withFrameNanos`, debug `Log.d`
- Preserve `onDeleteMessage` index mapping: convert reversed index back to original

```kotlin
@Composable
private fun ChatContentArea(
    currentSessionId: String?,
    messages: List<ChatMessage>,
    sessionState: SessionState,
    inputText: String,
    isGenerating: Boolean,
    partialContent: String?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDeleteMessage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val baseItems = remember(messages, isGenerating) {
        preprocessMessages(messages, isGenerating)
    }

    val displayItems = remember(baseItems) { baseItems.asReversed() }

    val streamingContent = if (isGenerating) {
        val pc = partialContent ?: ""
        if (pc.startsWith("Executing tool:")) null else pc
    } else null

    val showScrollToBottomFAB by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    LaunchedEffect(currentSessionId) {
        if (displayItems.isNotEmpty() || streamingContent != null) {
            listState.scrollToItem(0)
        }
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxHeight()) {
            if (sessionState is SessionState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sessionState.message.asString(context),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.chat_retry))
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    if (displayItems.isEmpty() && streamingContent == null && sessionState !is SessionState.Error) {
                        item(key = "empty_hint") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.chat_empty_state_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    if (streamingContent != null) {
                        item(key = "generating") {
                            GeneratingBubble(content = streamingContent)
                        }
                    } else if (isGenerating && streamingContent == null && displayItems.isEmpty()) {
                        item(key = "loading") {
                            GeneratingBubble(content = "")
                        }
                    }

                    itemsIndexed(
                        items = displayItems,
                        key = { index, item ->
                            when (item) {
                                is DisplayItem.Message -> item.message.id
                                is DisplayItem.ToolCard -> "tool_${item.toolName}_$index"
                            }
                        }
                    ) { reversedIndex, item ->
                        val baseIndex = baseItems.size - 1 - reversedIndex
                        when (item) {
                            is DisplayItem.Message -> {
                                var showActions by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showActions = !showActions }
                                ) {
                                    MessageBubble(
                                        message = item.message,
                                        showActions = showActions,
                                        onRetry = if (item.message.role == MessageRole.USER) {
                                            { onRetry() }
                                        } else null,
                                        onDelete = { onDeleteMessage(baseIndex) }
                                    )
                                }
                            }
                            is DisplayItem.ToolCard -> {
                                ToolExecutionCard(
                                    toolName = item.toolName,
                                    parsed = item.parsed,
                                    isLoading = item.isLoading
                                )
                            }
                        }
                    }
                }

                if (showScrollToBottomFAB && displayItems.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh
                                    .copy(alpha = 0.85f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                CircleShape
                            )
                            .clickable {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.chat_scroll_to_bottom),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            ChatInputArea(
                inputText = inputText,
                isGenerating = isGenerating,
                enabled = true,
                onInputChange = onInputChange,
                onSend = onSend,
                onStop = onStop
            )
        }
    }
}
```

- [ ] **Step 4: Update ChatTab composable — remove unused state and update ChatContentArea call**

Remove `var isScrollingToBottom` from ChatTab (line 69):
```kotlin
// REMOVE:
var isScrollingToBottom by remember { mutableStateOf(false) }
```

Remove `val isScrollingToBottom` reference from ChatContentArea call args and update to match new signature (lines 112-128):

Replace:
```kotlin
ChatContentArea(
    currentSessionId = currentSessionId,
    messages = messages,
    sessionState = sessionState,
    inputText = viewModel.inputText,
    isGenerating = isGenerating,
    partialContent = partialContent,
    isScrollingToBottom = isScrollingToBottom,
    onScrollToBottomStart = { isScrollingToBottom = true },
    onScrollToBottomEnd = { isScrollingToBottom = false },
    onInputChange = { viewModel.inputText = it },
    onSend = { viewModel.sendMessage() },
    onStop = { viewModel.stopGeneration() },
    onRetry = { viewModel.retryLastMessage() },
    onDeleteMessage = { deleteMessageIndex = it },
    modifier = Modifier.fillMaxSize()
)
```

With:
```kotlin
ChatContentArea(
    currentSessionId = currentSessionId,
    messages = messages,
    sessionState = sessionState,
    inputText = viewModel.inputText,
    isGenerating = isGenerating,
    partialContent = partialContent,
    onInputChange = { viewModel.inputText = it },
    onSend = { viewModel.sendMessage() },
    onStop = { viewModel.stopGeneration() },
    onRetry = { viewModel.retryLastMessage() },
    onDeleteMessage = { deleteMessageIndex = it },
    modifier = Modifier.fillMaxSize()
)
```

- [ ] **Step 5: Remove blocking overlay boxes and LinearProgressIndicator from ChatTab**

Remove the two `isScrollingToBottom` blocking overlay boxes (lines 146-161 and lines 789-803):

Remove outer overlay (lines 146-161):
```kotlin
// REMOVE:
if (isScrollingToBottom) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .zIndex(2f)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    )
}
```

Remove inner overlay (lines 789-803) — this was inside the Box that now contains only the new ChatContentArea implementation. With the old ChatContentArea replaced, this code is already gone. Verify it's not left over.

Remove `LinearProgressIndicator` (lines 775-777):
```kotlin
// REMOVE:
AnimatedVisibility(visible = isGenerating) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}
```

- [ ] **Step 6: Clean up unused imports**

Remove unused imports from ChatTab.kt:
```kotlin
// REMOVE if no longer referenced:
import androidx.compose.runtime.withFrameNanos  // only used in old scrollToBottom
import androidx.compose.ui.zIndex               // only used in blocking overlay boxes
import androidx.compose.ui.input.pointer.pointerInput  // only used in blocking overlay boxes
```

Keep `import androidx.compose.foundation.border` (now needed for FAB).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/universe_st/quickwriter/presentation/ui/screens/ChatTab.kt
git commit -m "refactor: rewrite ChatContentArea with reverseLayout auto-scroll, simplify streaming rendering"
```

---

### Task 3: Build Verification

**Files:** None

- [ ] **Step 1: Run debug build**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Review build warnings**

Check for any compiler warnings in the modified files and fix real issues.

- [ ] **Step 3: Cross-check spec requirements**

| Requirement | Implementation |
|-------------|---------------|
| AI replies + tool calls left-aligned | `AssistantMessageBubble` + `ToolExecutionCard` both inside `horizontalAlignment = Start` |
| User messages right-aligned | `UserMessageBubble` unchanged (`Alignment.End`) |
| Progress bar below AI reply during generation | `GeneratingBubble` includes `LinearProgressIndicator` below `MarkdownText` |
| AI replies use MarkdownText (also during generation) | `AssistantMessageBubble` always uses `MarkdownText`; `GeneratingBubble` uses `MarkdownText` for streaming content |
| User messages use plain Text | `UserMessageBubble` uses `Text()` |
| Auto-scroll to bottom on new messages | `reverseLayout = true` + `asReversed()` — new messages at index 0 auto-appear at bottom |
| SSE char-by-char + auto-scroll when at bottom | `GeneratingBubble` at index 0, `reverseLayout` anchors bottom — content growth stays visible without manual scroll calls |
| Scroll-to-bottom FAB when not at bottom | `derivedStateOf { listState.firstVisibleItemIndex > 0 }` |
