package com.notificationlog.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * A single logged notification.
 *
 * [updateCount] tracks how many times an identical posting of the same notification
 * key was received. Apps that re-post the same ongoing notification (music players,
 * step counters, downloads) used to create one row per update; they now bump this
 * counter instead. A *different* title/content for the same key is still a new row,
 * so chat messages are never merged.
 */
@Parcelize
@Entity(
    tableName = "notifications",
    indices = [Index("key"), Index("timestamp")]
)
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val key: String? = null,
    val updateCount: Int = 1,
    val lastUpdated: Long = 0
) : Parcelable
