package com.zbrowser.app

import android.net.Uri
import android.webkit.URLUtil

/**
 * Handles URL processing, input parsing, and search routing.
 * Extracted from MainActivity for testability and separation of concerns.
 */
object NavigationController {

    const val HOME_URL = "https://www.google.com"

    /**
     * Process raw user input into a loadable URL.
     * - Already a full URL → use as-is
     * - Looks like a domain → prepend https://
     * - Otherwise → search with Google
     */
    fun processInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return HOME_URL

        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            URLUtil.isValidUrl("https://$trimmed") && trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }
    }

    /**
     * Validate whether a URL scheme is safe to load in the WebView.
     * Only http and https are allowed.
     */
    fun isSafeUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        return scheme == "http" || scheme == "https"
    }

    /**
     * Determine if a URL scheme should be delegated to an external app.
     * Returns the scheme name if it should be external, null otherwise.
     */
    fun getExternalScheme(url: String?): String? {
        val scheme = Uri.parse(url ?: return null).scheme?.lowercase() ?: return null
        return when (scheme) {
            "tel", "mailto", "sms", "geo", "market" -> scheme
            else -> null
        }
    }
}
