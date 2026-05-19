package com.zbrowser.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AdBlocker (v3.1 — HashSet-based O(1) lookup).
 * Tests domain matching, host set contents, path pattern regex, and CSS injection.
 */
class AdBlockerTest {

    @Test
    fun adHosts_containsDoubleclick() {
        assertTrue(AdBlocker.AD_HOSTS.contains("doubleclick.net"))
    }

    @Test
    fun adHosts_containsGoogleAnalytics() {
        assertTrue(AdBlocker.AD_HOSTS.contains("google-analytics.com"))
    }

    @Test
    fun adHosts_containsTaboola() {
        assertTrue(AdBlocker.AD_HOSTS.contains("taboola.com"))
    }

    @Test
    fun adHosts_containsCriteo() {
        assertTrue(AdBlocker.AD_HOSTS.contains("criteo.com"))
    }

    @Test
    fun adHosts_doesNotContainGoogleSearch() {
        assertFalse(AdBlocker.AD_HOSTS.contains("google.com"))
    }

    @Test
    fun adHosts_doesNotContainGithub() {
        assertFalse(AdBlocker.AD_HOSTS.contains("github.com"))
    }

    @Test
    fun adPathPattern_matchesAdPath() {
        val matcher = AdBlocker.AD_PATH_PATTERN.matcher("/ads/banner.js")
        assertTrue(matcher.find())
    }

    @Test
    fun adPathPattern_matchesTrackingPath() {
        val matcher = AdBlocker.AD_PATH_PATTERN.matcher("/tracking/pixel.gif")
        assertTrue(matcher.find())
    }

    @Test
    fun adPathPattern_matchesBeaconPath() {
        val matcher = AdBlocker.AD_PATH_PATTERN.matcher("/beacon.gif")
        assertTrue(matcher.find())
    }

    @Test
    fun adPathPattern_doesNotMatchNormalPath() {
        val matcher = AdBlocker.AD_PATH_PATTERN.matcher("/articles/2024/tech-news")
        assertFalse(matcher.find())
    }

    @Test
    fun adHideCss_isNotBlank() {
        assertTrue(AdBlocker.AD_HIDE_CSS.isNotBlank())
    }

    @Test
    fun adHideCss_containsDisplayNone() {
        // v4.0: CSS uses minified format "display:none!important"
        assertTrue(AdBlocker.AD_HIDE_CSS.contains("display:none"))
    }

    @Test
    fun adHosts_isHashSet() {
        // Verify it's a HashSet for O(1) lookups
        assertTrue(AdBlocker.AD_HOSTS is java.util.HashSet)
    }
}
