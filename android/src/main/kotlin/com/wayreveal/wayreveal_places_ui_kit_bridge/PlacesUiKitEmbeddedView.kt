package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.widget.PlaceSearchFragment
import com.google.android.libraries.places.widget.PlaceSearchFragmentListener
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.util.WeakHashMap

internal class PlacesUiKitEmbeddedView(
    context: Context,
    private val viewId: Int,
    activity: Activity?,
    private val channel: MethodChannel,
    private val query: String,
) : PlatformView {
    private val root = FrameLayout(context)
    private val fragmentActivity = activity as? FragmentActivity
    private val fragmentTag = fragmentTagForViewId(viewId)
    private val container = FragmentContainerView(context).apply {
        id = containerIdForViewId(viewId)
    }
    private var disposed = false
    private var pendingAttach = false
    private var pendingRemoval = false
    private var listenerFragment: PlaceSearchFragment? = null
    private var configuredFragment: PlaceSearchFragment? = null

    private val listener = object : PlaceSearchFragmentListener {
        override fun onLoad(places: List<Place>) {
            val first = places.firstOrNull()
            channel.invokeMethod(
                "onPlaceSearchEvent",
                mapOf(
                    "type" to "load",
                    "count" to places.size,
                    "firstPlaceId" to first?.id,
                    "firstLatitude" to first?.location?.latitude,
                    "firstLongitude" to first?.location?.longitude,
                ),
            )
        }

        override fun onRequestError(e: Exception) {
            channel.invokeMethod(
                "onPlaceSearchEvent",
                mapOf("type" to "error", "message" to (e.message ?: e.javaClass.simpleName)),
            )
        }

        override fun onPlaceSelected(place: Place) {
            channel.invokeMethod(
                "onPlaceSearchEvent",
                selectedPlacePayload(place.id) + mapOf(
                    "latitude" to place.location?.latitude,
                    "longitude" to place.location?.longitude,
                ),
            )
        }
    }

    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attachFragment("container_attached")
        }

        override fun onViewDetachedFromWindow(v: View) {
            removeOwnedFragment("container_detached", reattachWhenReady = true)
        }
    }

    init {
        root.addView(container, FrameLayout.LayoutParams(-1, -1))
        if (fragmentActivity != null) {
            container.addOnAttachStateChangeListener(attachStateListener)
            PlacesUiKitFragmentOwnerRegistry.register(fragmentActivity, this)
        } else {
            root.addView(TextView(context).apply {
                text = "Embedded Places UI Kit requires a FragmentActivity host."
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(-1, -1))
        }
    }

    override fun getView(): View = root

    override fun dispose() {
        if (disposed) return
        disposed = true
        pendingAttach = false
        container.removeOnAttachStateChangeListener(attachStateListener)
        removeOwnedFragment("dispose", reattachWhenReady = false)
        fragmentActivity?.let { PlacesUiKitFragmentOwnerRegistry.unregister(it, this) }
    }

    internal fun prepareForStateSave() {
        if (disposed) return
        removeOwnedFragment("host_prepare_state_save", reattachWhenReady = true)
    }

    internal fun prepareForHostPause() {
        if (disposed) return
        removeOwnedFragment("host_prepare_pause", reattachWhenReady = true)
    }

    internal fun onHostPostResume() {
        if (disposed) return
        val owner = fragmentActivity ?: return
        val manager = owner.supportFragmentManager
        if (pendingRemoval && !manager.isStateSaved && !manager.isDestroyed) {
            removeOwnedFragment("host_resume_pending_removal", reattachWhenReady = true)
        }
        if (root.isAttachedToWindow && container.isAttachedToWindow) {
            attachFragment("host_post_resume")
        }
    }

    internal fun onHostDestroyed() {
        listenerFragment?.unregisterListener()
        listenerFragment = null
        pendingAttach = false
        pendingRemoval = false
        disposed = true
    }

    internal fun ownershipTag(): String = fragmentTag

    private fun attachFragment(reason: String) {
        if (disposed) return
        val owner = fragmentActivity ?: return
        val manager = owner.supportFragmentManager
        if (!root.isAttachedToWindow || !container.isAttachedToWindow) {
            pendingAttach = true
            return
        }
        if (manager.isStateSaved || manager.isDestroyed) {
            pendingAttach = true
            return
        }
        if (pendingRemoval) {
            removeOwnedFragment("attach_pending_removal", reattachWhenReady = true)
            if (pendingRemoval) return
        }

        val byTag = manager.findFragmentByTag(fragmentTag)
        val byId = manager.findFragmentById(container.id)
        check(byTag == null || byId == null || byTag === byId) {
            "Conflicting fragments own tag=$fragmentTag and containerId=${container.id}."
        }
        val existing = byTag ?: byId
        val fragment = when (existing) {
            null -> PlaceSearchFragment.newInstance(PlaceSearchFragment.STANDARD_CONTENT).apply {
                selectable = true
            }
            is PlaceSearchFragment -> existing
            else -> error("Unexpected fragment type ${existing.javaClass.name} for $fragmentTag.")
        }
        check(fragment.tag == null || fragment.tag == fragmentTag) {
            "Fragment tag ${fragment.tag} does not match $fragmentTag."
        }
        check(fragment.id == 0 || fragment.id == container.id) {
            "Fragment container ${fragment.id} does not match ${container.id}."
        }

        registerListenerOnce(fragment)
        if (!fragment.isAdded) {
            manager.beginTransaction()
                .add(container, fragment, fragmentTag)
                .commitNow()
        }
        configureOnce(fragment)
        pendingAttach = false
    }

    private fun configureOnce(fragment: PlaceSearchFragment) {
        if (configuredFragment === fragment) return
        val request = SearchByTextRequest.builder(
            query,
            listOf(Place.Field.ID, Place.Field.LOCATION),
        )
            .setMaxResultCount(10)
            .setRegionCode("GR")
            .build()
        fragment.configureFromSearchByTextRequest(request)
        configuredFragment = fragment
    }

    private fun registerListenerOnce(fragment: PlaceSearchFragment) {
        if (listenerFragment === fragment) return
        listenerFragment?.unregisterListener()
        fragment.registerListener(listener)
        listenerFragment = fragment
    }

    private fun removeOwnedFragment(reason: String, reattachWhenReady: Boolean) {
        pendingAttach = reattachWhenReady && !disposed
        val owner = fragmentActivity ?: return
        val manager = owner.supportFragmentManager
        val fragment = manager.findFragmentByTag(fragmentTag)
            ?: manager.findFragmentById(container.id)
            ?: listenerFragment

        if (fragment != null && fragment !is PlaceSearchFragment) {
            error("Unexpected fragment type ${fragment.javaClass.name} for $fragmentTag.")
        }
        val placeFragment = fragment as? PlaceSearchFragment
        if (placeFragment != null && listenerFragment === placeFragment) {
            placeFragment.unregisterListener()
            listenerFragment = null
        }
        if (placeFragment == null || !placeFragment.isAdded) {
            pendingRemoval = false
            return
        }
        if (manager.isStateSaved || manager.isDestroyed) {
            pendingRemoval = true
            return
        }

        try {
            manager.beginTransaction().remove(placeFragment).commitNow()
        } catch (error: IllegalStateException) {
            val removed = manager.findFragmentByTag(fragmentTag) == null &&
                manager.findFragmentById(container.id) == null
            if (!isExpectedUninitializedPlacesCleanup(error) || !removed) throw error
        }
        pendingRemoval = false
    }

    private fun isExpectedUninitializedPlacesCleanup(error: IllegalStateException): Boolean {
        if (Places.isInitialized()) return false
        return generateSequence<Throwable>(error) { it.cause }
            .any { it.message?.contains("Places must be initialized first") == true }
    }

}

internal object PlacesUiKitFragmentOwnerRegistry {
    private val owners = WeakHashMap<FragmentActivity, MutableMap<String, PlacesUiKitEmbeddedView>>()

    @Synchronized
    fun register(activity: FragmentActivity, view: PlacesUiKitEmbeddedView) {
        val activityOwners = owners.getOrPut(activity) { linkedMapOf() }
        val previous = activityOwners[view.ownershipTag()]
        check(previous == null || previous === view) {
            "Duplicate active Places platform view for tag=${view.ownershipTag()}."
        }
        activityOwners[view.ownershipTag()] = view
    }

    @Synchronized
    fun unregister(activity: FragmentActivity, view: PlacesUiKitEmbeddedView) {
        val activityOwners = owners[activity] ?: return
        if (activityOwners[view.ownershipTag()] === view) {
            activityOwners.remove(view.ownershipTag())
        }
        if (activityOwners.isEmpty()) owners.remove(activity)
    }

    @Synchronized
    fun prepareForStateSave(activity: FragmentActivity) {
        owners[activity]?.values?.toList()?.forEach { it.prepareForStateSave() }
    }

    @Synchronized
    fun prepareForHostPause(activity: FragmentActivity) {
        owners[activity]?.values?.toList()?.forEach { it.prepareForHostPause() }
    }

    @Synchronized
    fun onHostPostResume(activity: FragmentActivity) {
        owners[activity]?.values?.toList()?.forEach { it.onHostPostResume() }
    }

    @Synchronized
    fun onHostDestroyed(activity: FragmentActivity) {
        owners.remove(activity)?.values?.toList()?.forEach { it.onHostDestroyed() }
    }
}

private const val PLATFORM_VIEW_CONTAINER_ID_BASE = 0x00e00000
private const val PLATFORM_VIEW_ID_MASK = 0x000fffff

internal fun containerIdForViewId(viewId: Int): Int {
    require(viewId in 0..PLATFORM_VIEW_ID_MASK) {
        "Platform viewId $viewId cannot be represented by the deterministic container ID scheme."
    }
    return PLATFORM_VIEW_CONTAINER_ID_BASE or viewId
}

internal fun fragmentTagForViewId(viewId: Int): String = "wayreveal_places_search_$viewId"

internal fun selectedPlacePayload(placeId: String?): Map<String, Any> =
    buildMap {
        put("type", "selected")
        put("selected", true)
        placeId?.takeIf { it.isNotBlank() }?.let { put("placeId", it) }
    }
