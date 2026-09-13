package com.wayreveal.wayreveal_places_ui_kit_bridge

import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.model.PlaceActionProvider
import org.mockito.Mockito
import kotlin.test.*

internal class InAppOnlyPlaceActionsTest {
    @Test fun neitherInlineNorCornerCanOfferAnExternalMapAction() {
        val place = Mockito.mock(Place::class.java)
        assertTrue(InAppOnlyPlaceActions.getMainPlaceActions(place).isEmpty())
        assertTrue(InAppOnlyPlaceActions.getCornerPlaceActions(place).isEmpty())
        Mockito.verifyNoInteractions(place)
    }

    @Test fun immutableProviderDoesNotRetainOrNotifyListeners() {
        val listener = Mockito.mock(PlaceActionProvider.OnChangedListener::class.java)
        InAppOnlyPlaceActions.addPlaceActionsChangedListener(listener)
        InAppOnlyPlaceActions.removePlaceActionsChangedListener(listener)
        Mockito.verifyNoInteractions(listener)
    }

    @Test fun attemptsArePersistedBeforeLoadAndStopAtFifty() {
        var stored = 0
        repeat(50) { index ->
            assertEquals(index + 1, reserveDevelopmentDetailsAttempt({ stored }, { stored = it; true }))
        }
        repeat(3) { assertNull(reserveDevelopmentDetailsAttempt({ stored }, { fail("Over-budget write") })) }
        assertEquals(50, stored)
    }

    @Test fun storageFailureAndCorruptionFailClosed() {
        assertNull(reserveDevelopmentDetailsAttempt({ 0 }, { false }))
        for (used in listOf(-1, 50, 51, Int.MAX_VALUE)) {
            assertNull(reserveDevelopmentDetailsAttempt({ used }, { fail("Invalid counter write") }))
        }
    }

    @Test fun reconstructionDoesNotResetPersistedBudget() {
        var stored = 49
        assertEquals(50, reserveDevelopmentDetailsAttempt({ stored }, { stored = it; true }))
        assertNull(reserveDevelopmentDetailsAttempt({ stored }, { fail("Recreated budget reset") }))
    }
}
