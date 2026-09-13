package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class WayrevealPlacesUiKitBridgePlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var foregroundLocation: ForegroundDiscoveryLocation? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val context = binding.applicationContext
        PlacesLocaleBinding.ensure(context)
        channel = MethodChannel(binding.binaryMessenger, METHOD_CHANNEL)
        channel.setMethodCallHandler(this)
        binding.platformViewRegistry.registerViewFactory(
            EMBEDDED_VIEW_TYPE,
            PlacesUiKitEmbeddedViewFactory({ activity }, channel),
        )
        binding.platformViewRegistry.registerViewFactory(
            PLACE_DETAILS_VIEW_TYPE,
            PlaceDetailsUiKitEmbeddedViewFactory({ activity }, channel),
        )
        binding.platformViewRegistry.registerViewFactory(
            SELECTED_PLACE_MAP_VIEW_TYPE,
            SelectedPlaceMapEmbeddedViewFactory(channel),
        )
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getCapabilities" -> result.success(capabilities())
            "requestForegroundDiscoveryLocation" -> {
                val location = foregroundLocation
                if (location == null) result.success(mapOf("status" to "unavailable")) else location.request(result)
            }
            "cancelForegroundDiscoveryLocation" -> { foregroundLocation?.cancel(); result.success(null) }
            "probeLifecycle" -> result.success(
                mapOf(
                    "type" to "lifecycle",
                    "attached" to (activity != null),
                    "hostIsFragmentActivity" to (activity is FragmentActivity),
                ),
            )
            "openModalProbe" -> openModalProbe(result)
            else -> result.notImplemented()
        }
    }

    private fun capabilities(): Map<String, Any> = mapOf(
        "androidAvailable" to true,
        "androidEmbeddedSupported" to (activity is FragmentActivity),
        "androidModalSupported" to true,
        "iosAvailable" to false,
        "iosEmbeddedSupported" to false,
        "iosModalSupported" to false,
        "requiresHostActivityChange" to (activity !is FragmentActivity),
        "requiresHostAppDelegateChange" to false,
        "requiresGeneratedSourcePatch" to false,
        "iosDependencyTransportSupported" to false,
    )

    private fun openModalProbe(result: MethodChannel.Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("activity_unavailable", "No foreground Activity is attached.", null)
            return
        }
        currentActivity.startActivity(Intent(currentActivity, PlacesUiKitProbeActivity::class.java))
        result.success(mapOf("type" to "modal", "opened" to true))
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        detachActivity()
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        detachActivity()
        activity = binding.activity
        activityBinding = binding
        foregroundLocation = ForegroundDiscoveryLocation(binding.activity).also {
            binding.addRequestPermissionsResultListener(it)
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        detachActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        detachActivity()
    }

    private fun detachActivity() {
        foregroundLocation?.let {
            activityBinding?.removeRequestPermissionsResultListener(it)
            it.dispose()
        }
        foregroundLocation = null
        activityBinding = null
        activity = null
    }

    companion object {
        const val METHOD_CHANNEL = "wayreveal_places_ui_kit_bridge/methods"
        const val EMBEDDED_VIEW_TYPE = "wayreveal_places_ui_kit_bridge/place_search"
        const val PLACE_DETAILS_VIEW_TYPE = "wayreveal_places_ui_kit_bridge/place_details"
        const val SELECTED_PLACE_MAP_VIEW_TYPE = "wayreveal_places_ui_kit_bridge/selected_place_map"
    }
}
