package com.zbrowser.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for ZBrowser.
 * Provides access to BookmarkDao and HistoryDao.
 * Uses singleton pattern to prevent multiple database instances.
 *
 * v4.0: Database version 2 — bookmarks primary key changed from autoGenerate ID
 * to URL-based primary key. Migration drops and recreates the bookmarks table.
 */
@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class ZBrowserDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ZBrowserDatabase? = null

        /**
         * Migration from v1 (autoGenerate ID primary key) to v2 (URL primary key).
         * Preserves existing bookmark data by migrating URL + title.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new bookmarks table with URL as primary key
                db.execSQL("""
                    CREATE TABLE bookmarks_new (
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        faviconUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(url)
                    )
                """.trimIndent())

                // Copy data from old table to new table
                db.execSQL("""
                    INSERT INTO bookmarks_new (url, title, faviconUrl, createdAt)
                    SELECT url, title, faviconUrl, createdAt FROM bookmarks
                """.trimIndent())

                // Drop old table and rename new table
                db.execSQL("DROP TABLE bookmarks")
                db.execSQL("ALTER TABLE bookmarks_new RENAME TO bookmarks")
            }
        }

        @JvmStatic
        val MIGRATIONS = arrayOf<Migration>(MIGRATION_1_2)

        fun getDatabase(context: Context): ZBrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZBrowserDatabase::class.java,
                    "zbrowser_database"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
