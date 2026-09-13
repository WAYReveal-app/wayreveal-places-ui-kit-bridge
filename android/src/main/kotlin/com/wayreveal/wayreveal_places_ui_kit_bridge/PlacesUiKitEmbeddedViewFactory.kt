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
        val params = args as? Map<*, *> ?: emptyMap<Any, Any>()
        PlacesLocaleBinding.ensure(context, params["localeCode"])
        val query = params["query"] as? String
        val request = DiscoveryRequest.fromArgs(params)
        return PlacesUiKitEmbeddedView(
            context,
            viewId,
            activityProvider(),
            channel,
            query?.takeIf { it.isNotBlank() } ?: "",
            request,
            params.containsKey("requestId"),
        )
    }
}
