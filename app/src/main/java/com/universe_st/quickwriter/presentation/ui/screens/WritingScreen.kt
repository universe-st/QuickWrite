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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universe_st.markor_editor.HighlightingMode
import com.universe_st.quickwriter.R
import com.universe_st.markor_editor.MarkorEditor
import com.universe_st.quickwriter.presentation.viewmodel.ChapterFileInfo
import com.universe_st.quickwriter.presentation.viewmodel.WritingUiState
import com.universe_st.quickwriter.presentation.viewmodel.WritingViewModel
import com.universe_st.quickwriter.ui.theme.TextSecondary
import com.universe_st.quickwriter.util.AppEditorConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    viewModel: WritingViewModel,
    onNavigateToProjectList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewChapterDialog by remember { mutableStateOf(false) }
    var showChapterList by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WritingTopBar(
                uiState = uiState,
                onBack = onNavigateToProjectList,
                onSave = { viewModel.saveCurrentChapter() },
                showChapterList = showChapterList,
                onToggleChapterList = { showChapterList = !showChapterList }
            )
        },
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
                        NoProjectContent(onNavigateToProjectList)
                    }
                    is WritingUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is WritingUiState.Success -> {
                        when (state.selectedTab) {
                    0 -> EditorContent(
                        state = state,
                        showChapterList = showChapterList,
                        onToggleChapterList = { showChapterList = !showChapterList },
                        onSelectChapter = { viewModel.selectChapter(it) },
                                onMoveUp = { viewModel.moveChapter(it, it - 1) },
                                onMoveDown = { viewModel.moveChapter(it, it + 1) },
                                onCreateChapter = { showNewChapterDialog = true },
                                onDeleteChapter = { viewModel.deleteChapter(it) },
                                onContentChange = { viewModel.updateEditorContent(it) }
                            )
                            1 -> DialogPlaceholder()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingTopBar(
    uiState: WritingUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    showChapterList: Boolean,
    onToggleChapterList: () -> Unit
) {
    val title = when (uiState) {
        is WritingUiState.Success -> uiState.project.title
        else -> stringResource(R.string.writing_title)
    }
    val isSaving = uiState is WritingUiState.Success && uiState.isSaving
    val saveMessage = (uiState as? WritingUiState.Success)?.saveMessage
    val wordCount = (uiState as? WritingUiState.Success)?.wordCount ?: 0
    val hasChapters = (uiState as? WritingUiState.Success)?.chapters?.isNotEmpty() == true

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
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
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
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            }
            if (hasChapters) {
                IconButton(onClick = onToggleChapterList) {
                    Icon(
                        if (showChapterList) Icons.Default.Close else Icons.Default.Menu,
                        contentDescription = if (showChapterList) stringResource(R.string.writing_hide_chapter_list) else stringResource(R.string.writing_show_chapter_list)
                    )
                }
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = stringResource(R.string.writing_save_content_desc))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
    onDeleteChapter: (Int) -> Unit,
    onContentChange: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val editorConfig = remember(isDark) {
        AppEditorConfig(isDark = isDark)
    }

    if (state.chapters.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.writing_no_chapters),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    ChapterListPanel(
                        chapters = state.chapters,
                        currentIndex = state.currentChapterIndex,
                        onSelectChapter = onSelectChapter,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                        onCreateChapter = onCreateChapter,
                        onDeleteChapter = onDeleteChapter,
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight()
                    )
                }
            }

            if (showChapterList) {
                VerticalDivider()
            }

            if (state.currentChapterIndex >= 0) {
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
                    key(state.currentChapterIndex) {
                        MarkorEditor(
                            value = state.editorContent,
                            onValueChange = onContentChange,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            editorConfig = editorConfig,
                            highlightingMode = HighlightingMode.MARKDOWN,
                            enabled = true
                        )
                    }
                }
            }
        }

        if (!showChapterList) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(40.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 50f) onToggleChapterList()
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onToggleChapterList() })
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.writing_show_chapter_list),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterListPanel(
    chapters: List<ChapterFileInfo>,
    currentIndex: Int,
    onSelectChapter: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onCreateChapter: () -> Unit,
    onDeleteChapter: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.writing_chapters_header),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = onCreateChapter,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.writing_new_chapter),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(chapters) { index, chapter ->
                val isSelected = index == currentIndex
                ChapterListItem(
                    chapter = chapter,
                    isSelected = isSelected,
                    canMoveUp = index > 0,
                    canMoveDown = index < chapters.size - 1,
                    onClick = { onSelectChapter(index) },
                    onMoveUp = { onMoveUp(index) },
                    onMoveDown = { onMoveDown(index) },
                    onDelete = { onDeleteChapter(index) }
                )
            }
        }

        HorizontalDivider()

        Text(
            text = stringResource(R.string.writing_chapter_count, chapters.size),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ChapterListItem(
    chapter: ChapterFileInfo,
    isSelected: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
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
            .clickable(onClick = onClick)
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
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
            if (chapter.summary.isNotBlank()) {
                Text(
                    text = chapter.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 10.sp
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
                color = TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun DialogPlaceholder() {
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
                color = TextSecondary
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
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.writing_no_project_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onNavigateToProjectList) {
                Text(stringResource(R.string.writing_go_to_projects))
            }
        }
    }
}
