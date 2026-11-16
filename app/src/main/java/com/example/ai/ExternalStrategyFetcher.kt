package com.example.ai

import android.util.Log
import com.example.data.GameType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "ExternalStrategyFetcher"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 10_000
private const val MAX_EXCERPT_CHARS = 4_200

data class ExternalSourcePayload(
    val sourceTitle: String,
    val excerpt: String,
    val fetchedFromWeb: Boolean
)

interface ExternalStrategyFetcher {
    suspend fun fetchStrategyMaterial(
        gameTitle: String,
        externalSourceInput: String,
        sourceType: String,
        targetGame: GameType
    ): Result<ExternalSourcePayload>
}

class HttpExternalStrategyFetcher : ExternalStrategyFetcher {

    override suspend fun fetchStrategyMaterial(
        gameTitle: String,
        externalSourceInput: String,
        sourceType: String,
        targetGame: GameType
    ): Result<ExternalSourcePayload> = withContext(Dispatchers.IO) {
        val trimmedTitle = gameTitle.trim().ifBlank { targetGame.title }
        val trimmedSource = externalSourceInput.trim()

        if (trimmedSource.startsWith("http://", ignoreCase = true) ||
            trimmedSource.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext fetchDirectUrl(trimmedSource, trimmedTitle)
        }

        // Try Wikipedia search + plain-text extract for the requested game title
        val wikiResult = runCatching {
            fetchWikipediaStrategyExtract(trimmedTitle, trimmedSource)
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Log.w(TAG, "Wikipedia lookup skipped/failed for '$trimmedTitle': ${err.localizedMessage}")
        }.getOrNull()

        if (wikiResult != null) {
            return@withContext Result.success(wikiResult)
        }

        // Fall back to matching external curated dossier if user entered one of the Arena titles
        val curatedMatch = CuratedResearchLibrary.sources.firstOrNull { source ->
            source.gameType.title.equals(trimmedTitle, ignoreCase = true) ||
                trimmedTitle.contains(source.gameType.title, ignoreCase = true) ||
                (source.gameType == GameType.NEURAL_2048 && trimmedTitle.contains("2048", ignoreCase = true)) ||
                (source.gameType == GameType.CYBER_SNAKE && trimmedTitle.contains("snake", ignoreCase = true)) ||
                (source.gameType == GameType.TACTICAL_GRID && trimmedTitle.contains("tactical", ignoreCase = true))
        }
        if (curatedMatch != null) {
            return@withContext Result.success(
                ExternalSourcePayload(
                    sourceTitle = "$trimmedTitle — ${curatedMatch.title}".take(150),
                    excerpt = curatedMatch.contentExcerpt.take(MAX_EXCERPT_CHARS),
                    fetchedFromWeb = false
                )
            )
        }

        // Otherwise allow Gemini to research the game title directly with source focus context
        val sourceTopic = trimmedSource.ifBlank { sourceType.replace("_", " ") }
        Result.success(
            ExternalSourcePayload(
                sourceTitle = "$trimmedTitle ($sourceTopic)".take(150),
                excerpt = buildString {
                    appendLine("Game Title to Research: $trimmedTitle")
                    appendLine("Requested External Source Focus: $sourceTopic")
                    appendLine("Target Arena Engine Slot: ${targetGame.title}")
                    appendLine("Arena Engine Rules Reference: ${targetGame.rulesOverview}")
                    append("Synthesize core winning objectives and concrete tactical heuristics for '$trimmedTitle'.")
                }.take(MAX_EXCERPT_CHARS),
                fetchedFromWeb = false
            )
        )
    }

    private fun fetchDirectUrl(urlString: String, gameTitle: String): Result<ExternalSourcePayload> {
        return try {
            val rawHtml = executeHttpGet(urlString)
            val pageTitle = extractHtmlTitle(rawHtml)
            val cleanedText = stripHtmlToText(rawHtml).take(MAX_EXCERPT_CHARS)
            if (cleanedText.length < 40) {
                Result.failure(Exception("Fetched external source contained insufficient readable text."))
            } else {
                val host = runCatching { URL(urlString).host }.getOrDefault("Web Source")
                val resolvedTitle = (pageTitle?.takeIf { it.isNotBlank() } ?: "$gameTitle ($host)").take(150)
                Result.success(
                    ExternalSourcePayload(
                        sourceTitle = resolvedTitle,
                        excerpt = cleanedText,
                        fetchedFromWeb = true
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching external URL: $urlString", e)
            Result.failure(Exception("Could not fetch external URL: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    private fun fetchWikipediaStrategyExtract(
        gameTitle: String,
        sourceHint: String
    ): ExternalSourcePayload? {
        val searchQueries = listOfNotNull(
            listOf(gameTitle, sourceHint, "game").filter { it.isNotBlank() }.joinToString(" "),
            gameTitle
        ).distinct()

        for (query in searchQueries) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedQuery&srlimit=2&format=json"
            val searchJson = JSONObject(executeHttpGet(searchUrl))
            val searchArray = searchJson.optJSONObject("query")?.optJSONArray("search") ?: continue
            if (searchArray.length() == 0) continue

            val firstHit = searchArray.getJSONObject(0)
            val pageId = firstHit.optLong("pageid", -1L)
            val pageTitle = firstHit.optString("title", gameTitle)
            if (pageId <= 0L) continue

            val extractUrl = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&explaintext=true&exchars=3800&redirects=1&format=json&pageids=$pageId"
            val extractJson = JSONObject(executeHttpGet(extractUrl))
            val pagesObj = extractJson.optJSONObject("query")?.optJSONObject("pages") ?: continue
            val pageObj = pagesObj.optJSONObject(pageId.toString()) ?: continue
            val extractText = pageObj.optString("extract", "").trim().take(MAX_EXCERPT_CHARS)
            if (extractText.length >= 60) {
                return ExternalSourcePayload(
                    sourceTitle = "Wikipedia: $pageTitle ($gameTitle)".take(150),
                    excerpt = extractText,
                    fetchedFromWeb = true
                )
            }
        }
        return null
    }

    private fun executeHttpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "ApexMindResearchLab/1.0 (Android)")
            setRequestProperty("Accept", "application/json,text/html,text/plain")
        }
        try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                val errBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("HTTP $statusCode ${errBody.take(120)}".trim())
            }
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                val buffer = CharArray(4096)
                var totalRead = 0
                while (totalRead < 128_000) {
                    val count = reader.read(buffer)
                    if (count <= 0) break
                    sb.append(buffer, 0, count)
                    totalRead += count
                }
                return sb.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    internal fun stripHtmlToText(html: String): String {
        val withoutScripts = html
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<noscript[^>]*>.*?</noscript>"), " ")
        val withoutTags = withoutScripts.replace(Regex("<[^>]+>"), " ")
        return withoutTags
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    internal fun extractHtmlTitle(html: String): String? {
        val match = Regex("(?is)<title[^>]*>(.*?)</title>").find(html) ?: return null
        return stripHtmlToText(match.groupValues[1]).take(150).takeIf { it.isNotBlank() }
    }
}
