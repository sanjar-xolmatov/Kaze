package app.olauncher.universal

/**
 * Detects well-formed web URLs without ever contacting the network.
 *
 * Accepted forms:
 *   https://github.com/path?a=1#frag
 *   http://example.com
 *   github.com
 *   www.example.com/test
 *   example.com:8080?q=1
 *   unicode-doma\u00eene.example
 *
 * Rejected (low confidence, so the query falls through to app/web search):
 *   text with spaces ("hello world")            -> not a single URL
 *   non web schemes ("ftp://...", "mailto:...") -> not a browsable URL
 *   bare words without a dot ("firefox")        -> likely an app name
 *   suspicious host labels ("foo_bar", ".com", "a..b")
 *   IP literals without a scheme
 */
object UrlDetector {

    private const val PREFIX_HTTP = "http://"
    private const val PREFIX_HTTPS = "https://"

    /**
     * @return the trimmed candidate as the user typed it, or null when it is not a confident URL
     */
    fun detect(raw: String): String? {
        val candidate = raw.trim()
        if (candidate.isEmpty()) return null
        // A URL has no whitespace; sentences are not URLs.
        if (candidate.any { it.isWhitespace() }) return null

        var rest = candidate
        var hasScheme = false

        when {
            candidate.startsWith(PREFIX_HTTP) || candidate.startsWith(PREFIX_HTTPS) -> {
                hasScheme = true
                rest = candidate.removePrefix(PREFIX_HTTP).removePrefix(PREFIX_HTTPS)
            }

            candidate.contains("://") -> {
                val scheme = candidate.substringBefore("://").lowercase()
                if (scheme != "http" && scheme != "https") return null
                hasScheme = true
                rest = candidate.substringAfter("://")
            }
        }

        if (rest.isEmpty()) return null

        val pathStart = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
        val hostPort = if (pathStart >= 0) rest.substring(0, pathStart) else rest
        val host = hostPort
            .substringBefore(':')
            .removePrefix("www.")
            .dropLastWhile { it == '.' }

        if (host.isEmpty() || !isValidHost(host)) return null

        if (!hasScheme && host != "localhost") {
            // A scheme-less string needs a dot ("github.com") so "firefox" stays an app.
            if (!host.contains('.')) return null
            val tld = host.substringAfterLast('.')
            if (tld.length < 2 || !tld.all { it.isLetter() }) return null
        }

        return candidate
    }

    fun withScheme(url: String): String =
        if (url.startsWith(PREFIX_HTTP) || url.startsWith(PREFIX_HTTPS)) url
        else "$PREFIX_HTTPS$url"

    private fun isValidHost(host: String): Boolean {
        val labels = host.split('.')
        if (labels.isEmpty()) return false
        return labels.all { label ->
            label.isNotEmpty() &&
                label.first().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }
    }
}