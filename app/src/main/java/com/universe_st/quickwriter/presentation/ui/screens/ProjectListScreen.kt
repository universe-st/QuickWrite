package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.data.local.entity.ProjectEntity
import com.universe_st.quickwriter.domain.usecase.ProjectManagementUseCase
import com.universe_st.quickwriter.presentation.ui.components.ProjectCard
import com.universe_st.quickwriter.presentation.viewmodel.ProjectListUiState
import com.universe_st.quickwriter.presentation.viewmodel.ProjectListViewModel
import com.universe_st.quickwriter.util.FileManager
import com.universe_st.quickwriter.ui.theme.PrimaryGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onCreateProject: () -> Unit,
    onProjectLongClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    onImportProject: () -> Unit,
    viewModel: ProjectListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val currentProjectId by viewModel.currentProjectId.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showSortDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProjectListUiState.ImportSuccess -> {
                val title = state.message.asString(context)
                snackbarHostState.showSnackbar(
                    context.getString(R.string.project_import_success, title)
                )
                viewModel.resetImportState()
            }
            is ProjectListUiState.ImportError -> {
                val msg = state.message.asString(context)
                snackbarHostState.showSnackbar(
                    context.getString(R.string.project_import_failed_detail, msg)
                )
                viewModel.resetImportState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(modifier = Modifier.background(brush = PrimaryGradient)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.project_list_title)) },
                    actions = {
                    IconButton(
                        onClick = { showSortDialog = true }
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.project_list_sort))
                    }
                    Box {
                        IconButton(
                            onClick = { showMenu = true }
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.project_list_menu))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.project_list_import)) },
                                onClick = {
                                    showMenu = false
                                    onImportProject()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            }
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = FloatingActionButtonDefaults.shape,
                        clip = true
                    )
                    .background(
                        brush = PrimaryGradient,
                        shape = FloatingActionButtonDefaults.shape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onCreateProject
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.project_list_create),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ProjectListUiState.Loading, is ProjectListUiState.Importing -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        if (uiState is ProjectListUiState.Importing) {
                            Text(
                                text = stringResource(R.string.project_importing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                is ProjectListUiState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            text = stringResource(R.string.project_list_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.project_list_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                is ProjectListUiState.Success -> {
                    val projects = (uiState as ProjectListUiState.Success).projects
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects) { project ->
                            ProjectCard(
                                project = project,
                                isCurrentProject = currentProjectId == project.id,
                                onLongClick = { onProjectLongClick(project.id) },
                                onClick = { onProjectClick(project.id) }
                            )
                        }
                    }
                }
                is ProjectListUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.project_list_loading_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is ProjectListUiState.ImportSuccess,
                is ProjectListUiState.ImportError -> {
                    // Handled via LaunchedEffect + Snackbar
                }
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            currentSortOption = sortOption,
            onSortSelected = { option ->
                viewModel.sortProjects(option)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@Composable
fun SortDialog(
    currentSortOption: ProjectManagementUseCase.SortOption,
    onSortSelected: (ProjectManagementUseCase.SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_sort_title)) },
        text = {
            Column {
                ProjectManagementUseCase.SortOption.values().forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSortOption == option,
                            onClick = {
                                onSortSelected(option)
                                onDismiss()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (option) {
                                ProjectManagementUseCase.SortOption.MODIFIED_TIME -> stringResource(R.string.project_sort_by_modified)
                                ProjectManagementUseCase.SortOption.CREATED_TIME -> stringResource(R.string.project_sort_by_created)
                                ProjectManagementUseCase.SortOption.TITLE -> stringResource(R.string.project_sort_by_name)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}