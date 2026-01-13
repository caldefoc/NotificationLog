package com.notificationlog

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.notificationlog.model.NotificationItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        
        val title = extras.getString("android.title") ?: ""
        val content = extras.getString("android.text") ?: ""
        val timestamp = sbn.postTime
        
        // Get app name
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (e: Exception) {
            packageName
        }

        val notification = NotificationItem(
            packageName = packageName,
            appName = appName,
            title = title,
            content = content,
            timestamp = timestamp,
            key = sbn.key // This is the notification key for cancellation
        )

        // Save to database
        CoroutineScope(Dispatchers.IO).launch {
            NotificationApp.repository.insert(notification)
        }

        Log.d("NotificationListener", "Captured: $appName - $title")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d("NotificationListener", "Notification removed: ${sbn.key}")
    }

    // If you need to cancel a notification manually, use this method
    // It calls the parent class's cancelNotification method
    private fun cancelNotificationByKey(key: String) {
        try {
            cancelNotification(key) // This is the correct method name
        } catch (e: SecurityException) {
            Log.e("NotificationListener", "Cannot cancel notification: ${e.message}")
        }
    }
}