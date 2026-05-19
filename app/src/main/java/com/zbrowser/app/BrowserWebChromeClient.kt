package com.zbrowser.app

import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Custom WebChromeClient for the browser.
 * Handles progress updates and new window creation.
 * Uses a functional interface pattern instead of callback object for cleaner integration.
 */
class BrowserWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onNewWindowRequested: (android.os.Message?) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        onNewWindowRequested(resultMsg)
        return true
    }
}
