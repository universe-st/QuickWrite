package com.universe_st.quickwriter.domain.usecase

import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import com.universe_st.quickwriter.data.repository.AiModelConfigRepository
import com.universe_st.quickwriter.data.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsUseCase(
    private val userSettingsRepository: UserSettingsRepository,
    private val aiModelConfigRepository: AiModelConfigRepository
) {

    suspend fun getThemeMode(): String {
        return userSettingsRepository.getThemeMode()
    }

    suspend fun setThemeMode(mode: String): Result<Unit> {
        return userSettingsRepository.setThemeMode(mode)
    }

    suspend fun getFontSize(): Int {
        return userSettingsRepository.getFontSize()
    }

    suspend fun setFontSize(size: Int): Result<Unit> {
        return userSettingsRepository.setFontSize(size)
    }

    suspend fun getFontFamily(): String {
        return userSettingsRepository.getFontFamily()
    }

    suspend fun setFontFamily(family: String): Result<Unit> {
        return userSettingsRepository.setFontFamily(family)
    }

    suspend fun getAutoSaveInterval(): Int {
        return userSettingsRepository.getAutoSaveInterval()
    }

    suspend fun setAutoSaveInterval(minutes: Int): Result<Unit> {
        return userSettingsRepository.setAutoSaveInterval(minutes)
    }

    suspend fun getAutoSaveImmediately(): Boolean {
        return userSettingsRepository.getAutoSaveImmediately()
    }

    suspend fun setAutoSaveImmediately(enabled: Boolean): Result<Unit> {
        return userSettingsRepository.setAutoSaveImmediately(enabled)
    }

    suspend fun getDefaultTemperature(): Float {
        return userSettingsRepository.getDefaultTemperature()
    }

    suspend fun getUseModelConfig(): Boolean {
        return userSettingsRepository.getUseModelConfig()
    }

    suspend fun setUseModelConfig(useModelConfig: Boolean): Result<Unit> {
        return userSettingsRepository.setUseModelConfig(useModelConfig)
    }

    suspend fun setDefaultTemperature(temperature: Float): Result<Unit> {
        return userSettingsRepository.setDefaultTemperature(temperature)
    }

    suspend fun getDefaultMaxTokens(): Int {
        return userSettingsRepository.getDefaultMaxTokens()
    }

    suspend fun setDefaultMaxTokens(tokens: Int): Result<Unit> {
        return userSettingsRepository.setDefaultMaxTokens(tokens)
    }

    suspend fun getDefaultTopP(): Float {
        return userSettingsRepository.getDefaultTopP()
    }

    suspend fun setDefaultTopP(topP: Float): Result<Unit> {
        return userSettingsRepository.setDefaultTopP(topP)
    }

    suspend fun getMaxToolCallRounds(): Int {
        return userSettingsRepository.getMaxToolCallRounds()
    }

    suspend fun setMaxToolCallRounds(rounds: Int): Result<Unit> {
        return userSettingsRepository.setMaxToolCallRounds(rounds)
    }

    fun getAllAiConfigs(): Flow<List<AiModelConfigEntity>> {
        return aiModelConfigRepository.getAllConfigs()
    }

    suspend fun getDefaultAiConfig(): AiModelConfigEntity? {
        return aiModelConfigRepository.getDefaultConfig()
    }

    suspend fun getAiConfigById(id: Int): AiModelConfigEntity? {
        return aiModelConfigRepository.getConfigById(id)
    }

    suspend fun createAiConfig(
        configName: String,
        provider: String,
        apiKey: String,
        baseUrl: String?,
        modelName: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 2000,
        topP: Float = 1.0f,
        topK: Int = 50,
        frequencyPenalty: Float = 0.0f,
        presencePenalty: Float = 0.0f,
        isDefault: Boolean = false
    ): Result<AiModelConfigEntity> {
        return aiModelConfigRepository.createConfig(
            configName, provider, apiKey, baseUrl,
            modelName, temperature, maxTokens, topP, topK,
            frequencyPenalty, presencePenalty, isDefault
        )
    }

    suspend fun updateAiConfig(
        id: Int,
        configName: String,
        provider: String,
        apiKey: String,
        baseUrl: String?,
        modelName: String,
        temperature: Float,
        maxTokens: Int,
        topP: Float,
        topK: Int,
        frequencyPenalty: Float,
        presencePenalty: Float,
        isDefault: Boolean
    ): Result<Unit> {
        return aiModelConfigRepository.updateConfig(
            id, configName, provider, apiKey, baseUrl,
            modelName, temperature, maxTokens, topP, topK,
            frequencyPenalty, presencePenalty, isDefault
        )
    }

    suspend fun setDefaultAiConfig(id: Int): Result<Unit> {
        return aiModelConfigRepository.setDefaultConfig(id)
    }

    suspend fun deleteAiConfig(config: AiModelConfigEntity): Result<Unit> {
        return aiModelConfigRepository.deleteConfig(config)
    }

    suspend fun hasAnyAiConfig(): Boolean {
        return aiModelConfigRepository.hasAnyConfig()
    }

    suspend fun getCurrentProjectId(): String? {
        return userSettingsRepository.getCurrentProjectId()
    }

    fun getCurrentProjectIdAsFlow(): Flow<String?> {
        return userSettingsRepository.getCurrentProjectIdAsFlow()
    }

    suspend fun setCurrentProjectId(projectId: String?): Result<Unit> {
        return userSettingsRepository.setCurrentProjectId(projectId)
    }

    suspend fun getLanguage(): String {
        return userSettingsRepository.getLanguage()
    }

    suspend fun setLanguage(code: String): Result<Unit> {
        return userSettingsRepository.setLanguage(code)
    }
}