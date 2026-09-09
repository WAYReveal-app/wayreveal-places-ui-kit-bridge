package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PlaceDetailsUiKitFragmentOwnershipTest {
    @Test
    fun `details container identity is stable and isolated from search`() {
        assertEquals(0x00d00000, detailsContainerIdForViewId(0))
        assertEquals(0x00d0002a, detailsContainerIdForViewId(42))
        assertNotEquals(containerIdForViewId(42), detailsContainerIdForViewId(42))
    }

    @Test
    fun `details fragment tag is deterministic and namespaced`() {
        assertEquals("wayreveal_places_details_0", detailsFragmentTagForViewId(0))
        assertEquals("wayreveal_places_details_42", detailsFragmentTagForViewId(42))
    }

    @Test
    fun `out of range details platform view IDs fail explicitly`() {
        assertFailsWith<IllegalArgumentException> { detailsContainerIdForViewId(-1) }
        assertFailsWith<IllegalArgumentException> { detailsContainerIdForViewId(0x00100000) }
    }
}
