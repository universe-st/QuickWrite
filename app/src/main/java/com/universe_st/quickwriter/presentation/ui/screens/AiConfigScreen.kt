package com.universe_st.quickwriter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.data.repository.AiModelConfigRepository
import com.universe_st.quickwriter.presentation.ui.components.SettingsDivider
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection
import com.universe_st.quickwriter.presentation.ui.components.SettingsSliderItem
import com.universe_st.quickwriter.presentation.viewmodel.AiConfigFormData
import com.universe_st.quickwriter.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigListScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onAddConfig: () -> Unit,
    onEditConfig: (AiModelConfigEntity) -> Unit
) {
    val aiConfigList by viewModel.aiConfigList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var defaultConfigId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(defaultConfigId) {
        defaultConfigId?.let {
            viewModel.setDefaultAiConfig(it)
            defaultConfigId = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI模型配置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onAddConfig) {
                        Icon(Icons.Default.Add, contentDescription = "添加配置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                if (aiConfigList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "暂无AI配置",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "点击右上角添加按钮创建配置",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onAddConfig) {
                                Text("添加配置")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(aiConfigList) { config ->
                            AiConfigCard(
                                config = config,
                                onClick = { onEditConfig(config) },
                                onSetDefault = {
                                    defaultConfigId = config.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Error) {
            val errorMessage = (uiState as com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Error).message
            viewModel.clearUiMessage()
        }
    }
}

@Composable
fun AiConfigCard(
    config: AiModelConfigEntity,
    onClick: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (config.isDefault) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = config.configName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (config.isDefault) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "默认",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = "模型: ${config.modelName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "提供商: ${getProviderDisplayName(config.provider)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!config.isDefault) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSetDefault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("设为默认配置")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigEditScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val formData by viewModel.currentAiConfig.collectAsState()
    val isEditing by viewModel.isEditingAiConfig.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var providerMenuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "编辑AI配置" else "添加AI配置") },
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
            OutlinedTextField(
                value = formData.configName,
                onValueChange = { viewModel.updateAiConfigName(it) },
                label = { Text("配置名称") },
                isError = formData.configNameError != null,
                supportingText = formData.configNameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            ExposedDropdownMenuBox(
                expanded = providerMenuExpanded,
                onExpandedChange = { providerMenuExpanded = !providerMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = getProviderDisplayName(formData.provider),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("AI服务商") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = providerMenuExpanded,
                    onDismissRequest = { providerMenuExpanded = false }
                ) {
                    listOf(
                        AiModelConfigRepository.PROVIDER_OPENAI to "OpenAI",
                        AiModelConfigRepository.PROVIDER_ANTHROPIC to "Anthropic",
                        AiModelConfigRepository.PROVIDER_CUSTOM to "自定义API"
                    ).forEach { (provider, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                viewModel.updateAiProvider(provider)
                                providerMenuExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = formData.apiKey,
                onValueChange = { viewModel.updateAiApiKey(it) },
                label = { Text("API密钥") },
                isError = formData.apiKeyError != null,
                supportingText = formData.apiKeyError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (formData.provider == AiModelConfigRepository.PROVIDER_CUSTOM) {
                OutlinedTextField(
                    value = formData.baseUrl,
                    onValueChange = { viewModel.updateAiBaseUrl(it) },
                    label = { Text("基础URL") },
                    placeholder = { Text("https://api.example.com/v1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }

            OutlinedTextField(
                value = formData.modelName,
                onValueChange = { viewModel.updateAiModelName(it) },
                label = { Text("模型名称") },
                placeholder = { Text("gpt-3.5-turbo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            SettingsSection(title = "模型参数") {
                SettingsSliderItem(
                    title = "温度",
                    subtitle = "控制输出的随机性，值越高越有创意",
                    value = formData.temperature,
                    onValueChange = { viewModel.updateAiTemperature(it) },
                    valueRange = 0.1f..2.0f,
                    steps = 18,
                    valueText = String.format("%.1f", formData.temperature)
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "最大Tokens",
                    subtitle = "控制每次生成的最大长度",
                    value = formData.maxTokens.toFloat(),
                    onValueChange = { viewModel.updateAiMaxTokens(it.toInt()) },
                    valueRange = 100f..8000f,
                    steps = 78,
                    valueText = formData.maxTokens.toString()
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "Top P",
                    subtitle = "控制词汇采样的多样性",
                    value = formData.topP,
                    onValueChange = { viewModel.updateAiTopP(it) },
                    valueRange = 0f..1f,
                    steps = 9,
                    valueText = String.format("%.2f", formData.topP)
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "Top K",
                    subtitle = "限制每次采样的候选词汇数",
                    value = formData.topK.toFloat(),
                    onValueChange = { viewModel.updateAiTopK(it.toInt()) },
                    valueRange = 1f..100f,
                    steps = 0,
                    valueText = formData.topK.toString()
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "频率惩罚",
                    subtitle = "降低已出现词汇的重复概率",
                    value = formData.frequencyPenalty,
                    onValueChange = { viewModel.updateAiFrequencyPenalty(it) },
                    valueRange = -2f..2f,
                    steps = 39,
                    valueText = String.format("%.1f", formData.frequencyPenalty)
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = "存在惩罚",
                    subtitle = "鼓励生成新话题和内容",
                    value = formData.presencePenalty,
                    onValueChange = { viewModel.updateAiPresencePenalty(it) },
                    valueRange = -2f..2f,
                    steps = 39,
                    valueText = String.format("%.1f", formData.presencePenalty)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("设为默认配置", modifier = Modifier.weight(1f))
                Switch(
                    checked = formData.isDefault,
                    onCheckedChange = { viewModel.updateAiIsDefault(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.saveAiConfig() },
                    modifier = Modifier.weight(1f),
                    enabled = formData.configName.isNotBlank() && formData.apiKey.isNotBlank()
                ) {
                    Text(if (isEditing) "保存" else "创建")
                }
                
                if (isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
                    }
                }
            }

            if (uiState is com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Error) {
                val errorMessage = (uiState as com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Error).message
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Success) {
            viewModel.clearUiMessage()
            onNavigateBack()
        }
    }

    if (showDeleteConfirmDialog && isEditing) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("确认删除") },
            text = {
                Text("确定要删除这个AI配置吗？\n\n此操作不可恢复。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    val config = AiModelConfigEntity(
                        id = formData.id,
                        configName = formData.configName,
                        provider = formData.provider,
                        apiKey = formData.apiKey,
                        baseUrl = formData.baseUrl.takeIf { it.isNotBlank() },
                        modelName = formData.modelName,
                        temperature = formData.temperature,
                        maxTokens = formData.maxTokens,
                        topP = formData.topP,
                        topK = formData.topK,
                        frequencyPenalty = formData.frequencyPenalty,
                        presencePenalty = formData.presencePenalty,
                        isDefault = formData.isDefault
                    )
                    coroutineScope.launch {
                        viewModel.deleteAiConfig(config)
                    }
                }) {
                    Text(
                        "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun getProviderDisplayName(provider: String): String {
    return when (provider) {
        AiModelConfigRepository.PROVIDER_OPENAI -> "OpenAI"
        AiModelConfigRepository.PROVIDER_ANTHROPIC -> "Anthropic"
        AiModelConfigRepository.PROVIDER_CUSTOM -> "自定义API"
        else -> provider
    }
}