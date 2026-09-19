package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AppSettings
import com.example.data.model.LongVideoScript
import com.example.data.model.MultiPlatformSeo
import com.example.data.model.NewsStory
import com.example.data.model.ShortScript
import com.example.data.model.SourceItem
import com.example.data.repository.NewsRefreshState
import com.example.data.repository.NewsRepository
import com.example.data.repository.ScriptGenState
import com.example.data.repository.ScriptRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GeoNewsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val newsRepository = NewsRepository(database)
    private val scriptRepository = ScriptRepository(database)
    private val settingsRepository = SettingsRepository(database)

    val topStories: StateFlow<List<NewsStory>> = newsRepository.topStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shortScripts: StateFlow<List<ShortScript>> = scriptRepository.shortScripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val longScript: StateFlow<LongVideoScript?> = scriptRepository.longScript
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _refreshState = MutableStateFlow<NewsRefreshState>(NewsRefreshState.Idle)
    val refreshState: StateFlow<NewsRefreshState> = _refreshState.asStateFlow()

    private val _shortsGenState = MutableStateFlow<ScriptGenState>(ScriptGenState.Idle)
    val shortsGenState: StateFlow<ScriptGenState> = _shortsGenState.asStateFlow()

    private val _longVideoGenState = MutableStateFlow<ScriptGenState>(ScriptGenState.Idle)
    val longVideoGenState: StateFlow<ScriptGenState> = _longVideoGenState.asStateFlow()

    private val _visualPromptDialogShort = MutableStateFlow<ShortScript?>(null)
    val visualPromptDialogShort: StateFlow<ShortScript?> = _visualPromptDialogShort.asStateFlow()

    private val _seoGenState = MutableStateFlow<ScriptGenState>(ScriptGenState.Idle)
    val seoGenState: StateFlow<ScriptGenState> = _seoGenState.asStateFlow()

    private val _currentSeoData = MutableStateFlow<MultiPlatformSeo?>(null)
    val currentSeoData: StateFlow<MultiPlatformSeo?> = _currentSeoData.asStateFlow()

    private val _showSeoDialog = MutableStateFlow(false)
    val showSeoDialog: StateFlow<Boolean> = _showSeoDialog.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { loaded ->
                if (loaded != null) {
                    _settings.value = loaded
                }
            }
        }
        checkAndTriggerDailyRefresh()
    }

    private fun checkAndTriggerDailyRefresh() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsDirect()
            // If newsApiKey is empty in existing DB, ensure default provided key is attached
            val effectiveSettings = if (currentSettings.newsApiKey.isBlank()) {
                val updated = currentSettings.copy(newsApiKey = "b41ad4fea4c449efb30fd1d854cf397f")
                settingsRepository.saveSettings(updated)
                updated
            } else {
                currentSettings
            }
            _settings.value = effectiveSettings
            val lastRefresh = effectiveSettings.lastRefreshTime
            val now = System.currentTimeMillis()
            val twentyFourHoursMs = 24L * 60L * 60L * 1000L

            // If never updated or older than 24 hours
            if (lastRefresh == 0L || (now - lastRefresh) > twentyFourHoursMs) {
                refreshNews()
            }
        }
    }

    fun refreshNews() {
        if (_refreshState.value is NewsRefreshState.Loading) return
        viewModelScope.launch {
            _refreshState.value = NewsRefreshState.Loading("Fetching latest global news…")
            val result = newsRepository.refreshNews { stageMsg ->
                _refreshState.value = NewsRefreshState.Loading(stageMsg)
            }
            result.fold(
                onSuccess = { stories ->
                    _refreshState.value = NewsRefreshState.Success
                    val currentSettings = settingsRepository.getSettingsDirect()
                    _settings.value = currentSettings
                    // Auto generate shorts if none exist
                    if (shortScripts.value.isEmpty()) {
                        generateShorts()
                    }
                },
                onFailure = { error ->
                    val hasCached = topStories.value.isNotEmpty()
                    _refreshState.value = NewsRefreshState.Error(
                        message = error.message ?: "News could not be refreshed. Showing the last successfully retrieved news.",
                        usingCached = hasCached
                    )
                }
            )
        }
    }

    fun generateShorts(
        styleVariant: String = "standard",
        storyId: String? = null,
        targetDuration: Int? = null
    ) {
        viewModelScope.launch {
            val dur = targetDuration ?: _settings.value.shortsTargetDuration
            _shortsGenState.value = ScriptGenState.Generating("Synthesizing ${dur}s viral, factual Shorts script…")
            val result = scriptRepository.generateShorts(
                styleVariant = styleVariant,
                storyIdToRegenerate = storyId,
                targetDurationOverride = dur
            )
            result.fold(
                onSuccess = { (scripts, fallbackReason) ->
                    _shortsGenState.value = ScriptGenState.Success
                    if (fallbackReason != null) {
                        _userMessage.value = fallbackReason
                    }
                },
                onFailure = { err ->
                    _shortsGenState.value = ScriptGenState.Error(err.message ?: "AI script generation failed. Please try again.")
                }
            )
        }
    }

    fun generateLongVideo() {
        viewModelScope.launch {
            _longVideoGenState.value = ScriptGenState.Generating("Synthesizing ${_settings.value.longVideoDuration} documentary script…")
            val result = scriptRepository.generateLongVideo()
            result.fold(
                onSuccess = { (script, fallbackReason) ->
                    _longVideoGenState.value = ScriptGenState.Success
                    if (fallbackReason != null) {
                        _userMessage.value = fallbackReason
                    }
                },
                onFailure = { err ->
                    _longVideoGenState.value = ScriptGenState.Error(err.message ?: "AI script generation failed. Please try again.")
                }
            )
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.saveSettings(newSettings)
            _settings.value = newSettings
            _userMessage.value = "Settings saved securely."
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            newsRepository.clearCache()
            _userMessage.value = "Cached news and scripts cleared."
        }
    }

    fun showVisualPrompt(short: ShortScript) {
        _visualPromptDialogShort.value = short
    }

    fun dismissVisualPrompt() {
        _visualPromptDialogShort.value = null
    }

    fun generateMultiPlatformSeo(title: String, content: String) {
        viewModelScope.launch {
            _showSeoDialog.value = true
            _seoGenState.value = ScriptGenState.Generating("Generating YouTube, Instagram & Bilibili SEO via OpenRouter API…")
            val result = scriptRepository.generateSeo(title, content)
            result.fold(
                onSuccess = { (seo, fallbackReason) ->
                    _currentSeoData.value = seo
                    _seoGenState.value = ScriptGenState.Success
                    if (fallbackReason != null) {
                        _userMessage.value = fallbackReason
                    }
                },
                onFailure = { err ->
                    _seoGenState.value = ScriptGenState.Error(err.message ?: "Failed to generate SEO")
                }
            )
        }
    }

    fun dismissSeoDialog() {
        _showSeoDialog.value = false
        _seoGenState.value = ScriptGenState.Idle
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    fun formatLastUpdated(timestamp: Long): String {
        if (timestamp == 0L) return "Never (Pull to refresh)"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getAllSourcesList(): List<SourceItem> {
        val stories = topStories.value
        return stories.map { story ->
            val sType = when {
                story.sourceName.contains("Reuters", ignoreCase = true) || story.sourceName.contains("AP", ignoreCase = true) -> "Wire Service"
                story.sourceName.contains("BBC", ignoreCase = true) || story.sourceName.contains("DW", ignoreCase = true) -> "Public Broadcaster"
                story.sourceName.contains("UN", ignoreCase = true) -> "International Organization"
                story.sourceName.contains("Al Jazeera", ignoreCase = true) -> "International Network"
                else -> "Diplomatic / Regional News Source"
            }
            SourceItem(
                headline = story.headline,
                sourceName = story.sourceName,
                publishedAt = story.publishedAt,
                url = story.sourceUrl,
                sourceType = sType,
                relatedSources = story.relatedSources,
                factStatus = story.factStatus
            )
        }
    }
}
