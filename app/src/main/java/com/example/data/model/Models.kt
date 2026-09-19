package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_stories")
data class NewsStory(
    @PrimaryKey val id: String,
    val rank: Int,
    val headline: String,
    val summary: String,
    val countryRegion: String,
    val sourceName: String,
    val publishedAt: String,
    val sourceUrl: String,
    val categoryTag: String, // Geopolitics, Conflict, Diplomacy, Economy, Defence, International Relations
    val factStatus: String,  // CONFIRMED, OFFICIAL CLAIM, REPORTING, ALLEGATION, UNCLEAR / NOT INDEPENDENTLY VERIFIED
    val isShortCandidate: Boolean,
    val relatedSources: String, // Comma-separated or short note
    val rawContentSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "short_scripts")
data class ShortScript(
    @PrimaryKey val id: String, // "short_1", "short_2"
    val storyId: String,
    val headlineRef: String,
    val viralTitle: String,
    val shortsTitle: String,
    val thumbnailText: String,
    val scriptText: String,
    val visualDirection: String,
    val captionsText: String,
    val sourceName: String,
    val sourceUrl: String,
    val visualPrompt: String,
    val styleVariant: String = "standard", // "standard", "more_human", "more_punchy"
    val targetDurationSeconds: Int = 10,
    val updatedAt: Long = System.currentTimeMillis()
)

data class PlatformSeoData(
    val platformName: String, // "YouTube", "Instagram", "Bilibili"
    val title: String,
    val description: String,
    val tags: List<String>,
    val hashtags: List<String>,
    val callToAction: String,
    val engagementTips: String
)

data class MultiPlatformSeo(
    val scriptTitle: String,
    val youtube: PlatformSeoData,
    val instagram: PlatformSeoData,
    val bilibili: PlatformSeoData,
    val generatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "long_scripts")
data class LongVideoScript(
    @PrimaryKey val id: String = "primary_youtube_long",
    val title: String,
    val estimatedDuration: String,
    val openingHook: String,
    val whatHappened: String,
    val backgroundContext: String,
    val timeline: String,
    val officialClaims: String,
    val confirmedFacts: String,
    val whyItMatters: String,
    val implicationsAnalysis: String,
    val whatRemainsUncertain: String,
    val conclusion: String,
    val sourcesText: String,
    val fullCombinedScript: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val lastRefreshTime: Long = 0L,
    val newsApiKey: String = "b41ad4fea4c449efb30fd1d854cf397f",
    val newsApiEndpoint: String = "https://newsapi.org/v2/everything",
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash",
    val aiProvider: String = "Gemini", // "Gemini", "OpenRouter"
    val openRouterApiKey: String = "",
    val openRouterModel: String = "meta-llama/llama-3.3-70b-instruct:free",
    val preferredLanguage: String = "Hinglish", // "Hinglish", "Hindi", "English"
    val shortsCount: Int = 2,
    val shortsTargetDuration: Int = 10, // 10, 15, 20, 30, 45, 60 seconds
    val longVideoDuration: String = "8-12 min",
    val isDarkTheme: Boolean = true,
    val autoRefreshHours: Int = 24
)

data class SourceItem(
    val headline: String,
    val sourceName: String,
    val publishedAt: String,
    val url: String,
    val sourceType: String, // Wire Service, International Broadcaster, Public Broadcaster, etc.
    val relatedSources: String,
    val factStatus: String
)

data class RawArticle(
    val title: String,
    val description: String,
    val link: String,
    val sourceName: String,
    val pubDate: String,
    val timestamp: Long = System.currentTimeMillis()
)

