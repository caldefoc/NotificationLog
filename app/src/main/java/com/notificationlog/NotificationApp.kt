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
        
        // Initialize Room database
        val database = Room.databaseBuilder(
            applicationContext,
            NotificationDatabase::class.java,
            "notification_database"
        ).build()
        
        // Initialize repository
        repository = NotificationRepository(database)
    }
}