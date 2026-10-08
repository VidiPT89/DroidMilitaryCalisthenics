package dev.ividi.militarycalisthenics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ividi.militarycalisthenics.data.PlanRepository
import dev.ividi.militarycalisthenics.model.TrainingPlan
import dev.ividi.militarycalisthenics.model.UserProfile
import dev.ividi.militarycalisthenics.model.WeeklyPlan
import dev.ividi.militarycalisthenics.model.WeightEntry
import dev.ividi.militarycalisthenics.model.next
import dev.ividi.militarycalisthenics.planengine.PlanEngine
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PlanRepository) : ViewModel() {

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _plan = MutableStateFlow<TrainingPlan?>(null)
    val plan: StateFlow<TrainingPlan?> = _plan.asStateFlow()

    private val _lang = MutableStateFlow(Lang.PT)
    val lang: StateFlow<Lang> = _lang.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _weightHistory = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightHistory: StateFlow<List<WeightEntry>> = _weightHistory.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(false)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(18)
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _planCompletionAcknowledged = MutableStateFlow(false)
    val planCompletionAcknowledged: StateFlow<Boolean> = _planCompletionAcknowledged.asStateFlow()

    init {
        viewModelScope.launch {
            _plan.value = repository.currentPlan()
            _isLoaded.value = true
        }
        viewModelScope.launch {
            repository.langFlow.collect { _lang.value = it }
        }
        viewModelScope.launch {
            repository.themeModeFlow.collect { _themeMode.value = it }
        }
        viewModelScope.launch {
            repository.weightHistoryFlow.collect { _weightHistory.value = it }
        }
        viewModelScope.launch {
            repository.remindersEnabledFlow.collect { _remindersEnabled.value = it }
        }
        viewModelScope.launch {
            repository.reminderHourFlow.collect { _reminderHour.value = it }
        }
        viewModelScope.launch {
            repository.planCompletionAcknowledgedFlow.collect { _planCompletionAcknowledged.value = it }
        }
    }

    fun setReminders(enabled: Boolean, hour: Int) {
        _remindersEnabled.value = enabled
        _reminderHour.value = hour
        viewModelScope.launch { repository.setReminderPreference(enabled, hour) }
    }

    fun generatePlan(profile: UserProfile) {
        if (!profile.isValid || _plan.value?.profile == profile) return
        val generated = PlanEngine.generate(profile)
        _plan.value = generated
        _planCompletionAcknowledged.value = false
        viewModelScope.launch {
            repository.savePlan(generated)
            repository.setPlanCompletionAcknowledged(false)
        }
    }

    /** Re-runs the plan engine against the current profile without touching any inputs. */
    fun regeneratePlan() {
        val current = _plan.value ?: return
        val regenerated = PlanEngine.generate(current.profile)
        _plan.value = regenerated
        _planCompletionAcknowledged.value = false
        viewModelScope.launch {
            repository.savePlan(regenerated)
            repository.setPlanCompletionAcknowledged(false)
        }
    }

    /**
     * Deletes a logged weight entry. If it was the most recent one, recalibrates the plan to
     * whatever entry is now most recent, or leaves the profile's weight untouched if the
     * history becomes empty.
     */
    fun deleteWeightEntry(timestampMillis: Long) {
        viewModelScope.launch {
            val wasMostRecent = _weightHistory.value.maxByOrNull { it.timestampMillis }?.timestampMillis == timestampMillis
            val remaining = repository.removeWeightEntry(timestampMillis)
            if (!wasMostRecent) return@launch
            val current = _plan.value ?: return@launch
            val mostRecent = remaining.maxByOrNull { it.timestampMillis } ?: return@launch
            if (mostRecent.weightKg == current.profile.weightKg) return@launch
            val updatedProfile = current.profile.copy(weightKg = mostRecent.weightKg)
            val recalibrated = PlanEngine.generate(updatedProfile)
            _plan.value = recalibrated
            _planCompletionAcknowledged.value = false
            repository.savePlan(recalibrated)
            repository.setPlanCompletionAcknowledged(false)
        }
    }

    fun toggleWorkoutCompleted(weekIndex: Int, dayIndex: Int) {
        val current = _plan.value ?: return
        val updatedWeeks = current.weeks.map { week ->
            if (week.weekIndex != weekIndex) return@map week
            WeeklyPlan(
                weekIndex = week.weekIndex,
                workouts = week.workouts.map { workout ->
                    if (workout.dayIndex == dayIndex) workout.copy(completed = !workout.completed) else workout
                }
            )
        }
        val updated = current.copy(weeks = updatedWeeks)
        _plan.value = updated
        viewModelScope.launch { repository.savePlan(updated) }
    }

    /** Records that the user has seen the plan-completion prompt for the current plan. */
    fun acknowledgePlanComplete() {
        _planCompletionAcknowledged.value = true
        viewModelScope.launch { repository.setPlanCompletionAcknowledged(true) }
    }

    /** Moves the profile to the next FitnessLevel and regenerates the plan for it. No-op at ADVANCED. */
    fun levelUp() {
        val current = _plan.value ?: return
        val next = current.profile.level.next ?: return
        val updatedProfile = current.profile.copy(level = next)
        val regenerated = PlanEngine.generate(updatedProfile)
        _plan.value = regenerated
        _planCompletionAcknowledged.value = false
        viewModelScope.launch {
            repository.savePlan(regenerated)
            repository.setPlanCompletionAcknowledged(false)
        }
    }

    fun setLang(lang: Lang) {
        _lang.value = lang
        viewModelScope.launch { repository.setLang(lang) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    /**
     * Logs a new bodyweight reading and regenerates the plan through the same calibration
     * (weight/BMI signal, age, level, goal), per docs/plan-engine-spec.md.
     */
    fun logWeight(weightKg: Double, timestampMillis: Long = System.currentTimeMillis()) {
        if (weightKg !in UserProfile.WEIGHT_RANGE || timestampMillis > System.currentTimeMillis()) return
        if (_plan.value == null) return
        viewModelScope.launch {
            val history = repository.addWeightEntry(WeightEntry(timestampMillis, weightKg))
            val newest = history.lastOrNull() ?: return@launch
            if (newest.timestampMillis != timestampMillis) return@launch
            val current = _plan.value ?: return@launch
            if (current.profile.weightKg == newest.weightKg) return@launch
            val recalibrated = PlanEngine.generate(current.profile.copy(weightKg = newest.weightKg))
            _plan.value = recalibrated
            _planCompletionAcknowledged.value = false
            repository.savePlan(recalibrated)
            repository.setPlanCompletionAcknowledged(false)
        }
    }
}
