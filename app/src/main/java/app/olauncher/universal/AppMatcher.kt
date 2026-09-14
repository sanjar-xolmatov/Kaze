package app.olauncher.universal

import java.text.Normalizer

/**
 * Deterministic, cheap app-label ranking. Lower score means a stronger match.
 *
 * Exact > prefix > substring > normalized (diacritics/separators stripped) > package name.
 * Scores are stable: tie-broken by the original (collation-sorted) list order by the caller.
 */
object AppMatcher {

    const val EXACT = 0
    const val PREFIX = 1
    const val SUBSTRING = 2
    const val NORMALIZED = 3
    const val PACKAGE = 4

    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val separatorsRegex = Regex("[-_+,.`'\\s\\p{Z}]")

    fun score(appLabel: String, appPackage: String, query: String): Int? {
        val q = query.trim()
        if (q.isEmpty()) return null
        if (appLabel.equals(q, ignoreCase = true)) return EXACT
        if (appLabel.startsWith(q, ignoreCase = true)) return PREFIX
        if (appLabel.contains(q, ignoreCase = true)) return SUBSTRING
        val normalizedQuery = q.normalizeForSearch()
        if (normalizedQuery.isNotEmpty() &&
            appLabel.normalizeForSearch().contains(normalizedQuery, ignoreCase = true)
        ) return NORMALIZED
        if (appPackage.contains(q.lowercase())) return PACKAGE
        return null
    }

    private fun String.normalizeForSearch(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(diacriticsRegex, "")
            .replace(separatorsRegex, "")
}