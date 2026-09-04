package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.google.android.libraries.places.widget.PlaceSearchFragment

class PlacesUiKitProbeActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = FragmentContainerView(this).apply { id = View.generateViewId() }
        setContentView(container)
        if (savedInstanceState == null) {
            val fragment = PlaceSearchFragment.newInstance(PlaceSearchFragment.STANDARD_CONTENT)
            fragment.selectable = true
            supportFragmentManager.beginTransaction()
                .replace(container.id, fragment)
                .commitNow()
        }
    }
}
