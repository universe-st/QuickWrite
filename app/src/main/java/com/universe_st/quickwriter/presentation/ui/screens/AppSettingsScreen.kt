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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.presentation.ui.components.SettingsClickItem
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection
import com.universe_st.quickwriter.presentation.ui.components.SettingsSliderItem
import com.universe_st.quickwriter.presentation.ui.components.SettingsSwitchItem
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel
import com.universe_st.quickwriter.util.LocaleHelper

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
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = stringResource(R.string.app_settings_section_appearance)) {
                SettingsClickItem(
                    title = stringResource(R.string.app_settings_theme_mode),
                    subtitle = stringResource(R.string.app_settings_theme_mode_desc),
                    trailingText = stringResource(
                        when (appSettings.themeMode) {
                            "system" -> R.string.theme_mode_system
                            "light" -> R.string.theme_mode_light
                            "dark" -> R.string.theme_mode_dark
                            else -> R.string.theme_mode_system
                        }
                    ),
                    onClick = { showThemeDialog = true }
                )

                SettingsClickItem(
                    title = stringResource(R.string.app_settings_font_size),
                    subtitle = stringResource(R.string.app_settings_font_size_desc),
                    trailingText = stringResource(R.string.app_settings_font_size_unit, appSettings.fontSize),
                    onClick = { showFontDialog = true }
                )

                SettingsClickItem(
                    title = stringResource(R.string.app_settings_language),
                    subtitle = stringResource(R.string.app_settings_language_desc),
                    trailingText = stringResource(LocaleHelper.languageCodeToResourceId(appSettings.languageCode)),
                    onClick = { showLanguageDialog = true }
                )
            }

            SettingsSection(title = stringResource(R.string.app_settings_section_editor)) {
                SettingsSliderItem(
                    title = stringResource(R.string.app_settings_font_size_editor),
                    subtitle = stringResource(R.string.app_settings_font_size_editor_desc),
                    value = appSettings.fontSize.toFloat(),
                    onValueChange = { viewModel.updateFontSize(it.toInt()) },
                    valueRange = 10f..24f,
                    steps = 13,
                    valueText = stringResource(R.string.app_settings_font_size_unit, appSettings.fontSize)
                )
            }

            SettingsSection(title = stringResource(R.string.app_settings_section_auto_save)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.app_settings_instant_save),
                    subtitle = stringResource(R.string.app_settings_instant_save_desc),
                    checked = appSettings.autoSaveImmediately,
                    onCheckedChange = { viewModel.updateAutoSaveImmediately(it) }
                )
                
                SettingsClickItem(
                    title = stringResource(R.string.app_settings_auto_save_interval),
                    subtitle = stringResource(R.string.app_settings_auto_save_interval_desc),
                    trailingText = if (appSettings.autoSaveImmediately) stringResource(R.string.app_settings_save_realtime) else stringResource(R.string.app_settings_save_interval_minutes, appSettings.autoSaveInterval),
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

    if (showLanguageDialog) {
        val context = LocalContext.current
        LanguageDialog(
            currentLanguage = appSettings.languageCode,
            onLanguageSelected = { code ->
                viewModel.updateLanguage(code)
                showLanguageDialog = false
                val activity = context as android.app.Activity
                LocaleHelper.applyLocale(activity, code)
                activity.recreate()
            },
            onDismiss = { showLanguageDialog = false }
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
        "system" to stringResource(R.string.theme_mode_system),
        "light" to stringResource(R.string.theme_mode_light),
        "dark" to stringResource(R.string.theme_mode_dark)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_mode_dialog_title)) },
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
                Text(stringResource(R.string.common_close))
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
        title = { Text(stringResource(R.string.font_size_dialog_title)) },
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
                            text = stringResource(R.string.app_settings_font_size_unit, size),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
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

@Composable
fun AutoSaveIntervalDialog(
    currentInterval: Int,
    onIntervalSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auto_save_interval_dialog_title)) },
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
                        text = stringResource(R.string.auto_save_realtime),
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
                            text = stringResource(R.string.app_settings_save_interval_minutes, interval),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
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

@Composable
fun LanguageDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languageOptions = listOf(
        LocaleHelper.CODE_SYSTEM to stringResource(R.string.language_system),
        LocaleHelper.CODE_EN to stringResource(R.string.language_en),
        LocaleHelper.CODE_ZH_CN to stringResource(R.string.language_zh_cn),
        LocaleHelper.CODE_ZH_TW to stringResource(R.string.language_zh_tw)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_dialog_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                languageOptions.forEach { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = currentLanguage == code,
                            onClick = { onLanguageSelected(code) }
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
                Text(stringResource(R.string.common_close))
            }
        }
    )
}
