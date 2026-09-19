package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    @param:Json(name = "role") val role: String,
    @param:Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    @param:Json(name = "model") val model: String,
    @param:Json(name = "messages") val messages: List<OpenRouterMessage>,
    @param:Json(name = "temperature") val temperature: Float? = 0.3f,
    @param:Json(name = "response_format") val responseFormat: Map<String, String>? = mapOf("type" to "json_object")
)

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "choices") val choices: List<OpenRouterChoice>?
)

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    @param:Json(name = "message") val message: OpenRouterMessage?,
    @param:Json(name = "finish_reason") val finishReason: String?
)

interface OpenRouterApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/aistudio/geonews-studio",
        @Header("X-Title") title: String = "GeoNews Studio",
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}
