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

    /**
     * Free-text search over app, title and body. The pattern arrives already wrapped in
     * % and with LIKE metacharacters escaped, so typing "%" searches for a percent sign
     * rather than matching the whole table.
     */
    @Query(
        "SELECT * FROM notifications WHERE appName LIKE :pattern ESCAPE '\\' " +
            "OR title LIKE :pattern ESCAPE '\\' OR content LIKE :pattern ESCAPE '\\' " +
            "OR packageName LIKE :pattern ESCAPE '\\' " +
            "ORDER BY timestamp DESC, id DESC"
    )
    suspend fun searchByDate(pattern: String): List<NotificationItem>

    @Query(
        "SELECT * FROM notifications WHERE appName LIKE :pattern ESCAPE '\\' " +
            "OR title LIKE :pattern ESCAPE '\\' OR content LIKE :pattern ESCAPE '\\' " +
            "OR packageName LIKE :pattern ESCAPE '\\' " +
            "ORDER BY appName ASC, timestamp DESC, id DESC"
    )
    suspend fun searchByApp(pattern: String): List<NotificationItem>

    /** Newest row for a notification key, used to detect re-posts of the same content. */
    @Query("SELECT * FROM notifications WHERE key = :key ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun latestForKey(key: String): NotificationItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationItem)

    @Query("UPDATE notifications SET updateCount = updateCount + 1, lastUpdated = :lastUpdated WHERE id = :id")
    suspend fun bumpUpdateCount(id: Long, lastUpdated: Long)

    @Delete
    suspend fun delete(notification: NotificationItem)

    /**
     * Deletes every row the search queries would return, so "delete all matches" removes
     * exactly what the filtered list is showing. Returns the number of rows removed.
     */
    @Query(
        "DELETE FROM notifications WHERE appName LIKE :pattern ESCAPE '\\' " +
            "OR title LIKE :pattern ESCAPE '\\' OR content LIKE :pattern ESCAPE '\\' " +
            "OR packageName LIKE :pattern ESCAPE '\\'"
    )
    suspend fun deleteMatching(pattern: String): Int

    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}
