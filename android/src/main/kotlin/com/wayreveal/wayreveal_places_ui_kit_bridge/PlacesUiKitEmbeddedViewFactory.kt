package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.app.Activity
import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

internal class PlacesUiKitEmbeddedViewFactory(
    private val activityProvider: () -> Activity?,
    private val channel: MethodChannel,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val query = (args as? Map<*, *>)?.get("query") as? String
        return PlacesUiKitEmbeddedView(
            context,
            viewId,
            activityProvider(),
            channel,
            query?.takeIf { it.isNotBlank() } ?: "restaurants in Chania, Crete",
        )
    }
}
