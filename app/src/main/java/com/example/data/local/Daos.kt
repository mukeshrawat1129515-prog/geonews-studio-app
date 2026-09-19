package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AppSettings
import com.example.data.model.LongVideoScript
import com.example.data.model.NewsStory
import com.example.data.model.ShortScript
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_stories ORDER BY rank ASC LIMIT 4")
    fun getTopFourStories(): Flow<List<NewsStory>>

    @Query("SELECT * FROM news_stories ORDER BY rank ASC LIMIT 4")
    suspend fun getTopFourStoriesDirect(): List<NewsStory>

    @Query("SELECT * FROM news_stories WHERE id = :id LIMIT 1")
    suspend fun getStoryById(id: String): NewsStory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<NewsStory>)

    @Query("DELETE FROM news_stories")
    suspend fun clearStories()
}

@Dao
interface ScriptDao {
    @Query("SELECT * FROM short_scripts ORDER BY id ASC")
    fun getShortScripts(): Flow<List<ShortScript>>

    @Query("SELECT * FROM short_scripts ORDER BY id ASC")
    suspend fun getShortScriptsDirect(): List<ShortScript>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortScripts(scripts: List<ShortScript>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortScript(script: ShortScript)

    @Query("DELETE FROM short_scripts")
    suspend fun clearShortScripts()

    @Query("SELECT * FROM long_scripts WHERE id = 'primary_youtube_long' LIMIT 1")
    fun getLongScript(): Flow<LongVideoScript?>

    @Query("SELECT * FROM long_scripts WHERE id = 'primary_youtube_long' LIMIT 1")
    suspend fun getLongScriptDirect(): LongVideoScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLongScript(script: LongVideoScript)

    @Query("DELETE FROM long_scripts")
    suspend fun clearLongScript()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)
}
