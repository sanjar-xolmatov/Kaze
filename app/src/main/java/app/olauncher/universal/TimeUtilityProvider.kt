package app.olauncher.universal

import app.olauncher.helper.timeutils.TimeQueryParser
import app.olauncher.helper.timeutils.TimeUtility

/**
 * Time/date/timer utility provider. Delegates parsing to the existing
 * [TimeQueryParser]; Universal Search never re-implements date math.
 */
object TimeUtilityProvider : SearchProvider {

    const val PRIORITY = 30

    override fun search(query: String): List<SearchResult> {
        val utility = parse(query) ?: return emptyList()
        val type = when (utility) {
            is TimeUtility.Timer -> SearchType.TIMER
            is TimeUtility.Stopwatch -> SearchType.STOPWATCH
            else -> SearchType.UTILITY
        }
        return listOf(
            SearchResult(
                type = type,
                weight = PRIORITY,
                title = "Time utility",
                payload = utility,
            )
        )
    }

    fun parse(query: String): TimeUtility? = TimeQueryParser.parse(query)

    /** Utility types rendered as a result row in the list. */
    fun isDisplayType(utility: TimeUtility?): Boolean =
        utility is TimeUtility.Now ||
            utility is TimeUtility.DayOfWeek ||
            utility is TimeUtility.DaysBetween ||
            utility is TimeUtility.DaysUntil ||
            utility is TimeUtility.RelativeDate ||
            utility is TimeUtility.TimeIn

    /** Utility types whose value changes over time and needs a UI ticker. */
    fun needsTicker(utility: TimeUtility?): Boolean =
        when (utility) {
            is TimeUtility.Timer,
            is TimeUtility.Stopwatch,
            is TimeUtility.Now,
            is TimeUtility.TimeIn,
            -> true

            else -> false
        }
}