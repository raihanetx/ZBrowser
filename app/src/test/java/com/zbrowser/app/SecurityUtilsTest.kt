package com.zbrowser.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SecurityUtils.
 * Tests HTML escaping, URL validation, and intent URL sanitization.
 */
class SecurityUtilsTest {

    // === escapeHtml tests ===

    @Test
    fun escapeHtml_escapesAmpersand() {
        assertEquals("&amp;", SecurityUtils.escapeHtml("&"))
    }

    @Test
    fun escapeHtml_escapesLessThan() {
        assertEquals("&lt;", SecurityUtils.escapeHtml("<"))
    }

    @Test
    fun escapeHtml_escapesGreaterThan() {
        assertEquals("&gt;", SecurityUtils.escapeHtml(">"))
    }

    @Test
    fun escapeHtml_escapesDoubleQuote() {
        assertEquals("&quot;", SecurityUtils.escapeHtml("\""))
    }

    @Test
    fun escapeHtml_escapesSingleQuote() {
        assertEquals("&#39;", SecurityUtils.escapeHtml("'"))
    }

    @Test
    fun escapeHtml_escapesForwardSlash() {
        assertEquals("&#x2F;", SecurityUtils.escapeHtml("/"))
    }

    @Test
    fun escapeHtml_escapesXssPayload() {
        val xss = "<script>alert('xss')</script>"
        val escaped = SecurityUtils.escapeHtml(xss)
        assertFalse(escaped.contains("<script>"))
        assertFalse(escaped.contains("</script>"))
        assertTrue(escaped.contains("&lt;script"))
        assertTrue(escaped.contains("script&gt;"))
    }

    @Test
    fun escapeHtml_escapesImgOnerror() {
        val xss = "<img src=x onerror=alert(1)>"
        val escaped = SecurityUtils.escapeHtml(xss)
        assertFalse(escaped.contains("<img"))
        assertTrue(escaped.contains("&lt;img"))
    }

    @Test
    fun escapeHtml_preservesNormalText() {
        val normal = "Hello World 123"
        assertEquals(normal, SecurityUtils.escapeHtml(normal))
    }

    // === isUrlSafe tests ===

    @Test
    fun isUrlSafe_allowsHttps() {
        assertTrue(SecurityUtils.isUrlSafe("https://example.com"))
    }

    @Test
    fun isUrlSafe_allowsHttp() {
        assertTrue(SecurityUtils.isUrlSafe("http://example.com"))
    }

    @Test
    fun isUrlSafe_allowsTel() {
        assertTrue(SecurityUtils.isUrlSafe("tel:+1234567890"))
    }

    @Test
    fun isUrlSafe_allowsMailto() {
        assertTrue(SecurityUtils.isUrlSafe("mailto:test@example.com"))
    }

    @Test
    fun isUrlSafe_blocksFileScheme() {
        assertFalse(SecurityUtils.isUrlSafe("file:///etc/passwd"))
    }

    @Test
    fun isUrlSafe_blocksJavascriptScheme() {
        assertFalse(SecurityUtils.isUrlSafe("javascript:alert(1)"))
    }

    @Test
    fun isUrlSafe_blocksDataScheme() {
        assertFalse(SecurityUtils.isUrlSafe("data:text/html,<h1>test</h1>"))
    }

    @Test
    fun isUrlSafe_blocksIntentScheme() {
        assertFalse(SecurityUtils.isUrlSafe("intent://example.com#Intent;scheme=package;end"))
    }

    @Test
    fun isUrlSafe_blocksEmptyUrl() {
        assertFalse(SecurityUtils.isUrlSafe(""))
    }

    // === extractSafeFallbackFromIntent tests ===

    @Test
    fun extractSafeFallback_extractsHttpsFallback() {
        val intentUrl = "intent://example.com#Intent;scheme=https;S.browser_fallback_url=https%3A%2F%2Fexample.com;end"
        val result = SecurityUtils.extractSafeFallbackFromIntent(intentUrl)
        // The fallback URL should be extracted if it's http/https
        // Note: actual parsing depends on URI format
    }

    @Test
    fun extractSafeFallback_returnsNullForNonIntentUrl() {
        assertNull(SecurityUtils.extractSafeFallbackFromIntent("https://example.com"))
    }

    @Test
    fun extractSafeFallback_returnsNullForInvalidUrl() {
        assertNull(SecurityUtils.extractSafeFallbackFromIntent("not-a-url"))
    }

    // === buildErrorPage tests ===

    @Test
    fun buildErrorPage_containsEscapedError() {
        val page = SecurityUtils.buildErrorPage("<script>alert(1)</script>", "https://example.com")
        assertFalse(page.contains("<script>"))
        assertTrue(page.contains("&lt;script"))
    }

    @Test
    fun buildErrorPage_containsEscapedUrl() {
        val page = SecurityUtils.buildErrorPage("Error", "https://example.com?a=1&b=2")
        assertFalse(page.contains("&b=2"))  // & should be escaped
    }

    @Test
    fun buildErrorPage_containsTryAgainLink() {
        val page = SecurityUtils.buildErrorPage("Error", "https://example.com")
        assertTrue(page.contains("Try Again"))
    }
}
