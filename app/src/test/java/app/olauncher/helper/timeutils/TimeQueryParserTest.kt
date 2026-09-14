package app.olauncher.helper.timeutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TimeQueryParserTest {

    private val today = LocalDate.of(2026, 9, 13)

    private fun parse(query: String) = TimeQueryParser.parse(query, today)

    @Test
    fun `bare timer defaults to five minutes`() {
        assertEquals(TimeUtility.Timer(5 * 60_000L), parse("timer"))
        assertEquals(TimeUtility.Timer(5 * 60_000L), parse("set timer"))
        assertEquals(TimeUtility.Timer(5 * 60_000L), parse("  timer  "))
    }

    @Test
    fun `timer with explicit units`() {
        assertEquals(TimeUtility.Timer(300_000L), parse("timer 5m"))
        assertEquals(TimeUtility.Timer(5_400_000L), parse("timer 90m"))
        assertEquals(TimeUtility.Timer(300_000L), parse("timer 5 minute"))
        assertEquals(TimeUtility.Timer(300_000L), parse("timer 5 minutes"))
        assertEquals(TimeUtility.Timer(4_830_000L), parse("timer 1h 20m 30s"))
        assertEquals(TimeUtility.Timer(3600_000L), parse("set timer 1 hour"))
    }

    @Test
    fun `timer with bare number means minutes`() {
        assertEquals(TimeUtility.Timer(300_000L), parse("timer 5"))
        assertEquals(TimeUtility.Timer(TimeUtility.MAX_TIMER_MILLIS), parse("timer 10080"))
    }

    @Test
    fun `timer with at-prefixed number means minutes`() {
        assertEquals(TimeUtility.Timer(300_000L), parse("timer @5"))
    }

    @Test
    fun `invalid timer input falls through`() {
        assertNull(parse("timer abc"))
        assertNull(parse("timer -5m"))
        assertNull(parse("timer 5mish"))
        assertNull(parse("timers"))
    }

    @Test
    fun `stopwatch variants`() {
        assertEquals(TimeUtility.Stopwatch, parse("stopwatch"))
        assertEquals(TimeUtility.Stopwatch, parse("stop watch"))
        assertEquals(TimeUtility.Stopwatch, parse("start stopwatch"))
    }

    @Test
    fun `now phrases`() {
        for (query in listOf("time", "date", "today", "now", "current time", "current date", "what time is it", "what day is it")) {
            assertEquals("'$query' should parse to Now", TimeUtility.Now, parse(query))
        }
    }

    @Test
    fun `clock and bare weekdays are not utilities`() {
        assertNull(parse("clock"))
        assertNull(parse("monday"))
        assertNull(parse("sunday"))
        assertNull(parse(""))
        assertNull(parse("   "))
    }

    @Test
    fun `day of week for a date`() {
        assertEquals(
            TimeUtility.DayOfWeek(LocalDate.of(2026, 12, 25)),
            parse("day of week 2026-12-25")
        )
        assertEquals(
            TimeUtility.DayOfWeek(LocalDate.of(2026, 12, 25)),
            parse("day of week 2026/12/25")
        )
        assertEquals(
            TimeUtility.DayOfWeek(today.plusDays(1)),
            parse("day of week tomorrow")
        )
        assertEquals(
            TimeUtility.DayOfWeek(today.minusDays(1)),
            parse("weekday yesterday")
        )
    }

    @Test
    fun `days between numeric dates`() {
        val result = parse("days between 2026-09-13 and 2026-12-06") as? TimeUtility.DaysBetween
        assertNotNull(result)
        assertEquals(84L, TimeFormatters.daysBetween(result!!.from, result.to))
        assertEquals(LocalDate.of(2026, 9, 13), result.from)
        assertEquals(LocalDate.of(2026, 12, 6), result.to)
    }

    @Test
    fun `days between supports relative endpoints`() {
        val result = parse("days between yesterday and tomorrow") as? TimeUtility.DaysBetween
        assertNotNull(result)
        assertEquals(2L, TimeFormatters.daysBetween(result!!.from, result.to))
    }

    @Test
    fun `days until a date`() {
        val result = parse("days until 2026-12-06") as? TimeUtility.DaysUntil
        assertNotNull(result)
        assertEquals(LocalDate.of(2026, 12, 6), result!!.date)
    }

    @Test
    fun `days until unknown date falls through`() {
        assertNull(parse("days until soon"))
    }

    @Test
    fun `tomorrow and yesterday`() {
        assertEquals(
            TimeUtility.RelativeDate(today.plusDays(1), "Tomorrow"),
            parse("tomorrow")
        )
        assertEquals(
            TimeUtility.RelativeDate(today.minusDays(1), "Yesterday"),
            parse("yesterday")
        )
    }

    @Test
    fun `next weekday is upcoming occurrence`() {
        // today is Sunday 2026-09-13
        assertEquals(LocalDate.of(2026, 9, 14), (parse("next monday") as TimeUtility.RelativeDate).date)
        assertEquals(LocalDate.of(2026, 9, 20), (parse("next sunday") as TimeUtility.RelativeDate).date)
        assertEquals(LocalDate.of(2026, 9, 18), (parse("next fri") as TimeUtility.RelativeDate).date)
    }

    @Test
    fun `this weekday is same or nearest upcoming`() {
        assertEquals(LocalDate.of(2026, 9, 13), (parse("this sunday") as TimeUtility.RelativeDate).date)
        assertEquals(LocalDate.of(2026, 9, 14), (parse("this monday") as TimeUtility.RelativeDate).date)
    }

    @Test
    fun `last weekday is most recent past occurrence`() {
        assertEquals(LocalDate.of(2026, 9, 6), (parse("last sunday") as TimeUtility.RelativeDate).date)
        assertEquals(LocalDate.of(2026, 9, 7), (parse("last monday") as TimeUtility.RelativeDate).date)
    }

    @Test
    fun `relative date with unknown weekday falls through`() {
        assertNull(parse("next foobar"))
        assertNull(parse("hello monday"))
    }

    @Test
    fun `time in known city`() {
        assertEquals(
            TimeUtility.TimeIn("Tokyo", "Asia/Tokyo"),
            parse("time in tokyo")
        )
        assertEquals(
            TimeUtility.TimeIn("New York", "America/New_York"),
            parse("time in New York")
        )
        assertEquals(
            TimeUtility.TimeIn("London", "Europe/London"),
            parse("time in london")
        )
    }

    @Test
    fun `time in unknown city falls through`() {
        assertNull(parse("time in atlantis"))
        assertNull(parse("time in"))
    }
}