package com.wayreveal.wayreveal_places_ui_kit_bridge

/** Transient discovery input. Never contains a key, Google rich content or partner claims. */
internal data class DiscoveryRequest(
    val requestId: String,
    val category: String,
    val query: String?,
    val latitude: Double?,
    val longitude: Double?,
    val radiusMeters: Double,
    val intent: String = "",
) {
    val nearby: Boolean get() = latitude != null && longitude != null

    val includedTypes: List<String> get() = if (intent == "walk_explore") listOf("park") else when (category) {
        "food" -> listOf("restaurant", "cafe", "bakery")
        "experiences" -> listOf("tourist_attraction", "museum", "park")
        "wellness" -> listOf("spa", "wellness_center")
        "shopping" -> listOf("shopping_mall", "store")
        "transport" -> listOf("car_rental", "bus_station", "train_station")
        else -> emptyList()
    }

    companion object {
        val categories = setOf("all", "food", "experiences", "wellness", "shopping", "transport")

        fun fromArgs(args: Map<*, *>): DiscoveryRequest? {
            val requestId = (args["requestId"] as? String)?.trim()
                ?.takeIf { it.isNotEmpty() && it.length <= 160 && it.none(Char::isISOControl) }
                ?: return null
            val category = (args["categoryKey"] as? String) ?: "all"
            if (category !in categories) return null
            val intent = args["intentKey"] as? String ?: ""
            val intentCategories = mapOf("eat_or_drink" to "food", "find_experience" to "experiences", "walk_explore" to "experiences")
            if (intent.isNotEmpty() && intentCategories[intent] != category) return null
            val query = (args["query"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            if (query != null && (query.length > 200 || query.any(Char::isISOControl))) return null
            val lat = (args["centerLatitude"] as? Number)?.toDouble()
            val lng = (args["centerLongitude"] as? Number)?.toDouble()
            val hasCoordinateInput = args.containsKey("centerLatitude") || args.containsKey("centerLongitude")
            if (hasCoordinateInput && !validCoordinates(lat, lng)) return null
            if (!hasCoordinateInput && query == null) return null
            val radius = (args["radiusMeters"] as? Number)?.toDouble() ?: 5000.0
            if (!radius.isFinite() || radius !in 500.0..20000.0) return null
            return DiscoveryRequest(requestId, category, query, lat, lng, radius, intent)
        }
    }
}

internal fun validCoordinates(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && lat.isFinite() && lng.isFinite() &&
        lat in -90.0..90.0 && lng in -180.0..180.0

internal data class DiscoveryIdentity(val placeId: String, val latitude: Double, val longitude: Double) {
    fun toPayload(): Map<String, Any> = mapOf(
        "placeId" to placeId, "latitude" to latitude, "longitude" to longitude,
    )

    companion object {
        fun create(id: String?, latitude: Double?, longitude: Double?): DiscoveryIdentity? {
            val safeId = id?.trim()?.takeIf {
                it.isNotEmpty() && it.length <= 256 && it.none { c -> c.isWhitespace() || c.isISOControl() }
            } ?: return null
            if (!validCoordinates(latitude, longitude)) return null
            return DiscoveryIdentity(safeId, latitude!!, longitude!!)
        }
    }
}

/** Request generation is the authority: late responses never replace a newer area/category. */
internal class DiscoveryResultRound {
    var requestId: String? = null
        private set
    var results: List<DiscoveryIdentity> = emptyList()
        private set

    fun begin(id: String) { requestId = id; results = emptyList() }
    fun accept(id: String, incoming: List<DiscoveryIdentity>): Boolean {
        if (id != requestId) return false
        results = incoming.distinctBy { it.placeId }.take(10)
        return true
    }
    fun clear() { requestId = null; results = emptyList() }
}
