package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

internal class WayrevealPlacesUiKitBridgePluginTest {
    @Test
    fun selectedPayloadPreservesLegacyContractWithoutPlaceId() {
        val payload = selectedPlacePayload(null)

        assertEquals("selected", payload["type"])
        assertEquals(true, payload["selected"])
        assertFalse(payload.containsKey("placeId"))
    }

    @Test
    fun selectedPayloadAddsGooglePlaceIdWithoutReplacingLegacyFields() {
        val payload = selectedPlacePayload("place-id-wayreveal-test")

        assertEquals("selected", payload["type"])
        assertEquals(true, payload["selected"])
        assertEquals("place-id-wayreveal-test", payload["placeId"])
        assertTrue(payload.keys.containsAll(setOf("type", "selected", "placeId")))
    }

    @Test
    fun selectedPayloadOmitsBlankPlaceId() {
        assertFalse(selectedPlacePayload("   ").containsKey("placeId"))
    }
}
