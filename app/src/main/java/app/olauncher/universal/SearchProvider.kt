package app.olauncher.universal

/**
 * A search provider contributes zero or more results for a query.
 *
 * Providers:
 * - must be cheap to run on every keystroke
 * - must never throw: callers treat any exception as "no result"
 * - declare an explicit priority weight so ranking never depends on run order
 */
fun interface SearchProvider {
    fun search(query: String): List<SearchResult>

    companion object {
        fun of(vararg providers: SearchProvider): List<SearchProvider> = providers.toList()
    }
}