package com.universe_st.quickwriter.data.remote

import com.universe_st.quickwriter.data.remote.dto.ChatCompletionRequest
import com.universe_st.quickwriter.data.remote.dto.ChatCompletionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

interface AiApiService {

    @POST("/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @POST("/v1/chat/completions")
    @Streaming
    suspend fun chatCompletionStream(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
}
