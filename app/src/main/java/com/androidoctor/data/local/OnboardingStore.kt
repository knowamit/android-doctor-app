package com.androidoctor.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

@Singleton
class OnboardingStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val doneKey = booleanPreferencesKey("onboarding_done")

    val done: Flow<Boolean> = context.onboardingDataStore.data.map { it[doneKey] ?: false }

    suspend fun markDone() {
        context.onboardingDataStore.edit { it[doneKey] = true }
    }
}
