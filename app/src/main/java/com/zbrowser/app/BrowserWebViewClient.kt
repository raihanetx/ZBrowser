package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.zbrowser.app.data.HistoryDao
import com.zbrowser.app.data.HistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom WebViewClient optimized for maximum smoothness.
 *
 * v3.1 OPTIMIZATIONS:
 * - Debounced history recording (500ms) prevents Room write spam on redirects
 * - onRenderProcessGone gracefully handles WebView renderer crashes
 * - Ad blocker uses O(1) HashSet lookup (see AdBlocker class)
 * - Minimal work in shouldOverrideUrlLoading (fast return path)
 */
class BrowserWebViewClient(
    private val context: Context,
    private val tabLookup: (WebView?) -> BrowserTab?,
    private val adBlocker: AdBlocker,
    private val popupBlocker: PopupBlocker,
    private val historyDao: HistoryDao?,
    private val appScope: CoroutineScope? = null
) : WebViewClient() {

    interface Callback {
        fun onPageLoadStarted(url: String, isDesktopMode: Boolean)
        fun onPageLoadFinished(title: String, url: String, isDesktopMode: Boolean)
        fun onPageLoadError(errorMsg: String, url: String)
        fun onSslError(handler: SslErrorHandler, error: SslError)
        fun onPopupBlocked()
    }

    var callback: Callback? = null

    // Debounce timer for history recording
    private var historyJob: Job? = null

    // Viewport JavaScript — cached strings, never re-allocated
    private val desktopViewportJs = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (meta) {
                meta.setAttribute('content', 'width=1024, initial-scale=1');
            } else {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=1024, initial-scale=1';
                document.head.appendChild(meta);
            }
        })();
    """.trimIndent()

    private val mobileViewportJs = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (meta) {
                meta.setAttribute('content', 'width=device-width, initial-scale=1, maximum-scale=5');
            } else {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=device-width, initial-scale=1, maximum-scale=5';
                document.head.appendChild(meta);
            }
        })();
    """.trimIndent()

    /**
     * FEATURE 4: Ad Blocker — O(1) HashSet lookup via AdBlocker class.
     * shouldInterceptRequest runs on a background thread by default,
     * so blocking here does NOT cause UI jank.
     */
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (request != null && adBlocker.shouldBlock(request)) {
            return adBlocker.createBlockedResponse()
        }
        return super.shouldInterceptRequest(view, request)
    }

    /**
     * Fast-path URL override — minimal work for common schemes.
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        val scheme = request.url.scheme?.lowercase() ?: ""

        when (scheme) {
            "tel", "mailto", "sms", "geo" -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                    // No app to handle
                }
                return true
            }
            "intent" -> {
                val fallback = SecurityUtils.extractSafeFallbackFromIntent(url)
                if (fallback != null) {
                    view?.loadUrl(fallback)
                }
                return true
            }
            "file", "javascript", "data" -> return true
        }

        return false
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        val isDesktop = tabLookup(view)?.isDesktopMode ?: false
        view?.let { wv ->
            applyModeSettings(wv.settings, isDesktop)
            popupBlocker.applyToWebView(wv)
        }

        url?.let {
            callback?.onPageLoadStarted(it, isDesktop)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.let { wv ->
            val title = wv.title ?: ""
            val pageUrl = url ?: ""
            val isDesktop = tabLookup(wv)?.isDesktopMode ?: false

            // Apply viewport JS
            if (isDesktop) {
                wv.evaluateJavascript(desktopViewportJs, null)
            } else {
                wv.evaluateJavascript(mobileViewportJs, null)
            }

            // FEATURE 4: Inject ad-hiding CSS
            if (adBlocker.isEnabled) {
                wv.evaluateJavascript(AdBlocker.AD_HIDE_CSS, null)
            }

            // Record history with DEBOUNCE — prevents Room spam on redirects
            if (title.isNotEmpty() && pageUrl.startsWith("http")) {
                recordHistoryDebounced(title, pageUrl)
            }

            callback?.onPageLoadFinished(
                title = if (title.isNotEmpty()) title else pageUrl,
                url = pageUrl,
                isDesktopMode = isDesktop
            )
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val errorMsg = error?.description?.toString() ?: "Unknown error"
            val pageUrl = view?.url ?: ""
            callback?.onPageLoadError(errorMsg, pageUrl)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (handler != null && error != null) {
            callback?.onSslError(handler, error)
        }
    }

    /**
     * RENDER PROCESS CRASH GUARD — if the WebView renderer process dies
     * (GPU fault, OOM, etc.), we don't let it crash the entire app.
     * Returns true to indicate we handled it.
     */
    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        if (view != null) {
            view.destroy()
            // The Activity's handleRenderProcessCrash() will create a new WebView
        }
        return true  // We handled it — don't crash the app
    }

    /**
     * Debounced history recording — waits 500ms after the last page load
     * before writing to Room, preventing spam on multi-redirect pages.
     */
    private fun recordHistoryDebounced(title: String, url: String) {
        val dao = historyDao ?: return
        val scope = appScope ?: return

        historyJob?.cancel()
        historyJob = scope.launch(Dispatchers.IO) {
            delay(500) // Debounce window
            dao.insert(HistoryEntity(url = url, title = title))
        }
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        @SuppressLint("SetJavaScriptEnabled")
        fun applyModeSettings(settings: WebSettings, desktopMode: Boolean, mobileUserAgent: String? = null) {
            if (desktopMode) {
                settings.userAgentString = DESKTOP_USER_AGENT
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = false
            } else {
                settings.userAgentString = mobileUserAgent ?: settings.userAgentString
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
            }
        }
    }
}
