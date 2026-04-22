package com.universe_st.quickwriter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.domain.usecase.SettingsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    object Idle : SettingsUiState()
    data class Success(val message: String = "") : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}

data class AppSettingsData(
    val themeMode: String = "system",
    val fontSize: Int = 14,
    val fontFamily: String = "default",
    val autoSaveInterval: Int = 5,
    val autoSaveImmediately: Boolean = false,
    val useModelConfig: Boolean = true,
    val defaultTemperature: Float = 0.8f,
    val defaultMaxTokens: Int = 2000,
    val defaultTopP: Float = 1.0f,
    val modelConfigTemperature: Float = 0.7f,
    val modelConfigMaxTokens: Int = 2000,
    val modelConfigTopP: Float = 1.0f,
    val modelConfigName: String = ""
)

data class AiConfigFormData(
    val id: Int = 0,
    val configName: String = "",
    val provider: String = "openai",
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "gpt-3.5-turbo",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2000,
    val topP: Float = 1.0f,
    val topK: Int = 50,
    val frequencyPenalty: Float = 0.0f,
    val presencePenalty: Float = 0.0f,
    val isDefault: Boolean = false,
    val configNameError: String? = null,
    val apiKeyError: String? = null
)

class SettingsViewModel(
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _appSettingsData = MutableStateFlow(AppSettingsData())
    val appSettingsData: StateFlow<AppSettingsData> = _appSettingsData.asStateFlow()

    private val _aiConfigList = MutableStateFlow<List<AiModelConfigEntity>>(emptyList())
    val aiConfigList: StateFlow<List<AiModelConfigEntity>> = _aiConfigList.asStateFlow()

    private val _currentAiConfig = MutableStateFlow<AiConfigFormData>(AiConfigFormData())
    val currentAiConfig: StateFlow<AiConfigFormData> = _currentAiConfig.asStateFlow()

    private val _isEditingAiConfig = MutableStateFlow(false)
    val isEditingAiConfig: StateFlow<Boolean> = _isEditingAiConfig.asStateFlow()

    init {
        loadSettings()
        loadAiConfigs()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settingsUseCase = this@SettingsViewModel.settingsUseCase
                val defaultConfig = settingsUseCase.getDefaultAiConfig()
                _appSettingsData.value = AppSettingsData(
                    themeMode = settingsUseCase.getThemeMode(),
                    fontSize = settingsUseCase.getFontSize(),
                    fontFamily = settingsUseCase.getFontFamily(),
                    autoSaveInterval = settingsUseCase.getAutoSaveInterval(),
                    autoSaveImmediately = settingsUseCase.getAutoSaveImmediately(),
                    useModelConfig = settingsUseCase.getUseModelConfig(),
                    defaultTemperature = settingsUseCase.getDefaultTemperature(),
                    defaultMaxTokens = settingsUseCase.getDefaultMaxTokens(),
                    defaultTopP = settingsUseCase.getDefaultTopP(),
                    modelConfigTemperature = defaultConfig?.temperature ?: 0.7f,
                    modelConfigMaxTokens = defaultConfig?.maxTokens ?: 2000,
                    modelConfigTopP = defaultConfig?.topP ?: 1.0f,
                    modelConfigName = defaultConfig?.configName ?: ""
                )
                _uiState.value = SettingsUiState.Idle
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "加载设置失败")
            }
        }
    }

    private fun loadAiConfigs() {
        viewModelScope.launch {
            settingsUseCase.getAllAiConfigs().collect { configs ->
                _aiConfigList.value = configs
                val defaultConfig = configs.firstOrNull { it.isDefault }
                if (defaultConfig != null) {
                    _appSettingsData.value = _appSettingsData.value.copy(
                        modelConfigTemperature = defaultConfig.temperature,
                        modelConfigMaxTokens = defaultConfig.maxTokens,
                        modelConfigTopP = defaultConfig.topP,
                        modelConfigName = defaultConfig.configName
                    )
                }
            }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            try {
                settingsUseCase.setThemeMode(mode)
                _appSettingsData.value = _appSettingsData.value.copy(themeMode = mode)
                _uiState.value = SettingsUiState.Success("主题已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新主题失败")
            }
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            try {
                settingsUseCase.setFontSize(size)
                _appSettingsData.value = _appSettingsData.value.copy(fontSize = size)
                _uiState.value = SettingsUiState.Success("字体大小已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新字体大小失败")
            }
        }
    }

    fun updateFontFamily(family: String) {
        viewModelScope.launch {
            try {
                settingsUseCase.setFontFamily(family)
                _appSettingsData.value = _appSettingsData.value.copy(fontFamily = family)
                _uiState.value = SettingsUiState.Success("字体已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新字体失败")
            }
        }
    }

    fun updateAutoSaveInterval(minutes: Int) {
        viewModelScope.launch {
            try {
                settingsUseCase.setAutoSaveInterval(minutes)
                // 如果设置为0，表示实时保存
                if (minutes == 0) {
                    settingsUseCase.setAutoSaveImmediately(true)
                    _appSettingsData.value = _appSettingsData.value.copy(
                        autoSaveInterval = 0,
                        autoSaveImmediately = true
                    )
                    _uiState.value = SettingsUiState.Success("已启用实时自动保存")
                } else {
                    // 如果设置了非0的时间间隔，关闭即时保存
                    if (_appSettingsData.value.autoSaveImmediately) {
                        settingsUseCase.setAutoSaveImmediately(false)
                    }
                    _appSettingsData.value = _appSettingsData.value.copy(autoSaveInterval = minutes)
                    _uiState.value = SettingsUiState.Success("自动保存间隔已更新")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新自动保存间隔失败")
            }
        }
    }

    fun updateAutoSaveImmediately(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsUseCase.setAutoSaveImmediately(enabled)
                if (enabled) {
                    // 启用即时保存时，将间隔设为0
                    settingsUseCase.setAutoSaveInterval(0)
                    _appSettingsData.value = _appSettingsData.value.copy(
                        autoSaveImmediately = true,
                        autoSaveInterval = 0
                    )
                } else {
                    // 禁用即时保存时，恢复默认间隔
                    settingsUseCase.setAutoSaveInterval(5)
                    _appSettingsData.value = _appSettingsData.value.copy(
                        autoSaveImmediately = false,
                        autoSaveInterval = 5
                    )
                }
                _uiState.value = SettingsUiState.Success("即时自动保存${if (enabled) "已启用" else "已禁用"}")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新自动保存设置失败")
            }
        }
    }

    fun updateDefaultTemperature(temperature: Float) {
        viewModelScope.launch {
            try {
                settingsUseCase.setDefaultTemperature(temperature)
                _appSettingsData.value = _appSettingsData.value.copy(defaultTemperature = temperature)
                _uiState.value = SettingsUiState.Success("默认温度已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新默认温度失败")
            }
        }
    }

    fun updateDefaultMaxTokens(tokens: Int) {
        viewModelScope.launch {
            try {
                settingsUseCase.setDefaultMaxTokens(tokens)
                _appSettingsData.value = _appSettingsData.value.copy(defaultMaxTokens = tokens)
                _uiState.value = SettingsUiState.Success("默认最大Token数已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新默认最大Token数失败")
            }
        }
    }

    fun updateDefaultTopP(topP: Float) {
        viewModelScope.launch {
            try {
                settingsUseCase.setDefaultTopP(topP)
                _appSettingsData.value = _appSettingsData.value.copy(defaultTopP = topP)
                _uiState.value = SettingsUiState.Success("默认Top P已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新默认Top P失败")
            }
        }
    }

    fun updateUseModelConfig(useModelConfig: Boolean) {
        viewModelScope.launch {
            try {
                settingsUseCase.setUseModelConfig(useModelConfig)
                _appSettingsData.value = _appSettingsData.value.copy(useModelConfig = useModelConfig)
                _uiState.value = SettingsUiState.Success("参数来源已更新")
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "更新参数来源失败")
            }
        }
    }

    fun startAddAiConfig() {
        _isEditingAiConfig.value = false
        _currentAiConfig.value = AiConfigFormData()
    }

    fun startEditAiConfig(config: AiModelConfigEntity) {
        _isEditingAiConfig.value = true
        _currentAiConfig.value = AiConfigFormData(
            id = config.id,
            configName = config.configName,
            provider = config.provider,
            apiKey = config.apiKey,
            baseUrl = config.baseUrl ?: "",
            modelName = config.modelName,
            temperature = config.temperature,
            maxTokens = config.maxTokens,
            topP = config.topP,
            topK = config.topK,
            frequencyPenalty = config.frequencyPenalty,
            presencePenalty = config.presencePenalty,
            isDefault = config.isDefault
        )
    }

    fun updateAiConfigName(name: String) {
        _currentAiConfig.value = _currentAiConfig.value.copy(
            configName = name,
            configNameError = if (name.isBlank()) "配置名称不能为空" else null
        )
    }

    fun updateAiProvider(provider: String) {
        _currentAiConfig.value = _currentAiConfig.value.copy(provider = provider)
    }

    fun updateAiApiKey(apiKey: String) {
        _currentAiConfig.value = _currentAiConfig.value.copy(
            apiKey = apiKey,
            apiKeyError = if (apiKey.isBlank()) "API密钥不能为空" else null
        )
    }

    fun updateAiBaseUrl(baseUrl: String) {
        _currentAiConfig.value = _currentAiConfig.value.copy(baseUrl = baseUrl)
    }

    fun updateAiModelName(modelName: String) {
        _currentAiConfig.value = _currentAiConfig.value.copy(modelName = modelName)
    }

    fun updateAiTemperature(temperature: Float) {
        _currentAiConfig.value = _currentAiConfig.value.copy(temperature = temperature)
    }

    fun updateAiMaxTokens(maxTokens: Int) {
        _currentAiConfig.value = _currentAiConfig.value.copy(maxTokens = maxTokens)
    }

    fun updateAiTopP(topP: Float) {
        _currentAiConfig.value = _currentAiConfig.value.copy(topP = topP)
    }

    fun updateAiTopK(topK: Int) {
        _currentAiConfig.value = _currentAiConfig.value.copy(topK = topK)
    }

    fun updateAiFrequencyPenalty(penalty: Float) {
        _currentAiConfig.value = _currentAiConfig.value.copy(frequencyPenalty = penalty)
    }

    fun updateAiPresencePenalty(penalty: Float) {
        _currentAiConfig.value = _currentAiConfig.value.copy(presencePenalty = penalty)
    }

    fun updateAiIsDefault(isDefault: Boolean) {
        _currentAiConfig.value = _currentAiConfig.value.copy(isDefault = isDefault)
    }

    fun saveAiConfig() {
        val formData = _currentAiConfig.value
        
        if (formData.configName.isBlank()) {
            _currentAiConfig.value = formData.copy(configNameError = "配置名称不能为空")
            return
        }
        
        if (formData.apiKey.isBlank()) {
            _currentAiConfig.value = formData.copy(apiKeyError = "API密钥不能为空")
            return
        }

        viewModelScope.launch {
            try {
                if (_isEditingAiConfig.value) {
                    settingsUseCase.updateAiConfig(
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
                    _uiState.value = SettingsUiState.Success("AI配置已更新")
                } else {
                    settingsUseCase.createAiConfig(
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
                    _uiState.value = SettingsUiState.Success("AI配置已创建")
                }
            } catch (e: Exception) {
                _uiState.value = SettingsUiState.Error(e.message ?: "保存AI配置失败")
            }
        }
    }

    suspend fun setDefaultAiConfig(id: Int) {
        try {
            settingsUseCase.setDefaultAiConfig(id)
            _uiState.value = SettingsUiState.Success("已设置为默认配置")
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error(e.message ?: "设置默认配置失败")
        }
    }

    suspend fun deleteAiConfig(config: AiModelConfigEntity) {
        try {
            settingsUseCase.deleteAiConfig(config)
            _uiState.value = SettingsUiState.Success("AI配置已删除")
        } catch (e: Exception) {
            _uiState.value = SettingsUiState.Error(e.message ?: "删除AI配置失败")
        }
    }

    fun clearUiMessage() {
        _uiState.value = SettingsUiState.Idle
    }
}

class SettingsViewModelFactory(
    private val settingsUseCase: SettingsUseCase
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(settingsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}