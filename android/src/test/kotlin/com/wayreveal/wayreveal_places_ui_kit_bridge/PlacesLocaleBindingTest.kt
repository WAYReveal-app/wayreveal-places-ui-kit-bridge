package com.wayreveal.wayreveal_places_ui_kit_bridge

import kotlin.test.*

internal class PlacesLocaleBindingTest {
    @Test fun acceptedHungarianAndEnglishVariantsAreExplicit() {
        for (raw in listOf("hu", " hu-HU ", "HU")) assertEquals("hu", explicitDiscoveryLanguage(raw))
        for (raw in listOf("en", "en-GB", " en-US ")) assertEquals("en", explicitDiscoveryLanguage(raw))
    }

    @Test fun missingOrUnsupportedLanguageLeavesTheNativeDefaultAvailable() {
        for (raw in listOf(null, "", "invalid", "hu\nvalue", 42)) assertNull(explicitDiscoveryLanguage(raw))
    }
}
