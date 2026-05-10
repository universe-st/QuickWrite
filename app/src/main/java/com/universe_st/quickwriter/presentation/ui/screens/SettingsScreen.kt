package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel
import com.universe_st.quickwriter.presentation.ui.components.SettingsClickItem
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection
import com.universe_st.quickwriter.ui.theme.PrimaryGradient

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
            Box(modifier = Modifier.background(brush = PrimaryGradient)) {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
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
                title = stringResource(R.string.settings_section_ai),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = stringResource(R.string.settings_ai_config),
                    subtitle = if (aiConfigList.isEmpty()) stringResource(R.string.settings_ai_config_empty) else stringResource(R.string.settings_ai_config_count, aiConfigList.size),
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.AiConfigList) }
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_section_writing),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = stringResource(R.string.settings_writing_params),
                    subtitle = stringResource(R.string.settings_writing_params_desc),
                    trailingText = "${stringResource(R.string.settings_writing_temperature_label)} ${String.format("%.1f", appSettings.defaultTemperature)}",
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.WritingSettings) }
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_section_app),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = stringResource(R.string.settings_appearance_desc),
                    trailingText = stringResource(
                        when (appSettings.themeMode) {
                            "system" -> R.string.theme_mode_system
                            "light" -> R.string.theme_mode_light
                            "dark" -> R.string.theme_mode_dark
                            else -> R.string.theme_mode_system
                        }
                    ),
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.AppSettings) }
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_section_other),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                SettingsClickItem(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_desc),
                    trailingText = stringResource(R.string.settings_version),
                    onClick = { onNavigateToSubScreen(SettingsSubScreen.About) }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    initialSubScreen: SettingsSubScreen? = null
) {
    var currentSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }
    var editingConfig by remember { mutableStateOf<AiModelConfigEntity?>(null) }

    LaunchedEffect(initialSubScreen) {
        if (initialSubScreen != null) {
            currentSubScreen = initialSubScreen
        }
    }

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

