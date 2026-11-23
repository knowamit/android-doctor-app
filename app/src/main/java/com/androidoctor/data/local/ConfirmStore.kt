package com.androidoctor.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.confirmDataStore by preferencesDataStore(name = "confirm_prefs")

/** Remembers which system-change confirmations the user chose to skip. */
@Singleton
class ConfirmStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val skipKey = booleanPreferencesKey("skip_system_change_confirm")

    suspend fun skip(): Boolean = context.confirmDataStore.data.map { it[skipKey] ?: false }.first()

    suspend fun setSkip(value: Boolean) {
        context.confirmDataStore.edit { it[skipKey] = value }
    }
}
