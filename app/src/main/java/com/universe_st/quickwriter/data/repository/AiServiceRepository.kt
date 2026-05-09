package com.universe_st.quickwriter.data.repository

import com.universe_st.quickwriter.data.local.dao.AiModelConfigDao
import com.universe_st.quickwriter.data.remote.AiApiClient
import com.universe_st.quickwriter.data.remote.AiApiService
import com.universe_st.quickwriter.data.remote.dto.ChatCompletionRequest
import com.universe_st.quickwriter.data.remote.dto.ChatCompletionResponse
import okhttp3.ResponseBody
import retrofit2.Response

class AiServiceRepository(
    private val aiModelConfigDao: AiModelConfigDao,
    private val apiClient: AiApiClient
) {
    private val serviceCache: MutableMap<String, AiApiService> = mutableMapOf()

    private fun getChatCompletionsPath(provider: String): String {
        return when (provider) {
            AiModelConfigRepository.PROVIDER_ZHIPU -> "api/paas/v4/chat/completions"
            else -> "v1/chat/completions"
        }
    }

    private fun getDefaultBaseUrl(provider: String): String {
        return when (provider) {
            AiModelConfigRepository.PROVIDER_DEEPSEEK -> "https://api.deepseek.com"
            AiModelConfigRepository.PROVIDER_ZHIPU -> "https://open.bigmodel.cn"
            AiModelConfigRepository.PROVIDER_KIMI -> "https://api.moonshot.cn"
            AiModelConfigRepository.PROVIDER_ANTHROPIC -> "https://api.anthropic.com"
            AiModelConfigRepository.PROVIDER_SILICONFLOW -> "https://api.siliconflow.cn"
            else -> "https://api.openai.com"
        }
    }

    suspend fun chatCompletion(configId: Int, request: ChatCompletionRequest): Result<ChatCompletionResponse> {
        return try {
            val config = aiModelConfigDao.getConfigById(configId)
                ?: return Result.failure(IllegalStateException("AI model config not found: $configId"))

            val service = getOrCreateService(config.baseUrl ?: getDefaultBaseUrl(config.provider))
            val authHeader = "Bearer ${config.apiKey}"
            val endpoint = getChatCompletionsPath(config.provider)

            val response = service.chatCompletion(endpoint, authHeader, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatCompletionStream(configId: Int, request: ChatCompletionRequest): Result<Response<ResponseBody>> {
        return try {
            val config = aiModelConfigDao.getConfigById(configId)
                ?: return Result.failure(IllegalStateException("AI model config not found: $configId"))

            val service = getOrCreateService(config.baseUrl ?: getDefaultBaseUrl(config.provider))
            val authHeader = "Bearer ${config.apiKey}"
            val endpoint = getChatCompletionsPath(config.provider)

            val response = service.chatCompletionStream(endpoint, authHeader, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOrCreateService(baseUrl: String): AiApiService {
        return serviceCache.getOrPut(baseUrl) {
            apiClient.createService(baseUrl)
        }
    }

    fun clearCache() {
        serviceCache.clear()
    }
}
