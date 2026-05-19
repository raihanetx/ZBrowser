package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebViewClient for the browser.
 * Handles URL interception, page lifecycle callbacks, error handling, and security.
 * Separated from MainActivity for clean architecture and testability.
 */
class BrowserWebViewClient(
    private val context: Context,
    private val tabLookup: (WebView?) -> BrowserTab?
) : WebViewClient() {

    /**
     * Callback interface for page lifecycle events.
     */
    interface Callback {
        fun onPageLoadStarted(url: String, isDesktopMode: Boolean)
        fun onPageLoadFinished(title: String, url: String, isDesktopMode: Boolean)
        fun onPageLoadError(errorMsg: String, url: String)
        fun onSslError(handler: SslErrorHandler, error: SslError)
    }

    var callback: Callback? = null

    // Desktop mode viewport JavaScript - applied AFTER page load to avoid layout shift
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

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        val scheme = request.url.scheme?.lowercase() ?: ""

        when (scheme) {
            // Safe external schemes - launch in appropriate app
            "tel", "mailto", "sms", "geo" -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    // No app to handle this scheme
                }
                return true
            }
            // CRITICAL FIX: Block intent:// scheme - only extract safe fallback URL
            "intent" -> {
                val fallback = SecurityUtils.extractSafeFallbackFromIntent(url)
                if (fallback != null) {
                    view?.loadUrl(fallback)
                }
                // Always return true - never launch external apps from intent://
                return true
            }
            // Block all other dangerous schemes (file://, javascript:, data:, etc.)
            "file", "javascript", "data" -> return true
        }

        // All http/https URLs stay inside the WebView
        return false
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        // Determine the desktop mode for this tab
        val isDesktop = tabLookup(view)?.isDesktopMode ?: false

        // Re-apply desktop mode settings BEFORE every page load to ensure consistency
        view?.let { wv ->
            applyModeSettings(wv.settings, isDesktop)
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

            // Apply viewport JS AFTER page load to match the mode
            if (isDesktop) {
                wv.evaluateJavascript(desktopViewportJs, null)
            } else {
                wv.evaluateJavascript(mobileViewportJs, null)
            }

            callback?.onPageLoadFinished(
                title = if (title.isNotEmpty()) title else pageUrl,
                url = pageUrl,
                isDesktopMode = isDesktop
            )
        }
    }

    // CRITICAL FIX: Escape HTML in error page to prevent XSS
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

    companion object {
        // Modern desktop user agent (updated to Chrome 131)
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        /**
         * Apply desktop or mobile mode WebView settings.
         * Desktop mode = desktop UA + wide viewport + no overview mode
         */
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
