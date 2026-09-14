package app.olauncher.helper.timeutils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import app.olauncher.MainActivity
import app.olauncher.R
import app.olauncher.data.Prefs

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TimerAlarmManager.ACTION_TIMER_FINISHED) return

        val prefs = Prefs(context)
        prefs.timerRunning = false
        prefs.timerFinished = true
        prefs.timerEndElapsed = 0L

        showNotification(context)
    }

    private fun showNotification(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(context)

        val resultIntent = Intent(context, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(context.getString(R.string.timer_notification_title))
            .setContentText(
                context.getString(
                    R.string.timer_notification_text,
                    context.getString(R.string.app_name)
                )
            )
            .setAutoCancel(true)
            .setContentIntent(resultPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.timer_notification_channel),
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 2001
    }
}