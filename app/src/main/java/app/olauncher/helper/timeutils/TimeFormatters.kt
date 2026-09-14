package app.olauncher.helper.timeutils

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

object TimeFormatters {

    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE
    private val slashDate = DateTimeFormatter.ofPattern("uuuu/M/d")
    private val dotDate = DateTimeFormatter.ofPattern("uuuu.M.d")

    fun parseDate(text: String): LocalDate? {
        return tryParse(isoDate, text)
            ?: tryParse(slashDate, text)
            ?: tryParse(dotDate, text)
    }

    private fun tryParse(formatter: DateTimeFormatter, text: String): LocalDate? {
        return try {
            LocalDate.parse(text, formatter)
        } catch (_: Exception) {
            null
        }
    }

    fun dayName(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())

    fun fullDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))

    fun mediumDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))

    fun daysBetween(from: LocalDate, to: LocalDate): Long =
        ChronoUnit.DAYS.between(from, to)

    fun formatTimer(millis: Long): String {
        val totalSeconds = (millis + 999) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    fun formatStopwatch(millis: Long): String {
        val clamped = millis.coerceAtLeast(0)
        val cs = (clamped / 10) % 100
        val totalSeconds = clamped / 1000
        val s = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val m = totalMinutes % 60
        val h = totalMinutes / 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d.%02d", h, m, s, cs)
        else String.format(Locale.US, "%02d:%02d.%02d", m, s, cs)
    }

    fun parseDuration(text: String): Long? {
        val normalized = text.trim().lowercase(Locale.US)
        if (normalized.isBlank() || normalized.startsWith("-")) return null

        var totalMs = 0L
        var remaining = normalized
        var found = false

        val patterns = listOf(
            Regex("(\\d+)\\s*h(?:our)?s?") to 3_600_000L,
            Regex("(\\d+)\\s*m(?:in(?:ute)?)?s?") to 60_000L,
            Regex("(\\d+)\\s*s(?:ec(?:ond)?)?s?") to 1_000L,
        )

        for ((regex, factor) in patterns) {
            regex.findAll(remaining).forEach { match ->
                totalMs += match.groupValues[1].toLong() * factor
                found = true
            }
            remaining = regex.replace(remaining, "")
        }

        return if (found && remaining.trim().isEmpty()) totalMs.coerceAtMost(TimeUtility.MAX_TIMER_MILLIS) else null
    }

    fun formatUtcOffset(zoneId: ZoneId, epochMillis: Long): String {
        val tz = java.util.TimeZone.getTimeZone(zoneId)
        val offsetMillis = tz.getOffset(epochMillis)
        val offsetMinutes = offsetMillis / 60000
        val absMinutes = kotlin.math.abs(offsetMinutes)
        val h = absMinutes / 60
        val m = absMinutes % 60
        val sign = if (offsetMinutes >= 0) "+" else "-"
        return if (m == 0) "UTC$sign$h" else "UTC$sign$h:$m"
    }

    fun zoneAbbreviation(zoneId: ZoneId, epochMillis: Long): String {
        val tz = java.util.TimeZone.getTimeZone(zoneId)
        val name = tz.getDisplayName(
            tz.inDaylightTime(Date(epochMillis)),
            java.util.TimeZone.SHORT
        )
        return if (name.startsWith("GMT") || name.startsWith("+") || name.startsWith("-")) "" else name
    }
}
