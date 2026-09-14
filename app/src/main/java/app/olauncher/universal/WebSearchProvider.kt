package app.olauncher.universal

/**
 * Last-resort provider. Only signals eligibility for the configured web search
 * engine; the actual URL request is opened by the UI when the user picks the row,
 * and only for the configured, privacy-friendly provider — never from a keystroke.
 */
object WebSearchProvider : SearchProvider {

    const val PRIORITY = 50

    override fun search(query: String): List<SearchResult> {
        if (query.isBlank() || query.startsWith("!")) return emptyList()
        return listOf(
            SearchResult(
                type = SearchType.WEB,
                weight = PRIORITY,
                title = "Search the web",
                subtitle = query.trim(),
            )
        )
    }
}