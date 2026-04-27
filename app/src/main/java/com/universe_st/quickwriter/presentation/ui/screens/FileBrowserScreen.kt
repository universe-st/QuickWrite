package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.presentation.viewmodel.FileBrowserUiState
import com.universe_st.quickwriter.presentation.viewmodel.FileBrowserViewModel
import com.universe_st.quickwriter.presentation.viewmodel.FileEntry
import com.universe_st.quickwriter.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileBrowserScreen(
    viewModel: FileBrowserViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val previewContent by viewModel.previewContent.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createIsDir by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileEntry?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<FileEntry?>(null) }
    var showNewMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            val title = when (val s = uiState) {
                is FileBrowserUiState.Success -> s.project.title
                else -> stringResource(R.string.file_browser_title)
            }
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showNewMenu = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(R.string.file_browser_new))
                    }
                    DropdownMenu(
                        expanded = showNewMenu,
                        onDismissRequest = { showNewMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.file_browser_new_file)) },
                            onClick = {
                                showNewMenu = false
                                createIsDir = false
                                showCreateDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.file_browser_new_folder)) },
                            onClick = {
                                showNewMenu = false
                                createIsDir = true
                                showCreateDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is FileBrowserUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is FileBrowserUiState.Error -> {
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
                            Button(onClick = { viewModel.loadProject() }) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                }
                is FileBrowserUiState.Success -> {
                    BreadcrumbBar(
                        breadcrumbs = state.breadcrumbs,
                        onNavigate = { viewModel.navigateTo(it) },
                        onNavigateUp = { viewModel.navigateUp() }
                    )

                    if (state.entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    stringResource(R.string.file_browser_empty),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.file_browser_empty_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f)
                        ) {
                            items(state.entries, key = { it.path }) { entry ->
                                FileEntryItem(
                                    entry = entry,
                                    isSelected = state.selectedEntry?.path == entry.path,
                                    onClick = {
                                        if (entry.isDirectory) {
                                            viewModel.enterDirectory(entry.path)
                                        } else {
                                            viewModel.selectEntry(entry)
                                            if (state.selectedEntry?.path != entry.path) {
                                                viewModel.previewFile(entry)
                                            } else {
                                                viewModel.clearPreview()
                                            }
                                        }
                                    },
                                    onLongClick = { showRenameDialog = entry },
                                    onDelete = { showDeleteConfirm = entry }
                                )
                            }
                        }
                    }

                    if (previewContent != null) {
                        FilePreviewPanel(
                            content = previewContent!!,
                            onDismiss = { viewModel.clearPreview() }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEntryDialog(
            isDirectory = createIsDir,
            onConfirm = { name ->
                if (createIsDir) viewModel.createDirectory(name)
                else viewModel.createFile(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showRenameDialog != null) {
        RenameEntryDialog(
            entry = showRenameDialog!!,
            onConfirm = { newName ->
                viewModel.renameEntry(showRenameDialog!!.path, newName)
                showRenameDialog = null
            },
            onDismiss = { showRenameDialog = null }
        )
    }

    if (showDeleteConfirm != null) {
        DeleteFileConfirmDialog(
            entry = showDeleteConfirm!!,
            onConfirm = {
                viewModel.deleteEntry(showDeleteConfirm!!.path)
                showDeleteConfirm = null
            },
            onDismiss = { showDeleteConfirm = null }
        )
    }
}

@Composable
private fun BreadcrumbBar(
    breadcrumbs: List<com.universe_st.quickwriter.presentation.viewmodel.Breadcrumb>,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (breadcrumbs.size > 1) {
                IconButton(
                    onClick = onNavigateUp,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.file_browser_parent_dir), modifier = Modifier.size(18.dp))
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(ScrollState(0)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                breadcrumbs.forEachIndexed { index, crumb ->
                    TextButton(
                        onClick = { onNavigate(crumb.path) },
                        contentPadding = WindowInsets(0.dp).let { PaddingValues(horizontal = 4.dp) },
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = crumb.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (index == breadcrumbs.lastIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == breadcrumbs.lastIndex)
                                MaterialTheme.colorScheme.primary
                            else
                                TextSecondary
                        )
                    }
                    if (index < breadcrumbs.lastIndex) {
                        Text(
                            "/",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileEntryItem(
    entry: FileEntry,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon: ImageVector = when {
                entry.isDirectory -> Icons.Default.Folder
                entry.extension == "md" -> Icons.Default.Description
                entry.extension in listOf("jpg", "jpeg", "png", "bmp", "gif") -> Icons.Default.Image
                else -> Icons.AutoMirrored.Filled.InsertDriveFile
            }
            val tint = if (entry.isDirectory)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    if (!entry.isDirectory && entry.size > 0) {
                        Text(
                            text = FileBrowserViewModel.formatFileSize(entry.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (entry.lastModified > 0) {
                        Text(
                            text = FileBrowserViewModel.formatTimestamp(entry.lastModified),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (!entry.isDirectory) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        )
                }
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
private fun FilePreviewPanel(
    content: String,
    onDismiss: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.common_preview),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.file_browser_close_preview), modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(12.dp)
                    .verticalScroll(ScrollState(0))
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CreateEntryDialog(
    isDirectory: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val title = if (isDirectory) stringResource(R.string.file_dialog_new_folder_title) else stringResource(R.string.file_dialog_new_file_title)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isDirectory) stringResource(R.string.file_dialog_folder_name_label) else stringResource(R.string.file_dialog_file_name_label)) },
                placeholder = { Text(if (isDirectory) stringResource(R.string.file_dialog_folder_placeholder) else stringResource(R.string.file_dialog_file_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }
            ) { Text(stringResource(R.string.common_create)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun RenameEntryDialog(
    entry: FileEntry,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(entry.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.file_dialog_rename_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.file_dialog_rename_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                }
            ) { Text(stringResource(R.string.common_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

@Composable
private fun DeleteFileConfirmDialog(
    entry: FileEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val type = if (entry.isDirectory) stringResource(R.string.file_dialog_type_folder) else stringResource(R.string.file_dialog_type_file)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.file_dialog_confirm_delete_title)) },
        text = {
            Text(stringResource(R.string.file_dialog_confirm_delete_message, type, entry.name))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
