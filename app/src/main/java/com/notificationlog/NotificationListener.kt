package com.notificationlog

import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("NotificationListener", "Listener disconnected")
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
