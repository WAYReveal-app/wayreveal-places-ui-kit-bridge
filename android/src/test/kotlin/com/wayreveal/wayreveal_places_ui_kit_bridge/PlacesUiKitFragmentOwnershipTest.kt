package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PlacesUiKitFragmentOwnershipTest {
    @Test
    fun `container identity is stable for a Flutter platform view ID`() {
        assertEquals(containerIdForViewId(0), containerIdForViewId(0))
        assertEquals(0x00e00000, containerIdForViewId(0))
        assertEquals(0x00e0002a, containerIdForViewId(42))
        assertNotEquals(containerIdForViewId(1), containerIdForViewId(2))
    }

    @Test
    fun `fragment tag is deterministic and namespaced`() {
        assertEquals("wayreveal_places_search_0", fragmentTagForViewId(0))
        assertEquals("wayreveal_places_search_42", fragmentTagForViewId(42))
    }

    @Test
    fun `out of range platform view IDs fail explicitly`() {
        assertFailsWith<IllegalArgumentException> { containerIdForViewId(-1) }
        assertFailsWith<IllegalArgumentException> { containerIdForViewId(0x00100000) }
    }
}
