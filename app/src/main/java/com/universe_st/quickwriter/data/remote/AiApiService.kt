package com.universe_st.quickwriter.data.remote

import com.universe_st.quickwriter.data.remote.dto.ChatCompletionRequest
import com.universe_st.quickwriter.data.remote.dto.ChatCompletionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface AiApiService {

    @POST
    suspend fun chatCompletion(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @POST
    @Streaming
    suspend fun chatCompletionStream(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
}
