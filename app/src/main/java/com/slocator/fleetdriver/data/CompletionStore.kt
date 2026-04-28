package com.slocator.fleetdriver.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.time.LocalDate

/**
 * Per-driver-per-day part completion state.
 *
 * Storage key shape:    "done|{driverId}|{date-iso}|{partNumber}" -> Boolean
 *
 * We expose a snapshot-state map so Compose recomposes immediately on toggle,
 * while the SharedPreferences is the source of truth across launches.
 */
class CompletionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val state: SnapshotStateMap<String, Boolean> = mutableStateMapOf()

    init {
        for ((k, v) in prefs.all) {
            if (k.startsWith(KEY_PREFIX) && v is Boolean) state[k] = v
        }
    }

    fun isDone(driverId: String, date: LocalDate?, partNumber: Int): Boolean =
        state[key(driverId, date, partNumber)] == true

    fun setDone(driverId: String, date: LocalDate?, partNumber: Int, value: Boolean) {
        val k = key(driverId, date, partNumber)
        state[k] = value
        prefs.edit().putBoolean(k, value).apply()
    }

    fun stateMap(): SnapshotStateMap<String, Boolean> = state

    fun key(driverId: String, date: LocalDate?, partNumber: Int): String =
        "$KEY_PREFIX|${driverId.trim()}|${date?.toString() ?: "-"}|$partNumber"

    companion object {
        private const val PREFS = "slocator_completion"
        private const val KEY_PREFIX = "done"
    }
}
