package app.olauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchProviderTest {

    @Test
    fun `default provider is DuckDuckGo`() {
        assertEquals(SearchProvider.DUCKDUCKGO, SearchProvider.default())
    }

    @Test
    fun `simple query is url encoded with spaces as plus`() {
        assertEquals(
            "https://duckduckgo.com/?q=rust+ownership",
            SearchProvider.DUCKDUCKGO.buildSearchUrl("rust ownership")
        )
    }

    @Test
    fun `special characters are encoded`() {
        assertEquals(
            "https://duckduckgo.com/?q=a%26b%3Fc%2Fd%23e%25f%2Cg%3Bh",
            SearchProvider.DUCKDUCKGO.buildSearchUrl("a&b?c/d#e%f,g;h")
        )
    }

    @Test
    fun `unicode query is encoded`() {
        assertEquals(
            "https://duckduckgo.com/?q=h%C3%A9llo+w%C3%B6rld",
            SearchProvider.DUCKDUCKGO.buildSearchUrl("héllo wörld")
        )
    }

    @Test
    fun `other providers follow their own template`() {
        assertEquals(
            "https://www.google.com/search?q=rust+ownership",
            SearchProvider.GOOGLE.buildSearchUrl("rust ownership")
        )
        assertEquals(
            "https://www.bing.com/search?q=rust+ownership",
            SearchProvider.BING.buildSearchUrl("rust ownership")
        )
    }

    @Test
    fun `very long query is handled`() {
        val longQuery = "a".repeat(1000)
        val url = SearchProvider.DUCKDUCKGO.buildSearchUrl(longQuery)
        assertTrue(url.startsWith("https://duckduckgo.com/?q="))
        assertTrue(url.length > longQuery.length)
    }

    @Test
    fun `fromName resolves known providers`() {
        assertEquals(SearchProvider.DUCKDUCKGO, SearchProvider.fromName("DUCKDUCKGO"))
        assertEquals(SearchProvider.GOOGLE, SearchProvider.fromName("GOOGLE"))
        assertEquals(SearchProvider.BING, SearchProvider.fromName("BING"))
    }

    @Test
    fun `fromName falls back to default for unknown or null`() {
        assertEquals(SearchProvider.DUCKDUCKGO, SearchProvider.fromName("YAHOO"))
        assertEquals(SearchProvider.DUCKDUCKGO, SearchProvider.fromName(null))
        assertEquals(SearchProvider.DUCKDUCKGO, SearchProvider.fromName(""))
    }
}