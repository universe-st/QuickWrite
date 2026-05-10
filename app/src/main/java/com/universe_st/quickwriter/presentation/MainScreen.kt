package com.universe_st.quickwriter.presentation

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.universe_st.quickwriter.QuickWriteApplication
import com.universe_st.quickwriter.presentation.ui.screens.ProjectCreateScreen
import com.universe_st.quickwriter.presentation.ui.screens.ProjectDetailScreen
import com.universe_st.quickwriter.presentation.ui.screens.ProjectEditScreen
import com.universe_st.quickwriter.presentation.ui.screens.ProjectListScreen
import com.universe_st.quickwriter.presentation.ui.screens.SettingsScreen
import com.universe_st.quickwriter.presentation.ui.components.TxtImportDialog
import com.universe_st.quickwriter.presentation.viewmodel.ProjectCreateViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ProjectCreateViewModelFactory
import com.universe_st.quickwriter.presentation.viewmodel.ProjectDetailViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ProjectDetailViewModelFactory
import com.universe_st.quickwriter.presentation.viewmodel.ProjectEditViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ProjectEditViewModelFactory
import com.universe_st.quickwriter.presentation.viewmodel.ProjectListViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ProjectListViewModelFactory
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModelFactory
import com.universe_st.quickwriter.presentation.ui.screens.WritingScreen
import com.universe_st.quickwriter.presentation.viewmodel.WritingViewModel
import com.universe_st.quickwriter.presentation.viewmodel.WritingViewModelFactory
import com.universe_st.quickwriter.presentation.viewmodel.AiChatViewModel
import com.universe_st.quickwriter.presentation.viewmodel.AiChatViewModelFactory

sealed class Screen(val route: String) {
    object ProjectList : Screen("project_list")
    object ProjectCreate : Screen("project_create")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: String) = "project_detail/$projectId"
    }
    object ProjectEdit : Screen("project_edit/{projectId}") {
        fun createRoute(projectId: String) = "project_edit/$projectId"
    }
    object Writing : Screen("writing")
    object Settings : Screen("settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as QuickWriteApplication).appContainer

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedTab by remember { mutableStateOf(0) }
    var showActionDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<String?>(null) }
    var settingsInitialSubScreen by remember { mutableStateOf<com.universe_st.quickwriter.presentation.ui.screens.SettingsSubScreen?>(null) }

    val projectListViewModel: ProjectListViewModel = viewModel(
        factory = ProjectListViewModelFactory(appContainer.projectManagementUseCase, appContainer.settingsUseCase)
    )
    val zipFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            projectListViewModel.importProject(context, it)
        }
    }
    var showTxtImportDialog by remember { mutableStateOf<Uri?>(null) }
    val txtFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { showTxtImportDialog = it }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (currentRoute != Screen.ProjectCreate.route && currentRoute != Screen.ProjectEdit.route) {
                NavigationBar(
                    tonalElevation = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_projects)) },
                        label = { Text(stringResource(R.string.nav_projects)) },
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            navController.navigate(Screen.ProjectList.route) {
                                popUpTo(Screen.ProjectList.route) { inclusive = true }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.nav_writing)) },
                        label = { Text(stringResource(R.string.nav_writing)) },
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            navController.navigate(Screen.Writing.route) {
                                popUpTo(Screen.ProjectList.route)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.ProjectList.route)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.ProjectList.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(250)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(250)) }
        ) {
            composable(Screen.ProjectList.route) {
                selectedTab = 0

                ProjectListScreen(
                    onProjectLongClick = { projectId ->
                        showActionDialog = projectId
                    },
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    },
                    onCreateProject = {
                        navController.navigate(Screen.ProjectCreate.route)
                    },
                    onImportProject = {
                        zipFilePickerLauncher.launch(arrayOf("application/zip"))
                    },
                    onImportTxt = {
                        txtFilePickerLauncher.launch(arrayOf("text/plain", "application/octet-stream"))
                    },
                    viewModel = projectListViewModel
                )
            }

            composable(Screen.ProjectCreate.route) {
                selectedTab = 0
                val projectCreateViewModel: ProjectCreateViewModel = viewModel(
                    factory = ProjectCreateViewModelFactory(appContainer.projectManagementUseCase)
                )

                ProjectCreateScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    viewModel = projectCreateViewModel
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val projectDetailViewModel: ProjectDetailViewModel = viewModel(
                    factory = ProjectDetailViewModelFactory(appContainer.projectManagementUseCase, appContainer.settingsUseCase)
                )

                ProjectDetailScreen(
                    projectId = projectId,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onEdit = { id ->
                        navController.navigate(Screen.ProjectEdit.createRoute(id)) {
                            popUpTo(Screen.ProjectList.route)
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onStartWriting = {
                        navController.navigate(Screen.Writing.route) {
                            popUpTo(Screen.ProjectList.route)
                        }
                    },
                    viewModel = projectDetailViewModel
                )
            }

            composable(
                route = Screen.ProjectEdit.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
                val projectEditViewModel: ProjectEditViewModel = viewModel(
                    factory = ProjectEditViewModelFactory(appContainer.projectManagementUseCase)
                )

                ProjectEditScreen(
                    projectId = projectId,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    viewModel = projectEditViewModel
                )
            }

            composable(Screen.Writing.route) {
                selectedTab = 1
                val writingViewModel: WritingViewModel = viewModel(
                    factory = WritingViewModelFactory(appContainer.projectManagementUseCase, appContainer.settingsUseCase)
                )
                val aiChatViewModel: AiChatViewModel = viewModel(
                    factory = AiChatViewModelFactory(
                        context.applicationContext as android.app.Application,
                        appContainer.aiConversationRepository,
                        appContainer.aiModelConfigRepository,
                        appContainer.projectRepository
                    )
                )
                WritingScreen(
                    viewModel = writingViewModel,
                    aiChatViewModel = aiChatViewModel,
                    onNavigateToProjectList = {
                        navController.navigate(Screen.ProjectList.route) {
                            popUpTo(Screen.ProjectList.route) { inclusive = true }
                        }
                    },
                    onNavigateToAiConfig = {
                        settingsInitialSubScreen = com.universe_st.quickwriter.presentation.ui.screens.SettingsSubScreen.AiConfigList
                        selectedTab = 2
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(Screen.ProjectList.route)
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                selectedTab = 2
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(appContainer.settingsUseCase)
                )

                val initialSub = settingsInitialSubScreen
                LaunchedEffect(initialSub) {
                    settingsInitialSubScreen = null
                }

                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    initialSubScreen = initialSub
                )
            }
        }
    }

    if (showActionDialog != null) {
        val projectListViewModel = viewModel<ProjectListViewModel>(
            factory = ProjectListViewModelFactory(appContainer.projectManagementUseCase, appContainer.settingsUseCase)
        )
        val projectId = showActionDialog!!
        val uiState = projectListViewModel.uiState.collectAsState().value
        val projectTitle = if (uiState is com.universe_st.quickwriter.presentation.viewmodel.ProjectListUiState.Success) {
            uiState.projects.find { it.id == projectId }?.title
        } else null

        if (projectTitle != null) {
            ProjectActionDialog(
                projectTitle = projectTitle,
                onEdit = {
                    navController.navigate(Screen.ProjectEdit.createRoute(projectId))
                    showActionDialog = null
                },
                onDelete = {
                    showDeleteConfirmDialog = projectId
                    showActionDialog = null
                },
                onDismiss = {
                    showActionDialog = null
                }
            )
        }
    }

    if (showDeleteConfirmDialog != null) {
        val projectListViewModel = viewModel<ProjectListViewModel>(
            factory = ProjectListViewModelFactory(appContainer.projectManagementUseCase, appContainer.settingsUseCase)
        )
        val projectId = showDeleteConfirmDialog!!
        val uiState = projectListViewModel.uiState.collectAsState().value
        val projectTitle = if (uiState is com.universe_st.quickwriter.presentation.viewmodel.ProjectListUiState.Success) {
            uiState.projects.find { it.id == projectId }?.title
        } else null

        if (projectTitle != null) {
            DeleteConfirmDialog(
                projectTitle = projectTitle,
                onConfirm = {
                    projectListViewModel.deleteProject(projectId)
                    showDeleteConfirmDialog = null
                },
                onDismiss = {
                    showDeleteConfirmDialog = null
                }
            )
        }
    }

    showTxtImportDialog?.let { uri ->
        val fileName = uri.lastPathSegment?.substringAfterLast('/')?.removeSuffix(".txt") ?: ""
        TxtImportDialog(
            defaultTitle = fileName,
            onConfirm = { title, author, genre, patterns, customRegex ->
                showTxtImportDialog = null
                projectListViewModel.importFromTxt(context, uri, title, author, genre, patterns, customRegex.ifBlank { null })
            },
            onDismiss = {
                showTxtImportDialog = null
            }
        )
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text)
    }
}

@Composable
fun ProjectActionDialog(
    projectTitle: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_operations_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.project_operations_message, projectTitle))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.common_edit))
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.common_delete))
                    }
                }
            }
        },
        confirmButton = { }
    )
}

@Composable
fun DeleteConfirmDialog(
    projectTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.project_confirm_delete_title)) },
        text = {
            Text(stringResource(R.string.project_confirm_delete_message, projectTitle))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.common_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
