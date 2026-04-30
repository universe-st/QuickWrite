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
import androidx.compose.foundation.layout.WindowInsets
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.data.repository.AiModelConfigRepository
import com.universe_st.quickwriter.presentation.ui.components.SettingsDivider
import com.universe_st.quickwriter.presentation.ui.components.SettingsSection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.universe_st.quickwriter.R
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
                title = { Text(stringResource(R.string.ai_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddConfig) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ai_config_add))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
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
                                text = stringResource(R.string.ai_config_empty),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.ai_config_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onAddConfig) {
                                Text(stringResource(R.string.ai_config_add))
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
                            text = stringResource(R.string.ai_config_badge_default),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.ai_config_model_label, config.modelName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val providerName = stringResource(
                when (config.provider) {
                    AiModelConfigRepository.PROVIDER_OPENAI -> R.string.ai_provider_openai
                    AiModelConfigRepository.PROVIDER_ANTHROPIC -> R.string.ai_provider_anthropic
                    AiModelConfigRepository.PROVIDER_DEEPSEEK -> R.string.ai_provider_deepseek
                    AiModelConfigRepository.PROVIDER_ZHIPU -> R.string.ai_provider_zhipu
                    AiModelConfigRepository.PROVIDER_KIMI -> R.string.ai_provider_kimi
                    AiModelConfigRepository.PROVIDER_SILICONFLOW -> R.string.ai_provider_siliconflow
                    AiModelConfigRepository.PROVIDER_CUSTOM -> R.string.ai_provider_custom
                    else -> R.string.ai_provider_custom
                }
            )
            Text(
                text = stringResource(R.string.ai_config_provider_label, providerName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!config.isDefault) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSetDefault,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ai_config_set_default))
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
                title = { Text(stringResource(if (isEditing) R.string.ai_config_edit_title else R.string.ai_config_add_title)) },
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
                label = { Text(stringResource(R.string.ai_config_field_name)) },
                isError = formData.configNameError != null,
                supportingText = formData.configNameError?.let { error -> { Text(error.asString(LocalContext.current)) } },
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
                    value = stringResource(
                        when (formData.provider) {
                            AiModelConfigRepository.PROVIDER_OPENAI -> R.string.ai_provider_openai
                            AiModelConfigRepository.PROVIDER_ANTHROPIC -> R.string.ai_provider_anthropic
                            AiModelConfigRepository.PROVIDER_DEEPSEEK -> R.string.ai_provider_deepseek
                            AiModelConfigRepository.PROVIDER_ZHIPU -> R.string.ai_provider_zhipu
                            AiModelConfigRepository.PROVIDER_KIMI -> R.string.ai_provider_kimi
                            AiModelConfigRepository.PROVIDER_SILICONFLOW -> R.string.ai_provider_siliconflow
                            AiModelConfigRepository.PROVIDER_CUSTOM -> R.string.ai_provider_custom
                            else -> R.string.ai_provider_custom
                        }
                    ),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.ai_config_field_provider)) },
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
                        AiModelConfigRepository.PROVIDER_DEEPSEEK to stringResource(R.string.ai_provider_deepseek),
                        AiModelConfigRepository.PROVIDER_KIMI to stringResource(R.string.ai_provider_kimi),
                        AiModelConfigRepository.PROVIDER_ZHIPU to stringResource(R.string.ai_provider_zhipu),
                        AiModelConfigRepository.PROVIDER_OPENAI to stringResource(R.string.ai_provider_openai),
                        AiModelConfigRepository.PROVIDER_ANTHROPIC to stringResource(R.string.ai_provider_anthropic),
                        AiModelConfigRepository.PROVIDER_SILICONFLOW to stringResource(R.string.ai_provider_siliconflow),
                        AiModelConfigRepository.PROVIDER_CUSTOM to stringResource(R.string.ai_provider_custom)
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
                label = { Text(stringResource(R.string.ai_config_field_api_key)) },
                isError = formData.apiKeyError != null,
                supportingText = formData.apiKeyError?.let { error -> { Text(error.asString(LocalContext.current)) } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            if (formData.provider == AiModelConfigRepository.PROVIDER_CUSTOM) {
                OutlinedTextField(
                    value = formData.baseUrl,
                    onValueChange = { viewModel.updateAiBaseUrl(it) },
                    label = { Text(stringResource(R.string.ai_config_field_base_url)) },
                    placeholder = { Text(stringResource(R.string.ai_config_field_base_url_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }

            OutlinedTextField(
                value = formData.modelName,
                onValueChange = { viewModel.updateAiModelName(it) },
                label = { Text(stringResource(R.string.ai_config_field_model_name)) },
                placeholder = { Text(stringResource(R.string.ai_config_field_model_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            SettingsSection(title = stringResource(R.string.ai_config_section_params)) {
                SettingsSliderItem(
                    title = stringResource(R.string.ai_config_param_temperature),
                    subtitle = stringResource(R.string.ai_config_param_temperature_desc),
                    value = formData.temperature,
                    onValueChange = { viewModel.updateAiTemperature(it) },
                    valueRange = 0.1f..2.0f,
                    steps = 18,
                    valueText = String.format("%.1f", formData.temperature)
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.ai_config_param_max_tokens),
                    subtitle = stringResource(R.string.ai_config_param_max_tokens_desc),
                    value = formData.maxTokens.toFloat(),
                    onValueChange = { viewModel.updateAiMaxTokens(it.toInt()) },
                    valueRange = 100f..8000f,
                    steps = 78,
                    valueText = formData.maxTokens.toString()
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.ai_config_param_top_p),
                    subtitle = stringResource(R.string.ai_config_param_top_p_desc),
                    value = formData.topP,
                    onValueChange = { viewModel.updateAiTopP(it) },
                    valueRange = 0f..1f,
                    steps = 9,
                    valueText = String.format("%.2f", formData.topP)
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.ai_config_param_top_k),
                    subtitle = stringResource(R.string.ai_config_param_top_k_desc),
                    value = formData.topK.toFloat(),
                    onValueChange = { viewModel.updateAiTopK(it.toInt()) },
                    valueRange = 1f..100f,
                    steps = 0,
                    valueText = formData.topK.toString()
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.ai_config_param_frequency_penalty),
                    subtitle = stringResource(R.string.ai_config_param_frequency_penalty_desc),
                    value = formData.frequencyPenalty,
                    onValueChange = { viewModel.updateAiFrequencyPenalty(it) },
                    valueRange = -2f..2f,
                    steps = 39,
                    valueText = String.format("%.1f", formData.frequencyPenalty)
                )

                SettingsDivider()

                SettingsSliderItem(
                    title = stringResource(R.string.ai_config_param_presence_penalty),
                    subtitle = stringResource(R.string.ai_config_param_presence_penalty_desc),
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
                Text(stringResource(R.string.ai_config_default_switch), modifier = Modifier.weight(1f))
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
                    Text(stringResource(if (isEditing) R.string.common_save else R.string.common_create))
                }
                
                if (isEditing) {
                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.ai_config_delete))
                    }
                }
            }

            if (uiState is com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Error) {
                val errorMessage = (uiState as com.universe_st.quickwriter.presentation.viewmodel.SettingsUiState.Error).message
                val context = LocalContext.current
                Text(
                    text = errorMessage.asString(context),
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
            title = { Text(stringResource(R.string.ai_config_confirm_delete_title)) },
            text = {
                Text(stringResource(R.string.ai_config_confirm_delete_message))
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
                        stringResource(R.string.ai_config_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

