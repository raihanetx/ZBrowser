package com.zbrowser.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Manages bookmarks storage and retrieval.
 * Uses SharedPreferences with JSON array for structured bookmark data.
 * Each bookmark has: url, title, timestamp.
 */
class SettingsRepository(context: Context) {

    private val bookmarksPrefs: SharedPreferences =
        context.getSharedPreferences("zbrowser_bookmarks", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BOOKMARKS = "bookmarks_json"
    }

    data class Bookmark(
        val url: String,
        val title: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    // --- Bookmarks ---

    fun addBookmark(url: String, title: String) {
        val bookmarks = getBookmarks().toMutableList()
        // Avoid duplicates
        bookmarks.removeAll { it.url == url }
        bookmarks.add(0, Bookmark(url, title))
        saveBookmarks(bookmarks)
    }

    fun removeBookmark(url: String) {
        val bookmarks = getBookmarks().toMutableList()
        bookmarks.removeAll { it.url == url }
        saveBookmarks(bookmarks)
    }

    fun getBookmarks(): List<Bookmark> {
        val json = bookmarksPrefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Bookmark(
                    url = obj.getString("url"),
                    title = obj.getString("title"),
                    timestamp = obj.optLong("timestamp", 0L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isBookmarked(url: String): Boolean {
        return getBookmarks().any { it.url == url }
    }

    private fun saveBookmarks(bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        for (b in bookmarks) {
            val obj = org.json.JSONObject()
            obj.put("url", b.url)
            obj.put("title", b.title)
            obj.put("timestamp", b.timestamp)
            arr.put(obj)
        }
        bookmarksPrefs.edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun clearBookmarks() {
        bookmarksPrefs.edit().remove(KEY_BOOKMARKS).apply()
    }

    // --- Browsing Data ---

    fun clearAllBrowsingData(context: Context) {
        clearBookmarks()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.WebStorage.getInstance().deleteAllData()
        android.webkit.WebView.clearClientCertPreferences(null)
        context.cacheDir.deleteRecursively()
    }
}
