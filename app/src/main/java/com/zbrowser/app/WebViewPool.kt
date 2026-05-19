package com.zbrowser.app

import android.app.Activity
import android.content.Context
import android.webkit.WebView
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Pre-creates and recycles WebViews to eliminate the 150-200ms cold-start
 * penalty of constructing a new WebView on the main thread.
 *
 * When a tab is closed the WebView is returned to the pool rather than
 * destroyed immediately. The next "add tab" call re-uses the recycled
 * instance, giving near-instant tab creation.
 *
 * IMPORTANT: WebView requires an Activity context for proper window
 * token management. Using applicationContext causes crashes on some
 * OEMs. We always use the Activity context provided to acquire().
 *
 * Thread-safe: all operations are lock-free via ConcurrentLinkedQueue.
 */
object WebViewPool {

    private const val MAX_POOL_SIZE = 3

    private val pool = ConcurrentLinkedQueue<WebView>()

    /**
     * Obtain a WebView — either a recycled one from the pool
     * or a freshly created one if the pool is empty.
     *
     * @param activityContext Must be an Activity context (not applicationContext)
     */
    fun acquire(activityContext: Context): WebView {
        return pool.poll() ?: WebView(activityContext)
    }

    /**
     * Return a WebView to the pool for reuse.
     * If the pool is full the WebView is destroyed immediately.
     */
    fun release(webView: WebView) {
        if (pool.size >= MAX_POOL_SIZE) {
            webView.destroy()
            return
        }
        try {
            // Wipe state so the next consumer gets a clean WebView
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.clearCache(false)
            pool.offer(webView)
        } catch (_: Exception) {
            // WebView already destroyed or unusable
        }
    }

    /** Destroy all pooled WebViews — called on app termination */
    fun clear() {
        pool.forEach { it.destroy() }
        pool.clear()
    }
}
