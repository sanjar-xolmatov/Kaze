package app.olauncher.universal

enum class SearchType {
    APP, CALCULATOR, TIMER, STOPWATCH, UTILITY, URL, WEB
}

/**
 * A unified search result. The UI never needs to know which provider produced
 * a result; it only renders [title]/[subtitle] and dispatches on [type].
 *
 * @param weight lower values rank first, regardless of provider execution order
 * @param payload optional provider-specific data (e.g. the parsed [AppModel] or URL string)
 */
data class SearchResult(
    val type: SearchType,
    val weight: Int,
    val title: String,
    val subtitle: String = "",
    val payload: Any? = null,
) {
    val isApp: Boolean get() = type == SearchType.APP
}