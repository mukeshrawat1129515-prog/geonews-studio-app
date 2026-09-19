package com.example.data.remote

import android.util.Log
import android.util.Xml
import com.example.data.model.RawArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale

class RssNewsParser(private val okHttpClient: OkHttpClient) {

    private val feedSources = listOf(
        Pair("BBC News", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        Pair("Al Jazeera", "https://www.aljazeera.com/xml/rss/all.xml"),
        Pair("The Guardian", "https://www.theguardian.com/world/rss"),
        Pair("DW News", "https://rss.dw.com/xml/rss-en-world"),
        Pair("UN News", "https://news.un.org/feed/subscribe/en/news/all/rss.xml"),
        Pair("France 24", "https://www.france24.com/en/rss")
    )

    suspend fun fetchFromAllFeeds(): List<RawArticle> = withContext(Dispatchers.IO) {
        supervisorScope {
            val deferredList = feedSources.map { (sourceName, feedUrl) ->
                async {
                    try {
                        fetchFeed(sourceName, feedUrl)
                    } catch (e: Exception) {
                        Log.w("RssNewsParser", "Feed failed for $sourceName ($feedUrl): ${e.message}")
                        emptyList()
                    }
                }
            }

            val allArticles = mutableListOf<RawArticle>()
            for (deferred in deferredList) {
                try {
                    val articles = deferred.await()
                    allArticles.addAll(articles)
                } catch (e: Exception) {
                    Log.w("RssNewsParser", "Error awaiting feed result: ${e.message}")
                }
            }
            allArticles
        }
    }

    private fun fetchFeed(sourceName: String, url: String): List<RawArticle> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
            .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w("RssNewsParser", "HTTP error ${response.code} fetching $sourceName from $url")
            return emptyList()
        }

        val xmlBody = response.body?.string() ?: return emptyList()
        if (xmlBody.isBlank()) return emptyList()

        return parseRssXml(xmlBody, sourceName)
    }

    private fun parseRssXml(xml: String, defaultSource: String): List<RawArticle> {
        val articles = mutableListOf<RawArticle>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inItem = false
            var currentTitle = ""
            var currentLink = ""
            var currentDesc = ""
            var currentPubDate = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name?.lowercase(Locale.ROOT) ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            inItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentDesc = ""
                            currentPubDate = ""
                        } else if (inItem) {
                            when (tagName) {
                                "title" -> {
                                    val text = readTagContent(parser)
                                    if (currentTitle.isBlank() && text.isNotBlank()) {
                                        currentTitle = text
                                    }
                                }
                                "link" -> {
                                    val attrHref = parser.getAttributeValue(null, "href")
                                    if (!attrHref.isNullOrBlank()) {
                                        currentLink = attrHref.trim()
                                    } else {
                                        val text = readTagContent(parser).trim()
                                        if (text.isNotBlank() && currentLink.isBlank()) {
                                            currentLink = text
                                        }
                                    }
                                }
                                "guid", "id" -> {
                                    val isPermaLink = parser.getAttributeValue(null, "isPermaLink")
                                    val text = readTagContent(parser).trim()
                                    if (currentLink.isBlank() && (isPermaLink == null || isPermaLink.equals("true", ignoreCase = true))) {
                                        if (text.startsWith("http://") || text.startsWith("https://")) {
                                            currentLink = text
                                        }
                                    }
                                }
                                "description", "summary" -> {
                                    val text = readTagContent(parser)
                                    if (currentDesc.isBlank() && text.isNotBlank()) {
                                        currentDesc = text
                                    }
                                }
                                "content:encoded", "content" -> {
                                    val text = readTagContent(parser)
                                    if (currentDesc.isBlank() && text.isNotBlank()) {
                                        currentDesc = text
                                    }
                                }
                                "pubdate", "published", "updated", "dc:date" -> {
                                    val text = readTagContent(parser).trim()
                                    if (currentPubDate.isBlank() && text.isNotBlank()) {
                                        currentPubDate = text
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "item" || tagName == "entry") {
                            val cleanTitle = cleanHtml(currentTitle)
                            val cleanLink = currentLink.trim()
                            if (cleanTitle.isNotBlank() && cleanLink.isNotBlank()) {
                                val cleanDesc = cleanHtml(currentDesc).ifBlank {
                                    "Geopolitical development reported by $defaultSource."
                                }
                                articles.add(
                                    RawArticle(
                                        title = cleanTitle,
                                        description = cleanDesc,
                                        link = cleanLink,
                                        sourceName = defaultSource,
                                        pubDate = currentPubDate.ifBlank { "Recent" }
                                    )
                                )
                            }
                            inItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.w("RssNewsParser", "Error parsing RSS XML for $defaultSource: ${e.message}")
        }
        return articles
    }

    private fun readTagContent(parser: XmlPullParser): String {
        if (parser.isEmptyElementTag) return ""
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            val event = parser.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            when (event) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(parser.text ?: "")
            }
        }
        return sb.toString().trim()
    }

    private fun cleanHtml(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(Regex("<.*?>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
