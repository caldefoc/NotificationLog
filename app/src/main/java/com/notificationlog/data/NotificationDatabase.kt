package com.notificationlog.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notificationlog.model.NotificationItem

@Database(entities = [NotificationItem::class], version = 2, exportSchema = false)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        /**
         * v1 -> v2: de-duplication support plus indices for the per-key lookup that
         * every incoming notification now performs.
         *
         * Existing duplicate rows are deliberately left in place — collapsing them
         * would rewrite the user's history. Only new traffic is affected.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN updateCount INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE notifications ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE notifications SET lastUpdated = timestamp WHERE lastUpdated = 0"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_key ON notifications(key)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notifications_timestamp " +
                        "ON notifications(timestamp)"
                )
            }
        }
    }
}
