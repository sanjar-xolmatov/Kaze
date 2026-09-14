package app.olauncher.universal

import app.olauncher.data.AppModel

/**
 * Turns a confident URL query into an "Open URL" result row.
 * Runs entirely locally; opening the link is only done when the user picks it.
 */
object UrlProvider : SearchProvider {

    const val PRIORITY = 40

    override fun search(query: String): List<SearchResult> {
        val url = detect(query) ?: return emptyList()
        return listOf(
            SearchResult(
                type = SearchType.URL,
                weight = PRIORITY,
                title = "Open URL",
                subtitle = url,
                payload = url,
            )
        )
    }

    fun detect(query: String): String? = UrlDetector.detect(query)

    /** Builds the display row used by the results list. */
    fun row(query: String, openLabel: String): AppModel.UrlResult? {
        val url = detect(query) ?: return null
        return AppModel.UrlResult(
            title = openLabel,
            subtitle = url,
            url = UrlDetector.withScheme(url),
        )
    }
}