package app.olauncher.helper.timeutils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object TimerAlarmManager {

    private const val REQUEST_CODE = 1001
    const val ACTION_TIMER_FINISHED = "app.olauncher.action.TIMER_FINISHED"

    fun scheduleTimer(context: Context, remainingMillis: Long) {
        if (remainingMillis <= 0) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        val triggerAt = System.currentTimeMillis() + remainingMillis
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelTimer(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = ACTION_TIMER_FINISHED
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}