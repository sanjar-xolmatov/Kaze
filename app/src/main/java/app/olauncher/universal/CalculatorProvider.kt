package app.olauncher.universal

import app.olauncher.helper.MathEvaluator

/**
 * Calculator provider. Reuses the existing [MathEvaluator] — this is a thin
 * wrapper that owns the trigger rules, so ordinary words are never treated as math.
 */
object CalculatorProvider : SearchProvider {

    const val PRIORITY = 20

    override fun search(query: String): List<SearchResult> {
        val result = evaluate(query) ?: return emptyList()
        return listOf(
            SearchResult(
                type = SearchType.CALCULATOR,
                weight = PRIORITY,
                title = result,
                payload = query,
            )
        )
    }

    /** Returns the formatted result, or null when the query is not a confident math expression. */
    fun evaluate(query: String): String? {
        if (query.isBlank() || query.startsWith("!")) return null
        if (query.none { it == '+' || it == '-' || it == '*' || it == '/' || it == '%' || it == '(' || it == ')' }) return null
        val value = MathEvaluator.evaluate(query) ?: return null
        return MathEvaluator.formatResult(value)
    }
}