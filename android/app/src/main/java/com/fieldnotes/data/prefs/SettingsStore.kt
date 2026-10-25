package com.fieldnotes.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "fieldnotes_settings")

class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    private val planFilterKey = stringPreferencesKey("plan_filter")
    private val itemFilterKey = stringPreferencesKey("item_filter")

    val planFilter: Flow<String?> = dataStore.data.map { it[planFilterKey] }
    val itemFilter: Flow<String?> = dataStore.data.map { it[itemFilterKey] }

    suspend fun setPlanFilter(value: String) {
        dataStore.edit { it[planFilterKey] = value }
    }

    suspend fun setItemFilter(value: String) {
        dataStore.edit { it[itemFilterKey] = value }
    }
}
