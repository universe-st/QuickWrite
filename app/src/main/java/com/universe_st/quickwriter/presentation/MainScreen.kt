package com.universe_st.quickwriter.presentation

import android.annotation.SuppressLint
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
import androidx.compose.ui.unit.dp
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

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (currentRoute != Screen.ProjectCreate.route && currentRoute != Screen.ProjectEdit.route) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "项目") },
                        label = { Text("项目") },
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            navController.navigate(Screen.ProjectList.route) {
                                popUpTo(Screen.ProjectList.route) { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Edit, contentDescription = "写作") },
                        label = { Text("写作") },
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            navController.navigate(Screen.Writing.route) {
                                popUpTo(Screen.ProjectList.route)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.ProjectList.route)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.ProjectList.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.ProjectList.route) {
                selectedTab = 0
                val projectListViewModel: ProjectListViewModel = viewModel(
                    factory = ProjectListViewModelFactory(appContainer.projectManagementUseCase, appContainer.settingsUseCase)
                )

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
                PlaceholderScreen("写作功能正在开发中")
            }

            composable(Screen.Settings.route) {
                selectedTab = 2
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(appContainer.settingsUseCase)
                )

                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
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
        title = { Text("项目操作") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("对项目「${projectTitle}」进行的操作：")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("编辑")
                    }
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
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
