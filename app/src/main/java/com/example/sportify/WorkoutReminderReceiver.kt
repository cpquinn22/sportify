package com.example.sportify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("Reminder", "WorkoutReminderReceiver triggered")

        val channelId = "workout_reminder_channel"
        val notificationId = 101

       // Ensure Notification Permission is Granted Before Sending
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                Log.e("WorkoutReminderReceiver", "❌ Notification permission not granted. Cannot send notification!")
                return
            }
        }

        // Create Notification Channel (For Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily workout reminder notifications"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("🏋️ Workout Reminder")
            .setContentText("Time to log your workout! 💪")
            .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists in res/drawable
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Show the notification
        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, notification)
        }
    }
}