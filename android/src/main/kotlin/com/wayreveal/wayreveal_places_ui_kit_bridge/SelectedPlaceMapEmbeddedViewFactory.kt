package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.Context
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class SelectedPlaceMapEmbeddedViewFactory(
    private val channel: MethodChannel,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        val creationParams = (args as? Map<*, *>) ?: emptyMap<Any, Any>()
        return SelectedPlaceMapEmbeddedView(context, creationParams, channel, viewId)
    }
}
