package com.notificationlog

import android.app.Application
import androidx.room.Room
import com.notificationlog.data.NotificationDatabase
import com.notificationlog.repository.NotificationRepository

class NotificationApp : Application() {
    
    companion object {
        lateinit var repository: NotificationRepository
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room database. This is the single instance for the whole process —
        // NotificationListener used to build a second one on the same file.
        val database = Room.databaseBuilder(
            applicationContext,
            NotificationDatabase::class.java,
            "notification_database"
        ).addMigrations(NotificationDatabase.MIGRATION_1_2).build()
        
        // Initialize repository
        repository = NotificationRepository(database)
    }
}