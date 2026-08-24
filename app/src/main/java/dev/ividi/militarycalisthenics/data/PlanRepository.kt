package dev.ividi.militarycalisthenics.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ividi.militarycalisthenics.model.TrainingPlan
import dev.ividi.militarycalisthenics.model.WeightEntry
import dev.ividi.militarycalisthenics.ui.Lang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "military_calisthenics_store")

private val PLAN_KEY = stringPreferencesKey("training_plan")
private val LANG_KEY = stringPreferencesKey("language")
private val WEIGHT_HISTORY_KEY = stringPreferencesKey("weight_history")

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
}
