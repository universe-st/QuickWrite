package com.universe_st.quickwriter.presentation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.universe_st.quickwriter.QuickWriteApplication
import com.universe_st.quickwriter.presentation.ui.screens.ProjectCreateScreen
import com.universe_st.quickwriter.presentation.ui.screens.ProjectListScreen
import com.universe_st.quickwriter.presentation.viewmodel.ProjectCreateViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ProjectCreateViewModelFactory
import com.universe_st.quickwriter.presentation.viewmodel.ProjectListViewModel
import com.universe_st.quickwriter.presentation.viewmodel.ProjectListViewModelFactory

sealed class Screen(val route: String) {
    object ProjectList : Screen("project_list")
    object ProjectCreate : Screen("project_create")
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

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.ProjectCreate.route) {
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
                    factory = ProjectListViewModelFactory(appContainer.projectManagementUseCase)
                )
                
                ProjectListScreen(
                    onProjectClick = { projectId ->
                    },
                    onCreateProject = {
                        navController.navigate(Screen.ProjectCreate.route)
                    },
                    onProjectLongClick = { projectId ->
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

            composable(Screen.Writing.route) {
                selectedTab = 1
                PlaceholderScreen("写作功能正在开发中")
            }

            composable(Screen.Settings.route) {
                selectedTab = 2
                PlaceholderScreen("设置功能正在开发中")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(text)
    }
}