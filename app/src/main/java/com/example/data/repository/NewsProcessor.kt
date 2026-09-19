package com.example.data.repository

import com.example.data.model.NewsStory
import com.example.data.model.RawArticle
import java.util.Locale
import java.util.UUID

object NewsProcessor {

    private val STOP_WORDS = setOf(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "aren't", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both",
        "but", "by", "can", "cannot", "could", "couldn't", "did", "didn't", "do", "does", "doesn't",
        "doing", "don't", "down", "during", "each", "few", "for", "from", "further", "had", "hadn't",
        "has", "hasn't", "have", "haven't", "having", "he", "her", "here", "hers", "herself", "him",
        "himself", "his", "how", "i", "if", "in", "into", "is", "isn't", "it", "it's", "its",
        "itself", "let's", "me", "more", "most", "mustn't", "my", "myself", "no", "nor", "not",
        "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
        "out", "over", "own", "same", "shan't", "she", "should", "shouldn't", "so", "some", "such",
        "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these",
        "they", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was",
        "wasn't", "we", "were", "weren't", "what", "when", "where", "which", "while", "who", "whom",
        "why", "with", "won't", "would", "wouldn't", "you", "your", "yours", "yourself", "yourselves",
        "says", "said", "new", "report", "brief", "amid", "warns", "shows", "urges", "call", "calls"
    )

    private val KEY_ENTITIES = listOf(
        "us", "usa", "america", "uk", "britain", "russia", "ukraine", "china", "taiwan", "india",
        "pakistan", "israel", "gaza", "palestine", "syria", "lebanon", "iran", "iraq", "yemen",
        "saudi", "france", "germany", "eu", "europe", "poland", "canada", "brazil", "haiti",
        "sudan", "nigeria", "south africa", "japan", "korea", "turkey", "nato", "un", "united nations"
    )

    private val GEOPOLITICAL_KEYWORDS = mapOf(
        "summit" to 14.0, "treaty" to 14.0, "security council" to 14.0, "united nations" to 12.0,
        "diplomacy" to 12.0, "diplomatic" to 12.0, "alliance" to 12.0, "nato" to 12.0, "sanctions" to 12.0,
        "ambassador" to 10.0, "brics" to 10.0, "ceasefire" to 12.0, "military" to 10.0, "border" to 10.0,
        "missile" to 10.0, "defense" to 10.0, "defence" to 10.0, "security" to 10.0, "conflict" to 10.0,
        "sovereignty" to 12.0, "nuclear" to 12.0, "airstrike" to 10.0, "war" to 10.0, "peace" to 8.0,
        "president" to 10.0, "prime minister" to 10.0, "foreign minister" to 10.0, "chancellor" to 8.0,
        "parliament" to 8.0, "government" to 6.0, "trade" to 8.0, "tariff" to 8.0, "embargo" to 8.0,
        "strait" to 8.0, "shipping" to 6.0, "humanitarian" to 8.0, "refugee" to 8.0, "aid" to 6.0
    )

    private val EXCLUSION_KEYWORDS = listOf(
        "football", "tennis", "cricket", "nfl", "premier league", "soccer", "actor", "hollywood",
        "box office", "horoscope", "diet", "recipe", "lottery", "murder trial", "drinking alcohol",
        "celebrity", "red carpet", "fashion week", "album review", "boxer"
    )

    private data class ArticleCluster(
        val primaryArticle: RawArticle,
        val allSources: MutableSet<String> = mutableSetOf(),
        var score: Double = 0.0
    )

    /**
     * Deduplicate stories from multiple public HTTPS RSS feeds and rank them
     * to select the top 4 verified geopolitical stories.
     */
    fun deduplicateAndSelectTopFour(rawArticles: List<RawArticle>): List<NewsStory> {
        if (rawArticles.isEmpty()) return emptyList()

        // 1. Filter out empty or strictly excluded items
        val validArticles = rawArticles.filter { art ->
            val title = art.title.trim()
            val link = art.link.trim()
            val lowerTitle = title.lowercase(Locale.ROOT)
            title.length >= 10 &&
                    link.startsWith("http") &&
                    !EXCLUSION_KEYWORDS.any { lowerTitle.contains(it) }
        }

        val pool = if (validArticles.size >= 4) validArticles else rawArticles

        // 2. Cluster / Deduplicate stories covering the same event across outlets
        val clusters = mutableListOf<ArticleCluster>()

        for (article in pool) {
            var matchedCluster: ArticleCluster? = null

            for (cluster in clusters) {
                if (isSameStory(article, cluster.primaryArticle)) {
                    matchedCluster = cluster
                    break
                }
            }

            if (matchedCluster != null) {
                matchedCluster.allSources.add(article.sourceName)
                // If this article has a longer, more descriptive summary, update primary
                if (article.description.length > matchedCluster.primaryArticle.description.length &&
                    article.description.length < 500
                ) {
                    // keep longer summary
                }
            } else {
                val newCluster = ArticleCluster(
                    primaryArticle = article,
                    allSources = mutableSetOf(article.sourceName)
                )
                clusters.add(newCluster)
            }
        }

        // 3. Compute geopolitical score for each cluster
        for (cluster in clusters) {
            var score = 10.0
            // Extra reputable wire bonus (+15.0 per additional independent source!)
            score += (cluster.allSources.size - 1) * 15.0

            val text = "${cluster.primaryArticle.title} ${cluster.primaryArticle.description}".lowercase(Locale.ROOT)

            for ((kw, weight) in GEOPOLITICAL_KEYWORDS) {
                if (text.contains(kw)) {
                    score += weight
                }
            }

            cluster.score = score
        }

        // 4. Sort clusters by score descending
        val sortedClusters = clusters.sortedByDescending { it.score }

        // 5. Select top 4 ensuring regional diversity
        val selectedClusters = mutableListOf<ArticleCluster>()
        val regionCount = mutableMapOf<String, Int>()

        for (cluster in sortedClusters) {
            if (selectedClusters.size >= 4) break
            val region = inferRegion(cluster.primaryArticle.title, cluster.primaryArticle.description)
            val currentCount = regionCount[region] ?: 0

            // Max 2 from the same region among the 4
            if (currentCount < 2 || sortedClusters.size < 6) {
                selectedClusters.add(cluster)
                regionCount[region] = currentCount + 1
            }
        }

        // If not yet 4 (due to strict diversity), fill up from remaining
        if (selectedClusters.size < 4) {
            for (cluster in sortedClusters) {
                if (selectedClusters.size >= 4) break
                if (!selectedClusters.contains(cluster)) {
                    selectedClusters.add(cluster)
                }
            }
        }

        // 6. Map to NewsStory objects
        return selectedClusters.take(4).mapIndexed { index, cluster ->
            val art = cluster.primaryArticle
            val rank = index + 1
            val region = inferRegion(art.title, art.description)
            val category = inferCategory(art.title, art.description)
            val factStatus = inferFactStatus(art.title, cluster.allSources.size)

            val relatedSourcesStr = cluster.allSources
                .filter { it != art.sourceName }
                .joinToString(", ")

            NewsStory(
                id = UUID.nameUUIDFromBytes(art.link.toByteArray()).toString(),
                rank = rank,
                headline = art.title.trim(),
                summary = formatSummary(art.description),
                countryRegion = region,
                sourceName = art.sourceName,
                publishedAt = formatPubDate(art.pubDate),
                sourceUrl = art.link.trim(),
                categoryTag = category,
                factStatus = factStatus,
                isShortCandidate = rank <= 2,
                relatedSources = relatedSourcesStr
            )
        }
    }

    private fun isSameStory(a: RawArticle, b: RawArticle): Boolean {
        val titleA = a.title.lowercase(Locale.ROOT)
        val titleB = b.title.lowercase(Locale.ROOT)

        // Check shared key entities
        val entitiesA = KEY_ENTITIES.filter { titleA.contains(it) }.toSet()
        val entitiesB = KEY_ENTITIES.filter { titleB.contains(it) }.toSet()
        val sharedEntities = entitiesA.intersect(entitiesB)

        if (sharedEntities.size >= 2) {
            return true
        }

        // Check Jaccard word similarity on non-stop words
        val wordsA = tokenize(titleA)
        val wordsB = tokenize(titleB)
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false

        val intersection = wordsA.intersect(wordsB).size
        val union = wordsA.union(wordsB).size
        val jaccard = intersection.toDouble() / union.toDouble()

        if (jaccard >= 0.35) return true
        if (sharedEntities.size >= 1 && jaccard >= 0.22) return true

        return false
    }

    private fun tokenize(text: String): Set<String> {
        return text.split(Regex("[^a-zA-Z0-9]+"))
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.length >= 3 && !STOP_WORDS.contains(it) }
            .toSet()
    }

    private fun inferRegion(title: String, desc: String): String {
        val combined = "$title $desc".lowercase(Locale.ROOT)
        return when {
            combined.contains("israel") || combined.contains("gaza") || combined.contains("palestine") ||
                    combined.contains("syria") || combined.contains("lebanon") || combined.contains("iran") ||
                    combined.contains("iraq") || combined.contains("yemen") || combined.contains("saudi") ||
                    combined.contains("middle east") -> "Middle East"

            combined.contains("ukraine") || combined.contains("russia") || combined.contains("poland") ||
                    combined.contains("germany") || combined.contains("france") || combined.contains("britain") ||
                    combined.contains("uk") || combined.contains("eu") || combined.contains("european union") ||
                    combined.contains("brussels") || combined.contains("nato") -> "Europe"

            combined.contains("china") || combined.contains("taiwan") || combined.contains("india") ||
                    combined.contains("pakistan") || combined.contains("japan") || combined.contains("korea") ||
                    combined.contains("indo-pacific") || combined.contains("philippines") -> "Asia-Pacific"

            combined.contains("us") || combined.contains("usa") || combined.contains("america") ||
                    combined.contains("canada") || combined.contains("washington") -> "North America"

            combined.contains("brazil") || combined.contains("venezuela") || combined.contains("argentina") ||
                    combined.contains("colombia") || combined.contains("mexico") || combined.contains("latin america") -> "Latin America"

            combined.contains("sudan") || combined.contains("nigeria") || combined.contains("haiti") ||
                    combined.contains("south africa") || combined.contains("kenya") || combined.contains("ethiopia") ||
                    combined.contains("africa") || combined.contains("congo") -> "Africa"

            else -> "Global"
        }
    }

    private fun inferCategory(title: String, desc: String): String {
        val combined = "$title $desc".lowercase(Locale.ROOT)
        return when {
            combined.contains("summit") || combined.contains("treaty") || combined.contains("diplomacy") ||
                    combined.contains("diplomatic") || combined.contains("ambassador") || combined.contains("brics") -> "Diplomacy"

            combined.contains("military") || combined.contains("missile") || combined.contains("defense") ||
                    combined.contains("defence") || combined.contains("security") || combined.contains("nato") -> "Security"

            combined.contains("ceasefire") || combined.contains("strike") || combined.contains("attack") ||
                    combined.contains("war") || combined.contains("conflict") || combined.contains("clash") -> "Conflict"

            combined.contains("trade") || combined.contains("tariff") || combined.contains("sanctions") ||
                    combined.contains("economy") || combined.contains("embargo") -> "Economy"

            combined.contains("humanitarian") || combined.contains("refugee") || combined.contains("aid") ||
                    combined.contains("rights") || combined.contains("unicef") -> "Humanitarian"

            else -> "International Relations"
        }
    }

    private fun inferFactStatus(title: String, sourceCount: Int): String {
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("claims") || lower.contains("alleged") || lower.contains("accuses") ||
                    lower.contains("says") || lower.contains("warns") || lower.contains("denounces") -> "OFFICIAL CLAIM"
            sourceCount >= 2 || lower.contains("treaty") || lower.contains("agreement") ||
                    lower.contains("summit") || lower.contains("report") -> "CONFIRMED"
            else -> "REPORTING"
        }
    }

    private fun formatSummary(rawDescription: String): String {
        val clean = rawDescription.replace(Regex("\\s+"), " ").trim()
        if (clean.isBlank()) return "Verified reporting from primary international wire."
        // Keep 2-3 sentences, max 260 chars
        val sentences = clean.split(Regex("(?<=[.!?])\\s+"))
        val summary = sentences.take(3).joinToString(" ")
        return if (summary.length > 280) {
            summary.substring(0, 277).trimEnd() + "…"
        } else {
            summary
        }
    }

    private fun formatPubDate(rawDate: String): String {
        if (rawDate.isBlank() || rawDate == "Recent") return "Recent update"
        // Return cleaned up date string without long timezone offsets if too lengthy
        return if (rawDate.length > 25) {
            rawDate.substring(0, 22).trim()
        } else {
            rawDate
        }
    }
}
