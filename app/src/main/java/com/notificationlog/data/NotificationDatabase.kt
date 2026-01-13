package com.notificationlog.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notificationlog.model.NotificationItem

@Database(entities = [NotificationItem::class], version = 1, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}