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
import kotlinx.coroutines.Job
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
    val topStories = newsRepository.topStories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shortScripts = scriptRepository.shortScripts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val longScript = scriptRepository.longScript.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _refreshState = MutableStateFlow<NewsRefreshState>(NewsRefreshState.Idle)
    val refreshState: StateFlow<NewsRefreshState> = _refreshState.asStateFlow()
    private val _shortsGenState = MutableStateFlow<ScriptGenState>(ScriptGenState.Idle)
    val shortsGenState: StateFlow<ScriptGenState> = _shortsGenState.asStateFlow()
    private val _longVideoGenState = MutableStateFlow<ScriptGenState>(ScriptGenState.Idle)
    val longVideoGenState: StateFlow<ScriptGenState> = _longVideoGenState.asStateFlow()
    private val _visualPromptDialogShort = MutableStateFlow<ShortScript?>(null)
    val visualPromptDialogShort = _visualPromptDialogShort.asStateFlow()
    private val _seoGenState = MutableStateFlow<ScriptGenState>(ScriptGenState.Idle)
    val seoGenState = _seoGenState.asStateFlow()
    private val _currentSeoData = MutableStateFlow<MultiPlatformSeo?>(null)
    val currentSeoData = _currentSeoData.asStateFlow()
    private val _showSeoDialog = MutableStateFlow(false)
    val showSeoDialog = _showSeoDialog.asStateFlow()
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch { settingsRepository.settings.collect { if (it != null) _settings.value = it } }
        viewModelScope.launch {
            val current = settingsRepository.getSettingsDirect()
            _settings.value = current
            val age = System.currentTimeMillis() - current.lastRefreshTime
            if (current.lastRefreshTime == 0L || age > current.autoRefreshHours * 60L * 60L * 1000L) refreshNews()
        }
    }

    fun refreshNews() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _refreshState.value = NewsRefreshState.Loading("Fetching latest global news…")
            newsRepository.refreshNews { _refreshState.value = NewsRefreshState.Loading(it) }.fold(
                onSuccess = { _refreshState.value = NewsRefreshState.Success; _settings.value = settingsRepository.getSettingsDirect() },
                onFailure = { error -> _refreshState.value = NewsRefreshState.Error(error.message ?: "News refresh failed.", topStories.value.isNotEmpty()) }
            )
        }
    }

    fun generateShorts(styleVariant: String = "standard", storyId: String? = null, targetDuration: Int? = null) {
        viewModelScope.launch {
            val duration = targetDuration ?: _settings.value.shortsTargetDuration
            _shortsGenState.value = ScriptGenState.Generating("Synthesizing ${duration}s viral, factual Shorts script…")
            scriptRepository.generateShorts(styleVariant, storyId, duration).fold(
                { (_, fallback) -> _shortsGenState.value = ScriptGenState.Success; if (fallback != null) _userMessage.value = fallback },
                { error -> _shortsGenState.value = ScriptGenState.Error(error.message ?: "AI script generation failed. Please try again.") }
            )
        }
    }

    fun generateLongVideo() {
        viewModelScope.launch {
            _longVideoGenState.value = ScriptGenState.Generating("Synthesizing documentary script…")
            scriptRepository.generateLongVideo().fold(
                { (_, fallback) -> _longVideoGenState.value = ScriptGenState.Success; if (fallback != null) _userMessage.value = fallback },
                { error -> _longVideoGenState.value = ScriptGenState.Error(error.message ?: "AI script generation failed. Please try again.") }
            )
        }
    }

    fun updateSettings(newSettings: AppSettings) { viewModelScope.launch { settingsRepository.saveSettings(newSettings); _settings.value = newSettings; _userMessage.value = "Settings saved securely." } }
    fun clearCache() { viewModelScope.launch { newsRepository.clearCache(); _userMessage.value = "Cached news and scripts cleared." } }
    fun showVisualPrompt(short: ShortScript) { _visualPromptDialogShort.value = short }
    fun dismissVisualPrompt() { _visualPromptDialogShort.value = null }
    fun generateMultiPlatformSeo(title: String, content: String) { viewModelScope.launch { _showSeoDialog.value = true; _seoGenState.value = ScriptGenState.Generating("Generating SEO…"); scriptRepository.generateSeo(title, content).fold({ (seo, fallback) -> _currentSeoData.value = seo; _seoGenState.value = ScriptGenState.Success; if (fallback != null) _userMessage.value = fallback }, { error -> _seoGenState.value = ScriptGenState.Error(error.message ?: "Failed to generate SEO") }) } }
    fun dismissSeoDialog() { _showSeoDialog.value = false; _seoGenState.value = ScriptGenState.Idle }
    fun dismissUserMessage() { _userMessage.value = null }
    fun formatLastUpdated(timestamp: Long): String = if (timestamp == 0L) "Never (Pull to refresh)" else SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
    fun getAllSourcesList(): List<SourceItem> = topStories.value.map { story -> SourceItem(story.headline, story.sourceName, story.publishedAt, story.sourceUrl, "News Source", story.relatedSources, story.factStatus) }
}
