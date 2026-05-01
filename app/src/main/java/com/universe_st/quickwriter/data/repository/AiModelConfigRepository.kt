package com.universe_st.quickwriter.data.repository

import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.local.entity.AiModelConfigEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AiModelConfigRepository(
    private val aiModelConfigDao: AiModelConfigDao
) {

    companion object {
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_ANTHROPIC = "anthropic"
        const val PROVIDER_CUSTOM = "custom"
        const val PROVIDER_DEEPSEEK = "deepseek"
        const val PROVIDER_ZHIPU = "zhipu"
        const val PROVIDER_KIMI = "kimi"
        const val PROVIDER_SILICONFLOW = "siliconflow"
        
        const val MODEL_GPT_35_TURBO = "gpt-3.5-turbo"
        const val MODEL_GPT_4 = "gpt-4"
        const val MODEL_CLAUDE_3 = "claude-3-opus"
        const val MODEL_DEEPSEEK_CHAT = "deepseek-chat"
        const val MODEL_GLM4_FLASH = "glm-4-flash"
        const val MODEL_MOONSHOT_V1_8K = "moonshot-v1-8k"
        const val MODEL_DEEPSEEK_V3 = "deepseek-ai/DeepSeek-V3"
    }

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
        maxTokens: Int = 50000,
        topP: Float = 1.0f,
        topK: Int = 50,
        frequencyPenalty: Float = 0.0f,
        presencePenalty: Float = 0.0f,
        isDefault: Boolean = false
    ): Result<AiModelConfigEntity> {
        return try {
            val existingConfig = getConfigByName(configName)
            if (existingConfig != null) {
                return Result.failure(Exception("配置名称已存在"))
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

            val id = aiModelConfigDao.insertConfig(config).toInt()
            if (isDefault) {
                aiModelConfigDao.clearDefaultConfig()
                aiModelConfigDao.setDefaultConfig(id)
            }

            Result.success(config.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConfig(
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
        return try {
            val existingConfig = getConfigByName(configName)
            if (existingConfig != null && existingConfig.id != id) {
                return Result.failure(Exception("配置名称已存在"))
            }

            val config = AiModelConfigEntity(
                id = id,
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

            aiModelConfigDao.updateConfig(config)
            
            if (isDefault) {
                aiModelConfigDao.clearDefaultConfig()
                aiModelConfigDao.setDefaultConfig(id)
            }

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

    suspend fun deleteConfig(config: AiModelConfigEntity): Result<Unit> {
        return try {
            if (config.isDefault) {
                val configs = getAllConfigs()
                configs.map { list ->
                    if (list.size > 1) {
                        val newDefault = list.firstOrNull { it.id != config.id }
                        newDefault?.let {
                            setDefaultConfig(it.id)
                        }
                    }
                }
            }
            aiModelConfigDao.deleteConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasAnyConfig(): Boolean {
        return try {
            val config = getOneConfig()
            config != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getOneConfig(): AiModelConfigEntity? {
        return aiModelConfigDao.getAllConfigs().first().firstOrNull()
    }
}