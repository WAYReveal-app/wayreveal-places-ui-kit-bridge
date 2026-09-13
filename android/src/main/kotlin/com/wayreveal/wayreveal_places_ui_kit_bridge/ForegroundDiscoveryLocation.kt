package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

/** One foreground fix after explicit UI consent. No background service or location storage. */
internal class ForegroundDiscoveryLocation(private val activity: Activity) :
    PluginRegistry.RequestPermissionsResultListener, Application.ActivityLifecycleCallbacks {
    private val handler = Handler(Looper.getMainLooper())
    private val manager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var pending: MethodChannel.Result? = null
    private var listening = false
    private val timeout = Runnable { finish("timeout") }
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (pending == null || !listening) return
            val age = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
            if (!usableDiscoveryFix(location.latitude, location.longitude, age, location.accuracy.toDouble())) return
            finish("ready", mapOf(
                "latitude" to location.latitude, "longitude" to location.longitude,
                "approximate" to !granted(Manifest.permission.ACCESS_FINE_LOCATION),
            ))
        }
        override fun onProviderDisabled(provider: String) { if (listening) finish("unavailable") }
        override fun onProviderEnabled(provider: String) = Unit
        @Deprecated("Required for old Android implementations")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    init { activity.application.registerActivityLifecycleCallbacks(this) }

    fun request(result: MethodChannel.Result) {
        if (pending != null) { result.success(mapOf("status" to "busy")); return }
        if (activity.isFinishing || activity.isDestroyed) { result.success(mapOf("status" to "unavailable")); return }
        pending = result
        if (hasPermission()) acquire() else activity.requestPermissions(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE,
        )
    }

    private fun granted(permission: String) = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    private fun hasPermission() = granted(Manifest.permission.ACCESS_COARSE_LOCATION) || granted(Manifest.permission.ACCESS_FINE_LOCATION)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_CODE) return false
        if (pending == null) return true
        if (hasPermission()) acquire() else finish("denied")
        return true
    }

    private fun acquire() {
        if (pending == null) return
        try {
            val provider = when {
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                granted(Manifest.permission.ACCESS_FINE_LOCATION) && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> { finish("services_off"); return }
            }
            listening = true
            handler.postDelayed(timeout, 20_000)
            manager.requestLocationUpdates(provider, 1_000, 0f, listener, Looper.getMainLooper())
        } catch (_: SecurityException) { finish("denied") }
        catch (_: IllegalArgumentException) { finish("unavailable") }
    }

    fun cancel() = finish("cancelled")
    fun dispose() {
        cancel()
        activity.application.unregisterActivityLifecycleCallbacks(this)
    }

    private fun finish(status: String, extra: Map<String, Any> = emptyMap()) {
        handler.removeCallbacks(timeout)
        if (listening) {
            listening = false
            try { manager.removeUpdates(listener) } catch (_: SecurityException) { /* permission revoked */ }
        }
        val result = pending
        pending = null
        result?.success(mapOf("status" to status) + extra)
    }

    override fun onActivityStopped(a: Activity) { if (a === activity) cancel() }
    override fun onActivityDestroyed(a: Activity) { if (a === activity) dispose() }
    override fun onActivityCreated(a: Activity, state: Bundle?) = Unit
    override fun onActivityStarted(a: Activity) = Unit
    override fun onActivityResumed(a: Activity) = Unit
    override fun onActivityPaused(a: Activity) = Unit
    override fun onActivitySaveInstanceState(a: Activity, state: Bundle) = Unit

    companion object { const val REQUEST_CODE = 48261 }
}

internal fun usableDiscoveryFix(lat: Double, lng: Double, ageMs: Long, accuracyMeters: Double): Boolean =
    validCoordinates(lat, lng) && ageMs in 0..120_000 &&
        accuracyMeters.isFinite() && accuracyMeters in 0.0..10000.0
