package dev.ividi.militarycalisthenics.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ividi.militarycalisthenics.model.TrainingPlan
import dev.ividi.militarycalisthenics.model.WeightEntry
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "military_calisthenics_store")

private val PLAN_KEY = stringPreferencesKey("training_plan")
private val LANG_KEY = stringPreferencesKey("language")
private val WEIGHT_HISTORY_KEY = stringPreferencesKey("weight_history")
private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
private val REMINDER_HOUR_KEY = intPreferencesKey("reminder_hour")
private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

private val json = Json { ignoreUnknownKeys = true }

class PlanRepository(private val context: Context) {

    val planFlow: Flow<TrainingPlan?> = context.dataStore.data.map { prefs ->
        prefs[PLAN_KEY]?.let { raw ->
            runCatching { json.decodeFromString<TrainingPlan>(raw) }.getOrNull()
        }
    }

    val langFlow: Flow<Lang> = context.dataStore.data.map { prefs ->
        when (prefs[LANG_KEY]) {
            "EN" -> Lang.EN
            else -> Lang.PT
        }
    }

    suspend fun savePlan(plan: TrainingPlan) {
        context.dataStore.edit { it[PLAN_KEY] = json.encodeToString(plan) }
    }

    suspend fun clearPlan() {
        context.dataStore.edit { it.remove(PLAN_KEY) }
    }

    suspend fun setLang(lang: Lang) {
        context.dataStore.edit { it[LANG_KEY] = lang.name }
    }

    suspend fun currentPlan(): TrainingPlan? = planFlow.first()

    val weightHistoryFlow: Flow<List<WeightEntry>> = context.dataStore.data.map { prefs ->
        prefs[WEIGHT_HISTORY_KEY]?.let { raw ->
            runCatching { json.decodeFromString<List<WeightEntry>>(raw) }.getOrNull()
        }.orEmpty()
    }

    suspend fun addWeightEntry(entry: WeightEntry) {
        context.dataStore.edit { prefs ->
            val current = prefs[WEIGHT_HISTORY_KEY]?.let {
                runCatching { json.decodeFromString<List<WeightEntry>>(it) }.getOrNull()
            }.orEmpty()
            val updated = (current + entry).sortedBy { it.timestampMillis }
            prefs[WEIGHT_HISTORY_KEY] = json.encodeToString(updated)
        }
    }

    /** Removes one entry by timestamp and returns the remaining history, sorted oldest to newest. */
    suspend fun removeWeightEntry(timestampMillis: Long): List<WeightEntry> {
        var remaining: List<WeightEntry> = emptyList()
        context.dataStore.edit { prefs ->
            val current = prefs[WEIGHT_HISTORY_KEY]?.let {
                runCatching { json.decodeFromString<List<WeightEntry>>(it) }.getOrNull()
            }.orEmpty()
            remaining = current.filterNot { it.timestampMillis == timestampMillis }.sortedBy { it.timestampMillis }
            prefs[WEIGHT_HISTORY_KEY] = json.encodeToString(remaining)
        }
        return remaining
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "LIGHT" -> ThemeMode.LIGHT
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    val remindersEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[REMINDERS_ENABLED_KEY] ?: false }
    val reminderHourFlow: Flow<Int> = context.dataStore.data.map { it[REMINDER_HOUR_KEY] ?: 18 }

    suspend fun setReminderPreference(enabled: Boolean, hour: Int) {
        context.dataStore.edit { prefs ->
            prefs[REMINDERS_ENABLED_KEY] = enabled
            prefs[REMINDER_HOUR_KEY] = hour
        }
    }
}
