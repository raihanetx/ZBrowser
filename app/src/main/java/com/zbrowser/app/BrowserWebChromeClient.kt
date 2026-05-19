package com.zbrowser.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

/**
 * Custom WebChromeClient for the browser.
 * Handles progress updates, new window creation (with popup blocking),
 * geolocation permissions, file chooser, and download detection.
 *
 * v4.0 FIX: M7 — Added onShowFileChooser for <input type="file"> support.
 */
class BrowserWebChromeClient(
    private val onProgressChanged: (Int) -> Unit,
    private val onNewWindowRequested: (android.os.Message?) -> Unit,
    private val popupBlocker: PopupBlocker,
    private val permissionManager: PermissionManager,
    private val onPopupBlocked: () -> Unit = {},
    private val onShowFileChooser: ((ValueCallback<Array<Uri>>?, String?) -> Boolean)? = null
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

    /**
     * M7 FIX: Handle <input type="file"> file chooser requests.
     * Delegates to the Activity which can show a file picker intent.
     * Without this, file upload buttons on web pages do nothing.
     */
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // If the activity provided a custom handler, use it
        if (onShowFileChooser != null) {
            return onShowFileChooser(filePathCallback, fileChooserParams?.acceptTypes?.firstOrNull())
        }

        // Default: create a simple file chooser intent
        val intent = fileChooserParams?.createIntent()
        return try {
            if (intent != null && webView?.context is Activity) {
                @Suppress("DEPRECATION")
                (webView.context as Activity).startActivityForResult(intent, REQUEST_FILE_CHOOSER)
                pendingFilePathCallback = filePathCallback
                true
            } else {
                false
            }
        } catch (_: Exception) {
            filePathCallback?.onReceiveValue(null)
            false
        }
    }

    companion object {
        const val REQUEST_FILE_CHOOSER = 10001
        var pendingFilePathCallback: ValueCallback<Array<Uri>>? = null
    }
}
