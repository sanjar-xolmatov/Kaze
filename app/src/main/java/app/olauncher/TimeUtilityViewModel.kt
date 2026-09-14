package app.olauncher

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.olauncher.data.Prefs
import app.olauncher.helper.timeutils.StopwatchEngine
import app.olauncher.helper.timeutils.TimerAlarmManager
import app.olauncher.helper.timeutils.TimerEngine

enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }
enum class StopwatchState { IDLE, RUNNING, PAUSED }

class TimeUtilityViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val timerEngine = TimerEngine { SystemClock.elapsedRealtime() }
    private val stopwatchEngine = StopwatchEngine { SystemClock.elapsedRealtime() }

    private val _timerState = MutableLiveData(TimerState.IDLE)
    val timerState: LiveData<TimerState> = _timerState

    private val _timerRemaining = MutableLiveData(0L)
    val timerRemaining: LiveData<Long> = _timerRemaining

    private val _stopwatchState = MutableLiveData(StopwatchState.IDLE)
    val stopwatchState: LiveData<StopwatchState> = _stopwatchState

    private val _stopwatchElapsed = MutableLiveData(0L)
    val stopwatchElapsed: LiveData<Long> = _stopwatchElapsed

    init {
        restoreState()
    }

    fun configureTimer(durationMillis: Long) {
        if (timerEngine.isRunning || timerEngine.hasRun) return
        timerEngine.configure(durationMillis)
        _timerState.value = TimerState.IDLE
        _timerRemaining.value = durationMillis
        prefs.timerDuration = durationMillis
    }

    fun startTimer() {
        when (_timerState.value) {
            TimerState.RUNNING -> return
            TimerState.FINISHED -> {
                timerEngine.reset()
                timerEngine.start()
            }
            else -> timerEngine.start()
        }
        _timerState.value = TimerState.RUNNING
        _timerRemaining.value = timerEngine.remaining()
        saveTimerState()
        TimerAlarmManager.scheduleTimer(getApplication(), timerEngine.remaining())
    }

    fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        timerEngine.pause()
        _timerState.value = TimerState.PAUSED
        _timerRemaining.value = timerEngine.remaining()
        saveTimerState()
        TimerAlarmManager.cancelTimer(getApplication())
    }

    fun resetTimer() {
        timerEngine.reset()
        _timerState.value = TimerState.IDLE
        _timerRemaining.value = 0L
        prefs.timerRunning = false
        prefs.timerFinished = false
        prefs.timerDuration = 0L
        prefs.timerPausedRemaining = 0L
        prefs.timerEndElapsed = 0L
        TimerAlarmManager.cancelTimer(getApplication())
    }

    fun tickTimer() {
        if (_timerState.value == TimerState.RUNNING) {
            val remaining = timerEngine.remaining()
            _timerRemaining.value = remaining
            if (remaining <= 0) finishTimer()
        }
    }

    fun finishTimer() {
        timerEngine.markFinished()
        _timerState.value = TimerState.FINISHED
        _timerRemaining.value = 0L
        prefs.timerRunning = false
        prefs.timerFinished = true
        prefs.timerEndElapsed = 0L
        prefs.timerPausedRemaining = 0L
        TimerAlarmManager.cancelTimer(getApplication())
    }

    fun dismissFinishedTimer() {
        if (_timerState.value != TimerState.FINISHED) return
        timerEngine.reset()
        _timerState.value = TimerState.IDLE
        _timerRemaining.value = 0L
        prefs.timerFinished = false
        prefs.timerDuration = 0L
    }

    fun startStopwatch() {
        when (_stopwatchState.value) {
            StopwatchState.IDLE, StopwatchState.PAUSED -> stopwatchEngine.start()
            else -> return
        }
        _stopwatchState.value = StopwatchState.RUNNING
        _stopwatchElapsed.value = stopwatchEngine.elapsed()
        prefs.stopwatchRunning = true
        prefs.stopwatchStartElapsed = stopwatchEngine.startElapsedRealtime
    }

    fun pauseStopwatch() {
        if (_stopwatchState.value != StopwatchState.RUNNING) return
        stopwatchEngine.pause()
        _stopwatchState.value = StopwatchState.PAUSED
        _stopwatchElapsed.value = stopwatchEngine.elapsed()
        prefs.stopwatchRunning = false
        prefs.stopwatchAccumulated = stopwatchEngine.accumulatedMillis
    }

    fun resetStopwatch() {
        stopwatchEngine.reset()
        _stopwatchState.value = StopwatchState.IDLE
        _stopwatchElapsed.value = 0L
        prefs.stopwatchRunning = false
        prefs.stopwatchAccumulated = 0L
        prefs.stopwatchStartElapsed = 0L
    }

    fun tickStopwatch() {
        if (_stopwatchState.value == StopwatchState.RUNNING) {
            _stopwatchElapsed.value = stopwatchEngine.elapsed()
        }
    }

    private fun saveTimerState() {
        prefs.timerRunning = timerEngine.isRunning
        prefs.timerEndElapsed = timerEngine.endElapsedRealtime
        prefs.timerPausedRemaining = timerEngine.pausedRemainingMillis
        prefs.timerDuration = timerEngine.durationMillis
    }

    private fun restoreState() {
        timerEngine.restore(
            endElapsedRealtime = prefs.timerEndElapsed,
            pausedRemainingMillis = prefs.timerPausedRemaining,
            isRunning = prefs.timerRunning && prefs.timerEndElapsed > 0,
            hasRun = prefs.timerDuration > 0,
            durationMillis = prefs.timerDuration,
        )
        _timerState.value = when {
            prefs.timerFinished -> TimerState.FINISHED
            prefs.timerRunning && prefs.timerEndElapsed > 0 -> TimerState.RUNNING
            prefs.timerDuration > 0 -> TimerState.PAUSED
            else -> TimerState.IDLE
        }
        _timerRemaining.value = timerEngine.remaining()

        stopwatchEngine.restore(
            accumulatedMillis = prefs.stopwatchAccumulated,
            startElapsedRealtime = prefs.stopwatchStartElapsed,
            isRunning = prefs.stopwatchRunning,
        )
        _stopwatchState.value = when {
            prefs.stopwatchRunning -> StopwatchState.RUNNING
            prefs.stopwatchAccumulated > 0 -> StopwatchState.PAUSED
            else -> StopwatchState.IDLE
        }
        _stopwatchElapsed.value = stopwatchEngine.elapsed()
    }
}