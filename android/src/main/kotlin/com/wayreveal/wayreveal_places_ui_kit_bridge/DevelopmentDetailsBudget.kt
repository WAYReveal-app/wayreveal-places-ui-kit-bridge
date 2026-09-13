package com.wayreveal.wayreveal_places_ui_kit_bridge

import android.content.Context

/** Temporary development guard, not a production billing policy.
 * Conservative proof guard: at most 50 Details load attempts. The latest owner
 * decision makes Essentials the default; no Advanced/Pro requests are made.
 * Keeping the same counter does not replenish or expand the earlier allowance.
 * Only an integer is stored. Never reset this counter during the proof campaign.
 * Requests on another emulator/install must be subtracted in the proof ledger.
 */
internal object DevelopmentDetailsBudget {
    const val LIMIT = 50
    private const val STORE = "wayreveal_pro_details_proof_20260913"

    @Synchronized
    fun reserve(context: Context): Int? {
        val preferences = context.applicationContext.getSharedPreferences(STORE, Context.MODE_PRIVATE)
        return reserveDevelopmentDetailsAttempt(
            read = { preferences.getInt("attempts", 0) },
            persist = { preferences.edit().putInt("attempts", it).commit() },
        )
    }
}

/** Reserve before invoking Google. Failure/timeout still consumes the reservation. */
internal fun reserveDevelopmentDetailsAttempt(read: () -> Int, persist: (Int) -> Boolean): Int? {
    val used = read()
    if (used !in 0 until DevelopmentDetailsBudget.LIMIT) return null
    val next = used + 1
    return if (persist(next)) next else null
}
