package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import com.universe_st.markor_editor.HighlightingMode
import com.universe_st.quickwriter.R
import com.universe_st.markor_editor.MarkorEditor
import com.universe_st.quickwriter.presentation.viewmodel.AiChatViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ChapterFileInfo
import com.universe_st.quickwriter.presentation.viewmodel.WritingUiState
import com.universe_st.quickwriter.presentation.viewmodel.WritingViewModel
import com.universe_st.quickwriter.data.remote.SessionManager
// TextSecondary removed — use MaterialTheme.colorScheme.onSurfaceVariant instead
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import com.universe_st.quickwriter.presentation.viewmodel.FileBrowserMode
import com.universe_st.quickwriter.util.AppEditorConfig
import com.universe_st.quickwriter.util.ChapterMeta
import com.universe_st.quickwriter.util.FileTreeItem
import com.universe_st.quickwriter.presentation.viewmodel.ReferenceBlock
import android.widget.Toast

private data class DeleteConfirmData(
    val name: String,
    val path: String,
    val isChapter: Boolean,
    val chapterIndex: Int
)

private data class RenameDialogData(
    val oldPath: String,
    val oldName: String,
    val isChapter: Boolean,
    val chapterIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    viewModel: WritingViewModel,
    aiChatViewModel: AiChatViewModel,
    onNavigateToProjectList: () -> Unit,
    onNavigateToAiConfig: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewChapterDialog by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }
    var deleteConfirmData by remember { mutableStateOf<DeleteConfirmData?>(null) }
    var renameDialogData by remember { mutableStateOf<RenameDialogData?>(null) }
    var editMetaChapterIndex by remember { mutableStateOf<Int?>(null) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.writing_copied)

    val isChatTab = uiState is WritingUiState.NoProject || (uiState is WritingUiState.Success && (uiState as WritingUiState.Success).selectedTab == 1)

    Scaffold(
        topBar = {
            WritingTopBar(
                uiState = uiState,
                onBack = onNavigateToProjectList,
                onSave = {
                    val s = viewModel.uiState.value as? WritingUiState.Success ?: return@WritingTopBar
                    if (s.fileBrowserMode == FileBrowserMode.CHAPTERS) {
                        viewModel.saveCurrentChapter()
                    } else {
                        viewModel.saveCurrentFile()
                    }
                },
                showChapterList = showChapterList,
                onToggleChapterList = { showChapterList = !showChapterList },
                isChatTab = isChatTab,
                showChatSidebar = aiChatViewModel.showSidebar,
                onToggleChatSidebar = { aiChatViewModel.showSidebar = !aiChatViewModel.showSidebar },
                onCopyFullText = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                },
                onCopyPlainText = { text ->
                    clipboardManager.setText(AnnotatedString(stripMarkdown(text)))
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            WritingStatusBar(uiState)
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            WritingTabRow(
                uiState = uiState,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                        when (val state = uiState) {
                    is WritingUiState.NoProject -> {
                        ChatTab(
                            viewModel = aiChatViewModel,
                            projectId = SessionManager.NO_PROJECT_ID,
                            onNavigateToAiConfig = onNavigateToAiConfig,
                            isNoProjectMode = true,
                            onNavigateToProjectList = onNavigateToProjectList,
                            referenceBlocks = emptyList()
                        )
                    }
                    is WritingUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is WritingUiState.Initializing -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(
                                    R.string.writing_initializing_chapters,
                                    state.current,
                                    state.total
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is WritingUiState.Success -> {
                        LaunchedEffect(state.project.id) {
                            aiChatViewModel.loadSessions(state.project.id)
                        }
                        val context = LocalContext.current
                        Box(modifier = Modifier.fillMaxSize()) {
                            EditorContent(
                                state = state,
                                showChapterList = showChapterList,
                                onToggleChapterList = { showChapterList = !showChapterList },
                                onSelectChapter = { viewModel.selectChapter(it) },
                                onMoveUp = { viewModel.moveChapter(it, it - 1) },
                                onMoveDown = { viewModel.moveChapter(it, it + 1) },
                                onCreateChapter = { showNewChapterDialog = true },
                                onContentChange = { viewModel.updateEditorContent(it) },
                                onBrowseModeChange = { viewModel.switchBrowseMode(it) },
                                onSelectNonChapterFile = { viewModel.selectNonChapterFile(it) },
                                onToggleFolder = { viewModel.toggleFolderExpanded(it) },
                                onFileDeleteRequest = { name, path, isChapter, chapterIndex ->
                                    deleteConfirmData = DeleteConfirmData(name, path, isChapter, chapterIndex)
                                },
                                onFileRenameRequest = { oldPath, oldName, isChapter, chapterIndex ->
                                    renameDialogData = RenameDialogData(oldPath, oldName, isChapter, chapterIndex)
                                },
                                onCreateNewFile = { showNewFileDialog = true },
                                onCreateNewFolder = { showNewFolderDialog = true },
                                onEditChapterMeta = { editMetaChapterIndex = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(if (state.selectedTab == 0) 1f else 0f),
                                editorEnabled = state.selectedTab == 0,
                                onEditorDispose = { scrollY, selStart ->
                                    viewModel.saveEditorScrollPosition(scrollY, selStart)
                                },
                                onAddToConversation = { selectedText, startLine, endLine ->
                                    if (aiChatViewModel.currentSessionId == null) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.chat_no_session_selected),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@EditorContent
                                    }
                                    val filePath = when {
                                        state.fileBrowserMode == FileBrowserMode.CHAPTERS && state.currentChapterIndex >= 0 ->
                                            state.chapters[state.currentChapterIndex].filePath
                                        state.currentFilePath != null ->
                                            state.currentFilePath
                                        else -> return@EditorContent
                                    }
                                    viewModel.addReference(filePath, selectedText, startLine, endLine)
                                },
                                addToConversationLabel = stringResource(R.string.chat_add_to_conversation)
                            )

                            if (state.selectedTab == 1) {
                                ChatTab(
                                    viewModel = aiChatViewModel,
                                    projectId = state.project.id,
                                    onNavigateToAiConfig = onNavigateToAiConfig,
                                    referenceBlocks = state.referenceBlocks,
                                    onRemoveReference = { id -> viewModel.removeReference(id) },
                                    onReferencesCleared = { viewModel.clearReferences() }
                                )
                            }
                        }
                    }
                    is WritingUiState.Error -> {
                        val context = LocalContext.current
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message.asString(context),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.retry() }) {
                                    Text(stringResource(R.string.common_retry))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChapterDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewChapterDialog = false },
            title = { Text(stringResource(R.string.chapter_new_title)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.chapter_field_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.createNewChapter(title.trim())
                            showNewChapterDialog = false
                        }
                    }
                ) { Text(stringResource(R.string.common_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewChapterDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    deleteConfirmData?.let { data ->
        FileDeleteConfirmDialog(
            name = data.name,
            onConfirm = {
                if (data.isChapter) {
                    viewModel.deleteChapterWithConfirm(data.chapterIndex)
                } else {
                    viewModel.deleteFileOrFolder(FileTreeItem(
                        name = data.name,
                        relativePath = "",
                        absolutePath = data.path,
                        isDirectory = false,
                        lastModified = 0
                    ))
                }
                deleteConfirmData = null
            },
            onDismiss = { deleteConfirmData = null }
        )
    }

    renameDialogData?.let { data ->
        RenameFileDialog(
            oldName = data.oldName,
            onConfirm = { newName ->
                viewModel.renameFile(data.oldPath, newName, data.isChapter, data.chapterIndex)
                renameDialogData = null
            },
            onDismiss = { renameDialogData = null }
        )
    }

    editMetaChapterIndex?.let { idx ->
        val currentState = (viewModel.uiState.value as? WritingUiState.Success) ?: return@let
        val chapter = currentState.chapters.getOrNull(idx) ?: return@let
        EditChapterMetaDialog(
            currentMeta = ChapterMeta(
                title = chapter.title,
                order = chapter.order,
                volume = chapter.volume,
                summary = chapter.summary
            ),
            onConfirm = { meta ->
                viewModel.editChapterMeta(idx, meta)
                editMetaChapterIndex = null
            },
            onDismiss = { editMetaChapterIndex = null }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            onConfirm = { name ->
                viewModel.createNewFileInCurrentDir(name)
                showNewFileDialog = false
            },
            onDismiss = { showNewFileDialog = false }
        )
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name ->
                viewModel.createNewFolderInCurrentDir(name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }
}

private fun stripMarkdown(text: String): String {
    return text
        .replace(Regex("```[\\s\\S]*?```")) { "" }
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("!\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        .replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("__(.+?)__"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("_(.+?)_"), "$1")
        .replace(Regex("~~(.+?)~~"), "$1")
        .replace(Regex("^[>*+-]\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^---+$", RegexOption.MULTILINE), "")
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingTopBar(
    uiState: WritingUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    showChapterList: Boolean,
    onToggleChapterList: () -> Unit,
    isChatTab: Boolean = false,
    showChatSidebar: Boolean = false,
    onToggleChatSidebar: () -> Unit = {},
    onCopyFullText: (String) -> Unit = {},
    onCopyPlainText: (String) -> Unit = {}
) {
    val title = when (uiState) {
        is WritingUiState.Success -> uiState.project.title
        else -> stringResource(R.string.writing_title)
    }
    val isSaving = uiState is WritingUiState.Success && uiState.isSaving
    val saveMessage = (uiState as? WritingUiState.Success)?.saveMessage
    val wordCount = (uiState as? WritingUiState.Success)?.wordCount ?: 0
    val hasChapters = (uiState as? WritingUiState.Success)?.chapters?.isNotEmpty() == true
    val showSaveButton = (uiState as? WritingUiState.Success)?.autoSaveImmediately != true
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
                if (wordCount > 0) {
                    Text(
                        text = stringResource(R.string.word_count_single, wordCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
            }
        },
        actions = {
            if (saveMessage != null) {
                Text(
                    text = saveMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    strokeWidth = 2.dp
                )
            }
            if (isChatTab) {
                IconButton(onClick = onToggleChatSidebar) {
                    Icon(
                        if (showChatSidebar) Icons.Default.Close else Icons.Default.Menu,
                        contentDescription = stringResource(R.string.chat_sidebar_toggle)
                    )
                }
            } else {
                if (uiState is WritingUiState.Success) {
                    IconButton(onClick = onToggleChapterList) {
                        Icon(
                            if (showChapterList) Icons.Default.Close else Icons.Default.Menu,
                            contentDescription = if (showChapterList) stringResource(R.string.writing_hide_chapter_list) else stringResource(R.string.writing_show_chapter_list)
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.writing_more_actions))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (showSaveButton) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.writing_save_content_desc)) },
                                onClick = {
                                    menuExpanded = false
                                    onSave()
                                },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) }
                            )
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.writing_copy_full_text)) },
                            onClick = {
                                menuExpanded = false
                                val content = (uiState as? WritingUiState.Success)?.editorContent ?: ""
                                onCopyFullText(content)
                            },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.writing_copy_plain_text)) },
                            onClick = {
                                menuExpanded = false
                                val content = (uiState as? WritingUiState.Success)?.editorContent ?: ""
                                onCopyPlainText(content)
                            },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun WritingTabRow(
    uiState: WritingUiState,
    onTabSelected: (Int) -> Unit
) {
    val editorEnabled = uiState is WritingUiState.Success
    val selectedTab = when (uiState) {
        is WritingUiState.Success -> uiState.selectedTab
        is WritingUiState.NoProject -> 1
        else -> 0
    }

    // TODO: Migrate to non-deprecated TabRow API when available in the Compose version used
    @Suppress("DEPRECATION")
    TabRow(
        selectedTabIndex = if (!editorEnabled && selectedTab == 0) 1 else selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Tab(
            selected = selectedTab == 0 && editorEnabled,
            onClick = { if (editorEnabled) onTabSelected(0) },
            enabled = editorEnabled,
            text = {
                Text(
                    stringResource(R.string.writing_editor_tab),
                    color = if (editorEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text(stringResource(R.string.writing_chat_tab)) }
        )
    }
}

@Composable
private fun EditorContent(
    state: WritingUiState.Success,
    showChapterList: Boolean,
    onToggleChapterList: () -> Unit,
    onSelectChapter: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateChapter: () -> Unit,
    onContentChange: (String) -> Unit,
    onBrowseModeChange: (FileBrowserMode) -> Unit,
    onSelectNonChapterFile: (FileTreeItem) -> Unit,
    onToggleFolder: (String) -> Unit,
    onFileDeleteRequest: (String, String, Boolean, Int) -> Unit,
    onFileRenameRequest: (String, String, Boolean, Int) -> Unit,
    onCreateNewFile: () -> Unit,
    onCreateNewFolder: () -> Unit,
    onEditChapterMeta: (Int) -> Unit,
    modifier: Modifier = Modifier,
    editorEnabled: Boolean = true,
    onEditorDispose: ((scrollY: Int, selectionStart: Int) -> Unit)? = null,
    onAddToConversation: ((selectedText: String, startLine: Int, endLine: Int) -> Unit)? = null,
    addToConversationLabel: String = "Add to Chat"
) {
    val isDarkTheme = isSystemInDarkTheme()
    val editorConfig = remember(isDarkTheme) { AppEditorConfig(isDark = isDarkTheme) }
    val isChapterMode = state.fileBrowserMode == FileBrowserMode.CHAPTERS

    if (!showChapterList && isChapterMode && state.chapters.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.writing_no_chapters),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onCreateChapter) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.writing_create_first_chapter))
                }
            }
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showChapterList,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                Box(
                    modifier = Modifier.pointerInput(showChapterList) {
                        if (!showChapterList) return@pointerInput
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = { totalDrag = 0f },
                            onDragCancel = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                                if (totalDrag < -100f) {
                                    totalDrag = 0f
                                    onToggleChapterList()
                                }
                            }
                        )
                    }
                ) {
                    FileListPanel(
                        state = state,
                        onSelectChapter = onSelectChapter,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onCreateChapter = onCreateChapter,
                        onBrowseModeChange = onBrowseModeChange,
                        onSelectNonChapterFile = onSelectNonChapterFile,
                        onToggleFolder = onToggleFolder,
                        onFileDeleteRequest = onFileDeleteRequest,
                        onFileRenameRequest = onFileRenameRequest,
                        onCreateNewFile = onCreateNewFile,
                        onCreateNewFolder = onCreateNewFolder,
                        onEditChapterMeta = onEditChapterMeta,
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                    )
                }
            }

            if (showChapterList) { VerticalDivider() }

            val showEditor = if (isChapterMode) state.currentChapterIndex >= 0
                             else state.currentFilePath != null

            if (showEditor || !showChapterList) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (showChapterList) Modifier.pointerInput(showChapterList) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    onToggleChapterList()
                                }
                            } else Modifier
                        )
                ) {
                    if (showEditor) {
                        MarkorEditor(
                            value = state.editorContent,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            editorConfig = editorConfig,
                            highlightingMode = HighlightingMode.MARKDOWN,
                            enabled = editorEnabled,
                            initialScrollY = state.editorScrollY,
                            initialSelectionStart = state.editorSelectionStart,
                            onDispose = onEditorDispose,
                            onAddToConversation = onAddToConversation,
                            addToConversationLabel = addToConversationLabel
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.writing_no_files),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListPanel(
    state: WritingUiState.Success,
    onSelectChapter: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateChapter: () -> Unit,
    onBrowseModeChange: (FileBrowserMode) -> Unit,
    onSelectNonChapterFile: (FileTreeItem) -> Unit,
    onToggleFolder: (String) -> Unit,
    onFileDeleteRequest: (String, String, Boolean, Int) -> Unit,
    onFileRenameRequest: (String, String, Boolean, Int) -> Unit,
    onCreateNewFile: () -> Unit,
    onCreateNewFolder: () -> Unit,
    onEditChapterMeta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChapterMode = state.fileBrowserMode == FileBrowserMode.CHAPTERS
    var showModeDropdown by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showChapterContextMenu by remember { mutableStateOf<Int?>(null) }
    var showFileContextMenu by remember { mutableStateOf<FileTreeItem?>(null) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(
                    onClick = { showModeDropdown = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        stringResource(state.fileBrowserMode.displayNameResId()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showModeDropdown,
                    onDismissRequest = { showModeDropdown = false }
                ) {
                    FileBrowserMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(stringResource(mode.displayNameResId())) },
                            onClick = {
                                showModeDropdown = false
                                onBrowseModeChange(mode)
                            },
                            leadingIcon = if (mode == state.fileBrowserMode) {
                                { Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box {
                FilledTonalIconButton(
                    onClick = {
                        if (isChapterMode) onCreateChapter()
                        else showAddMenu = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(
                            if (isChapterMode) R.string.writing_new_chapter
                            else R.string.writing_add_file
                        ),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (!isChapterMode) {
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.writing_add_file)) },
                            onClick = { showAddMenu = false; onCreateNewFile() },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.writing_add_folder)) },
                            onClick = { showAddMenu = false; onCreateNewFolder() },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) }
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        if (isChapterMode) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(state.chapters) { index, chapter ->
                    val isSelected = index == state.currentChapterIndex
                    ChapterListItem(
                        chapter = chapter,
                        isSelected = isSelected,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.chapters.size - 1,
                        onClick = { onSelectChapter(index) },
                        onLongPress = { showChapterContextMenu = index },
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onDelete = { onFileDeleteRequest(chapter.fileName, chapter.filePath, true, index) }
                    )
                }
            }

            showChapterContextMenu?.let { idx ->
                val chapter = state.chapters.getOrNull(idx) ?: return@let
                AlertDialog(
                    onDismissRequest = { showChapterContextMenu = null },
                    title = { Text(chapter.title) },
                    text = {
                        Column {
                            TextButton(onClick = {
                                showChapterContextMenu = null
                                onFileRenameRequest(chapter.filePath, chapter.fileName, true, idx)
                            }) {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.writing_rename_file))
                            }
                            TextButton(onClick = {
                                showChapterContextMenu = null
                                onEditChapterMeta(idx)
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.writing_edit_meta))
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showChapterContextMenu = null }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
        } else {
            if (state.fileTree.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.writing_no_files),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.fileTree, key = { it.relativePath }) { item ->
                        FileTreeItemNode(
                            item = item,
                            depth = 0,
                            isExpanded = state.expandedFolders.contains(item.relativePath),
                            isSelected = state.currentFilePath == item.absolutePath,
                            onToggleFolder = onToggleFolder,
                            onSelectFile = onSelectNonChapterFile,
                            onDelete = { onFileDeleteRequest(item.name, item.absolutePath, false, -1) },
                            onLongPress = { showFileContextMenu = item }
                        )
                    }
                }
            }

            showFileContextMenu?.let { item ->
                AlertDialog(
                    onDismissRequest = { showFileContextMenu = null },
                    title = { Text(item.name) },
                    text = {
                        TextButton(onClick = {
                            showFileContextMenu = null
                            onFileRenameRequest(item.absolutePath, item.name, false, -1)
                        }) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.writing_rename_file))
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showFileContextMenu = null }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
        }

        HorizontalDivider()

        Text(
            text = if (isChapterMode) {
                stringResource(R.string.writing_chapter_count, state.chapters.size)
            } else {
                stringResource(R.string.writing_file_count, countFilesInTree(state.fileTree))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun countFilesInTree(tree: List<FileTreeItem>): Int {
    var count = 0
    for (item in tree) {
        if (!item.isDirectory) count++
        count += countFilesInTree(item.children)
    }
    return count
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeItemNode(
    item: FileTreeItem,
    depth: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleFolder: (String) -> Unit,
    onSelectFile: (FileTreeItem) -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else Color.Transparent

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .combinedClickable(
                    onClick = { onSelectFile(item) },
                    onLongClick = onLongPress
                )
                .padding(start = (8 + depth * 12).dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isDirectory) {
                Icon(
                    if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.width(18.dp))
            }

            Icon(
                if (item.isDirectory) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp).padding(start = 4.dp),
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }

        if (item.isDirectory && isExpanded) {
            item.children.forEach { child ->
                FileTreeItemNode(
                    item = child,
                    depth = depth + 1,
                    isExpanded = false,
                    isSelected = false,
                    onToggleFolder = onToggleFolder,
                    onSelectFile = onSelectFile,
                    onDelete = { },
                    onLongPress = { }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChapterListItem(
    chapter: ChapterFileInfo,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 2.dp, end = 4.dp)
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.writing_move_up),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.writing_move_down),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (chapter.volume.isNotBlank()) {
                Text(
                    text = chapter.volume,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (chapter.summary.isNotBlank()) {
                Text(
                    text = chapter.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.common_delete),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun WritingStatusBar(uiState: WritingUiState) {
    val info = when (uiState) {
        is WritingUiState.Success -> {
            val total = uiState.chapters.size
            val current = if (uiState.currentChapterIndex >= 0) "${uiState.currentChapterIndex + 1}/$total" else "-"
            val wordCount = uiState.wordCount
            stringResource(R.string.writing_status_bar, current, wordCount)
        }
        else -> ""
    }
    if (info.isNotBlank()) {
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun NoProjectContent(onNavigateToProjectList: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.writing_ai_chat),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.writing_under_development),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.writing_no_project_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateToProjectList) {
                Text(stringResource(R.string.writing_go_to_projects))
            }
        }
    }
}

@Composable
private fun FileDeleteConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_delete_confirm_title)) },
        text = {
            Text(stringResource(R.string.writing_delete_confirm_message, name))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(stringResource(R.string.common_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun RenameFileDialog(
    oldName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(oldName.removeSuffix(".md")) }
    val error = getFileNameError(newName, oldName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_rename_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.writing_field_file_name)) },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) { Text(stringResource(R.string.common_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun EditChapterMetaDialog(
    currentMeta: ChapterMeta,
    onConfirm: (ChapterMeta) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentMeta.title) }
    var volume by remember { mutableStateOf(currentMeta.volume) }
    var summary by remember { mutableStateOf(currentMeta.summary) }
    var order by remember { mutableStateOf(currentMeta.order.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_edit_meta_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.writing_field_title)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = volume, onValueChange = { volume = it },
                    label = { Text(stringResource(R.string.writing_field_volume)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary, onValueChange = { summary = it },
                    label = { Text(stringResource(R.string.writing_field_summary)) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3
                )
                OutlinedTextField(
                    value = order, onValueChange = { order = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.writing_field_order)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newMeta = ChapterMeta(
                    title = title.ifBlank { currentMeta.title },
                    order = order.toIntOrNull() ?: currentMeta.order,
                    volume = volume, summary = summary
                )
                onConfirm(newMeta)
            }) { Text(stringResource(R.string.writing_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun NewFileDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_new_file_title)) },
        text = {
            OutlinedTextField(
                value = fileName, onValueChange = { fileName = it },
                label = { Text(stringResource(R.string.writing_field_file_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(fileName) },
                enabled = fileName.isNotBlank()
            ) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun NewFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.writing_new_folder_title)) },
        text = {
            OutlinedTextField(
                value = folderName, onValueChange = { folderName = it },
                label = { Text(stringResource(R.string.writing_field_folder_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun getFileNameError(name: String, oldName: String?): String? {
    if (name.isBlank()) return stringResource(R.string.error_invalid_file_name)
    val invalidChars = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    if (name.any { it in invalidChars }) return stringResource(R.string.error_invalid_file_name)
    if (oldName != null && name == oldName) return null
    return null
}
