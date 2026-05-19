package com.zbrowser.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages browser bookmarks with encrypted storage using EncryptedSharedPreferences.
 * Prevents unauthorized access to bookmark data on rooted devices.
 */
class BookmarkManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_bookmarks",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Add a bookmark. URL is the key, title is the value.
     * Overwrites existing bookmark for the same URL.
     */
    fun addBookmark(url: String, title: String) {
        if (url.isNotBlank()) {
            prefs.edit().putString(url, title).apply()
        }
    }

    /**
     * Remove a bookmark by URL.
     */
    fun removeBookmark(url: String) {
        prefs.edit().remove(url).apply()
    }

    /**
     * Check if a URL is bookmarked.
     */
    fun isBookmarked(url: String): Boolean {
        return prefs.contains(url)
    }

    /**
     * Get the title for a bookmarked URL, or null if not bookmarked.
     */
    fun getBookmarkTitle(url: String): String? {
        return prefs.getString(url, null)
    }

    /**
     * Get all bookmarks as a list of (title, url) pairs, sorted by title.
     */
    fun getAllBookmarks(): List<Pair<String, String>> {
        return prefs.all.map { (url, title) ->
            (title as? String ?: url) to url
        }.sortedBy { it.first.lowercase() }
    }

    /**
     * Clear all bookmarks.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
