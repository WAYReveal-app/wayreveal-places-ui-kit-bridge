package com.wayreveal.wayreveal_places_ui_kit_bridge

/** Google identities/coordinates only, never fabricated Partner/Benefit/Featured flags. */
internal data class DiscoveryMapState(
    val requestId: String,
    val category: String,
    val places: List<DiscoveryIdentity>,
    val selectedPlaceId: String?,
) {
    companion object {
        fun fromArgs(params: Map<*, *>): DiscoveryMapState? {
            val records = params["identities"] as? List<*>
            val places = if (records != null) records.take(10).mapNotNull { item ->
                val record = item as? Map<*, *> ?: return@mapNotNull null
                DiscoveryIdentity.create(record["placeId"] as? String,
                    (record["latitude"] as? Number)?.toDouble(), (record["longitude"] as? Number)?.toDouble())
            } else listOfNotNull(DiscoveryIdentity.create(params["placeId"] as? String,
                (params["latitude"] as? Number)?.toDouble(), (params["longitude"] as? Number)?.toDouble()))
            val unique = places.distinctBy { it.placeId }
            if (unique.isEmpty()) return null
            val selected = (params["placeId"] as? String)?.takeIf { id -> unique.any { it.placeId == id } }
            val category = (params["categoryKey"] as? String)?.takeIf { it in DiscoveryRequest.categories } ?: "all"
            val requestId = (params["requestId"] as? String)?.takeIf { it.isNotBlank() && it.length <= 160 && it.none(Char::isISOControl) }
                ?: "legacy-map"
            return DiscoveryMapState(requestId, category, unique, selected)
        }
    }
}
