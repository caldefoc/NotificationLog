package com.notificationlog

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * This service is the core of the app. It's registered with the Android system
 * and is called whenever a notification is posted or removed.
 */
class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn != null) {
            val packageName = sbn.packageName
            val title = sbn.notification.extras.getString("android.title")
            val text = sbn.notification.extras.getString("android.text")

            Log.d("NotificationLog", "Captured: App='${packageName}', Title='${title}', Text='${text}'")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Log.d("NotificationLog", "A notification was removed.")
    }
}
