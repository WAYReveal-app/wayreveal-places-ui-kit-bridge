package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.Context
import android.location.Location
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Marker
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView

/** In-app map of the current UI Kit result round. No external map intents. */
class SelectedPlaceMapEmbeddedView(
    context: Context,
    private val params: Map<*, *>,
    private val channel: MethodChannel,
    private val viewId: Int,
) : PlatformView {
    private val mapView = MapView(context)
    private var disposed = false
    private val state = DiscoveryMapState.fromArgs(params)
    private var userMovedCamera = false
    private var initialCameraPositioned = false

    init {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        renderPlaces()
    }

    private fun renderPlaces() {
        val data = state
        if (data == null) {
            channel.invokeMethod("onSelectedPlaceMapEvent", mapOf("type" to "error", "reason" to "invalid_identity", "viewId" to viewId))
            return
        }
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
            val hu = params["localeCode"] == "hu"
            val markers = mutableListOf<Marker>()
            val regularIcon = discoveryMarkerBitmap(mapView.context, data.category, false)
            val selectedIcon = discoveryMarkerBitmap(mapView.context, data.category, true)
            var selectedId = data.selectedPlaceId
            for ((index, place) in data.places.withIndex()) {
                val selected = place.placeId == data.selectedPlaceId
                map.addMarker(MarkerOptions()
                    .position(LatLng(place.latitude, place.longitude))
                    .anchor(0.5f, 0.95f)
                    .zIndex(if (selected) 2f else 1f)
                    .title(if (hu) "${index + 1}. hely" else "Place ${index + 1}")
                    .icon(if (selected) selectedIcon else regularIcon))?.let { marker ->
                        marker.tag = place
                        markers.add(marker)
                    }
            }
            map.setOnMarkerClickListener { marker ->
                val place = marker.tag as? DiscoveryIdentity ?: return@setOnMarkerClickListener true
                if (disposed) return@setOnMarkerClickListener true
                if (selectedId != place.placeId) {
                    selectedId = place.placeId
                    markers.forEach { existing ->
                        val active = (existing.tag as? DiscoveryIdentity)?.placeId == selectedId
                        existing.setIcon(if (active) selectedIcon else regularIcon)
                        existing.zIndex = if (active) 2f else 1f
                    }
                }
                if (!disposed) channel.invokeMethod("onSelectedPlaceMapEvent",
                    place.toPayload() + mapOf("type" to "selected", "viewId" to viewId, "requestId" to data.requestId, "selected" to true))
                true // Selection stays inside WAYReveal; no Maps toolbar/info-window intent.
            }
            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) userMovedCamera = true
            }
            map.setOnCameraIdleListener {
                if (!disposed && userMovedCamera) {
                    userMovedCamera = false
                    val center = map.cameraPosition.target
                    val corner = map.projection.visibleRegion.farRight
                    val distance = FloatArray(1)
                    Location.distanceBetween(center.latitude, center.longitude, corner.latitude, corner.longitude, distance)
                    channel.invokeMethod("onSelectedPlaceMapEvent", mapOf(
                        "type" to "viewport", "viewId" to viewId, "requestId" to data.requestId,
                        "latitude" to center.latitude, "longitude" to center.longitude,
                        "radiusMeters" to distance[0].toDouble().coerceIn(500.0, 20000.0),
                    ))
                }
            }
            map.setOnMapLoadedCallback {
                if (disposed || initialCameraPositioned) return@setOnMapLoadedCallback
                initialCameraPositioned = true
                val selected = data.places.firstOrNull { it.placeId == data.selectedPlaceId }
                if (selected != null || data.places.size == 1) {
                    val place = selected ?: data.places.first()
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(place.latitude, place.longitude), 15f))
                } else {
                    val bounds = LatLngBounds.builder()
                    data.places.forEach { bounds.include(LatLng(it.latitude, it.longitude)) }
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), (48 * mapView.resources.displayMetrics.density).toInt()))
                }
                channel.invokeMethod("onSelectedPlaceMapEvent", mapOf(
                    "type" to "ready", "viewId" to viewId, "requestId" to data.requestId, "count" to data.places.size,
                ))
            }
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
