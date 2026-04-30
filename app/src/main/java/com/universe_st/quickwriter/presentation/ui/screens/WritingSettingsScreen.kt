package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.presentation.ui.components.SettingsDivider
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection
import com.universe_st.quickwriter.presentation.ui.components.SettingsSliderItem
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R
import com.universe_st.quickwriter.presentation.ui.components.SettingsSwitchItem
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val appSettings by viewModel.appSettingsData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.writing_settings_title)) },
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
            SettingsSection(title = stringResource(R.string.writing_settings_section_source)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.writing_settings_use_model_config),
                    subtitle = if (appSettings.useModelConfig && appSettings.modelConfigName.isNotBlank())
                        stringResource(R.string.writing_settings_use_model_config_subtitle, appSettings.modelConfigName)
                    else if (appSettings.useModelConfig)
                        stringResource(R.string.writing_settings_no_default_model)
                    else
                        stringResource(R.string.writing_settings_use_custom),
                    checked = appSettings.useModelConfig,
                    onCheckedChange = { viewModel.updateUseModelConfig(it) }
                )
            }

            SettingsSection(title = stringResource(R.string.writing_settings_section_params)) {
                val sliderEnabled = !appSettings.useModelConfig
                val displayTemperature = if (appSettings.useModelConfig) appSettings.modelConfigTemperature else appSettings.defaultTemperature
                val displayMaxTokens = if (appSettings.useModelConfig) appSettings.modelConfigMaxTokens else appSettings.defaultMaxTokens
                val displayTopP = if (appSettings.useModelConfig) appSettings.modelConfigTopP else appSettings.defaultTopP

                SettingsSliderItem(
                    title = stringResource(R.string.writing_settings_temperature),
                    subtitle = stringResource(R.string.writing_settings_temperature_desc),
                    value = displayTemperature,
                    onValueChange = { viewModel.updateDefaultTemperature(it) },
                    valueRange = 0.1f..2.0f,
                    steps = 18,
                    valueText = String.format("%.1f", displayTemperature),
                    enabled = sliderEnabled
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.writing_settings_max_tokens),
                    subtitle = stringResource(R.string.writing_settings_max_tokens_desc),
                    value = displayMaxTokens.toFloat(),
                    onValueChange = { viewModel.updateDefaultMaxTokens(it.toInt()) },
                    valueRange = 100f..8000f,
                    steps = 78,
                    valueText = displayMaxTokens.toString(),
                    enabled = sliderEnabled
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.writing_settings_top_p),
                    subtitle = stringResource(R.string.writing_settings_top_p_desc),
                    value = displayTopP,
                    onValueChange = { viewModel.updateDefaultTopP(it) },
                    valueRange = 0f..1f,
                    steps = 10,
                    valueText = String.format("%.2f", displayTopP),
                    enabled = sliderEnabled
                )
            }

            SettingsSection(title = stringResource(R.string.writing_settings_section_advanced)) {
                SettingsSliderItem(
                    title = stringResource(R.string.writing_settings_max_tool_rounds),
                    subtitle = stringResource(R.string.writing_settings_max_tool_rounds_desc),
                    value = appSettings.maxToolCallRounds.toFloat(),
                    onValueChange = { viewModel.updateMaxToolCallRounds(it.toInt()) },
                    valueRange = 5f..100f,
                    steps = 18,
                    valueText = appSettings.maxToolCallRounds.toString()
                )
            }

            SettingsSection(title = stringResource(R.string.writing_settings_section_info)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParameterInfoCard(
                        title = stringResource(R.string.writing_settings_temp_info_title),
                        description = stringResource(R.string.writing_settings_temp_info_desc)
                    )
                    
                    ParameterInfoCard(
                        title = stringResource(R.string.writing_settings_max_tokens_info_title),
                        description = stringResource(R.string.writing_settings_max_tokens_info_desc)
                    )
                    
                    ParameterInfoCard(
                        title = stringResource(R.string.writing_settings_top_p_info_title),
                        description = stringResource(R.string.writing_settings_top_p_info_desc)
                    )
                }
            }
        }
    }
}

@Composable
fun ParameterInfoCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}