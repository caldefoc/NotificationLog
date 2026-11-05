package com.notificationlog

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
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

        // Apply the stored theme before any activity exists. Doing this in onCreate of
        // the activity makes AppCompat recreate it once per cold start, which showed the
        // first-run disclosure twice.
        val prefs = getSharedPreferences("com.notificationlog.prefs", MODE_PRIVATE)
        val dark = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

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