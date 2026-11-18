package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppSettings
import com.example.domain.model.DistanceUnit
import com.example.domain.model.ThemePreference
import com.example.domain.model.TravelMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "rahnama_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_pref")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val SELECTED_VOICE = stringPreferencesKey("selected_voice")
        val DEFAULT_TRAVEL_MODE = stringPreferencesKey("default_travel_mode")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val LOCATION_ENABLED = booleanPreferencesKey("location_enabled")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val SHOW_SAVED_ON_MAP = booleanPreferencesKey("show_saved_on_map")
        val SHOW_REPORTS_ON_MAP = booleanPreferencesKey("show_reports_on_map")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_AVATAR = stringPreferencesKey("profile_avatar")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themePreference = prefs[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            voiceEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.9f,
            selectedVoiceName = prefs[Keys.SELECTED_VOICE] ?: "",
            defaultTravelMode = prefs[Keys.DEFAULT_TRAVEL_MODE]?.let { runCatching { TravelMode.valueOf(it) }.getOrNull() }
                ?: TravelMode.CAR,
            distanceUnit = prefs[Keys.DISTANCE_UNIT]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                ?: DistanceUnit.KM,
            locationSharingEnabled = prefs[Keys.LOCATION_ENABLED] ?: true,
            offlineModeEnabled = prefs[Keys.OFFLINE_MODE] ?: false,
            showSavedOnMap = prefs[Keys.SHOW_SAVED_ON_MAP] ?: true,
            showReportsOnMap = prefs[Keys.SHOW_REPORTS_ON_MAP] ?: true,
            profileName = prefs[Keys.PROFILE_NAME] ?: "کاربر مهمان",
            profileAvatarUri = prefs[Keys.PROFILE_AVATAR]
        )
    }

    suspend fun setTheme(pref: ThemePreference) {
        context.settingsDataStore.edit { it[Keys.THEME] = pref.name }
    }

    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }

    suspend fun setVoiceVolume(volume: Float) {
        context.settingsDataStore.edit { it[Keys.VOICE_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSelectedVoice(voiceName: String) {
        context.settingsDataStore.edit { it[Keys.SELECTED_VOICE] = voiceName }
    }

    suspend fun setDefaultTravelMode(mode: TravelMode) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_TRAVEL_MODE] = mode.name }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        context.settingsDataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    }

    suspend fun setLocationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LOCATION_ENABLED] = enabled }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setShowSavedOnMap(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_SAVED_ON_MAP] = show }
    }

    suspend fun setShowReportsOnMap(show: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_REPORTS_ON_MAP] = show }
    }

    suspend fun setProfile(name: String, avatarUri: String?) {
        context.settingsDataStore.edit {
            it[Keys.PROFILE_NAME] = name.ifBlank { "کاربر مهمان" }
            if (avatarUri != null) {
                it[Keys.PROFILE_AVATAR] = avatarUri
            } else {
                it.remove(Keys.PROFILE_AVATAR)
            }
        }
    }
}
