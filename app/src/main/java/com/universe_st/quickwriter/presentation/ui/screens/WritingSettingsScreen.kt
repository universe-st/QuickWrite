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
                title = { Text("创作参数") },
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
            SettingsSection(title = "参数来源") {
                SettingsSwitchItem(
                    title = "使用模型配置",
                    subtitle = if (appSettings.useModelConfig && appSettings.modelConfigName.isNotBlank())
                        "当前: ${appSettings.modelConfigName}"
                    else if (appSettings.useModelConfig)
                        "未设置默认模型"
                    else
                        "使用下方自定义参数",
                    checked = appSettings.useModelConfig,
                    onCheckedChange = { viewModel.updateUseModelConfig(it) }
                )
            }

            SettingsSection(title = "AI生成参数") {
                val sliderEnabled = !appSettings.useModelConfig
                val displayTemperature = if (appSettings.useModelConfig) appSettings.modelConfigTemperature else appSettings.defaultTemperature
                val displayMaxTokens = if (appSettings.useModelConfig) appSettings.modelConfigMaxTokens else appSettings.defaultMaxTokens
                val displayTopP = if (appSettings.useModelConfig) appSettings.modelConfigTopP else appSettings.defaultTopP

                SettingsSliderItem(
                    title = "温度",
                    subtitle = "控制输出的随机性和创意性，值越高越有创意",
                    value = displayTemperature,
                    onValueChange = { viewModel.updateDefaultTemperature(it) },
                    valueRange = 0.1f..2.0f,
                    steps = 18,
                    valueText = String.format("%.1f", displayTemperature),
                    enabled = sliderEnabled
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "最大生成长度",
                    subtitle = "控制每次生成回复的最大字符数",
                    value = displayMaxTokens.toFloat(),
                    onValueChange = { viewModel.updateDefaultMaxTokens(it.toInt()) },
                    valueRange = 100f..8000f,
                    steps = 78,
                    valueText = displayMaxTokens.toString(),
                    enabled = sliderEnabled
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "Top P",
                    subtitle = "控制词汇采样的多样性和质量",
                    value = displayTopP,
                    onValueChange = { viewModel.updateDefaultTopP(it) },
                    valueRange = 0f..1f,
                    steps = 10,
                    valueText = String.format("%.2f", displayTopP),
                    enabled = sliderEnabled
                )
            }

            SettingsSection(title = "参数说明") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ParameterInfoCard(
                        title = "温度",
                        description = "低值（0.1-0.3）：输出更确定、更保守\n中值（0.4-0.7）：平衡多样性和准确性\n高值（0.8-2.0）：更有创意和多样性"
                    )
                    
                    ParameterInfoCard(
                        title = "最大生成长度",
                        description = "控制AI每次回复的字符数限制，值越大回复越长但消耗更多令牌"
                    )
                    
                    ParameterInfoCard(
                        title = "Top P",
                        description = "控制从概率最高的累积概率P中采样，较低值会使输出更集中"
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