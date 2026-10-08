package dev.ividi.militarycalisthenics.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
private val ACK_KEY = booleanPreferencesKey("plan_completion_acknowledged")
private val json = Json { ignoreUnknownKeys = true }

data class StoredState(
    val plan: TrainingPlan? = null,
    val weightHistory: List<WeightEntry> = emptyList(),
    val lang: Lang = Lang.PT,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 18,
    val completionAcknowledged: Boolean = false
)

/** Existing preference keys are preserved; each change is one durable transaction. */
class PlanRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.dataStore)

    val stateFlow: Flow<StoredState> = store.data.map(::decode)

    suspend fun update(transform: (StoredState) -> StoredState): StoredState {
        var result = StoredState()
        store.edit { prefs ->
            // Malformed data must fail visibly, never become an empty replacement.
            result = transform(decode(prefs))
            require(result.plan == null || result.plan!!.profile.isValid) { "Invalid profile update" }
            require(result.weightHistory.all { it.weightKg in 30.0..250.0 }) { "Invalid weight update" }
            if (result.plan == null) prefs.remove(PLAN_KEY)
            else prefs[PLAN_KEY] = json.encodeToString(result.plan)
            prefs[WEIGHT_HISTORY_KEY] = json.encodeToString(result.weightHistory)
            prefs[LANG_KEY] = result.lang.name
            prefs[THEME_MODE_KEY] = result.themeMode.name
            prefs[REMINDERS_ENABLED_KEY] = result.remindersEnabled
            prefs[REMINDER_HOUR_KEY] = result.reminderHour
            prefs[ACK_KEY] = result.completionAcknowledged
            result = decode(prefs)
        }
        return result
    }

    private fun decode(prefs: Preferences): StoredState {
        val plan = prefs[PLAN_KEY]?.let { json.decodeFromString<TrainingPlan>(it) }
        require(plan == null || plan.profile.isValid) { "Invalid stored profile" }
        val history = prefs[WEIGHT_HISTORY_KEY]?.let { json.decodeFromString<List<WeightEntry>>(it) }.orEmpty()
        require(history.all { it.weightKg in 30.0..250.0 }) { "Invalid stored weight" }
        return StoredState(
            plan = plan,
            weightHistory = history.sortedBy { it.timestampMillis },
            lang = if (prefs[LANG_KEY] == "EN") Lang.EN else Lang.PT,
            themeMode = ThemeMode.entries.find { it.name == prefs[THEME_MODE_KEY] } ?: ThemeMode.DARK,
            remindersEnabled = prefs[REMINDERS_ENABLED_KEY] ?: false,
            reminderHour = (prefs[REMINDER_HOUR_KEY] ?: 18).coerceIn(0, 23),
            completionAcknowledged = prefs[ACK_KEY] ?: false
        )
    }
}
