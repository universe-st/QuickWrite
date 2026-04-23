package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel
import com.universe_st.quickwriter.presentation.ui.components.SettingsClickItem
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection

sealed class SettingsSubScreen(val route: String) {
    object AiConfigList : SettingsSubScreen("ai_config_list")
    object AiConfigEdit : SettingsSubScreen("ai_config_edit")
    object WritingSettings : SettingsSubScreen("writing_settings")
    object AppSettings : SettingsSubScreen("app_settings")
    object About : SettingsSubScreen("about")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    viewModel: SettingsViewModel,
    onNavigateToSubScreen: (SettingsSubScreen) -> Unit
) {
    val appSettings by viewModel.appSettingsData.collectAsState()
    val aiConfigList by viewModel.aiConfigList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(
                title = "AI配置",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = "AI模型配置",
                    subtitle = if (aiConfigList.isEmpty()) "暂无配置" else "已配置 ${aiConfigList.size} 个模型",
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.AiConfigList) }
                )
            }

            SettingsSection(
                title = "创作设置",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = "创作参数",
                    subtitle = "调整AI生成参数和偏好",
                    trailingText = "温度: ${String.format("%.1f", appSettings.defaultTemperature)}",
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.WritingSettings) }
                )
            }

            SettingsSection(
                title = "应用设置",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = "外观与字体",
                    subtitle = "主题模式、字体大小等",
                    trailingText = getThemeModeDisplayName(appSettings.themeMode),
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.AppSettings) }
                )
            }

            SettingsSection(
                title = "其他",
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = "关于",
                    subtitle = "版本信息和开发者",
                    trailingText = "v1.0.0",
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.About) }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }
    var editingConfig by remember { mutableStateOf<AiModelConfigEntity?>(null) }

    when (currentSubScreen) {
        null -> {
            SettingsMainScreen(
                viewModel = viewModel,
                onNavigateToSubScreen = { subScreen ->
                    currentSubScreen = subScreen
                }
            )
        }
        SettingsSubScreen.AiConfigList -> {
            AiConfigListScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubScreen = null },
                onAddConfig = {
                    viewModel.startAddAiConfig()
                    currentSubScreen = SettingsSubScreen.AiConfigEdit
                },
                onEditConfig = { config ->
                    editingConfig = config
                    currentSubScreen = SettingsSubScreen.AiConfigEdit
                }
            )
        }
        SettingsSubScreen.AiConfigEdit -> {
            LaunchedEffect(editingConfig) {
                if (editingConfig != null) {
                    viewModel.startEditAiConfig(editingConfig!!)
                }
            }
            AiConfigEditScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    editingConfig = null
                    currentSubScreen = SettingsSubScreen.AiConfigList
                }
            )
        }
        SettingsSubScreen.WritingSettings -> {
            WritingSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubScreen = null }
            )
        }
        SettingsSubScreen.AppSettings -> {
            AppSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentSubScreen = null }
            )
        }
        SettingsSubScreen.About -> {
            AboutScreen(
                onNavigateBack = { currentSubScreen = null }
            )
        }
    }
}

private fun getThemeModeDisplayName(mode: String): String {
    return when (mode) {
        "system" -> "跟随系统"
        "light" -> "浅色"
        "dark" -> "深色"
        else -> mode
    }
}