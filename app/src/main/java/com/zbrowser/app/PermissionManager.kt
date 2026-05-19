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
 *
 * NOTE: Not provided via Hilt @Singleton — instead created directly in
 * MainActivity because it needs an Activity reference. Hilt @Singleton
 * cannot inject Activity-scoped objects.
 */
class PermissionManager(private val activity: Activity) {

    companion object {
        const val RC_GEOLOCATION = 1001
        const val RC_CAMERA = 1002
        const val RC_MICROPHONE = 1003
        const val RC_FILE_ACCESS = 1004

        private val RESOURCE_TO_PERMISSION = mapOf(
            PermissionRequest.RESOURCE_VIDEO_CAPTURE to Manifest.permission.CAMERA,
            PermissionRequest.RESOURCE_AUDIO_CAPTURE to Manifest.permission.RECORD_AUDIO,
            PermissionRequest.RESOURCE_GEOLOCATION to Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private var pendingGeoCallback: GeolocationPermissions.Callback? = null
    private var pendingGeoOrigin: String? = null
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingPermissionRequestCode: Int = RC_CAMERA

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

    fun onPermissionRequest(request: PermissionRequest) {
        val requestedResources = request.resources
        val neededPermissions = mutableListOf<String>()
        var requestCode = RC_CAMERA  // Default

        for (resource in requestedResources) {
            val permission = RESOURCE_TO_PERMISSION[resource]
            if (permission != null && !hasPermission(permission)) {
                neededPermissions.add(permission)
                // Set the correct request code based on the resource type
                when (resource) {
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> requestCode = RC_CAMERA
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> requestCode = RC_MICROPHONE
                    PermissionRequest.RESOURCE_GEOLOCATION -> requestCode = RC_GEOLOCATION
                }
            }
        }

        if (neededPermissions.isEmpty()) {
            request.grant(requestedResources)
        } else {
            pendingPermissionRequest = request
            pendingPermissionRequestCode = requestCode
            ActivityCompat.requestPermissions(
                activity,
                neededPermissions.toTypedArray(),
                requestCode
            )
        }
    }

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
                    false
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

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}
