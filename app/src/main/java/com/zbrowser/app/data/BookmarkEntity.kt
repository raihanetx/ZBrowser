package com.zbrowser.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a bookmark in the database.
 *
 * v4.0 FIX: URL is now the primary key (was autoGenerate ID + unique URL index).
 * This prevents the delete-and-reinsert issue when bookmarking the same URL,
 * and makes OnConflictStrategy.REPLACE work correctly by URL.
 * The auto-generated ID was never used as a foreign key reference.
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val url: String,
    val title: String,
    val faviconUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
