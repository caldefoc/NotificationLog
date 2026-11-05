package com.notificationlog.data

import androidx.room.*
import com.notificationlog.model.NotificationItem

@Dao
interface NotificationDao {

    // id DESC is a stable tie-break: notifications posted in the same millisecond
    // (or re-posted with an unchanged postTime) would otherwise shuffle between loads.
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC, id DESC")
    suspend fun getAll(): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC, id DESC")
    suspend fun getNotificationsSortedByDate(): List<NotificationItem>

    @Query("SELECT * FROM notifications ORDER BY appName ASC, timestamp DESC, id DESC")
    suspend fun getNotificationsSortedByApp(): List<NotificationItem>

    /** Newest row for a notification key, used to detect re-posts of the same content. */
    @Query("SELECT * FROM notifications WHERE key = :key ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun latestForKey(key: String): NotificationItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationItem)

    @Query("UPDATE notifications SET updateCount = updateCount + 1, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun bumpUpdateCount(id: Long, lastUpdated: Long)

    @Delete
    suspend fun delete(notification: NotificationItem)

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}
