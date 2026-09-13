package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places
import java.util.Locale

internal fun explicitDiscoveryLanguage(raw: Any?): String? = when ((raw as? String)?.trim()?.lowercase(Locale.ROOT)) {
    "hu", "hu-hu" -> "hu"
    "en", "en-us", "en-gb" -> "en"
    else -> null
}

/** Public SDK locale binding; never changes system/app auth locale or stores a key. */
internal object PlacesLocaleBinding {
    private var boundLanguageTag: String? = null

    @Synchronized fun ensure(context: Context, requested: Any? = null) {
        val appContext = context.applicationContext
        val locale = explicitDiscoveryLanguage(requested)?.let(Locale::forLanguageTag)
            ?: appContext.resources.configuration.locales[0]
        val tag = locale.toLanguageTag()
        if (Places.isInitialized() && boundLanguageTag == tag) return
        val appInfo = appContext.packageManager.getApplicationInfo(appContext.packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData?.getString("com.wayreveal.PLACES_API_KEY")?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error("WAYReveal local PLACES_API_KEY binding is unavailable.")
        // Documented reinitialization updates locale for existing clients/widgets.
        // No deinitialize, request, key logging, key persistence or quota mutation.
        Places.initializeWithNewPlacesApiEnabled(appContext, apiKey, locale)
        boundLanguageTag = tag
    }
}
