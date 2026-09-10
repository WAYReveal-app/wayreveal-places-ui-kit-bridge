package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.Context
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

class SelectedPlaceMapEmbeddedView(
    context: Context,
    private val params: Map<*, *>,
    private val channel: MethodChannel,
) : PlatformView {
    private val mapView = MapView(context)
    private var disposed = false

    init {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        renderSelectedPlace()
    }

    private fun renderSelectedPlace() {
        val placeId = (params["placeId"] as? String)?.trim().orEmpty()
        val latitude = (params["latitude"] as? Number)?.toDouble()
        val longitude = (params["longitude"] as? Number)?.toDouble()
        val valid = placeId.isNotEmpty() &&
            latitude != null && latitude.isFinite() && latitude in -90.0..90.0 &&
            longitude != null && longitude.isFinite() && longitude in -180.0..180.0
        if (!valid) {
            channel.invokeMethod(
                "onSelectedPlaceMapEvent",
                mapOf("type" to "error", "reason" to "invalid_identity"),
            )
            return
        }

        val selectedLocation = LatLng(latitude, longitude)
        mapView.getMapAsync { map ->
            if (disposed) return@getMapAsync
            map.clear()
            map.uiSettings.isZoomControlsEnabled = true
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isMapToolbarEnabled = false
            map.uiSettings.isScrollGesturesEnabled = true
            map.uiSettings.isZoomGesturesEnabled = true
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isTiltGesturesEnabled = true
            map.addMarker(MarkerOptions().position(selectedLocation))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15.5f))
            channel.invokeMethod(
                "onSelectedPlaceMapEvent",
                mapOf("type" to "ready", "placeId" to placeId),
            )
        }
    }

    override fun getView(): View = mapView

    override fun dispose() {
        disposed = true
        mapView.onPause()
        mapView.onStop()
        mapView.onDestroy()
    }
}
