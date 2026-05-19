package com.zbrowser.app

import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Custom WebChromeClient for the browser.
 * Handles progress updates, new window creation (with popup blocking),
 * geolocation permissions, file chooser, and download detection.
 */
class BrowserWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onNewWindowRequested: (android.os.Message?) -> Unit,
    private val popupBlocker: PopupBlocker,
    private val permissionManager: PermissionManager,
    private val onPopupBlocked: () -> Unit = {}
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }

    /**
     * FEATURE 1: Popup Blocker - block window.open() when enabled.
     */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message?
    ): Boolean {
        if (popupBlocker.shouldBlockPopup()) {
            // Block the popup and notify the user
            onPopupBlocked()
            return false  // Returning false prevents the window from being created
        }
        onNewWindowRequested(resultMsg)
        return true
    }

    /**
     * FEATURE 10: Runtime Permissions - handle geolocation requests from web content.
     */
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        if (origin != null && callback != null) {
            permissionManager.onGeolocationPermissionsShowPrompt(origin, callback)
        }
    }

    /**
     * FEATURE 10: Runtime Permissions - handle camera/mic requests from web content.
     */
    override fun onPermissionRequest(request: PermissionRequest?) {
        if (request != null) {
            permissionManager.onPermissionRequest(request)
        }
    }
}
