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

    suspend fun chatCompletion(configId: Int, request: ChatCompletionRequest): Result<ChatCompletionResponse> {
        return try {
            val config = aiModelConfigDao.getConfigById(configId)
                ?: return Result.failure(IllegalStateException("AI model config not found: $configId"))

            val service = getOrCreateService(config.baseUrl ?: "https://api.openai.com")
            val authHeader = "Bearer ${config.apiKey}"

            val response = service.chatCompletion(authHeader, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatCompletionStream(configId: Int, request: ChatCompletionRequest): Result<Response<ResponseBody>> {
        return try {
            val config = aiModelConfigDao.getConfigById(configId)
                ?: return Result.failure(IllegalStateException("AI model config not found: $configId"))

            val service = getOrCreateService(config.baseUrl ?: "https://api.openai.com")
            val authHeader = "Bearer ${config.apiKey}"

            val response = service.chatCompletionStream(authHeader, request)
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
