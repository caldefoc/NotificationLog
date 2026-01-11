package com.notificationlog

import android.service.notification.NotificationListenerServiceimport android.service.notification.StatusBarNotification
import android.util.Log

/**
 * This service is the core of the app. It gets registered with the Android system
 * and is automatically called every time a notification is posted or removed.
 */
class NotificationListener : NotificationListenerService() {

    /**
     * This function is triggered whenever a new notification is posted by any app.
     * @param sbn The notification object, which contains all the details.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        // The 'sbn' (StatusBarNotification) object can sometimes be null, so we check first.
        if (sbn != null) {
            // Extract the useful information from the notification.
            val packageName = sbn.packageName
            val title = sbn.notification.extras.getString("android.title")
            val text = sbn.notification.extras.getString("android.text")

            // For now, we print the captured data to the Logcat window.
            // This lets us confirm the listener is working before we build the database.
            // You can view this output in the "Logcat" tab in Android Studio.
            Log.d("NotificationLog", "Captured: App='${packageName}', Title='${title}', Text='${text}'")
        }
    }

    /**
     * This function is triggered when a notification is dismissed by the user or removed by the app.
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // We can add logic here later if needed, e.g., to update our UI.
        Log.d("NotificationLog", "A notification was removed.")
    }
}
