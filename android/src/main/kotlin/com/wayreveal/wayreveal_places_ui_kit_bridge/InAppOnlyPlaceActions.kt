package com.wayreveal.wayreveal_places_ui_kit_bridge

import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.model.CornerPlaceAction
import com.google.android.libraries.places.widget.model.PlaceAction
import com.google.android.libraries.places.widget.model.PlaceActionProvider

/** Official Advanced Details extension point; Google attribution is untouched. */
internal object InAppOnlyPlaceActions : PlaceActionProvider {
    override fun getMainPlaceActions(place: Place): List<PlaceAction> = emptyList()
    override fun getCornerPlaceActions(place: Place): List<CornerPlaceAction> = emptyList()

    // The provider is immutable and never retains an Activity or a listener.
    override fun addPlaceActionsChangedListener(listener: PlaceActionProvider.OnChangedListener) = Unit
    override fun removePlaceActionsChangedListener(listener: PlaceActionProvider.OnChangedListener) = Unit
}
