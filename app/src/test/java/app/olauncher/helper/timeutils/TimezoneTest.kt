package app.olauncher.helper.timeutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimezoneTest {

    @Test
    fun `known cities resolve to expected zones`() {
        assertEquals(CityZone("Tokyo", "Asia/Tokyo"), TimezoneRegistry.resolve("Tokyo"))
        assertEquals(CityZone("London", "Europe/London"), TimezoneRegistry.resolve("London"))
        assertEquals(CityZone("New York", "America/New_York"), TimezoneRegistry.resolve("New York"))
        assertEquals(CityZone("Los Angeles", "America/Los_Angeles"), TimezoneRegistry.resolve("Los Angeles"))
        assertEquals(CityZone("Sydney", "Australia/Sydney"), TimezoneRegistry.resolve("Sydney"))
        assertEquals(CityZone("Mumbai", "Asia/Kolkata"), TimezoneRegistry.resolve("Mumbai"))
        assertEquals(CityZone("New Delhi", "Asia/Kolkata"), TimezoneRegistry.resolve("New Delhi"))
        assertEquals(CityZone("Singapore", "Asia/Singapore"), TimezoneRegistry.resolve("Singapore"))
    }

    @Test
    fun `resolution is case insensitive`() {
        assertEquals(CityZone("Tokyo", "Asia/Tokyo"), TimezoneRegistry.resolve("tokyo"))
        assertEquals(CityZone("Tokyo", "Asia/Tokyo"), TimezoneRegistry.resolve("TOKYO"))
        assertEquals(CityZone("Paris", "Europe/Paris"), TimezoneRegistry.resolve("  paris  "))
    }

    @Test
    fun `unknown places resolve to null`() {
        assertNull(TimezoneRegistry.resolve("Atlantis"))
        assertNull(TimezoneRegistry.resolve(""))
        assertNull(TimezoneRegistry.resolve("New York City"))
    }

    @Test
    fun `aliased names map to the same zone`() {
        assertEquals(
            TimezoneRegistry.resolve("Mumbai")!!.zoneId,
            TimezoneRegistry.resolve("Delhi")!!.zoneId
        )
        assertEquals(
            TimezoneRegistry.resolve("Beijing")!!.zoneId,
            TimezoneRegistry.resolve("Shanghai")!!.zoneId
        )
    }
}