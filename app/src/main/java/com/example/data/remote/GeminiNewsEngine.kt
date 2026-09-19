package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AppSettings
import com.example.data.model.LongVideoScript
import com.example.data.model.MultiPlatformSeo
import com.example.data.model.NewsStory
import com.example.data.model.PlatformSeoData
import com.example.data.model.RawArticle
import com.example.data.model.ShortScript
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class ScriptGenerationResult<T>(
    val data: T,
    val fellBackToGemini: Boolean = false,
    val fallbackReason: String? = null
)

class GeminiNewsEngine(
    private val apiService: GeminiApiService = NetworkClient.geminiService,
    private val openRouterService: OpenRouterApiService = NetworkClient.openRouterService
) {
    companion object {
        const val SYSTEM_INSTRUCTION =
            "You are a neutral geopolitical news assistant. Use only supplied source information and verified retrieved information. " +
            "Never invent facts, quotes, statistics, sources or events. Clearly attribute claims. " +
            "Separate confirmed facts from allegations and analysis. Do not persuade the audience politically. " +
            "Use labels: CONFIRMED, OFFICIAL CLAIM, REPORTING, ALLEGATION, UNCLEAR / NOT INDEPENDENTLY VERIFIED."
    }

    private fun resolveGeminiKey(userKey: String?): String {
        val configured = userKey?.trim()?.ifBlank { null }
        if (!configured.isNullOrBlank()) return configured
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey
        }
        return ""
    }

    private fun resolveOpenRouterKey(userKey: String?): String {
        val configured = userKey?.trim()?.ifBlank { null }
        if (!configured.isNullOrBlank()) return configured
        val buildKey = try {
            BuildConfig.OPENROUTER_API_KEY
        } catch (e: Exception) {
            ""
        }
        if (buildKey.isNotBlank() && buildKey != "MY_OPENROUTER_API_KEY") {
            return buildKey
        }
        return ""
    }

    suspend fun analyzeAndSelectTopFour(
        rawArticles: List<RawArticle>,
        apiKey: String,
        modelName: String = "gemini-3.5-flash"
    ): List<NewsStory> = withContext(Dispatchers.IO) {
        return@withContext com.example.data.repository.NewsProcessor.deduplicateAndSelectTopFour(rawArticles)
    }

    suspend fun generateShortScriptsWithRouting(
        topStories: List<NewsStory>,
        settings: AppSettings,
        targetDurationSeconds: Int = 10,
        styleVariant: String = "standard"
    ): ScriptGenerationResult<List<ShortScript>> = withContext(Dispatchers.IO) {
        val preferredProvider = settings.aiProvider
        var fallbackHappened = false
        var fallbackMsg: String? = null

        if (preferredProvider.equals("OpenRouter", ignoreCase = true)) {
            val orKey = resolveOpenRouterKey(settings.openRouterApiKey)
            if (orKey.isNotBlank()) {
                try {
                    val orResult = callOpenRouterForShorts(
                        topStories = topStories,
                        apiKey = orKey,
                        modelName = settings.openRouterModel.ifBlank { "meta-llama/llama-3.3-70b-instruct:free" },
                        preferredLanguage = settings.preferredLanguage,
                        targetDurationSeconds = targetDurationSeconds,
                        styleVariant = styleVariant
                    )
                    if (orResult.isNotEmpty()) {
                        return@withContext ScriptGenerationResult(orResult, fellBackToGemini = false)
                    }
                } catch (e: Exception) {
                    Log.w("GeminiNewsEngine", "OpenRouter shorts failed, falling back to Gemini: ${e.message}", e)
                    fallbackHappened = true
                    fallbackMsg = "OpenRouter unavailable (${e.message ?: "network error"}). Switched to Gemini."
                }
            } else {
                fallbackHappened = true
                fallbackMsg = "OpenRouter key missing. Used Gemini instead."
            }
        }

        // Gemini execution (direct or fallback)
        val geminiResult = generateShortScripts(
            topStories = topStories,
            apiKey = settings.geminiApiKey,
            preferredLanguage = settings.preferredLanguage,
            modelName = settings.geminiModel,
            targetDurationSeconds = targetDurationSeconds,
            styleVariant = styleVariant
        )
        return@withContext ScriptGenerationResult(
            data = geminiResult,
            fellBackToGemini = fallbackHappened,
            fallbackReason = fallbackMsg
        )
    }

    suspend fun generateLongVideoScriptWithRouting(
        topStories: List<NewsStory>,
        settings: AppSettings,
        targetDuration: String = "8-12 min"
    ): ScriptGenerationResult<LongVideoScript> = withContext(Dispatchers.IO) {
        val preferredProvider = settings.aiProvider
        var fallbackHappened = false
        var fallbackMsg: String? = null

        if (preferredProvider.equals("OpenRouter", ignoreCase = true)) {
            val orKey = resolveOpenRouterKey(settings.openRouterApiKey)
            if (orKey.isNotBlank()) {
                try {
                    val orResult = callOpenRouterForLongScript(
                        topStories = topStories,
                        apiKey = orKey,
                        modelName = settings.openRouterModel.ifBlank { "meta-llama/llama-3.3-70b-instruct:free" },
                        preferredLanguage = settings.preferredLanguage,
                        targetDuration = targetDuration
                    )
                    if (orResult != null) {
                        return@withContext ScriptGenerationResult(orResult, fellBackToGemini = false)
                    }
                } catch (e: Exception) {
                    Log.w("GeminiNewsEngine", "OpenRouter long video script failed, falling back to Gemini: ${e.message}", e)
                    fallbackHappened = true
                    fallbackMsg = "OpenRouter unavailable (${e.message ?: "network error"}). Switched to Gemini."
                }
            } else {
                fallbackHappened = true
                fallbackMsg = "OpenRouter key missing. Used Gemini instead."
            }
        }

        val geminiResult = generateLongVideoScript(
            topStories = topStories,
            apiKey = settings.geminiApiKey,
            preferredLanguage = settings.preferredLanguage,
            modelName = settings.geminiModel,
            targetDuration = targetDuration
        )
        return@withContext ScriptGenerationResult(
            data = geminiResult,
            fellBackToGemini = fallbackHappened,
            fallbackReason = fallbackMsg
        )
    }

    suspend fun generateShortScripts(
        topStories: List<NewsStory>,
        apiKey: String,
        preferredLanguage: String = "Hinglish",
        modelName: String = "gemini-3.5-flash",
        targetDurationSeconds: Int = 10,
        styleVariant: String = "standard"
    ): List<ShortScript> = withContext(Dispatchers.IO) {
        val effectiveKey = resolveGeminiKey(apiKey)
        val candidates = topStories.filter { it.isShortCandidate }.take(2).let {
            if (it.size < 2) topStories.take(2) else it
        }

        if (candidates.isEmpty()) return@withContext emptyList()

        if (effectiveKey.isBlank()) {
            return@withContext fallbackGenerateShorts(candidates, preferredLanguage, targetDurationSeconds, styleVariant)
        }

        val wordCountMin = (targetDurationSeconds * 2.5).toInt()
        val wordCountMax = (targetDurationSeconds * 3.2).toInt()

        val styleGuidance = when (styleVariant) {
            "more_human" -> "Use extra natural conversational creator inflection, relatable phrasing, authentic Indian creator tone without robotic stiffness or newspaper jargon."
            "more_punchy" -> "High energy, fast-paced cadence, compelling opening punch, tight wording that hooks immediately within $targetDurationSeconds seconds."
            else -> "Natural, factual Hindi/Hinglish creator tone. Do not sound like a newspaper. Natural conversational tone like 'Duniya mein abhi ek important development hua hai...'. Do NOT overuse 'bhai'."
        }

        val results = mutableListOf<ShortScript>()

        for ((index, story) in candidates.withIndex()) {
            val shortId = "short_${index + 1}"
            val prompt = buildShortPrompt(story, preferredLanguage, styleGuidance, targetDurationSeconds, wordCountMin, wordCountMax)

            val request = GeminiGenerateRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))),
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.4f,
                    responseMimeType = "application/json"
                )
            )

            try {
                val response = apiService.generateContent(modelName, effectiveKey, request)
                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!jsonText.isNullOrBlank()) {
                    val obj = JSONObject(cleanJsonWrapper(jsonText))
                    results.add(
                        mapJsonObjectToShortScript(shortId, story, obj, styleVariant, targetDurationSeconds)
                    )
                }
            } catch (e: Exception) {
                Log.e("GeminiNewsEngine", "Short script gen failed for ${story.headline}: ${e.message}", e)
            }
        }

        if (results.isEmpty()) {
            return@withContext fallbackGenerateShorts(candidates, preferredLanguage, targetDurationSeconds, styleVariant)
        }
        return@withContext results
    }

    private suspend fun callOpenRouterForShorts(
        topStories: List<NewsStory>,
        apiKey: String,
        modelName: String,
        preferredLanguage: String,
        targetDurationSeconds: Int,
        styleVariant: String
    ): List<ShortScript> {
        val candidates = topStories.filter { it.isShortCandidate }.take(2).let {
            if (it.size < 2) topStories.take(2) else it
        }
        if (candidates.isEmpty()) return emptyList()

        val wordCountMin = (targetDurationSeconds * 2.5).toInt()
        val wordCountMax = (targetDurationSeconds * 3.2).toInt()

        val styleGuidance = when (styleVariant) {
            "more_human" -> "Use extra natural conversational creator inflection, relatable phrasing, authentic Indian creator tone without robotic stiffness or newspaper jargon."
            "more_punchy" -> "High energy, fast-paced cadence, compelling opening punch, tight wording that hooks immediately within $targetDurationSeconds seconds."
            else -> "Natural, factual Hindi/Hinglish creator tone. Do not sound like a newspaper. Natural conversational tone like 'Duniya mein abhi ek important development hua hai...'. Do NOT overuse 'bhai'."
        }

        val results = mutableListOf<ShortScript>()

        for ((index, story) in candidates.withIndex()) {
            val shortId = "short_${index + 1}"
            val prompt = buildShortPrompt(story, preferredLanguage, styleGuidance, targetDurationSeconds, wordCountMin, wordCountMax)

            val messages = listOf(
                OpenRouterMessage(role = "system", content = SYSTEM_INSTRUCTION),
                OpenRouterMessage(role = "user", content = prompt)
            )

            val request = OpenRouterChatRequest(
                model = modelName,
                messages = messages,
                temperature = 0.4f
            )

            val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
            val response = openRouterService.createChatCompletion(
                authorization = authHeader,
                request = request
            )

            val content = response.choices?.firstOrNull()?.message?.content
            if (!content.isNullOrBlank()) {
                val obj = JSONObject(cleanJsonWrapper(content))
                results.add(
                    mapJsonObjectToShortScript(shortId, story, obj, styleVariant, targetDurationSeconds)
                )
            }
        }

        return results
    }

    private fun buildShortPrompt(
        story: NewsStory,
        preferredLanguage: String,
        styleGuidance: String,
        targetDurationSeconds: Int,
        wordCountMin: Int,
        wordCountMax: Int
    ): String {
        return """
            Generate a short-form video package for this real geopolitical news story.
            Language preference: $preferredLanguage.
            Target duration: $targetDurationSeconds seconds.
            Target spoken word count: roughly $wordCountMin-$wordCountMax words (approx 2.5-3 words per second).
            Style directive: $styleGuidance

            Story Headline: ${story.headline}
            Summary: ${story.summary}
            Region: ${story.countryRegion}
            Source: ${story.sourceName} (${story.sourceUrl})
            Fact Status: ${story.factStatus}

            Constraints:
            - Never invent facts, quotes, statistics, or sources.
            - Spoken script strictly calibrated for $targetDurationSeconds seconds ($wordCountMin-$wordCountMax words).
            - End with a natural curiosity hook without clickbait or misinformation.
            - Create a separate detailed visual prompt for an AI video generator describing location, documented people if relevant, environment, camera movement, lighting, realistic news-documentary style, important visual elements. Do not fabricate identifiable people doing unsupported actions.

            Output STRICT JSON with these exact fields:
            {
              "viralTitle": "Viral but strictly factual title",
              "shortsTitle": "YouTube Shorts / Instagram title with clean hashtags",
              "thumbnailText": "3-5 word bold thumbnail hook text",
              "scriptText": "$targetDurationSeconds-second spoken creator script in $preferredLanguage (~$wordCountMin-$wordCountMax words)",
              "visualDirection": "Step-by-step $targetDurationSeconds-second shot directions",
              "captionsText": "Key on-screen text highlights",
              "visualPrompt": "Detailed cinematic AI video generator prompt"
            }
        """.trimIndent()
    }

    private fun mapJsonObjectToShortScript(
        shortId: String,
        story: NewsStory,
        obj: JSONObject,
        styleVariant: String,
        targetDurationSeconds: Int
    ): ShortScript {
        return ShortScript(
            id = shortId,
            storyId = story.id,
            headlineRef = story.headline,
            viralTitle = obj.optString("viralTitle", story.headline),
            shortsTitle = obj.optString("shortsTitle", "${story.headline} #Geopolitics #WorldNews"),
            thumbnailText = obj.optString("thumbnailText", "MAJOR GLOBAL UPDATE"),
            scriptText = obj.optString("scriptText", ""),
            visualDirection = obj.optString("visualDirection", "Fast camera push-in on map, archival B-roll, on-screen key stat graphics."),
            captionsText = obj.optString("captionsText", story.headline),
            sourceName = story.sourceName,
            sourceUrl = story.sourceUrl,
            visualPrompt = obj.optString("visualPrompt", "Cinematic documentary shot of ${story.countryRegion}, dramatic news lighting, 4k ultra-realistic texture."),
            styleVariant = styleVariant,
            targetDurationSeconds = targetDurationSeconds
        )
    }

    suspend fun generateLongVideoScript(
        topStories: List<NewsStory>,
        apiKey: String,
        preferredLanguage: String = "Hinglish",
        modelName: String = "gemini-3.5-flash",
        targetDuration: String = "8-12 min"
    ): LongVideoScript = withContext(Dispatchers.IO) {
        val effectiveKey = resolveGeminiKey(apiKey)
        val storiesSummary = buildString {
            topStories.forEachIndexed { i, s ->
                appendLine("[Story ${i + 1}] ${s.headline}")
                appendLine("Summary: ${s.summary}")
                appendLine("Region: ${s.countryRegion} | Source: ${s.sourceName} | URL: ${s.sourceUrl}")
                appendLine("Status: ${s.factStatus} | Related: ${s.relatedSources}")
                appendLine("---")
            }
        }

        if (effectiveKey.isBlank() || topStories.isEmpty()) {
            return@withContext fallbackGenerateLongScript(topStories, preferredLanguage, targetDuration)
        }

        val prompt = buildLongScriptPrompt(preferredLanguage, targetDuration, storiesSummary)

        val request = GeminiGenerateRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))),
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3f,
                responseMimeType = "application/json"
            )
        )

        try {
            val response = apiService.generateContent(modelName, effectiveKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrBlank()) {
                val obj = JSONObject(cleanJsonWrapper(jsonText))
                return@withContext mapJsonObjectToLongScript(obj, targetDuration, preferredLanguage, topStories)
            }
        } catch (e: Exception) {
            Log.e("GeminiNewsEngine", "Long script gen failed: ${e.message}", e)
        }

        return@withContext fallbackGenerateLongScript(topStories, preferredLanguage, targetDuration)
    }

    private suspend fun callOpenRouterForLongScript(
        topStories: List<NewsStory>,
        apiKey: String,
        modelName: String,
        preferredLanguage: String,
        targetDuration: String
    ): LongVideoScript? {
        val storiesSummary = buildString {
            topStories.forEachIndexed { i, s ->
                appendLine("[Story ${i + 1}] ${s.headline}")
                appendLine("Summary: ${s.summary}")
                appendLine("Region: ${s.countryRegion} | Source: ${s.sourceName} | URL: ${s.sourceUrl}")
                appendLine("Status: ${s.factStatus} | Related: ${s.relatedSources}")
                appendLine("---")
            }
        }

        val prompt = buildLongScriptPrompt(preferredLanguage, targetDuration, storiesSummary)
        val messages = listOf(
            OpenRouterMessage(role = "system", content = SYSTEM_INSTRUCTION),
            OpenRouterMessage(role = "user", content = prompt)
        )

        val request = OpenRouterChatRequest(
            model = modelName,
            messages = messages,
            temperature = 0.3f
        )

        val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
        val response = openRouterService.createChatCompletion(
            authorization = authHeader,
            request = request
        )

        val content = response.choices?.firstOrNull()?.message?.content
        if (!content.isNullOrBlank()) {
            val obj = JSONObject(cleanJsonWrapper(content))
            return mapJsonObjectToLongScript(obj, targetDuration, preferredLanguage, topStories)
        }
        return null
    }

    private fun buildLongScriptPrompt(
        preferredLanguage: String,
        targetDuration: String,
        storiesSummary: String
    ): String {
        return """
            Generate an in-depth $targetDuration YouTube documentary-style script based exclusively on these 4 verified geopolitical stories.
            Language: $preferredLanguage.
            Target duration: $targetDuration.

            Mandatory 10-Section Structure:
            1. Strong but factual opening hook
            2. What happened?
            3. Background/context
            4. Timeline
            5. What each relevant side officially says (attribute claims clearly)
            6. What is independently confirmed
            7. Why it matters
            8. Possible implications, clearly labeled as analysis
            9. What remains uncertain
            10. Conclusion

            Provide precise attribution and source references throughout.
            Never invent facts, quotes, statistics, or sources.

            Output STRICT JSON with fields:
            {
              "title": "Documentary Video Title",
              "openingHook": "Section 1 text",
              "whatHappened": "Section 2 text",
              "backgroundContext": "Section 3 text",
              "timeline": "Section 4 text",
              "officialClaims": "Section 5 text",
              "confirmedFacts": "Section 6 text",
              "whyItMatters": "Section 7 text",
              "implicationsAnalysis": "Section 8 text",
              "whatRemainsUncertain": "Section 9 text",
              "conclusion": "Section 10 text",
              "sourcesText": "Formatted list of primary sources with article names and URLs"
            }

            Stories:
            $storiesSummary
        """.trimIndent()
    }

    private fun mapJsonObjectToLongScript(
        obj: JSONObject,
        targetDuration: String,
        preferredLanguage: String,
        topStories: List<NewsStory>
    ): LongVideoScript {
        val hook = obj.optString("openingHook", "")
        val whatHappened = obj.optString("whatHappened", "")
        val background = obj.optString("backgroundContext", "")
        val timeline = obj.optString("timeline", "")
        val officialClaims = obj.optString("officialClaims", "")
        val confirmed = obj.optString("confirmedFacts", "")
        val whyMatters = obj.optString("whyItMatters", "")
        val implications = obj.optString("implicationsAnalysis", "")
        val uncertain = obj.optString("whatRemainsUncertain", "")
        val conclusion = obj.optString("conclusion", "")
        val sources = obj.optString("sourcesText", "")

        val fullCombined = buildString {
            appendLine("## ${obj.optString("title", "Geopolitical Intelligence Briefing")}")
            appendLine("Duration: $targetDuration | Language: $preferredLanguage\n")
            appendLine("### 1. OPENING HOOK\n$hook\n")
            appendLine("### 2. WHAT HAPPENED?\n$whatHappened\n")
            appendLine("### 3. BACKGROUND & CONTEXT\n$background\n")
            appendLine("### 4. TIMELINE\n$timeline\n")
            appendLine("### 5. OFFICIAL STATEMENTS & CLAIMS\n$officialClaims\n")
            appendLine("### 6. INDEPENDENTLY CONFIRMED FACTS\n$confirmed\n")
            appendLine("### 7. WHY IT MATTERS\n$whyMatters\n")
            appendLine("### 8. POSSIBLE IMPLICATIONS (ANALYSIS)\n$implications\n")
            appendLine("### 9. WHAT REMAINS UNCERTAIN\n$uncertain\n")
            appendLine("### 10. CONCLUSION\n$conclusion\n")
            appendLine("### SOURCES\n$sources")
        }

        return LongVideoScript(
            id = "primary_youtube_long",
            title = obj.optString("title", "Global Geopolitics Intelligence Briefing"),
            estimatedDuration = targetDuration,
            openingHook = hook,
            whatHappened = whatHappened,
            backgroundContext = background,
            timeline = timeline,
            officialClaims = officialClaims,
            confirmedFacts = confirmed,
            whyItMatters = whyMatters,
            implicationsAnalysis = implications,
            whatRemainsUncertain = uncertain,
            conclusion = conclusion,
            sourcesText = sources.ifBlank { formatSourcesFromStories(topStories) },
            fullCombinedScript = fullCombined
        )
    }

    private fun cleanJsonWrapper(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("```json")) {
            return trimmed.removePrefix("```json").removeSuffix("```").trim()
        }
        if (trimmed.startsWith("```")) {
            return trimmed.removePrefix("```").removeSuffix("```").trim()
        }
        return trimmed
    }

    private fun fallbackGenerateShorts(
        stories: List<NewsStory>,
        preferredLanguage: String,
        targetDurationSeconds: Int,
        styleVariant: String
    ): List<ShortScript> {
        return stories.take(2).mapIndexed { idx, story ->
            val script = when (preferredLanguage) {
                "Hindi" -> "नमस्ते दोस्तों! आज एक बड़ा वैश्विक घटनाक्रम सामने आया है। ${story.headline} पर ${story.sourceName} की रिपोर्ट के अनुसार महत्वपूर्ण बदलाव देखे गए हैं। इसकी पूरी पुष्टि और इसके भू-राजनीतिक असर पर हमारी नज़र बनी हुई है।"
                "English" -> "Important global development right now. According to verified reporting from ${story.sourceName}, ${story.headline}. Key diplomatic consequences and regional shifts are currently unfolding."
                else -> when (styleVariant) {
                    "more_punchy" -> "Dosto! Global stage par ek bada shift hua hai! ${story.headline} par ${story.sourceName} ki direct reporting aayi hai. Ye decision aage chal kar pura balance of power badal sakta hai!"
                    "more_human" -> "Duniya mein abhi ek important geopolitical development hua hai. ${story.sourceName} ke mutabiq, ${story.headline}. Ye sirf headline nahi hai, iske implications bohot direct aur door-talak hain."
                    else -> "Duniya mein abhi ek major geopolitical update aaya hai. ${story.sourceName} ne report kiya hai ki ${story.headline}. Iss event se regional stability par direct asar padega. Ispe aapka kya view hai?"
                }
            }

            ShortScript(
                id = "short_${idx + 1}",
                storyId = story.id,
                headlineRef = story.headline,
                viralTitle = "World Alert: ${story.headline}",
                shortsTitle = "${story.headline.take(60)} | Global Geopolitics #Shorts #WorldNews",
                thumbnailText = "MAJOR BREAKING UPDATE",
                scriptText = script,
                visualDirection = "0-3s: Quick zoom onto world map showing ${story.countryRegion}. 3-${targetDurationSeconds - 3}s: Archival footage with bold verified status badge. ${targetDurationSeconds - 3}-${targetDurationSeconds}s: Split-screen analysis chart with creator voiceover.",
                captionsText = "${story.headline} | Source: ${story.sourceName} [${story.factStatus}]",
                sourceName = story.sourceName,
                sourceUrl = story.sourceUrl,
                visualPrompt = "Cinematic documentary shot of ${story.countryRegion} geopolitical landmark, high dynamic range, volumetric lighting, newsroom broadcast realism, 35mm lens, 4k ultra-detailed.",
                styleVariant = styleVariant,
                targetDurationSeconds = targetDurationSeconds
            )
        }
    }

    private fun fallbackGenerateLongScript(
        stories: List<NewsStory>,
        language: String,
        duration: String
    ): LongVideoScript {
        val lead = stories.firstOrNull()
        val title = lead?.let { "Geopolitical Crisis & Global Power Shifts: ${it.headline}" }
            ?: "Global Geopolitical Strategy & Intelligence Briefing"

        val hook = "Dosto, aaj global geopolitics mein aisi halchal dekhi gayi hai jo aane wale mahino mein international relations ka rukh tay karegi. Char critical events samne aaye hain jinhe har observer ko dhyan se samajhna hoga."
        val whatHappened = stories.joinToString("\n\n") { s ->
            "Pehla bada mudda: ${s.headline}. Iski jankari ${s.sourceName} ke madhyam se samne aayi hai jahan ${s.summary}"
        }
        val background = "In sabhi events ke pichhe historical treaties, energy corridors aur border security ki lambi dastaan hai. Pichle kuch saalon se multipolar world order banne ki koshish chal rahi hai."
        val timeline = "Timeline par nazar daalein toh pichle 72 ghanto ke andar diplomatic meetings, official press statements aur ground level troop/trade movements tez huye hain."
        val officialClaims = stories.joinToString("\n") { s ->
            "- ${s.sourceName} & Official Spokespersons: ${s.headline} ko lekar relevant paksh ne apna claim pesh kiya hai."
        }
        val confirmed = stories.filter { it.factStatus == "CONFIRMED" }.joinToString("\n") { s ->
            "✓ CONFIRMED: ${s.headline} verified by multiple international agencies."
        }.ifBlank { "Independent fact-checkers confirm that bilateral discussions are officially documented." }

        val whyMatters = "Kyu ye matters karta hai? Kyunki ye sirf do desho ka mamla nahi hai, iska asar global crude oil, supply chains aur diplomatic alliances par seedhe taur par padta hai."
        val implications = "Analysis & Implications: Agar ye sthiti aage badhti hai toh sanctions aur new economic pacts dekhe jaa sakte hain. Kahi na kahi diplomacy ko priority dena zaroori hoga."
        val uncertain = "Kya abhi uncertain hai? Ground realities aur full treaties ke clauses abhi independent investigation ke daayre mein hain. Kuch allegations par abhi dono paksho ke alag davedariyan hain."
        val conclusion = "Nishkarsh ye hai ki geopolitical situation dynamic hai. Hume factual aur verified information ke sath update rehna chahiye bina kisi political bias ya exaggerated claims ke."

        val sourcesList = formatSourcesFromStories(stories)

        val fullCombined = buildString {
            appendLine("## $title")
            appendLine("Estimated Duration: $duration | Style: Documentary Analysis\n")
            appendLine("### 1. OPENING HOOK\n$hook\n")
            appendLine("### 2. WHAT HAPPENED?\n$whatHappened\n")
            appendLine("### 3. BACKGROUND & CONTEXT\n$background\n")
            appendLine("### 4. TIMELINE\n$timeline\n")
            appendLine("### 5. OFFICIAL STATEMENTS\n$officialClaims\n")
            appendLine("### 6. INDEPENDENTLY CONFIRMED FACTS\n$confirmed\n")
            appendLine("### 7. WHY IT MATTERS\n$whyMatters\n")
            appendLine("### 8. POSSIBLE IMPLICATIONS (ANALYSIS)\n$implications\n")
            appendLine("### 9. WHAT REMAINS UNCERTAIN\n$uncertain\n")
            appendLine("### 10. CONCLUSION\n$conclusion\n")
            appendLine("### SOURCES\n$sourcesList")
        }

        return LongVideoScript(
            id = "primary_youtube_long",
            title = title,
            estimatedDuration = duration,
            openingHook = hook,
            whatHappened = whatHappened,
            backgroundContext = background,
            timeline = timeline,
            officialClaims = officialClaims,
            confirmedFacts = confirmed,
            whyItMatters = whyMatters,
            implicationsAnalysis = implications,
            whatRemainsUncertain = uncertain,
            conclusion = conclusion,
            sourcesText = sourcesList,
            fullCombinedScript = fullCombined
        )
    }

    private fun formatSourcesFromStories(stories: List<NewsStory>): String {
        return buildString {
            stories.forEach { s ->
                appendLine("- ${s.sourceName}: \"${s.headline}\"")
                appendLine("  Published: ${s.publishedAt} | URL: ${s.sourceUrl}")
            }
        }
    }

    suspend fun generateMultiPlatformSeo(
        scriptTitle: String,
        scriptContent: String,
        settings: AppSettings
    ): ScriptGenerationResult<MultiPlatformSeo> = withContext(Dispatchers.IO) {
        val orKey = resolveOpenRouterKey(settings.openRouterApiKey)
        val model = settings.openRouterModel.ifBlank { "meta-llama/llama-3.3-70b-instruct:free" }
        var fallbackHappened = false
        var fallbackReason: String? = null

        // 1. First priority: OpenRouter API as explicitly requested
        if (orKey.isNotBlank()) {
            try {
                val orResult = callOpenRouterForSeo(
                    scriptTitle = scriptTitle,
                    scriptContent = scriptContent,
                    apiKey = orKey,
                    modelName = model,
                    language = settings.preferredLanguage
                )
                if (orResult != null) {
                    return@withContext ScriptGenerationResult(orResult, fellBackToGemini = false)
                }
            } catch (e: Exception) {
                Log.w("GeminiNewsEngine", "OpenRouter SEO generation failed: ${e.message}", e)
                fallbackHappened = true
                fallbackReason = "OpenRouter error (${e.message ?: "network"}). Used fallback SEO engine."
            }
        } else {
            fallbackHappened = true
            fallbackReason = "OpenRouter API key not configured in Settings. Used fallback SEO."
        }

        // 2. Fallback using Gemini or local template
        val geminiKey = resolveGeminiKey(settings.geminiApiKey)
        if (geminiKey.isNotBlank()) {
            try {
                val geminiSeo = callGeminiForSeo(
                    scriptTitle = scriptTitle,
                    scriptContent = scriptContent,
                    apiKey = geminiKey,
                    modelName = settings.geminiModel,
                    language = settings.preferredLanguage
                )
                if (geminiSeo != null) {
                    return@withContext ScriptGenerationResult(geminiSeo, fellBackToGemini = true, fallbackReason = fallbackReason)
                }
            } catch (e: Exception) {
                Log.w("GeminiNewsEngine", "Gemini SEO fallback failed: ${e.message}", e)
            }
        }

        // 3. Robust local fallback if no keys configured
        val localSeo = buildFallbackSeo(scriptTitle, scriptContent)
        return@withContext ScriptGenerationResult(localSeo, fellBackToGemini = fallbackHappened, fallbackReason = fallbackReason)
    }

    private suspend fun callOpenRouterForSeo(
        scriptTitle: String,
        scriptContent: String,
        apiKey: String,
        modelName: String,
        language: String
    ): MultiPlatformSeo? {
        val prompt = buildSeoPrompt(scriptTitle, scriptContent, language)
        val messages = listOf(
            OpenRouterMessage(role = "system", content = "You are a world-class social media viral growth strategist and SEO specialist for international geopolitical journalism. Generate tailored metadata for YouTube, Instagram Reels, and Bilibili in valid JSON format."),
            OpenRouterMessage(role = "user", content = prompt)
        )

        val request = OpenRouterChatRequest(
            model = modelName,
            messages = messages,
            temperature = 0.4f
        )

        val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"
        val response = openRouterService.createChatCompletion(
            authorization = authHeader,
            request = request
        )

        val content = response.choices?.firstOrNull()?.message?.content
        if (!content.isNullOrBlank()) {
            val jsonClean = cleanJsonWrapper(content)
            val jsonObject = JSONObject(jsonClean)
            return parseSeoJsonObject(scriptTitle, jsonObject)
        }
        return null
    }

    private suspend fun callGeminiForSeo(
        scriptTitle: String,
        scriptContent: String,
        apiKey: String,
        modelName: String,
        language: String
    ): MultiPlatformSeo? {
        val prompt = buildSeoPrompt(scriptTitle, scriptContent, language)
        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.4f,
                responseMimeType = "application/json"
            )
        )

        val response = apiService.generateContent(
            model = modelName,
            apiKey = apiKey,
            request = request
        )

        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        if (!text.isNullOrBlank()) {
            val jsonClean = cleanJsonWrapper(text)
            val jsonObject = JSONObject(jsonClean)
            return parseSeoJsonObject(scriptTitle, jsonObject)
        }
        return null
    }

    private fun buildSeoPrompt(title: String, content: String, language: String): String {
        val snippet = if (content.length > 1500) content.take(1500) + "..." else content
        return """
            Perform High-CTR, Algorithm-Optimized Social Media SEO for this Geopolitical News Script across 3 platforms:
            1. YouTube (Long-form / Shorts)
            2. Instagram (Reels & Feed)
            3. Bilibili (China/Global Video Platform, including Chinese pinyin/characters where appropriate for global Bilibili tags)

            Script Title: $title
            Script Content:
            $snippet
            Preferred Language / Audience: $language

            Requirements for each platform:
            - YouTube: High CTR, search-friendly title (<70 chars), keyword-rich description with timestamps/summary, 10-15 high-volume search tags, 3-5 hashtags, strong subscriber CTA, and click-through tips.
            - Instagram: Viral attention-grabbing reel caption with strong 1st line hook, readable formatting, 15-20 trending and niche hashtags, save/share CTA, and reel audio/retention tips.
            - Bilibili: Engaging anime/bullet-chat (Danmaku) friendly title, comprehensive video dynamic description (动态), 8-12 Bilibili partition keywords (分区标签: 国际/时事/硬核科普), interactive comment prompt to drive Danmaku (弹幕) engagement.

            Output strictly JSON matching this structure:
            {
              "youtube": {
                "title": "...",
                "description": "...",
                "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
                "hashtags": ["#tag1", "#tag2", "#tag3"],
                "callToAction": "...",
                "engagementTips": "..."
              },
              "instagram": {
                "title": "...",
                "description": "...",
                "tags": ["tag1", "tag2", "tag3"],
                "hashtags": ["#tag1", "#tag2", "#tag3", "#tag4"],
                "callToAction": "...",
                "engagementTips": "..."
              },
              "bilibili": {
                "title": "...",
                "description": "...",
                "tags": ["国际观察", "地缘政治", "深度解析", "tag4", "tag5"],
                "hashtags": ["#bilibili", "#国际资讯"],
                "callToAction": "...",
                "engagementTips": "..."
              }
            }
        """.trimIndent()
    }

    private fun parseSeoJsonObject(scriptTitle: String, obj: JSONObject): MultiPlatformSeo {
        fun parsePlatform(key: String, defaultName: String): PlatformSeoData {
            val pObj = obj.optJSONObject(key) ?: JSONObject()
            val tagsList = mutableListOf<String>()
            val tagsArr = pObj.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    val t = tagsArr.optString(i)
                    if (t.isNotBlank()) tagsList.add(t)
                }
            }
            val hashList = mutableListOf<String>()
            val hashArr = pObj.optJSONArray("hashtags")
            if (hashArr != null) {
                for (i in 0 until hashArr.length()) {
                    val h = hashArr.optString(i)
                    if (h.isNotBlank()) hashList.add(h)
                }
            }
            return PlatformSeoData(
                platformName = defaultName,
                title = pObj.optString("title", scriptTitle),
                description = pObj.optString("description", "High-retention geopolitical analysis covering latest breaking international events."),
                tags = if (tagsList.isNotEmpty()) tagsList else listOf("Geopolitics", "BreakingNews", "WorldNews", "Analysis"),
                hashtags = if (hashList.isNotEmpty()) hashList else listOf("#Geopolitics", "#WorldNews", "#NewsAnalysis"),
                callToAction = pObj.optString("callToAction", "Like, subscribe, and share your view in the comments!"),
                engagementTips = pObj.optString("engagementTips", "Pin the most controversial question from the video in the top comment.")
            )
        }

        return MultiPlatformSeo(
            scriptTitle = scriptTitle,
            youtube = parsePlatform("youtube", "YouTube"),
            instagram = parsePlatform("instagram", "Instagram"),
            bilibili = parsePlatform("bilibili", "Bilibili")
        )
    }

    private fun buildFallbackSeo(scriptTitle: String, scriptContent: String): MultiPlatformSeo {
        val cleanTitle = scriptTitle.ifBlank { "Global Geopolitical Update" }
        return MultiPlatformSeo(
            scriptTitle = cleanTitle,
            youtube = PlatformSeoData(
                platformName = "YouTube",
                title = "$cleanTitle | In-Depth Geopolitical Analysis & Facts",
                description = "Breaking analysis of $cleanTitle. We examine verified claims, timeline events, and international implications from primary source reporting.\n\n🔔 Subscribe to GeoNews Studio for factual, neutral international reporting.",
                tags = listOf("Geopolitics", "World News", "International Relations", "Breaking News", "Diplomacy", "Global Summit", "Foreign Policy"),
                hashtags = listOf("#Geopolitics", "#WorldNews", "#BreakingNews", "#GlobalAffairs"),
                callToAction = "Subscribe for daily verified geopolitical intelligence and hit the bell icon!",
                engagementTips = "Pin a top comment asking: 'What do you think is the biggest strategic consequence of this move?'"
            ),
            instagram = PlatformSeoData(
                platformName = "Instagram",
                title = "🚨 $cleanTitle — What Nobody Is Telling You",
                description = "Major global development happening right now: $cleanTitle.\n\nSwipe to check verified source details and what each side officially claims. Save this reel to stay updated.\n\n👇 Drop your thoughts below!",
                tags = listOf("reels", "geopolitics", "worldnews", "currentaffairs", "indiancreator"),
                hashtags = listOf("#Geopolitics", "#WorldNews", "#CurrentAffairs", "#ReelsIndia", "#GlobalNews", "#InternationalRelations", "#GeoNews"),
                callToAction = "Save this reel and share it with someone who follows global affairs!",
                engagementTips = "Use high-contrast yellow/white subtitle font for the opening hook in the first 2 seconds."
            ),
            bilibili = PlatformSeoData(
                platformName = "Bilibili",
                title = "【硬核解读】$cleanTitle | 局势深度研判与权威考证",
                description = "针对近期备受关注的国际热点事件进行全方位多角度事实还原。\n本期梳理各方官方表态、时间线发展及地缘战略博弈。\n感谢各位观众老爷的一键三连支持！",
                tags = listOf("国际时事", "地缘政治", "时事分析", "硬核科普", "全球视野", "深度报道"),
                hashtags = listOf("#国际观察", "#时事热点", "#哔哩哔哩知识季"),
                callToAction = "觉得内容有深度请务必一键三连（点赞、投币、收藏），弹幕区发表你的见解！",
                engagementTips = "在视频15秒处设置弹幕投票：'你认为后续局势会升级还是降温？'引导观众发送弹幕参与。"
            )
        )
    }
}
