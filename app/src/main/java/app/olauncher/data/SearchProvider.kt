package app.olauncher.data

import java.net.URLEncoder

enum class SearchProvider(
    val label: String,
    private val urlTemplate: String,
) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q={query}"),
    GOOGLE("Google", "https://www.google.com/search?q={query}"),
    BING("Bing", "https://www.bing.com/search?q={query}");

    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return urlTemplate.replace("{query}", encoded)
    }

    companion object {
        fun default(): SearchProvider = DUCKDUCKGO

        fun fromName(name: String?): SearchProvider =
            entries.firstOrNull { it.name == name } ?: default()
    }
}