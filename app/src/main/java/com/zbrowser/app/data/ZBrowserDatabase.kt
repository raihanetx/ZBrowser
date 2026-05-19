package com.zbrowser.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for ZBrowser.
 * Provides access to BookmarkDao and HistoryDao.
 * Uses singleton pattern to prevent multiple database instances.
 */
@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ZBrowserDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ZBrowserDatabase? = null

        fun getDatabase(context: Context): ZBrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZBrowserDatabase::class.java,
                    "zbrowser_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
