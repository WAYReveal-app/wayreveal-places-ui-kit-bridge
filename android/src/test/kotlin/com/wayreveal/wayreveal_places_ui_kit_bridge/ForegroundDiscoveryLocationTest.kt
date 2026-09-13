package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.*

internal class ForegroundDiscoveryLocationTest {
    @Test fun approximateAndZeroCoordinateFixesAreUsable() {
        assertTrue(usableDiscoveryFix(47.49, 19.04, 1000, 3000.0))
        assertTrue(usableDiscoveryFix(0.0, 0.0, 0, 10.0))
    }
    @Test fun staleFutureInvalidAndUnboundedAccuracyAreRejected() {
        assertFalse(usableDiscoveryFix(47.49, 19.04, 120001, 10.0))
        assertFalse(usableDiscoveryFix(47.49, 19.04, -1, 10.0))
        assertFalse(usableDiscoveryFix(91.0, 19.04, 0, 10.0))
        assertFalse(usableDiscoveryFix(47.49, Double.NaN, 0, 10.0))
        assertFalse(usableDiscoveryFix(47.49, 19.04, 0, Double.NaN))
        assertFalse(usableDiscoveryFix(47.49, 19.04, 0, 10001.0))
    }
}
