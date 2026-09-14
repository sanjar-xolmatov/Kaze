package app.olauncher.helper.timeutils

import android.os.SystemClock

class TimerEngine(private val timeSource: () -> Long = { SystemClock.elapsedRealtime() }) {

    var endElapsedRealtime: Long = 0
        private set

    var pausedRemainingMillis: Long = 0
        private set

    var isRunning: Boolean = false
        private set

    var hasRun: Boolean = false
        private set

    var durationMillis: Long = 0
        private set

    fun configure(durationMillis: Long) {
        if (isRunning || hasRun) return
        this.durationMillis = durationMillis.coerceAtMost(TimeUtility.MAX_TIMER_MILLIS)
        this.pausedRemainingMillis = this.durationMillis
    }

    fun start() {
        val remaining = if (pausedRemainingMillis > 0) pausedRemainingMillis else durationMillis
        if (remaining <= 0) return
        endElapsedRealtime = timeSource() + remaining
        isRunning = true
        hasRun = true
    }

    fun pause() {
        if (!isRunning) return
        pausedRemainingMillis = remaining()
        isRunning = false
    }

    fun reset() {
        isRunning = false
        hasRun = false
        endElapsedRealtime = 0
        pausedRemainingMillis = 0
        durationMillis = 0
    }

    fun remaining(): Long {
        if (!isRunning) return pausedRemainingMillis
        val diff = endElapsedRealtime - timeSource()
        return if (diff > 0) diff else 0
    }

    fun markFinished() {
        endElapsedRealtime = 0
        isRunning = false
        pausedRemainingMillis = 0
    }

    fun restore(
        endElapsedRealtime: Long,
        pausedRemainingMillis: Long,
        isRunning: Boolean,
        hasRun: Boolean,
        durationMillis: Long,
    ) {
        this.endElapsedRealtime = endElapsedRealtime
        this.pausedRemainingMillis = pausedRemainingMillis
        this.isRunning = isRunning
        this.hasRun = hasRun
        this.durationMillis = durationMillis
    }
}
