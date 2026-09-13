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
import com.google.android.libraries.places.widget.PlaceDetailsCompactFragment
import com.google.android.libraries.places.widget.PlaceLoadListener
import com.google.android.libraries.places.widget.model.Orientation
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import java.util.WeakHashMap

internal class PlaceDetailsUiKitEmbeddedView(
    context: Context,
    private val viewId: Int,
    activity: Activity?,
    private val channel: MethodChannel,
    private val placeId: String?,
) : PlatformView {
    private val root = FrameLayout(context)
    private val fragmentActivity = activity as? FragmentActivity
    private val fragmentTag = detailsFragmentTagForViewId(viewId)
    private val container = FragmentContainerView(context).apply {
        id = detailsContainerIdForViewId(viewId)
    }
    private var disposed = false
    private var pendingAttach = false
    private var pendingRemoval = false
    private var activeFragment: PlaceDetailsCompactFragment? = null

    private val listener = object : PlaceLoadListener {
        override fun onSuccess(place: Place) {
            if (disposed) return
            channel.invokeMethod(
                "onPlaceDetailsEvent",
                mapOf("type" to "ready", "placeId" to placeId, "viewId" to viewId),
            )
        }

        override fun onFailure(e: Exception) {
            if (disposed) return
            channel.invokeMethod(
                "onPlaceDetailsEvent",
                mapOf("type" to "error", "placeId" to placeId, "viewId" to viewId),
            )
        }
    }

    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attachFragment()
        }

        override fun onViewDetachedFromWindow(v: View) {
            removeOwnedFragment(reattachWhenReady = true)
        }
    }

    init {
        root.addView(container, FrameLayout.LayoutParams(-1, -1))
        if (fragmentActivity != null && placeId != null) {
            container.addOnAttachStateChangeListener(attachStateListener)
            PlaceDetailsUiKitFragmentOwnerRegistry.register(fragmentActivity, this)
        } else {
            root.addView(TextView(context).apply {
                text = if (fragmentActivity == null) {
                    "Place details require a FragmentActivity host."
                } else {
                    "No valid place is selected."
                }
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
        removeOwnedFragment(reattachWhenReady = false)
        fragmentActivity?.let { PlaceDetailsUiKitFragmentOwnerRegistry.unregister(it, this) }
    }

    internal fun prepareForStateSave() {
        if (!disposed) removeOwnedFragment(reattachWhenReady = true)
    }

    internal fun prepareForHostPause() {
        if (!disposed) removeOwnedFragment(reattachWhenReady = true)
    }

    internal fun onHostPostResume() {
        if (disposed) return
        val owner = fragmentActivity ?: return
        val manager = owner.supportFragmentManager
        if (pendingRemoval && !manager.isStateSaved && !manager.isDestroyed) {
            removeOwnedFragment(reattachWhenReady = true)
        }
        if (root.isAttachedToWindow && container.isAttachedToWindow) attachFragment()
    }

    internal fun onHostDestroyed() {
        disposed = true
        activeFragment?.setPlaceLoadListener(DetachedPlaceLoadListener)
        activeFragment = null
        pendingAttach = false
        pendingRemoval = false
    }

    internal fun ownershipTag(): String = fragmentTag

    private fun attachFragment() {
        if (disposed) return
        val selectedPlaceId = placeId ?: return
        val owner = fragmentActivity ?: return
        val manager = owner.supportFragmentManager
        if (!root.isAttachedToWindow || !container.isAttachedToWindow ||
            manager.isStateSaved || manager.isDestroyed
        ) {
            pendingAttach = true
            return
        }
        if (pendingRemoval) {
            removeOwnedFragment(reattachWhenReady = true)
            if (pendingRemoval) return
        }

        val byTag = manager.findFragmentByTag(fragmentTag)
        val byId = manager.findFragmentById(container.id)
        check(byTag == null || byId == null || byTag === byId) {
            "Conflicting fragments own tag=$fragmentTag and containerId=${container.id}."
        }
        val existing = byTag ?: byId
        val fragment = when (existing) {
            null -> PlaceDetailsCompactFragment.newInstance(
                PlaceDetailsCompactFragment.STANDARD_CONTENT,
                Orientation.VERTICAL,
                com.google.android.libraries.places.R.style.PlacesMaterialTheme,
            )
            is PlaceDetailsCompactFragment -> existing
            else -> error("Unexpected fragment type ${existing.javaClass.name} for $fragmentTag.")
        }
        // Owner's latest cost/UX direction: Essentials is the default. Google's
        // own actions and attribution remain intact; WAYReveal's primary
        // discovery actions stay in-app. No Advanced/Pro fragment or prefetch.
        fragment.preferTruncation = false
        fragment.setPlaceLoadListener(listener)
        activeFragment = fragment
        if (!fragment.isAdded) {
            manager.beginTransaction().add(container, fragment, fragmentTag).commitNow()
            root.post {
                if (disposed || activeFragment !== fragment || !fragment.isAdded) return@post
                if (!Places.isInitialized()) {
                    channel.invokeMethod("onPlaceDetailsEvent", mapOf("type" to "error", "reason" to "not_initialized", "placeId" to placeId, "viewId" to viewId))
                    return@post
                }
                val attempt = DevelopmentDetailsBudget.reserve(root.context)
                if (attempt == null) {
                    channel.invokeMethod("onPlaceDetailsEvent", mapOf("type" to "error", "reason" to "development_budget_exhausted", "placeId" to placeId, "viewId" to viewId))
                    return@post
                }
                channel.invokeMethod("onPlaceDetailsEvent", mapOf("type" to "development_request", "componentTier" to "essentials", "attempt" to attempt, "placeId" to placeId, "viewId" to viewId))
                fragment.loadWithPlaceId(selectedPlaceId)
            }
        }
        pendingAttach = false
    }

    private fun removeOwnedFragment(reattachWhenReady: Boolean) {
        pendingAttach = reattachWhenReady && !disposed
        val owner = fragmentActivity ?: return
        val manager = owner.supportFragmentManager
        val fragment = manager.findFragmentByTag(fragmentTag)
            ?: manager.findFragmentById(container.id)
            ?: activeFragment
        if (fragment != null && fragment !is PlaceDetailsCompactFragment) {
            error("Unexpected fragment type ${fragment.javaClass.name} for $fragmentTag.")
        }
        val details = fragment
        if (details != null && activeFragment === details) {
            details.setPlaceLoadListener(DetachedPlaceLoadListener)
            activeFragment = null
        }
        if (details == null || !details.isAdded) {
            pendingRemoval = false
            return
        }
        if (manager.isStateSaved || manager.isDestroyed) {
            pendingRemoval = true
            return
        }
        try {
            manager.beginTransaction().remove(details).commitNow()
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

private object DetachedPlaceLoadListener : PlaceLoadListener {
    override fun onSuccess(place: Place) = Unit

    override fun onFailure(e: Exception) = Unit
}

internal object PlaceDetailsUiKitFragmentOwnerRegistry {
    private val owners =
        WeakHashMap<FragmentActivity, MutableMap<String, PlaceDetailsUiKitEmbeddedView>>()

    @Synchronized
    fun register(activity: FragmentActivity, view: PlaceDetailsUiKitEmbeddedView) {
        val activityOwners = owners.getOrPut(activity) { linkedMapOf() }
        val previous = activityOwners[view.ownershipTag()]
        check(previous == null || previous === view) {
            "Duplicate active Place Details platform view for tag=${view.ownershipTag()}."
        }
        activityOwners[view.ownershipTag()] = view
    }

    @Synchronized
    fun unregister(activity: FragmentActivity, view: PlaceDetailsUiKitEmbeddedView) {
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

private const val DETAILS_PLATFORM_VIEW_CONTAINER_ID_BASE = 0x00d00000
private const val DETAILS_PLATFORM_VIEW_ID_MASK = 0x000fffff

internal fun detailsContainerIdForViewId(viewId: Int): Int {
    require(viewId in 0..DETAILS_PLATFORM_VIEW_ID_MASK) {
        "Platform viewId $viewId cannot be represented by the deterministic details container scheme."
    }
    return DETAILS_PLATFORM_VIEW_CONTAINER_ID_BASE or viewId
}

internal fun detailsFragmentTagForViewId(viewId: Int): String =
    "wayreveal_places_details_$viewId"
