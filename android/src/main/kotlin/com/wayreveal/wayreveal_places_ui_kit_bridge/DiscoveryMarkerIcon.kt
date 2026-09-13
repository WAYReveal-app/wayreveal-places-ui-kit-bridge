package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Category pictograms; teal is brand/selection, never an inferred Partner claim. */
internal fun discoveryMarkerBitmap(context: Context, category: String, selected: Boolean): BitmapDescriptor {
    val size = (64 * context.resources.displayMetrics.density).toInt().coerceAtLeast(64)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(size / 64f, size / 64f)
    val teal = Color.rgb(16, 130, 134)
    val ink = Color.rgb(10, 94, 98)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = if (selected) 25f else 21f
    paint.color = Color.argb(32, 14, 44, 48)
    canvas.drawOval(RectF(14f, 52f, 50f, 62f), paint)
    val pointer = Path().apply { moveTo(24f, 44f); lineTo(32f, 60f); lineTo(40f, 44f); close() }
    paint.color = if (selected) teal else Color.WHITE
    canvas.drawPath(pointer, paint)
    canvas.drawCircle(32f, 29f, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = if (selected) 2.5f else 1.5f
    paint.color = if (selected) Color.WHITE else ink
    canvas.drawCircle(32f, 29f, radius - 2f, paint)
    paint.style = Paint.Style.FILL
    paint.color = if (selected) Color.WHITE else ink
    val pathData = when (category) {
        "food" -> "M7 2v7H5V2H3v7c0 2.2 1.8 4 4 4v9h2v-9c2.2 0 4-1.8 4-4V2h-2v7H9V2H7zM17 2v12h3v8h2V2h-5z"
        "shopping" -> "M19 6h-2V5a5 5 0 0 0-10 0v1H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2zM9 5a3 3 0 0 1 6 0v1H9V5zM7 9h2v3H7V9zM15 9h2v3h-2V9z"
        "transport" -> "M5 3h14l3 9v8h-3v-3H5v3H2v-8l3-9zM6 5l-2 6h16l-2-6H6zM5 13a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zM19 13a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z"
        "wellness" -> "M20 3C9 1 2 7 4 15c4-5 7-7 12-8-5 3-8 6-10 10-2 3-2 4-2 5h2c0-2 1-4 3-6 7 2 13-3 11-13z"
        "experiences" -> "M12 2 15 8 22 9 17 14 18 21 12 18 6 21 7 14 2 9 9 8z"
        else -> "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM16.5 7.5l-3 6-6 3 3-6 6-3z"
    }
    canvas.save()
    canvas.translate(20f, 17f)
    canvas.drawPath(PathParser.createPathFromPathData(pathData), paint)
    canvas.restore()
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
