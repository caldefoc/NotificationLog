package com.notificationlog.repository

import com.notificationlog.data.NotificationDatabase
import com.notificationlog.model.NotificationItem

class NotificationRepository(private val database: NotificationDatabase) {

    suspend fun getAllNotifications() = database.notificationDao().getAll()

    suspend fun getNotificationsSortedByDate() =
        database.notificationDao().getNotificationsSortedByDate()

    suspend fun getNotificationsSortedByApp() =
        database.notificationDao().getNotificationsSortedByApp()

    suspend fun latestForKey(key: String) = database.notificationDao().latestForKey(key)

    suspend fun insert(notification: NotificationItem) {
        database.notificationDao().insert(notification)
    }

    suspend fun bumpUpdateCount(id: Long, lastUpdated: Long) {
        database.notificationDao().bumpUpdateCount(id, lastUpdated)
    }

    suspend fun delete(notification: NotificationItem) {
        database.notificationDao().delete(notification)
    }

    suspend fun deleteAll() {
        database.notificationDao().deleteAll()
    }
}
