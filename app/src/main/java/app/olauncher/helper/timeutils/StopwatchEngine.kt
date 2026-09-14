package app.olauncher.helper.timeutils

import android.os.SystemClock

class StopwatchEngine(private val timeSource: () -> Long = { SystemClock.elapsedRealtime() }) {

    var accumulatedMillis: Long = 0
        private set

    var startElapsedRealtime: Long = 0
        private set

    var isRunning: Boolean = false
        private set

    fun start() {
        startElapsedRealtime = timeSource()
        isRunning = true
    }

    fun pause() {
        if (!isRunning) return
        accumulatedMillis += timeSource() - startElapsedRealtime
        isRunning = false
    }

    fun reset() {
        isRunning = false
        accumulatedMillis = 0
        startElapsedRealtime = 0
    }

    fun elapsed(): Long {
        return if (isRunning) {
            accumulatedMillis + (timeSource() - startElapsedRealtime)
        } else {
            accumulatedMillis
        }
    }

    fun restore(
        accumulatedMillis: Long,
        startElapsedRealtime: Long,
        isRunning: Boolean,
    ) {
        this.accumulatedMillis = accumulatedMillis
        this.startElapsedRealtime = startElapsedRealtime
        this.isRunning = isRunning
    }
}
