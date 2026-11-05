package com.notificationlog.repository

import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
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

    /**
     * Collapses rows that share package, key, title and content into one row whose
     * updateCount is the number of occurrences. Returns the number of rows removed.
     *
     * This is a repair action for history written before de-duplication existed. The
     * grouping is staged in a scratch table because a correlated subquery per row over
     * 30k rows is far too slow on a phone; the scratch table has about one row per
     * distinct notification, so every lookup is an index probe.
     */
    suspend fun compactDuplicates(): Int = database.withTransaction {
        val db = database.openHelper.writableDatabase
        val before = countRows(db)

        db.execSQL("DROP TABLE IF EXISTS compact_tmp")
        db.execSQL(
            """
            CREATE TABLE compact_tmp AS
            SELECT MIN(id) AS keep_id, COUNT(*) AS cnt, MAX(lastUpdated) AS last_upd
            FROM notifications
            GROUP BY packageName, IFNULL(key, ''), title, content
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS compact_tmp_keep ON compact_tmp(keep_id)")

        db.execSQL(
            """
            UPDATE notifications SET
              updateCount = (SELECT cnt FROM compact_tmp WHERE keep_id = notifications.id),
              lastUpdated = (SELECT last_upd FROM compact_tmp WHERE keep_id = notifications.id)
            WHERE id IN (SELECT keep_id FROM compact_tmp)
            """.trimIndent()
        )
        db.execSQL(
            """
            DELETE FROM notifications
            WHERE NOT EXISTS (SELECT 1 FROM compact_tmp WHERE keep_id = notifications.id)
            """.trimIndent()
        )
        db.execSQL("DROP TABLE IF EXISTS compact_tmp")

        before - countRows(db)
    }

    private fun countRows(db: SupportSQLiteDatabase): Int =
        db.query("SELECT COUNT(*) FROM notifications").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
}
