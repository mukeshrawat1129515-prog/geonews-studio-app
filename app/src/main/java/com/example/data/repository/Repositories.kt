package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.LongVideoScript
import com.example.data.model.MultiPlatformSeo
import com.example.data.model.NewsStory
import com.example.data.model.RawArticle
import com.example.data.model.ShortScript
import com.example.data.model.SourceItem
import com.example.data.remote.GeminiNewsEngine
import com.example.data.remote.NetworkClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

sealed class NewsRefreshState {
    object Idle : NewsRefreshState()
    data class Loading(val stageMessage: String) : NewsRefreshState()
    object Success : NewsRefreshState()
    data class Error(val message: String, val usingCached: Boolean) : NewsRefreshState()
}

sealed class ScriptGenState {
    object Idle : ScriptGenState()
    data class Generating(val message: String) : ScriptGenState()
    object Success : ScriptGenState()
    data class Error(val message: String) : ScriptGenState()
}

class NewsRepository(
    private val database: AppDatabase
) {
    val topStories: Flow<List<NewsStory>> = database.newsDao().getTopFourStories()

    suspend fun getTopStoriesDirect(): List<NewsStory> {
        return database.newsDao().getTopFourStoriesDirect()
    }

    suspend fun refreshNews(
        onStageUpdate: (String) -> Unit
    ): Result<List<NewsStory>> {
        val existingCached = database.newsDao().getTopFourStoriesDirect()
        try {
            // Stage 1: Fetching latest global news from public HTTPS RSS feeds
            onStageUpdate("Fetching latest global news from public feeds…")
            val settings = database.settingsDao().getSettingsDirect() ?: AppSettings()
            val rawArticles = mutableListOf<RawArticle>()

            // 1. Fetch from verified open primary feeds (BBC, Al Jazeera, Guardian, DW, UN, France 24)
            val rssArticles = try {
                NetworkClient.rssParser.fetchFromAllFeeds()
            } catch (e: Exception) {
                Log.w("NewsRepository", "Error fetching RSS feeds: ${e.message}")
                emptyList()
            }
            rawArticles.addAll(rssArticles)

            // 2. Fetch from custom NewsAPI if configured
            if (settings.newsApiKey.isNotBlank()) {
                try {
                    val customUrl = if (settings.newsApiEndpoint.isNotBlank()) {
                        "${settings.newsApiEndpoint}?q=geopolitics+OR+diplomacy+OR+summit&language=en&sortBy=publishedAt"
                    } else {
                        "https://newsapi.org/v2/everything?q=geopolitics+OR+diplomacy+OR+summit&language=en&sortBy=publishedAt"
                    }
                    val apiRes = NetworkClient.newsApiService.getNewsFromCustomUrl(customUrl, settings.newsApiKey)
                    apiRes.articles?.forEach { item ->
                        if (!item.title.isNullOrBlank() && !item.url.isNullOrBlank()) {
                            rawArticles.add(
                                RawArticle(
                                    title = item.title,
                                    description = item.description ?: "Verified reporting on international developments.",
                                    link = item.url,
                                    sourceName = item.source?.name ?: "News Wire",
                                    pubDate = item.publishedAt ?: "Recent"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("NewsRepository", "Custom News API fetch error: ${e.message}")
                }
            }

            if (rawArticles.isEmpty()) {
                if (existingCached.isNotEmpty()) {
                    return Result.failure(Exception("News could not be refreshed. Showing the last successfully retrieved news."))
                } else {
                    return Result.failure(Exception("Unable to reach news sources. Please check your internet connection."))
                }
            }

            // Stage 2: Analyzing and removing duplicate stories across sources
            onStageUpdate("Analyzing and removing duplicate stories…")

            // Stage 3: Selecting today's top 4 verified geopolitical developments
            onStageUpdate("Selecting today's top 4…")
            val selectedStories = NewsProcessor.deduplicateAndSelectTopFour(rawArticles)

            if (selectedStories.isEmpty()) {
                if (existingCached.isNotEmpty()) {
                    return Result.failure(Exception("News could not be refreshed. Showing the last successfully retrieved news."))
                }
                return Result.failure(Exception("No verified geopolitical news stories found."))
            }

            // Save to local Room database
            database.newsDao().clearStories()
            database.newsDao().insertStories(selectedStories)

            // Update last refresh timestamp
            val updatedSettings = settings.copy(lastRefreshTime = System.currentTimeMillis())
            database.settingsDao().saveSettings(updatedSettings)

            return Result.success(selectedStories)
        } catch (e: Exception) {
            Log.e("NewsRepository", "News refresh error", e)
            val msg = if (existingCached.isNotEmpty()) {
                "News could not be refreshed. Showing the last successfully retrieved news."
            } else {
                e.message ?: "Failed to retrieve news."
            }
            return Result.failure(Exception(msg))
        }
    }

    suspend fun clearCache() {
        database.newsDao().clearStories()
        database.scriptDao().clearShortScripts()
        database.scriptDao().clearLongScript()
        val settings = database.settingsDao().getSettingsDirect() ?: AppSettings()
        database.settingsDao().saveSettings(settings.copy(lastRefreshTime = 0L))
    }
}

class ScriptRepository(
    private val database: AppDatabase,
    private val geminiEngine: GeminiNewsEngine = GeminiNewsEngine()
) {
    val shortScripts: Flow<List<ShortScript>> = database.scriptDao().getShortScripts()
    val longScript: Flow<LongVideoScript?> = database.scriptDao().getLongScript()

    suspend fun generateShorts(
        styleVariant: String = "standard",
        storyIdToRegenerate: String? = null,
        targetDurationOverride: Int? = null
    ): Result<Pair<List<ShortScript>, String?>> {
        try {
            val stories = database.newsDao().getTopFourStoriesDirect()
            if (stories.isEmpty()) {
                return Result.failure(Exception("No stories available. Please refresh news first."))
            }
            val settings = database.settingsDao().getSettingsDirect() ?: AppSettings()
            val durationSeconds = targetDurationOverride ?: settings.shortsTargetDuration

            val targets = if (storyIdToRegenerate != null) {
                stories.filter { it.id == storyIdToRegenerate }
            } else {
                stories
            }

            val genResult = geminiEngine.generateShortScriptsWithRouting(
                topStories = targets,
                settings = settings,
                targetDurationSeconds = durationSeconds,
                styleVariant = styleVariant
            )
            val scripts = genResult.data

            if (scripts.isEmpty()) {
                return Result.failure(Exception("AI script generation failed. Please try again."))
            }

            if (storyIdToRegenerate != null) {
                scripts.forEach { database.scriptDao().insertShortScript(it) }
            } else {
                database.scriptDao().clearShortScripts()
                database.scriptDao().insertShortScripts(scripts)
            }

            return Result.success(Pair(scripts, genResult.fallbackReason))
        } catch (e: Exception) {
            Log.e("ScriptRepository", "Error generating shorts", e)
            return Result.failure(Exception("AI script generation failed. Please try again."))
        }
    }

    suspend fun generateLongVideo(): Result<Pair<LongVideoScript, String?>> {
        try {
            val stories = database.newsDao().getTopFourStoriesDirect()
            if (stories.isEmpty()) {
                return Result.failure(Exception("No stories available. Please refresh news first."))
            }
            val settings = database.settingsDao().getSettingsDirect() ?: AppSettings()

            val genResult = geminiEngine.generateLongVideoScriptWithRouting(
                topStories = stories,
                settings = settings,
                targetDuration = settings.longVideoDuration
            )
            val script = genResult.data

            database.scriptDao().insertLongScript(script)
            return Result.success(Pair(script, genResult.fallbackReason))
        } catch (e: Exception) {
            Log.e("ScriptRepository", "Error generating long video script", e)
            return Result.failure(Exception("AI script generation failed. Please try again."))
        }
    }

    suspend fun generateSeo(
        scriptTitle: String,
        scriptContent: String
    ): Result<Pair<MultiPlatformSeo, String?>> {
        try {
            val settings = database.settingsDao().getSettingsDirect() ?: AppSettings()
            val seoResult = geminiEngine.generateMultiPlatformSeo(
                scriptTitle = scriptTitle,
                scriptContent = scriptContent,
                settings = settings
            )
            return Result.success(Pair(seoResult.data, seoResult.fallbackReason))
        } catch (e: Exception) {
            Log.e("ScriptRepository", "Error generating multi-platform SEO", e)
            return Result.failure(Exception("SEO generation failed: ${e.message ?: "network error"}"))
        }
    }
}

class SettingsRepository(
    private val database: AppDatabase
) {
    val settings: Flow<AppSettings?> = database.settingsDao().getSettings()

    suspend fun getSettingsDirect(): AppSettings {
        return database.settingsDao().getSettingsDirect() ?: AppSettings()
    }

    suspend fun saveSettings(newSettings: AppSettings) {
        database.settingsDao().saveSettings(newSettings)
    }
}
