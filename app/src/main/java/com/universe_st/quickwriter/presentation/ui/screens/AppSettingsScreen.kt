package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.presentation.ui.components.SettingsClickItem
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection
import com.universe_st.quickwriter.presentation.ui.components.SettingsSliderItem
import com.universe_st.quickwriter.presentation.ui.components.SettingsSwitchItem
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val appSettings by viewModel.appSettingsData.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showAutoSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "外观设置") {
                SettingsClickItem(
                    title = "主题模式",
                    subtitle = "选择应用的主题风格",
                    trailingText = getThemeModeDisplayName(appSettings.themeMode),
                    onClick = { showThemeDialog = true }
                )

                SettingsClickItem(
                    title = "字体大小",
                    subtitle = "调整编辑器字体大小",
                    trailingText = "${appSettings.fontSize}sp",
                    onClick = { showFontDialog = true }
                )
            }

            SettingsSection(title = "编辑器设置") {
                SettingsSliderItem(
                    title = "字体大小",
                    subtitle = "调整编辑器字体大小",
                    value = appSettings.fontSize.toFloat(),
                    onValueChange = { viewModel.updateFontSize(it.toInt()) },
                    valueRange = 10f..24f,
                    steps = 13,
                    valueText = "${appSettings.fontSize}sp"
                )
            }

            SettingsSection(title = "自动保存") {
                SettingsSwitchItem(
                    title = "即时自动保存",
                    subtitle = "有修改时立即自动保存，无需等待固定间隔",
                    checked = appSettings.autoSaveImmediately,
                    onCheckedChange = { viewModel.updateAutoSaveImmediately(it) }
                )
                
                SettingsClickItem(
                    title = "自动保存间隔",
                    subtitle = "设置定时自动保存的时间间隔（即时保存启用时不可用）",
                    trailingText = if (appSettings.autoSaveImmediately) "实时保存" else "${appSettings.autoSaveInterval}分钟",
                    onClick = { 
                        if (!appSettings.autoSaveImmediately) {
                            showAutoSaveDialog = true 
                        }
                    },
                    modifier = Modifier.alpha(if (appSettings.autoSaveImmediately) 0.5f else 1f)
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            currentTheme = appSettings.themeMode,
            onThemeSelected = { 
                viewModel.updateThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showFontDialog) {
        FontSizeDialog(
            currentSize = appSettings.fontSize,
            onSizeSelected = {
                viewModel.updateFontSize(it)
                showFontDialog = false
            },
            onDismiss = { showFontDialog = false }
        )
    }

    if (showAutoSaveDialog) {
        AutoSaveIntervalDialog(
            currentInterval = appSettings.autoSaveInterval,
            onIntervalSelected = {
                viewModel.updateAutoSaveInterval(it)
                showAutoSaveDialog = false
            },
            onDismiss = { showAutoSaveDialog = false }
        )
    }
}

@Composable
fun ThemeModeDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themeOptions = listOf(
        "system" to "跟随系统",
        "light" to "浅色模式",
        "dark" to "深色模式"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题模式") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                themeOptions.forEach { (mode, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = currentTheme == mode,
                            onClick = { onThemeSelected(mode) }
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun FontSizeDialog(
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sizeOptions = listOf(10, 12, 14, 16, 18, 20, 22, 24)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("字体大小") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sizeOptions.forEach { size ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = currentSize == size,
                            onClick = { onSizeSelected(size) }
                        )
                        Text(
                            text = "${size}sp",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun AutoSaveIntervalDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自动保存间隔") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = currentInterval == 0,
                        onClick = { onIntervalSelected(0) }
                    )
                    Text(
                        text = "实时保存",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                listOf(1, 3, 5, 10, 15).forEach { interval ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = currentInterval == interval,
                            onClick = { onIntervalSelected(interval) }
                        )
                        Text(
                            text = "${interval}分钟",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

private fun getThemeModeDisplayName(mode: String): String {
    return when (mode) {
        "system" -> "跟随系统"
        "light" -> "浅色模式"
        "dark" -> "深色模式"
        else -> mode
    }
}