package com.zbrowser.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a bookmark in the database.
 * URL is the primary key since each URL can only be bookmarked once.
 */
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["url"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
