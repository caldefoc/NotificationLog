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
import com.notificationlog.model.NotificationItem
import com.notificationlog.repository.NotificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationListener : NotificationListenerService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // The read-then-write de-duplication below is not atomic. Two identical
    // notifications arriving back to back both read "no existing row" and both
    // insert — observed on device as two rows sharing a key with updateCount 1.
    private val insertMutex = Mutex()

    // The Application owns the only Room instance. This used to build a second
    // database connection to the same file, which meant no shared invalidation
    // and avoidable lock contention.
    private val repository: NotificationRepository
        get() = NotificationApp.repository

    companion object {
        const val ACTION_UPDATED = "com.notificationlog.NOTIFICATIONS_UPDATED"
        private const val TAG = "NotificationListener"
        private const val PREFS = "com.notificationlog.prefs"
        private const val KEY_CAPTURE_ENABLED = "capture_enabled"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        ListenerConnection.setConnected(true)
        Log.d("NotificationListener", "Listener connected")
        // Cancel any previous warning notification
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(2001)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ListenerConnection.setConnected(false)
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
        val pkg = sbn.packageName

        // Never record our own notifications (the "capture is running" status and the
        // "access lost" warning). Without this the app logs itself on every toggle.
        if (pkg == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val content = extras.getString("android.text") ?: ""

        // Group summaries and progress stubs carry nothing worth keeping.
        if (title.isBlank() && content.isBlank()) {
            Log.d(TAG, "Skipping empty notification from $pkg")
            return
        }

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_CAPTURE_ENABLED, true)) {
            Log.d(TAG, "Capture disabled, ignoring notification from $pkg")
            return
        }

        val key = sbn.key
        val now = System.currentTimeMillis()

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) {
            pkg
        }

        scope.launch {
            insertMutex.withLock {
                try {
                    val latest = if (key != null) repository.latestForKey(key) else null

                    if (latest != null && latest.title == title && latest.content == content) {
                        // Same key, same content: an ongoing notification refreshing itself.
                        // Bump the counter instead of appending a duplicate row.
                        repository.bumpUpdateCount(latest.id, now)
                        Log.d(TAG, "Refreshed existing entry: $appName x${latest.updateCount + 1}")
                    } else {
                        repository.insert(
                            NotificationItem(
                                packageName = pkg,
                                appName = appName,
                                title = title,
                                content = content,
                                timestamp = if (sbn.postTime > 0) sbn.postTime else now,
                                key = key,
                                lastUpdated = now
                            )
                        )
                        Log.d(TAG, "Captured: $appName - $title")
                    }

                    sendBroadcast(
                        Intent(ACTION_UPDATED)
                            .setPackage(packageName)
                            .putExtra("last_captured", now)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error recording notification: ${e.message}", e)
                }
            }
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
            Log.e(TAG, "Cannot cancel notification: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ListenerConnection.setConnected(false)
        job.cancel()
    }
}
