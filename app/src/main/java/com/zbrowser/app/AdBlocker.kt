package com.zbrowser.app

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import java.io.ByteArrayInputStream
import java.util.regex.Pattern

/**
 * Ad and tracker blocker for the browser.
 * Uses a curated list of ad/tracker domain patterns to block requests before they load.
 * Blocks both the request AND prevents the empty space/click-to-load issue
 * by injecting CSS to hide common ad containers.
 *
 * The blocker list is embedded (no network fetch needed) and the enabled
 * state is persisted in SharedPreferences.
 */
class AdBlocker(private val context: Context, private val prefs: SharedPreferences) {

    companion object {
        private const val KEY_AD_BLOCKER_ENABLED = "ad_blocker_enabled"
        const val DEFAULT_ENABLED = true

        /**
         * Curated list of ad/tracker domain patterns.
         * These cover the major ad networks, tracking services, and analytics platforms.
         * Using contains matching for broad coverage without false positives on main content.
         */
        private val AD_DOMAINS = setOf(
            // Major ad networks
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "google-analytics.com", "googletagmanager.com",
            "adnxs.com", "adsrvr.org", "adform.net", "adroll.com",
            "amazon-adsystem.com", "ads.amazon.com",
            "facebook.net/tr", "connect.facebook.net/en_US/fbevents.js",

            // Programmatic ad exchanges
            "rubiconproject.com", "pubmatic.com", "openx.net", "indexww.com",
            "casalemedia.com", "criteo.com", "criteo.net", "taboola.com",
            "outbrain.com", "mgid.com", "revcontent.com",

            // Tracking & analytics
            "scorecardresearch.com", "quantserve.com", "moatads.com",
            "chartbeat.com", "hotjar.com", "mixpanel.com", "segment.io",
            "segment.com/v1", "amplitude.com", "fullstory.com",
            "newrelic.com", "nr-data.net",

            // Malvertising / suspicious
            "adsterra", "popads", "propellerads", "hilltopads",
            "clickadu", "propeller.pw", "ad-maven",

            // Common ad serving paths
            "/ads/", "/ad/", "/adv/", "/banner/", "/banners/",
            "/adserver/", "/advertising/", "/advert/",
            "/tracking/", "/tracker/", "/pixel.", "/beacon/"
        )

        /**
         * CSS to inject into pages to hide common ad containers.
         * Uses display:none !important to override inline styles.
         */
        const val AD_HIDE_CSS = """
            (function() {
                var style = document.createElement('style');
                style.textContent = '
                    [id^="google_ads"], [id^="div-gpt-ad"], [class*="ad-container"], '
                    + '[class*="ad-wrapper"], [class*="ad-banner"], [class*="sponsored"], '
                    + '[class*="taboola"], [class*="outbrain"], [class*="recommendation"], '
                    + 'ins.adsbygoogle, div[id^="taboola-"], div[class^="taboola-"], '
                    + 'iframe[src*="doubleclick"], iframe[src*="googlesyndication"], '
                    + 'iframe[src*="amazon-adsystem"], iframe[src*="facebook.net/tr"], '
                    + 'div[data-ad], div[data-ad-slot], div[data-adunit], '
                    + '[class*="sticky-ad"], [class*="floating-ad"], [id*="push-notification"] '
                    + '{ display: none !important; height: 0 !important; overflow: hidden !important; }';
                document.head.appendChild(style);
            })();
        """

        /**
         * Empty response to return when a request is blocked.
         * Using a minimal empty HTML prevents "web page not available" errors.
         */
        private val EMPTY_RESPONSE = ByteArrayInputStream("".toByteArray())
    }

    /** Whether the ad blocker is currently active */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_AD_BLOCKER_ENABLED, DEFAULT_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_AD_BLOCKER_ENABLED, value).apply()

    /**
     * Check if a resource request should be blocked.
     * Blocks requests to known ad/tracker domains.
     * Only blocks sub-resource requests (not the main page).
     */
    fun shouldBlock(request: WebResourceRequest): Boolean {
        if (!isEnabled) return false
        if (request.isForMainFrame) return false  // Never block the main page

        val url = request.url.toString().lowercase()
        val host = request.url.host?.lowercase() ?: ""

        // Check against known ad domains
        for (adDomain in AD_DOMAINS) {
            if (host.contains(adDomain) || url.contains(adDomain)) {
                return true
            }
        }

        return false
    }

    /**
     * Create an empty WebResourceResponse for blocked requests.
     */
    fun createBlockedResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            EMPTY_RESPONSE
        )
    }
}
