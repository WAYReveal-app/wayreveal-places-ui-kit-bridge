package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.*

internal class DiscoveryMapStateTest {
    private fun record(id: String) = mapOf("placeId" to id, "latitude" to 47.5, "longitude" to 19.0)
    @Test fun mapShowsAllRealRoundIdentitiesAndSelectedIdentity() {
        val state = assertNotNull(DiscoveryMapState.fromArgs(mapOf(
            "identities" to listOf(record("a"), record("b"), record("a")),
            "placeId" to "b", "requestId" to "round", "categoryKey" to "food",
        )))
        assertEquals(listOf("a", "b"), state.places.map { it.placeId })
        assertEquals("b", state.selectedPlaceId)
        assertEquals("round", state.requestId)
    }
    @Test fun noStaleSelectionOrFabricatedPartnerFlags() {
        val state = assertNotNull(DiscoveryMapState.fromArgs(mapOf(
            "identities" to listOf(record("a") + mapOf("partner" to true, "featured" to true)),
            "placeId" to "stale", "categoryKey" to "partner",
        )))
        assertNull(state.selectedPlaceId)
        assertEquals("all", state.category)
        assertEquals(setOf("placeId", "latitude", "longitude"), state.places.single().toPayload().keys)
    }
    @Test fun invalidEmptyOrOversizedInputCannotProduceUnboundedMarkers() {
        assertNull(DiscoveryMapState.fromArgs(mapOf("identities" to emptyList<Any>())))
        assertNull(DiscoveryMapState.fromArgs(mapOf("identities" to listOf(record("bad id")))))
        val state = assertNotNull(DiscoveryMapState.fromArgs(mapOf("identities" to List(100) { record("p$it") })))
        assertEquals(10, state.places.size)
    }
    @Test fun existingSingleSelectionContractStillWorks() {
        val state = assertNotNull(DiscoveryMapState.fromArgs(record("selected")))
        assertEquals("selected", state.selectedPlaceId)
        assertEquals(1, state.places.size)
    }
}
