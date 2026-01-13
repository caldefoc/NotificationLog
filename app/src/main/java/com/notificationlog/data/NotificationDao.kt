package com.notificationlog.data

import androidx.room.*
import com.notificationlog.model.NotificationItem

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getAll(): List<NotificationItem>
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    suspend fun getNotificationsSortedByDate(): List<NotificationItem>
    
    @Query("SELECT * FROM notifications ORDER BY appName ASC, timestamp DESC")
    suspend fun getNotificationsSortedByApp(): List<NotificationItem>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationItem)
    
    @Delete
    suspend fun delete(notification: NotificationItem)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()
}