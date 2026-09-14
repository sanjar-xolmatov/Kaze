package app.olauncher.universal

import app.olauncher.helper.timeutils.TimeUtility

/**
 * Central search orchestrator.
 *
 * Responsibilities:
 *  - run every provider so multiple providers can contribute simultaneously
 *  - isolate providers: a throwing provider is treated as "no result" and never
 *    takes the search UI down
 *  - keep ranking deterministic via explicit [SearchResult.weight] values
 *
 * Local-first: providers are cheap, offline, and never make network requests.
 * Web search is represented as eligibility only ([SearchResponse.webEligible]).
 */
class UniversalSearch(
    private val providers: List<SearchProvider> = listOf(
        CalculatorProvider,
        TimeUtilityProvider,
        UrlProvider,
        WebSearchProvider,
    ),
) {

    data class SearchResponse(
        val calculatorResult: String? = null,
        val timeUtility: TimeUtility? = null,
        val url: String? = null,
        val webEligible: Boolean = false,
    ) {
        val hasLocalResult: Boolean
            get() = calculatorResult != null || timeUtility != null || url != null
    }

    fun response(query: String, includeWeb: Boolean = true): SearchResponse {
        var calculatorResult: String? = null
        var timeUtility: TimeUtility? = null
        var url: String? = null
        var webEligible = false

        providers.forEach { provider ->
            val results = runCatching { provider.search(query) }.getOrDefault(emptyList())
            results.forEach { result ->
                when (result.type) {
                    SearchType.CALCULATOR -> calculatorResult = result.title
                    SearchType.TIMER, SearchType.STOPWATCH, SearchType.UTILITY ->
                        timeUtility = result.payload as? TimeUtility ?: timeUtility

                    SearchType.URL -> url = result.payload as? String ?: url
                    SearchType.WEB -> webEligible = true
                    SearchType.APP -> {} // apps are ranked by the app search itself
                }
            }
        }

        return SearchResponse(
            calculatorResult = calculatorResult,
            timeUtility = timeUtility,
            url = url,
            webEligible = webEligible && includeWeb,
        )
    }
}