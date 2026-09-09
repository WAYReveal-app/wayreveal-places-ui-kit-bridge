package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.res.Configuration
import android.os.Bundle
import io.flutter.embedding.android.FlutterFragmentActivity

/**
 * Optional Flutter host for applications whose native integrations require a
 * FragmentActivity. The application manifest remains the source of truth for
 * whether this host is used as the launcher activity.
 */
open class WayRevealFlutterFragmentActivity : FlutterFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onPostResume() {
        super.onPostResume()
        PlacesUiKitFragmentOwnerRegistry.onHostPostResume(this)
        PlaceDetailsUiKitFragmentOwnerRegistry.onHostPostResume(this)
    }

    override fun onPause() {
        PlacesUiKitFragmentOwnerRegistry.prepareForHostPause(this)
        PlaceDetailsUiKitFragmentOwnerRegistry.prepareForHostPause(this)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        PlacesUiKitFragmentOwnerRegistry.prepareForStateSave(this)
        PlaceDetailsUiKitFragmentOwnerRegistry.prepareForStateSave(this)
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        PlacesUiKitFragmentOwnerRegistry.onHostDestroyed(this)
        PlaceDetailsUiKitFragmentOwnerRegistry.onHostDestroyed(this)
        super.onDestroy()
    }
}
