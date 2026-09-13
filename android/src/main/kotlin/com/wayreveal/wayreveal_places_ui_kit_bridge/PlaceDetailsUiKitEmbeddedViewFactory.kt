package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.app.Activity
import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

internal class PlaceDetailsUiKitEmbeddedViewFactory(
    private val activityProvider: () -> Activity?,
    private val channel: MethodChannel,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        PlacesLocaleBinding.ensure(context, (args as? Map<*, *>)?.get("localeCode"))
        val placeId = ((args as? Map<*, *>)?.get("placeId") as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it.length <= 256 && it.none(Char::isWhitespace) }
        return PlaceDetailsUiKitEmbeddedView(
            context,
            viewId,
            activityProvider(),
            channel,
            placeId,
        )
    }
}
