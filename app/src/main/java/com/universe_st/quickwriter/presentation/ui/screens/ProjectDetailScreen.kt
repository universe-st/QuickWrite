package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.presentation.viewmodel.ProjectDetailUiState
import com.universe_st.quickwriter.presentation.viewmodel.ProjectDetailViewModel
import com.universe_st.quickwriter.ui.theme.TextSecondary
import com.universe_st.quickwriter.util.AppUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBackPressed: () -> Unit,
    onEdit: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProjectDetailViewModel
) {
    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isCurrentProject by viewModel.isCurrentProject.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("项目详情") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导出")
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("TXT 格式") },
                            onClick = {
                                showExportMenu = false
                            },
                            enabled = false
                        )
                        DropdownMenuItem(
                            text = { Text("EPUB 格式") },
                            onClick = {
                                showExportMenu = false
                            },
                            enabled = false
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ProjectDetailUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ProjectDetailUiState.Success -> {
                    val project = (uiState as ProjectDetailUiState.Success).project
                    ProjectDetailContent(
                        project = project,
                        isCurrentProject = isCurrentProject,
                        onEdit = { onEdit(project.id) },
                        onDelete = { showDeleteDialog = true },
onSetCurrent = { 
                        if (isCurrentProject) {
                            viewModel.unsetCurrentProject()
                        } else {
                            viewModel.setCurrentProject(project.id)
                        }
                    },
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is ProjectDetailUiState.Error -> {
                    val errorMessage = (uiState as ProjectDetailUiState.Error).message
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onNavigateBack) {
                            Text("返回")
                        }
                    }
                }
                else -> {}
            }
        }
    }

    if (showDeleteDialog) {
        val projectTitle = when (val state = uiState) {
            is ProjectDetailUiState.Success -> state.project.title
            else -> ""
        }
        DeleteConfirmDialog(
            projectTitle = projectTitle,
            onConfirm = {
                viewModel.deleteProject(projectId)
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ProjectDetailUiState.DeleteSuccess,
            ProjectDetailUiState.SetCurrentSuccess -> {
                onNavigateBack()
            }
            else -> {}
        }
    }
}

@Composable
fun ProjectDetailContent(
    project: ProjectEntity,
    isCurrentProject: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp, 100.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (!project.coverImagePath.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(project.coverImagePath)
                                .crossfade(true)
                                .build(),
                            contentDescription = "项目封面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = project.title.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = project.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = project.author,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    SuggestionChip(
                        onClick = { },
                        label = { Text(project.genre) },
                        enabled = false
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "项目信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (!project.description.isNullOrEmpty()) {
                    Text(
                        text = "描述",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Divider()
                }

                InfoRow("创建时间", AppUtils.formatTimestamp(project.createdTime))
                InfoRow("修改时间", AppUtils.formatTimestamp(project.modifiedTime))
                InfoRow("字数", AppUtils.formatWordCount(project.wordCount))
                InfoRow("章节数", "${project.chapterCount} 章")
                InfoRow("状态", project.status)
                InfoRow("存储路径", project.storagePath, maxLines = 2)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("编辑")
            }
            
            Button(
                onClick = onSetCurrent,
                modifier = Modifier.weight(1f),
                colors = if (isCurrentProject) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Icon(
                    if (isCurrentProject) Icons.Default.PushPin else Icons.Rounded.PushPin,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isCurrentProject) "主项目" else "设为主项目")
            }
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("删除项目")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, maxLines: Int = 1) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    projectTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = {
            Text("确定要删除项目「${projectTitle}」吗？\n\n此操作将删除项目的所有数据，包括数据库记录、文件目录和所有相关文件。此操作不可恢复。")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "删除",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
