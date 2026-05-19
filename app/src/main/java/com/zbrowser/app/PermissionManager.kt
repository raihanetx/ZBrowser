package com.zbrowser.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Manages runtime permissions for the browser.
 * Handles geolocation, camera, microphone, and other web content permissions.
 * Shows user-friendly prompts before granting permissions to web pages.
 */
class PermissionManager(private val activity: Activity) {

    companion object {
        // Request codes
        const val RC_GEOLOCATION = 1001
        const val RC_CAMERA = 1002
        const val RC_MICROPHONE = 1003
        const val RC_FILE_ACCESS = 1004

        /** Maps WebKit PermissionRequest resources to Android permissions */
        private val RESOURCE_TO_PERMISSION = mapOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE to Manifest.permission.CAMERA,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE to Manifest.permission.RECORD_AUDIO,
            PermissionRequest.RESOURCE_GEOLOCATION to Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // Pending permission callbacks
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private var pendingPermissionRequest: PermissionRequest? = null

    /**
     * Handle a geolocation permission request from web content.
     * Checks if permission is already granted, otherwise requests it.
     */
    fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            callback.invoke(origin, true, false)
        } else {
            pendingGeoCallback = callback
            pendingGeoOrigin = origin
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                RC_GEOLOCATION
            )
        }
    }

    /**
     * Handle a WebKit PermissionRequest (camera, microphone, etc.).
     */
    fun onPermissionRequest(request: PermissionRequest) {
        val requestedResources = request.resources
        val neededPermissions = mutableListOf<String>()

        for (resource in requestedResources) {
            val permission = RESOURCE_TO_PERMISSION[resource]
            if (permission != null && !hasPermission(permission)) {
                neededPermissions.add(permission)
            }
        }

        if (neededPermissions.isEmpty()) {
            // All permissions already granted
            request.grant(requestedResources)
        } else {
            // Save the request and ask the user
            pendingPermissionRequest = request
            ActivityCompat.requestPermissions(
                activity,
                neededPermissions.toTypedArray(),
                RC_CAMERA  // Use first matching code
            )
        }
    }

    /**
     * Handle the result of a permission request.
     * Must be called from Activity.onRequestPermissionsResult.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val allGranted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        when (requestCode) {
            RC_GEOLOCATION -> {
                pendingGeoCallback?.invoke(
                    pendingGeoOrigin ?: "",
                    allGranted,
                    false  // Don't remember
                )
                pendingGeoCallback = null
                pendingGeoOrigin = null
            }
            RC_CAMERA, RC_MICROPHONE -> {
                if (allGranted) {
                    pendingPermissionRequest?.grant(pendingPermissionRequest?.resources)
                } else {
                    pendingPermissionRequest?.deny()
                }
                pendingPermissionRequest = null
            }
        }
    }

    /**
     * Check if a permission is already granted.
     */
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}
