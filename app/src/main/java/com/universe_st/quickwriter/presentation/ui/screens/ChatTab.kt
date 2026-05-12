package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.presentation.ui.components.*
import com.universe_st.quickwriter.presentation.viewmodel.AiChatViewModel
import com.universe_st.quickwriter.domain.model.ChatMessage
import com.universe_st.quickwriter.domain.model.MessageRole
import com.universe_st.quickwriter.domain.model.SessionState
import com.universe_st.quickwriter.domain.model.SessionSummary
import com.universe_st.quickwriter.domain.model.ExpandableItem
import com.universe_st.quickwriter.domain.model.StatItem
import com.universe_st.quickwriter.domain.model.ToolResultParsed
import com.universe_st.quickwriter.util.ToolResultParser
import com.universe_st.quickwriter.util.UiText
import com.universe_st.quickwriter.presentation.viewmodel.ReferenceBlock
import com.universe_st.quickwriter.data.remote.SessionManager

@Composable
fun ChatTab(
    viewModel: AiChatViewModel,
    projectId: String,
    onNavigateToAiConfig: () -> Unit = {},
    isNoProjectMode: Boolean = false,
    onNavigateToProjectList: () -> Unit = {},
    referenceBlocks: List<ReferenceBlock> = emptyList(),
    onRemoveReference: (String) -> Unit = {},
    onReferencesCleared: () -> Unit = {}
) {
    LaunchedEffect(projectId, isNoProjectMode) {
        if (isNoProjectMode) {
            viewModel.isNoProjectMode = true
            viewModel.loadSessionsNoProject()
        } else {
            viewModel.loadSessions(projectId)
        }
    }

    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val currentSessionId = viewModel.currentSessionId
    val showSidebar = viewModel.showSidebar
    val sidebarTransition = remember { MutableTransitionState(false) }
    sidebarTransition.targetState = showSidebar

    var showInput by remember { mutableStateOf(true) }
    LaunchedEffect(showSidebar, sidebarTransition.currentState, sidebarTransition.isIdle) {
        showInput = when {
            showSidebar -> false
            else -> sidebarTransition.isIdle && !sidebarTransition.currentState
        }
    }
    var deleteConfirmTarget by remember { mutableStateOf<String?>(null) }
    var deleteMessageIndex by remember { mutableStateOf<Int?>(null) }

    val isGenerating = sessionState is SessionState.Generating
    val partialContent = (sessionState as? SessionState.Generating)?.partialContent
    val streamingReasoning = (sessionState as? SessionState.Generating)?.reasoningContent

    if (!viewModel.hasModelConfig) {
        NoModelConfigState(onNavigateToAiConfig = onNavigateToAiConfig)
        return
    }

    if (!viewModel.sessionsLoaded) {
        return
    }

    if (sessions.isEmpty()) {
        ChatEmptyState(
            onCreateSession = {
                if (isNoProjectMode) viewModel.createSessionNoProject()
                else viewModel.createSession(projectId)
            },
            isNoProjectMode = isNoProjectMode,
            onNavigateToProjectList = onNavigateToProjectList
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isNoProjectMode) {
            NoProjectGuidanceBanner(onNavigateToProjectList)
        }

        Box(modifier = Modifier.weight(1f)) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visibleState = sidebarTransition,
                enter = slideInHorizontally(initialOffsetX = { -it }),
                exit = slideOutHorizontally(targetOffsetX = { -it })
            ) {
                SessionSidebar(
                    sessions = sessions,
                    currentSessionId = currentSessionId,
                    onSelect = { viewModel.selectSession(it) },
                    onDelete = { deleteConfirmTarget = it },
                    onCreate = {
                        if (isNoProjectMode) viewModel.createSessionNoProject()
                        else viewModel.createSession(projectId)
                    },
                    onClose = { viewModel.showSidebar = false },
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                )
            }

            if (sidebarTransition.currentState) {
                VerticalDivider()
            }

            Box(modifier = Modifier.weight(1f)) {
                ChatContentArea(
                    currentSessionId = currentSessionId,
                    messages = messages,
                    sessionState = sessionState,
                    inputText = viewModel.inputText,
                    isGenerating = isGenerating,
                    partialContent = partialContent,
                    streamingReasoning = streamingReasoning,
                    showInput = showInput,
                    onInputChange = { viewModel.inputText = it },
                    onSend = {
                        if (referenceBlocks.isNotEmpty()) {
                            val input = viewModel.inputText.trim()
                            if (input.isNotEmpty()) {
                                val message = ReferenceBlock.buildReferenceText(referenceBlocks, input)
                                viewModel.sendMessageWithContent(message)
                                viewModel.inputText = ""
                                onReferencesCleared()
                            }
                        } else {
                            viewModel.sendMessage()
                        }
                    },
                    onStop = { viewModel.stopGeneration() },
                    onRetry = { viewModel.retryLastMessage() },
                    onDeleteMessage = { deleteMessageIndex = it },
                    referenceBlocks = referenceBlocks,
                    onRemoveReference = onRemoveReference,
                    modifier = Modifier.fillMaxSize()
                )

                if (sidebarTransition.currentState) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = null,
                                indication = null
                            ) {
                                viewModel.showSidebar = false
                            }
                    )
                }
            }
        }
        }
    }

    if (deleteConfirmTarget != null) {
        val sessionId = deleteConfirmTarget!!
        val sessionTitle = sessions.find { it.sessionId == sessionId }?.title ?: ""
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            title = { Text(stringResource(R.string.chat_delete_session)) },
            text = { Text(stringResource(R.string.chat_delete_session_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(sessionId)
                        deleteConfirmTarget = null
                    }
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (deleteMessageIndex != null) {
        val index = deleteMessageIndex!!
        AlertDialog(
            onDismissRequest = { deleteMessageIndex = null },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.chat_delete_message_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(index)
                        deleteMessageIndex = null
                    }
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteMessageIndex = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

private sealed class DisplayItem {
    data class Message(val message: ChatMessage) : DisplayItem()
    data class ToolCard(
        val toolName: String,
        val parsed: ToolResultParsed?,
        val isLoading: Boolean
    ) : DisplayItem()
}

private fun preprocessMessages(
    messages: List<ChatMessage>,
    isGenerating: Boolean
): List<DisplayItem> {
    val items = mutableListOf<DisplayItem>()
    val toolCallResults = mutableMapOf<String, String>()
    val toolCallArgs = mutableMapOf<String, String>()

    for (msg in messages) {
        if (msg.role == MessageRole.TOOL && msg.toolCallId != null) {
            toolCallResults[msg.toolCallId] = msg.content
        }
        if (msg.role == MessageRole.ASSISTANT && msg.toolCalls != null) {
            for (tc in msg.toolCalls) {
                toolCallArgs[tc.id] = tc.function.arguments
            }
        }
    }

    for (msg in messages) {
        if (msg.role == MessageRole.SYSTEM) continue
        if (msg.role == MessageRole.TOOL) continue

        if (msg.role == MessageRole.ASSISTANT && msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
            val strippedMsg = msg.copy(toolCalls = null, id = msg.id)
            if (strippedMsg.content.isNotBlank() || !strippedMsg.reasoningContent.isNullOrEmpty()) {
                items.add(DisplayItem.Message(strippedMsg))
            }
            for (tc in msg.toolCalls) {
                val resultJson = toolCallResults[tc.id]
                val argsJson = toolCallArgs[tc.id]
                if (resultJson != null) {
                    val parsed = ToolResultParser.parse(tc.function.name, resultJson, argsJson)
                    items.add(DisplayItem.ToolCard(tc.function.name, parsed, isLoading = false))
                } else if (isGenerating) {
                    items.add(DisplayItem.ToolCard(tc.function.name, null, isLoading = true))
                } else {
                    items.add(DisplayItem.ToolCard(tc.function.name, null, isLoading = false))
                }
            }
        } else {
            items.add(DisplayItem.Message(msg))
        }
    }

    return items
}

@Composable
private fun NoProjectGuidanceBanner(onNavigateToProjectList: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chat_no_project_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onNavigateToProjectList) {
                Text(
                    text = stringResource(R.string.writing_go_to_projects),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NoModelConfigState(
    onNavigateToAiConfig: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_no_config_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.chat_no_config_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNavigateToAiConfig) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.chat_no_config_action))
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    onCreateSession: () -> Unit,
    isNoProjectMode: Boolean = false,
    onNavigateToProjectList: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isNoProjectMode) {
                Text(
                    text = stringResource(R.string.chat_no_project_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.chat_no_project_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onNavigateToProjectList) {
                    Text(stringResource(R.string.writing_go_to_projects))
                }
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 40.dp))
                Spacer(modifier = Modifier.height(20.dp))
            }
            Text(
                text = stringResource(R.string.chat_empty_state),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.chat_empty_state_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateSession) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.chat_new_session))
            }
        }
    }
}

@Composable
private fun SessionSidebar(
    sessions: List<SessionSummary>,
    currentSessionId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCreate: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .offset(x = dragOffsetX.coerceAtMost(0f).dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffsetX < -120f) {
                            onClose()
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        // only respond to leftward drag
                        if (dragAmount < 0f) {
                            dragOffsetX += dragAmount
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.chat_sessions_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = onCreate,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.chat_new_session),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.chat_sidebar_toggle),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = sessions,
                key = { it.sessionId }
            ) { session ->
                SessionListItem(
                    session = session,
                    isSelected = session.sessionId == currentSessionId,
                    onClick = { onSelect(session.sessionId) },
                    onLongClick = { onDelete(session.sessionId) }
                )
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.writing_chapter_count, sessions.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionListItem(
    session: SessionSummary,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0f)
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = session.title.ifBlank { "New Chat" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor
            )
            if (session.lastMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ChatContentArea(
    currentSessionId: String?,
    messages: List<ChatMessage>,
    sessionState: SessionState,
    inputText: String,
    isGenerating: Boolean,
    partialContent: String?,
    streamingReasoning: String?,
    showInput: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDeleteMessage: (Int) -> Unit,
    referenceBlocks: List<ReferenceBlock> = emptyList(),
    onRemoveReference: (String) -> Unit = {},
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
        // TODO: Replace startsWith("Executing tool:") with proper state field for tool execution status
        if (pc.startsWith("Executing tool:")) null else pc
    } else null

    var userHasScrolledUp by remember { mutableStateOf(false) }

    val showScrollToBottomFAB by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // Detect when user manually scrolls away from bottom
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisible ->
                userHasScrolledUp = firstVisible > 0
            }
    }

    // Auto-scroll when new base items appear (new messages from user or AI)
    LaunchedEffect(baseItems.size) {
        if (!userHasScrolledUp && baseItems.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Auto-scroll during streaming content growth (char by char)
    LaunchedEffect(partialContent) {
        if (!userHasScrolledUp && isGenerating) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(currentSessionId) {
        userHasScrolledUp = false
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
                            GeneratingBubble(content = streamingContent, reasoningContent = streamingReasoning)
                        }
                    } else if (isGenerating && displayItems.isEmpty()) {
                        item(key = "loading") {
                            GeneratingBubble(content = "", reasoningContent = streamingReasoning)
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
                                    userHasScrolledUp = false
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

            if (referenceBlocks.isNotEmpty()) {
                ReferenceBlockBar(
                    blocks = referenceBlocks,
                    onRemove = onRemoveReference,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showInput) {
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
}

@Composable
private fun ChatInputArea(
    inputText: String,
    isGenerating: Boolean,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(stringResource(R.string.chat_input_hint))
                },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                enabled = enabled,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp),
                    enabled = enabled,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = stringResource(R.string.chat_stop),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                FilledIconButton(
                    onClick = {
                        keyboardController?.hide()
                        onSend()
                    },
                    modifier = Modifier.size(48.dp),
                    enabled = enabled && inputText.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.chat_send),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceBlockBar(
    blocks: List<ReferenceBlock>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${stringResource(R.string.chat_reference_label)} ${block.filePath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = block.contentPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onRemove(block.id) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.chat_reference_remove),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
