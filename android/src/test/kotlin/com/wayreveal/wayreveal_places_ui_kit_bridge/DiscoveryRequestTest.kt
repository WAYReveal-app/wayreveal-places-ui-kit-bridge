package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.*

internal class DiscoveryRequestTest {
    private fun args() = mapOf<String, Any>(
        "requestId" to "round-1", "categoryKey" to "food",
        "centerLatitude" to 47.4979, "centerLongitude" to 19.0402,
    )

    @Test fun nearbyUsesExplicitCoordinatesAndBoundedRadius() {
        val request = assertNotNull(DiscoveryRequest.fromArgs(args()))
        assertTrue(request.nearby)
        assertEquals(47.4979, request.latitude)
        assertEquals(19.0402, request.longitude)
        assertEquals(5000.0, request.radiusMeters)
        assertEquals(listOf("restaurant", "cafe", "bakery"), request.includedTypes)
        assertNull(request.query)
    }
    @Test fun noImplicitDestinationIsInvented() {
        assertNull(DiscoveryRequest.fromArgs(mapOf("requestId" to "round-1")))
        val explicit = assertNotNull(DiscoveryRequest.fromArgs(mapOf("requestId" to "round-2", "query" to "cafes in Vienna")))
        assertEquals("cafes in Vienna", explicit.query)
        assertFalse(explicit.nearby)
    }
    @Test fun walkingIntentIsNotTheSameNearbySearchAsGeneralExperiences() {
        val walking = assertNotNull(DiscoveryRequest.fromArgs(args() + mapOf(
            "categoryKey" to "experiences", "intentKey" to "walk_explore")))
        assertEquals(listOf("park"), walking.includedTypes)
        val experience = assertNotNull(DiscoveryRequest.fromArgs(args() + mapOf(
            "categoryKey" to "experiences", "intentKey" to "find_experience")))
        assertEquals(listOf("tourist_attraction", "museum", "park"), experience.includedTypes)
    }
    @Test fun unknownOrMismatchedIntentIsRejected() {
        assertNull(DiscoveryRequest.fromArgs(args() + ("intentKey" to "invented_intent")))
        assertNull(DiscoveryRequest.fromArgs(args() + mapOf("categoryKey" to "food", "intentKey" to "walk_explore")))
    }
    @Test fun malformedInputsFailClosedRatherThanFallBackToCrete() {
        for (lat in listOf(Double.NaN, Double.POSITIVE_INFINITY, -91.0, 91.0)) {
            assertNull(DiscoveryRequest.fromArgs(args() + ("centerLatitude" to lat)))
        }
        assertNull(DiscoveryRequest.fromArgs(args() - "centerLongitude"))
        assertNull(DiscoveryRequest.fromArgs(args() + ("centerLongitude" to 181.0)))
        assertNull(DiscoveryRequest.fromArgs(args() + ("radiusMeters" to 50001.0)))
        assertNull(DiscoveryRequest.fromArgs(args() + ("radiusMeters" to Double.NaN)))
        assertNull(DiscoveryRequest.fromArgs(args() + ("categoryKey" to "partner")))
        assertNull(DiscoveryRequest.fromArgs(args() + ("query" to "line\nbreak")))
        assertNull(DiscoveryRequest.fromArgs(args() + ("requestId" to "")))
    }
    @Test fun zeroCoordinatesAreValidWhenExplicit() {
        val request = assertNotNull(DiscoveryRequest.fromArgs(args() + mapOf("centerLatitude" to 0.0, "centerLongitude" to 0.0)))
        assertTrue(request.nearby)
    }
    @Test fun secondAreaActuallyChangesInput() {
        val a = assertNotNull(DiscoveryRequest.fromArgs(args()))
        val b = assertNotNull(DiscoveryRequest.fromArgs(args() + mapOf("centerLatitude" to 48.2082, "centerLongitude" to 16.3738)))
        assertNotEquals(a.latitude, b.latitude)
        assertNotEquals(a.longitude, b.longitude)
    }
    @Test fun identityPayloadCannotLeakRichGoogleOrPartnerContent() {
        val identity = assertNotNull(DiscoveryIdentity.create("place-id", 0.0, 0.0))
        assertEquals(setOf("placeId", "latitude", "longitude"), identity.toPayload().keys)
        assertNull(DiscoveryIdentity.create("bad id", 1.0, 2.0))
        assertNull(DiscoveryIdentity.create("x", null, 2.0))
    }
    @Test fun lateResponsesDoNotOverwriteCurrentRound() {
        val round = DiscoveryResultRound()
        round.begin("first")
        round.begin("second")
        assertFalse(round.accept("first", listOf(DiscoveryIdentity("stale", 1.0, 2.0))))
        assertTrue(round.results.isEmpty())
        assertTrue(round.accept("second", listOf(DiscoveryIdentity("fresh", 3.0, 4.0))))
        assertEquals("fresh", round.results.single().placeId)
    }
    @Test fun identityResultsAreUniqueBoundedAndClearable() {
        val round = DiscoveryResultRound()
        round.begin("first")
        val input = List(24) { DiscoveryIdentity("id-${it / 2}", 1.0, 2.0) }
        assertTrue(round.accept("first", input))
        assertEquals(10, round.results.size)
        assertEquals(10, round.results.map { it.placeId }.toSet().size)
        round.begin("new")
        assertTrue(round.results.isEmpty())
        round.clear()
        assertNull(round.requestId)
        assertFalse(round.accept("new", input))
    }
}
