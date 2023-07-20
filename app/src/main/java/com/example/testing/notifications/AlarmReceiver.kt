package com.example.testing.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.testing.Graph
import com.example.testing.MainActivity
import com.example.testing.R
import com.example.testing.utils.Utils
import java.util.*

class AlarmReceiver: BroadcastReceiver() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "DailyQuestionnaireChannel"
        private const val NOTIFICATION_ID = 1
        private const val DAILY_REMINDER_HOUR = 18

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

            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else PendingIntent.FLAG_UPDATE_CURRENT

            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, flag)

            val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("OnReceive", "AlarmReceiver")

        if (!Utils.readSharedSettingBoolean("isQuestionnaireAnswered", false)) {
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        createNotificationChannel(context)
        Log.d("NotificationReceiver", "Shownotification")
        val appIntent = Intent(Graph.appContext, MainActivity::class.java)
        val retunIntent = PendingIntent.getActivity(Graph.appContext, 0, appIntent, PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle("Daily Questionnaire")
            .setContentText("Remember to answer your daily questionnaire")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(retunIntent)

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