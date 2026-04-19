package com.notificationlog

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.notificationlog.data.NotificationDatabase
import com.notificationlog.model.NotificationItem
import com.notificationlog.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val repository by lazy {
        val database = Room.databaseBuilder(
            applicationContext,
            NotificationDatabase::class.java,
            "notification_database"
        ).build()
        NotificationRepository(database)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Listener connected")
        // Cancel any previous warning notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(2001)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w("NotificationListener", "Listener disconnected — notification access may have been revoked")

        // Try to request rebind on API 33+ (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                requestRebind(ComponentName(this, NotificationListener::class.java))
                Log.i("NotificationListener", "Requested rebind after disconnect")
            } catch (e: Exception) {
                Log.e("NotificationListener", "Rebind request failed: ${e.message}")
            }
        }

        // Show a high-priority warning notification
        showAccessRevokedNotification()

        // Notify the Activity (if it's running) so it can show a dialog
        val intent = Intent("com.notificationlog.LISTENER_DISCONNECTED")
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    private fun showAccessRevokedNotification() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create warning channel if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "warning_channel",
                    "Permission Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Warnings when notification access is lost"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }

            val activityIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, "warning_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Notification access lost!")
                .setContentText("Tap to re-enable notification capture")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(2001, notification)
        } catch (e: Exception) {
            Log.e("NotificationListener", "Failed to show warning notification: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationListener", "onNotificationPosted: ${sbn.packageName}")
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        val title = extras.getString("android.title") ?: ""
        val content = extras.getString("android.text") ?: ""
        val timestamp = sbn.postTime
        
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }

        val prefs = getSharedPreferences("com.notificationlog.prefs", Context.MODE_PRIVATE)
        val captureEnabled = prefs.getBoolean("capture_enabled", true)

        if (captureEnabled) {
            val notification = NotificationItem(
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                timestamp = timestamp,
                key = sbn.key
            )

            scope.launch {
                try {
                    repository.insert(notification)
                    val intent = Intent("com.notificationlog.NOTIFICATIONS_UPDATED")
                    intent.putExtra("last_captured", timestamp)
                    sendBroadcast(intent)
                    Log.d("NotificationListener", "Captured: $appName - $title")
                } catch (e: Exception) {
                    Log.e("NotificationListener", "Error inserting notification: ${e.message}", e)
                }
            }
        } else {
            Log.d("NotificationListener", "Capture disabled, ignoring notification from $appName")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d("NotificationListener", "Notification removed: ${sbn.key}")
    }

    private fun cancelNotificationByKey(key: String) {
        try {
            cancelNotification(key)
        } catch (e: SecurityException) {
            Log.e("NotificationListener", "Cannot cancel notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
