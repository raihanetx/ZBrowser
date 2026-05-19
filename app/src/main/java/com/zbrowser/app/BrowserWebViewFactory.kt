package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Factory and configuration for browser WebViews.
 * Centralizes all WebView settings in one place for consistency and security.
 */
object BrowserWebViewFactory {

    private var mobileUserAgent: String? = null

    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    // JavaScript to force desktop viewport (1024px wide)
    const val DESKTOP_VIEWPORT_JS = """
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

    // JavaScript to restore mobile viewport
    const val MOBILE_VIEWPORT_JS = """
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

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context, desktopMode: Boolean): WebView {
        val webView = WebView(context)

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.cacheMode = WebSettings.LOAD_DEFAULT

        // Security: Disable file/content access from web content
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.loadsImagesAutomatically = true

        // Cache mobile UA from the first WebView
        if (mobileUserAgent == null) {
            mobileUserAgent = s.userAgentString
        }

        applyModeSettings(s, desktopMode)
        return webView
    }

    fun applyModeSettings(settings: WebSettings, desktopMode: Boolean) {
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

    fun getMobileUserAgent(): String? = mobileUserAgent
}
