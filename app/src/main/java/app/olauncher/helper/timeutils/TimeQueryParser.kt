package app.olauncher.helper.timeutils

import java.time.DayOfWeek as JDayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object TimeQueryParser {

    fun parse(query: String, today: LocalDate = LocalDate.now()): TimeUtility? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null

        return parseTimer(trimmed)
            ?: parseStopwatch(trimmed)
            ?: parseNow(trimmed)
            ?: parseDayOfWeek(trimmed, today)
            ?: parseDaysBetween(trimmed, today)
            ?: parseDaysUntil(trimmed, today)
            ?: parseRelativeDate(trimmed, today)
            ?: parseTimeIn(trimmed)
    }

    private fun parseTimer(text: String): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()

        if (lower == "timer" || lower == "set timer") {
            return TimeUtility.Timer(TimeUtility.DEFAULT_TIMER_MILLIS)
        }

        val timerPrefix = Regex("^(?:set\\s+)?timer\\s+(.+)$", RegexOption.IGNORE_CASE)
        val match = timerPrefix.find(lower) ?: return null
        val durationPart = match.groupValues[1].trim()

        if (durationPart.isBlank()) return TimeUtility.Timer(TimeUtility.DEFAULT_TIMER_MILLIS)

        val atNumber = Regex("^@(\\d+)$")
        atNumber.matchEntire(durationPart)?.let { m ->
            val minutes = m.groupValues[1].toLongOrNull() ?: return null
            if (minutes <= 0) return null
            val ms = minutes * 60_000L
            return if (ms <= TimeUtility.MAX_TIMER_MILLIS) TimeUtility.Timer(ms) else null
        }

        val bareNumber = durationPart.toLongOrNull()
        if (bareNumber != null && bareNumber > 0) {
            val ms = bareNumber * 60_000L
            return if (ms <= TimeUtility.MAX_TIMER_MILLIS) TimeUtility.Timer(ms) else null
        }

        val duration = TimeFormatters.parseDuration(durationPart)
        if (duration != null && duration > 0) return TimeUtility.Timer(duration)

        return null
    }

    private fun parseStopwatch(text: String): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()
        return when (lower) {
            "stopwatch", "stop watch", "start stopwatch", "start stop watch" -> TimeUtility.Stopwatch
            else -> null
        }
    }

    private fun parseNow(text: String): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()
        val nowPhrases = setOf(
            "time", "date", "today", "now",
            "current time", "current date",
            "what time is it", "what's the time", "whats the time",
            "what day is it", "what's the date", "whats the date",
            "what is the time", "what is the date",
            "current date and time", "date and time",
        )
        return if (lower in nowPhrases) TimeUtility.Now else null
    }

    private fun parseDayOfWeek(text: String, today: LocalDate): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()
        val prefix = Regex("^(?:day\\s+of\\s+week|weekday)\\s+(.+)$", RegexOption.IGNORE_CASE)
        val match = prefix.find(lower) ?: return null
        val datePart = match.groupValues[1].trim()

        TimeFormatters.parseDate(datePart)?.let { return TimeUtility.DayOfWeek(it) }

        when (datePart) {
            "tomorrow" -> return TimeUtility.DayOfWeek(today.plusDays(1))
            "yesterday" -> return TimeUtility.DayOfWeek(today.minusDays(1))
            "today" -> return TimeUtility.DayOfWeek(today)
        }

        parseRelativeDate(datePart, today)?.let {
            if (it is TimeUtility.RelativeDate) return TimeUtility.DayOfWeek(it.date)
        }

        return null
    }

    private fun parseDaysBetween(text: String, today: LocalDate): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()
        val regex = Regex("^days\\s+between\\s+(.+?)\\s+and\\s+(.+)$", RegexOption.IGNORE_CASE)
        val match = regex.find(lower) ?: return null

        val fromStr = match.groupValues[1].trim()
        val toStr = match.groupValues[2].trim()

        val from = resolveDate(fromStr, today) ?: return null
        val to = resolveDate(toStr, today) ?: return null

        return TimeUtility.DaysBetween(from, to)
    }

    private fun parseDaysUntil(text: String, today: LocalDate): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()
        val regex = Regex("^days\\s+until\\s+(.+)$", RegexOption.IGNORE_CASE)
        val match = regex.find(lower) ?: return null

        val dateStr = match.groupValues[1].trim()
        val date = resolveDate(dateStr, today) ?: return null

        return TimeUtility.DaysUntil(date)
    }

    private fun parseRelativeDate(text: String, today: LocalDate): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()

        when (lower) {
            "tomorrow" -> return TimeUtility.RelativeDate(today.plusDays(1), "Tomorrow")
            "yesterday" -> return TimeUtility.RelativeDate(today.minusDays(1), "Yesterday")
        }

        val relativePattern = Regex("^(next|this|last)\\s+(\\w+)$", RegexOption.IGNORE_CASE)
        val match = relativePattern.find(lower) ?: return null

        val direction = match.groupValues[1].lowercase(Locale.US)
        val dayName = match.groupValues[2]
        val dayOfWeek = parseDayOfWeekName(dayName) ?: return null

        return when (direction) {
            "next" -> {
                val candidate = today.with(TemporalAdjusters.next(dayOfWeek))
                TimeUtility.RelativeDate(candidate, TimeFormatters.dayName(candidate))
            }
            "this" -> {
                val candidate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                TimeUtility.RelativeDate(candidate, TimeFormatters.dayName(candidate))
            }
            "last" -> {
                var candidate = today.with(TemporalAdjusters.previousOrSame(dayOfWeek))
                if (candidate == today) candidate = candidate.minusDays(7)
                TimeUtility.RelativeDate(candidate, TimeFormatters.dayName(candidate))
            }
            else -> null
        }
    }

    private fun parseTimeIn(text: String): TimeUtility? {
        val lower = text.lowercase(Locale.US).trim()
        val regex = Regex("^time\\s+in\\s+(.+)$", RegexOption.IGNORE_CASE)
        val match = regex.find(lower) ?: return null

        val cityStr = match.groupValues[1].trim()
        if (cityStr.isBlank()) return null

        val cityZone = TimezoneRegistry.resolve(cityStr) ?: return null
        return TimeUtility.TimeIn(cityZone.city, cityZone.zoneId)
    }

    private fun resolveDate(text: String, today: LocalDate): LocalDate? {
        return when (text.lowercase(Locale.US)) {
            "today" -> today
            "tomorrow" -> today.plusDays(1)
            "yesterday" -> today.minusDays(1)
            else -> TimeFormatters.parseDate(text)
        }
    }

    private fun parseDayOfWeekName(name: String): JDayOfWeek? {
        return try {
            JDayOfWeek.valueOf(name.uppercase(Locale.US))
        } catch (_: Exception) {
            when (name.lowercase(Locale.US)) {
                "mon" -> JDayOfWeek.MONDAY
                "tue", "tues" -> JDayOfWeek.TUESDAY
                "wed" -> JDayOfWeek.WEDNESDAY
                "thu", "thur", "thurs" -> JDayOfWeek.THURSDAY
                "fri" -> JDayOfWeek.FRIDAY
                "sat" -> JDayOfWeek.SATURDAY
                "sun" -> JDayOfWeek.SUNDAY
                else -> null
            }
        }
    }
}
