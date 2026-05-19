package com.zbrowser.app

import android.content.Context
import android.webkit.WebView
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Pre-creates and recycles WebViews to eliminate the 150-200ms cold-start
 * penalty of constructing a new WebView on the main thread.
 *
 * On app launch the pool is seeded with one WebView; when a tab is closed
 * the WebView is returned to the pool rather than destroyed immediately.
 * The next "add tab" call re-uses the recycled instance, giving
 * near-instant tab creation.
 *
 * Thread-safe: all operations are lock-free via ConcurrentLinkedQueue.
 */
object WebViewPool {

    private const val MAX_POOL_SIZE = 3

    private val pool = ConcurrentLinkedQueue<WebView>()
    private var appContext: Context? = null

    /** Must be called once in Application.onCreate() */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Obtain a WebView — either a recycled one from the pool
     * or a freshly created one if the pool is empty.
     */
    fun acquire(context: Context): WebView {
        return pool.poll() ?: WebView(context.applicationContext)
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
