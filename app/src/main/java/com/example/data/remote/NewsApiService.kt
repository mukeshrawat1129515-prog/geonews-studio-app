package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class NewsApiResponse(
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "totalResults") val totalResults: Int?,
    @param:Json(name = "articles") val articles: List<NewsApiArticle>?
)

@JsonClass(generateAdapter = true)
data class NewsApiArticle(
    @param:Json(name = "source") val source: NewsApiSource?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "url") val url: String?,
    @param:Json(name = "publishedAt") val publishedAt: String?
)

@JsonClass(generateAdapter = true)
data class NewsApiSource(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "name") val name: String?
)

interface NewsApiService {
    @GET
    suspend fun getNewsFromCustomUrl(
        @Url url: String,
        @Query("apiKey") apiKey: String? = null
    ): NewsApiResponse
}
