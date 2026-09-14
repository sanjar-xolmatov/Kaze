package app.olauncher.helper.timeutils

import java.time.LocalDate

sealed class TimeUtility {
    data class Timer(val durationMillis: Long) : TimeUtility()
    object Stopwatch : TimeUtility()
    object Now : TimeUtility()
    data class DayOfWeek(val date: LocalDate) : TimeUtility()
    data class DaysBetween(val from: LocalDate, val to: LocalDate) : TimeUtility()
    data class DaysUntil(val date: LocalDate) : TimeUtility()
    data class RelativeDate(val date: LocalDate, val label: String) : TimeUtility()
    data class TimeIn(val city: String, val zoneId: String) : TimeUtility()

    companion object {
        const val DEFAULT_TIMER_MILLIS = 5 * 60_000L
        const val MAX_TIMER_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}
