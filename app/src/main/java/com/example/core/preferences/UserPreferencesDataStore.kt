package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "canva_ao_prefs")

class UserPreferencesDataStore(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val languageKey = stringPreferencesKey("language")
    private val soundEnabledKey = booleanPreferencesKey("sound_enabled")

    val themeModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[themeModeKey] ?: "dark"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[languageKey] ?: "pt"
    }

    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[soundEnabledKey] ?: true
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[languageKey] = lang }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[soundEnabledKey] = enabled }
    }
}
