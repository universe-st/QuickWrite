package com.universe_st.quickwriter.data.repository

import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import kotlinx.coroutines.flow.Flow

class AiModelConfigRepository(
    private val aiModelConfigDao: AiModelConfigDao
) {

    fun getAllConfigs(): Flow<List<AiModelConfigEntity>> {
        return aiModelConfigDao.getAllConfigs()
    }

    suspend fun getDefaultConfig(): AiModelConfigEntity? {
        return aiModelConfigDao.getDefaultConfig()
    }

    suspend fun getConfigById(id: Int): AiModelConfigEntity? {
        return aiModelConfigDao.getConfigById(id)
    }

    suspend fun getConfigByName(configName: String): AiModelConfigEntity? {
        return aiModelConfigDao.getConfigByName(configName)
    }

    suspend fun createConfig(
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
    ): Result<Long> {
        return try {
            val existingConfig = getConfigByName(configName)
            if (existingConfig != null) {
                return Result.failure(IllegalArgumentException("配置名称已存在"))
            }

            val config = AiModelConfigEntity(
                configName = configName,
                provider = provider,
                apiKey = apiKey,
                baseUrl = baseUrl,
                modelName = modelName,
                temperature = temperature,
                maxTokens = maxTokens,
                topP = topP,
                topK = topK,
                frequencyPenalty = frequencyPenalty,
                presencePenalty = presencePenalty,
                isDefault = isDefault
            )

            if (isDefault) {
                aiModelConfigDao.clearDefaultConfig()
            }

            val id = aiModelConfigDao.insertConfig(config)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConfig(
        config: AiModelConfigEntity
    ): Result<Unit> {
        return try {
            if (config.isDefault) {
                aiModelConfigDao.clearDefaultConfig()
            }
            aiModelConfigDao.updateConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteConfig(config: AiModelConfigEntity): Result<Unit> {
        return try {
            aiModelConfigDao.deleteConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDefaultConfig(id: Int): Result<Unit> {
        return try {
            aiModelConfigDao.clearDefaultConfig()
            aiModelConfigDao.setDefaultConfig(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}