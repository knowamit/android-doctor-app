package com.androidoctor.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quickScanDataStore by preferencesDataStore(name = "quick_scan_prefs")

@Singleton
class ScanCountStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val countKey = intPreferencesKey("scan_count")

    // Returns the scan index BEFORE incrementing (0 = first ever scan), so
    // callers can size scan duration off "how many times has this run before."
    suspend fun getAndIncrement(): Int {
        val current = context.quickScanDataStore.data.first()[countKey] ?: 0
        context.quickScanDataStore.edit { prefs -> prefs[countKey] = current + 1 }
        return current
    }
}
