package app.olauncher.universal

import app.olauncher.helper.timeutils.TimeUtility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUtilityProviderTest {

    @Test
    fun `timer queries parse to Timer`() {
        assertEquals(TimeUtility.Timer(300_000L), TimeUtilityProvider.parse("timer 5m"))
        val results = TimeUtilityProvider.search("timer 5m")
        assertEquals(SearchType.TIMER, results.single().type)
        assertEquals(TimeUtility.Timer(300_000L), results.single().payload)
    }

    @Test
    fun `stopwatch queries parse to Stopwatch`() {
        assertEquals(TimeUtility.Stopwatch, TimeUtilityProvider.parse("stopwatch"))
        assertEquals(SearchType.STOPWATCH, TimeUtilityProvider.search("stopwatch").single().type)
    }

    @Test
    fun `now and time-in queries are UTILITY results`() {
        assertEquals(SearchType.UTILITY, TimeUtilityProvider.search("time").single().type)
        assertEquals(SearchType.UTILITY, TimeUtilityProvider.search("time in tokyo").single().type)
    }

    @Test
    fun `non utility queries parse to null`() {
        assertNull(TimeUtilityProvider.parse("firefox"))
        assertNull(TimeUtilityProvider.parse("calendar"))
        assertNull(TimeUtilityProvider.parse("25*48"))
    }

    @Test
    fun `display types are the row rendering ones`() {
        assertTrue(TimeUtilityProvider.isDisplayType(TimeUtility.Now))
        assertTrue(TimeUtilityProvider.isDisplayType(TimeUtilityProvider.parse("time in tokyo")))
        assertTrue(TimeUtilityProvider.isDisplayType(TimeUtilityProvider.parse("days until 2099-01-01")))
        assertFalse(TimeUtilityProvider.isDisplayType(TimeUtility.Timer(60_000L)))
        assertFalse(TimeUtilityProvider.isDisplayType(TimeUtility.Stopwatch))
        assertFalse(TimeUtilityProvider.isDisplayType(null))
    }

    @Test
    fun `needsTicker covers live and timer utilities`() {
        assertTrue(TimeUtilityProvider.needsTicker(TimeUtility.Timer(60_000L)))
        assertTrue(TimeUtilityProvider.needsTicker(TimeUtility.Stopwatch))
        assertTrue(TimeUtilityProvider.needsTicker(TimeUtility.Now))
        assertTrue(TimeUtilityProvider.needsTicker(TimeUtilityProvider.parse("time in tokyo")))
        assertFalse(TimeUtilityProvider.needsTicker(TimeUtilityProvider.parse("days until 2099-01-01")))
        assertFalse(TimeUtilityProvider.needsTicker(null))
    }
}