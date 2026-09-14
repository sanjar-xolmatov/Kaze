package app.olauncher.helper.timeutils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone

class TimeFormattersTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(Locale.getDefault())
        TimeZone.setDefault(TimeZone.getDefault())
    }

    @Test
    fun `parseDate supports ISO format`() {
        assertEquals(LocalDate.of(2026, 9, 13), TimeFormatters.parseDate("2026-09-13"))
    }

    @Test
    fun `parseDate supports slash and dot formats`() {
        assertEquals(LocalDate.of(2026, 12, 6), TimeFormatters.parseDate("2026/12/6"))
        assertEquals(LocalDate.of(2026, 12, 6), TimeFormatters.parseDate("2026.12.6"))
    }

    @Test
    fun `parseDate rejects garbage`() {
        assertNull(TimeFormatters.parseDate("soon"))
        assertNull(TimeFormatters.parseDate("13/09/2026"))
        assertNull(TimeFormatters.parseDate(""))
    }

    @Test
    fun `dayName returns full weekday`() {
        assertEquals("Sunday", TimeFormatters.dayName(LocalDate.of(2026, 9, 13)))
        assertEquals("Sunday", TimeFormatters.dayName(LocalDate.of(2026, 12, 6)))
        assertEquals("Monday", TimeFormatters.dayName(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `fullDate formatting`() {
        assertEquals("September 13, 2026", TimeFormatters.fullDate(LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun `mediumDate formatting`() {
        assertEquals("Sep 13, 2026", TimeFormatters.mediumDate(LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun `daysBetween counts days`() {
        assertEquals(84, TimeFormatters.daysBetween(LocalDate.of(2026, 9, 13), LocalDate.of(2026, 12, 6)))
        assertEquals(-84, TimeFormatters.daysBetween(LocalDate.of(2026, 12, 6), LocalDate.of(2026, 9, 13)))
    }

    @Test
    fun `formatTimer shows mmss`() {
        assertEquals("00:00", TimeFormatters.formatTimer(0))
        assertEquals("00:30", TimeFormatters.formatTimer(30_000))
        assertEquals("05:00", TimeFormatters.formatTimer(5 * 60_000L))
        assertEquals("59:59", TimeFormatters.formatTimer(59 * 60_000L + 59_000))
    }

    @Test
    fun `formatTimer shows hhmmss for an hour or more`() {
        assertEquals("1:00:00", TimeFormatters.formatTimer(60 * 60_000L))
        assertEquals("1:01:00", TimeFormatters.formatTimer(61 * 60_000L))
        assertEquals("10:00:00", TimeFormatters.formatTimer(10 * 3_600_000L))
    }

    @Test
    fun `formatTimer rounds up to the nearest second`() {
        assertEquals("00:01", TimeFormatters.formatTimer(1))
        assertEquals("01:00", TimeFormatters.formatTimer(59_001))
    }

    @Test
    fun `formatStopwatch shows centiseconds`() {
        assertEquals("00:00.00", TimeFormatters.formatStopwatch(0))
        assertEquals("00:01.23", TimeFormatters.formatStopwatch(1_234))
        assertEquals("01:00.00", TimeFormatters.formatStopwatch(60_000))
    }

    @Test
    fun `formatStopwatch shows hours when needed`() {
        assertEquals("1:00:00.00", TimeFormatters.formatStopwatch(3_600_000))
    }

    @Test
    fun `formatStopwatch clamps negatives`() {
        assertEquals("00:00.00", TimeFormatters.formatStopwatch(-1))
    }

    @Test
    fun `parseDuration handles unit combinations`() {
        assertEquals(300_000L, TimeFormatters.parseDuration("5m"))
        assertEquals(4_830_000L, TimeFormatters.parseDuration("1h 20m 30s"))
        assertEquals(90_000L, TimeFormatters.parseDuration("90s"))
        assertEquals(300_000L, TimeFormatters.parseDuration("5 minute"))
        assertEquals(300_000L, TimeFormatters.parseDuration("5 minutes"))
        assertEquals(7_200_000L, TimeFormatters.parseDuration("2 hours"))
        assertEquals(5_400_000L, TimeFormatters.parseDuration("1h30m"))
    }

    @Test
    fun `parseDuration rejects non-durations`() {
        assertNull(TimeFormatters.parseDuration("5"))
        assertNull(TimeFormatters.parseDuration("abc"))
        assertNull(TimeFormatters.parseDuration(""))
        assertNull(TimeFormatters.parseDuration("timer"))
        assertNull(TimeFormatters.parseDuration("-5m"))
    }

    @Test
    fun `parseDuration caps at max timer`() {
        assertEquals(TimeUtility.MAX_TIMER_MILLIS, TimeFormatters.parseDuration("1000000h"))
    }

    @Test
    fun `formatUtcOffset renders fixed offsets`() {
        val tokyoEpoch = LocalDate.of(2026, 9, 13).atStartOfDay(java.time.ZoneId.of("Asia/Tokyo"))
            .toInstant().toEpochMilli()
        assertEquals("UTC+9", TimeFormatters.formatUtcOffset(java.time.ZoneId.of("Asia/Tokyo"), tokyoEpoch))

        val kolkataEpoch = LocalDate.of(2026, 9, 13).atStartOfDay(java.time.ZoneId.of("Asia/Kolkata"))
            .toInstant().toEpochMilli()
        assertEquals("UTC+5:30", TimeFormatters.formatUtcOffset(java.time.ZoneId.of("Asia/Kolkata"), kolkataEpoch))
    }

    @Test
    fun `formatUtcOffset reflects daylight saving time`() {
        val ny = java.time.ZoneId.of("America/New_York")
        val summer = LocalDate.of(2026, 7, 1).atStartOfDay(ny).toInstant().toEpochMilli()
        val winter = LocalDate.of(2026, 1, 15).atStartOfDay(ny).toInstant().toEpochMilli()
        assertEquals("UTC-4", TimeFormatters.formatUtcOffset(ny, summer))
        assertEquals("UTC-5", TimeFormatters.formatUtcOffset(ny, winter))
    }
}