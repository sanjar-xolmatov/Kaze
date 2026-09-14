package app.olauncher.universal

import app.olauncher.helper.timeutils.TimeUtility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalSearchTest {

    private val search = UniversalSearch()

    @Test
    fun `calculator result is returned`() {
        val response = search.response("25*48")
        assertEquals("1200", response.calculatorResult)
        assertTrue(response.hasLocalResult)
    }

    @Test
    fun `timer query returns a Timer utility`() {
        assertEquals(TimeUtility.Timer(300_000L), search.response("timer 5m").timeUtility)
    }

    @Test
    fun `stopwatch query returns a Stopwatch utility`() {
        assertEquals(TimeUtility.Stopwatch, search.response("stopwatch").timeUtility)
    }

    @Test
    fun `time-in query returns a TimeIn utility`() {
        val utility = search.response("time in tokyo").timeUtility
        assertTrue(utility is TimeUtility.TimeIn)
    }

    @Test
    fun `url query is detected without a hit on web`() {
        val response = search.response("github.com")
        assertEquals("github.com", response.url)
        assertTrue(response.hasLocalResult)
    }

    @Test
    fun `https url with path is detected`() {
        assertEquals("https://github.com/a/b", search.response("https://github.com/a/b").url)
    }

    @Test
    fun `ordinary words are only web-eligible`() {
        val response = search.response("firefox")
        assertNull(response.calculatorResult)
        assertNull(response.timeUtility)
        assertNull(response.url)
        assertTrue(response.webEligible)
        assertFalse(response.hasLocalResult)
    }

    @Test
    fun `blank and bang queries produce nothing`() {
        val blank = search.response("")
        assertFalse(blank.hasLocalResult)
        assertFalse(blank.webEligible)

        val bang = search.response("!bang")
        assertNull(bang.calculatorResult)
        assertFalse(bang.webEligible)
    }

    @Test
    fun `includeWeb false disables web eligibility`() {
        assertFalse(search.response("firefox", includeWeb = false).webEligible)
    }

    @Test
    fun `a throwing provider is isolated`() {
        val broken = object : SearchProvider {
            override fun search(query: String): List<SearchResult> =
                throw RuntimeException("boom")
        }
        val search = UniversalSearch(listOf(broken, CalculatorProvider, WebSearchProvider))
        val response = search.response("25*48")
        assertEquals("1200", response.calculatorResult)
        assertTrue(response.webEligible)
    }

    @Test
    fun `provider priorities keep ranking deterministic`() {
        listOf(
            CalculatorProvider.PRIORITY to TimeUtilityProvider.PRIORITY,
            TimeUtilityProvider.PRIORITY to UrlProvider.PRIORITY,
            UrlProvider.PRIORITY to WebSearchProvider.PRIORITY,
        ).forEach { (a, b) ->
            assertTrue("$a should rank before $b", a < b)
        }
    }
}