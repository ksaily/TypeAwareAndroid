package com.example.testing.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.testing.R
import java.util.*

class AlarmReceiver: BroadcastReceiver() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "DailyQuestionnaireChannel"
        private const val NOTIFICATION_ID = 1
        private const val DAILY_REMINDER_HOUR = 22

        fun scheduleNotification(context: Context) {
            Log.d("ScheduleNotification", "created")
            val calendar = GregorianCalendar.getInstance().apply {
                if (get(Calendar.HOUR_OF_DAY) >= DAILY_REMINDER_HOUR) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }

                set(Calendar.HOUR_OF_DAY, DAILY_REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Ensure that receiver only triggered by the intended system
            Log.d("NotificationReceiver", "onReceive")
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        createNotificationChannel(context)

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Daily Questionnaire")
            .setContentText("Remember to answer your daily questionnaire")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        Log.d("NotificationReceiver", "showNotification")
    }

    private fun createNotificationChannel(context: Context) {
        Log.d("CreateNotificationChannel", "created")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Daily Questionnaire"
            val channelDescription = "Daily questionnaire reminder"
            val importance = NotificationManager.IMPORTANCE_DEFAULT

            val channel =
                NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance).apply {
                    description = channelDescription
                }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}