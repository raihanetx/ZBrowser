package com.zbrowser.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.room.Room
import com.zbrowser.app.BookmarkManager
import com.zbrowser.app.LegacyBookmarks
import com.zbrowser.app.ZBrowserApp
import com.zbrowser.app.data.BookmarkDao
import com.zbrowser.app.data.HistoryDao
import com.zbrowser.app.data.ZBrowserDatabase
import com.zbrowser.app.AdBlocker
import com.zbrowser.app.DownloadManagerHelper
import com.zbrowser.app.PermissionManager
import com.zbrowser.app.PopupBlocker
import com.zbrowser.app.TabManager
import com.zbrowser.app.SecurityUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module that provides singleton dependencies for the entire app.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    @LegacyBookmarks
    fun provideLegacyBookmarksPrefs(@ApplicationContext context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "secure_bookmarks",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ZBrowserDatabase {
        return ZBrowserDatabase.getDatabase(context)
    }

    @Provides
    fun provideBookmarkDao(database: ZBrowserDatabase): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideHistoryDao(database: ZBrowserDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    @Singleton
    fun provideAdBlocker(@ApplicationContext context: Context, prefs: SharedPreferences): AdBlocker {
        return AdBlocker(context, prefs)
    }

    @Provides
    @Singleton
    fun providePopupBlocker(prefs: SharedPreferences): PopupBlocker {
        return PopupBlocker(prefs)
    }

    @Provides
    @Singleton
    fun provideDownloadManagerHelper(@ApplicationContext context: Context): DownloadManagerHelper {
        return DownloadManagerHelper(context)
    }

    @Provides
    @Singleton
    fun providePermissionManager(activity: android.app.Activity): PermissionManager {
        return PermissionManager(activity)
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
