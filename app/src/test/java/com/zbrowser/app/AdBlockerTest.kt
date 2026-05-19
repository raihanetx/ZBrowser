package com.zbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AdBlocker.
 * Tests domain matching, URL blocking logic, and CSS injection.
 */
class AdBlockerTest {

    // Test the domain matching logic directly
    // (AdBlocker needs Context for SharedPreferences, so we test the matching patterns)

    @Test
    fun adDomains_containsDoubleclick() {
        assertTrue(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("doubleclick.net"))
    }

    @Test
    fun adDomains_containsGoogleAnalytics() {
        assertTrue(com.zbrowser.app.AdBlocker.AD_BLOCKER_ENABLED)
        assertTrue(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("google-analytics.com"))
    }

    @Test
    fun adDomains_containsFacebookTracking() {
        assertTrue(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("facebook.net/tr"))
    }

    @Test
    fun adDomains_containsAdPaths() {
        assertTrue(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("/ads/"))
        assertTrue(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("/tracking/"))
    }

    @Test
    fun adDomains_doesNotContainGoogleSearch() {
        assertFalse(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("google.com"))
    }

    @Test
    fun adDomains_doesNotContainGithub() {
        assertFalse(com.zbrowser.app.AdBlocker.AD_DOMAINS.contains("github.com"))
    }

    @Test
    fun adHideCss_isNotBlank() {
        assertTrue(com.zbrowser.app.AdBlocker.AD_HIDE_CSS.isNotBlank())
    }

    @Test
    fun adHideCss_containsDisplayNone() {
        assertTrue(com.zbrowser.app.AdBlocker.AD_HIDE_CSS.contains("display: none"))
    }
}
