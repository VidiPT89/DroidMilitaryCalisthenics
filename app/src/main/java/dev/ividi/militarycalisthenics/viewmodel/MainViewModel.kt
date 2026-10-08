package dev.ividi.militarycalisthenics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ividi.militarycalisthenics.data.PlanRepository
import dev.ividi.militarycalisthenics.data.StoredState
import dev.ividi.militarycalisthenics.model.UserProfile
import dev.ividi.militarycalisthenics.model.WeightEntry
import dev.ividi.militarycalisthenics.model.toggleWorkout
import dev.ividi.militarycalisthenics.model.next
import dev.ividi.militarycalisthenics.planengine.PlanEngine
import dev.ividi.militarycalisthenics.ui.Lang
import dev.ividi.militarycalisthenics.ui.theme.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(private val repository: PlanRepository) : ViewModel() {
    private val _state = MutableStateFlow(StoredState())
    val state = _state.asStateFlow()
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()
    private val _errorKey = MutableStateFlow<String?>(null)
    val errorKey = _errorKey.asStateFlow()
    private val mutationMutex = Mutex()
    private var loadingJob: Job? = null

    init { retryLoad() }

    fun retryLoad() {
        loadingJob?.cancel()
        _errorKey.value = null
        loadingJob = viewModelScope.launch {
            try {
                repository.stateFlow.collect {
                    _state.value = it
                    _isLoaded.value = true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _isLoaded.value = false
                _errorKey.value = "storage_read_error"
            }
        }
    }

    fun dismissError() { _errorKey.value = null }

    private fun update(onSaved: () -> Unit = {}, transform: (StoredState) -> StoredState) {
        if (!_isLoaded.value) return
        viewModelScope.launch {
            try {
                // DataStore serializes transformations against its latest durable state.
                // Only the collector publishes state, avoiding stale optimistic snapshots.
                mutationMutex.withLock {
                    val saved = repository.update(transform)
                    _state.first { it == saved }
                    onSaved()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _errorKey.value = "storage_save_error"
            }
        }
    }

    fun setReminders(enabled: Boolean, hour: Int) = update {
        it.copy(remindersEnabled = enabled, reminderHour = hour.coerceIn(0, 23))
    }

    fun generatePlan(profile: UserProfile, onSaved: () -> Unit = {}) {
        if (!profile.isValid) return
        update(onSaved) { state ->
            if (state.plan?.profile == profile) state
            else state.copy(plan = PlanEngine.generate(profile), completionAcknowledged = false)
        }
    }

    fun regeneratePlan(onSaved: () -> Unit = {}) = update(onSaved) { state ->
        state.plan?.let { state.copy(plan = PlanEngine.generate(it.profile), completionAcknowledged = false) } ?: state
    }

    fun deleteWeightEntry(timestampMillis: Long) = update { state ->
        val history = state.weightHistory.filterNot { it.timestampMillis == timestampMillis }
        val weight = if (state.weightHistory.lastOrNull()?.timestampMillis == timestampMillis) history.lastOrNull()?.weightKg else null
        recalibrate(state.copy(weightHistory = history), weight)
    }

    fun toggleWorkoutCompleted(weekIndex: Int, dayIndex: Int) = update { state ->
        state.copy(plan = state.plan?.toggleWorkout(weekIndex, dayIndex))
    }

    fun markWorkoutComplete(weekIndex: Int, dayIndex: Int) = update { state ->
        state.copy(plan = state.plan?.toggleWorkout(weekIndex, dayIndex, completed = true))
    }

    fun acknowledgePlanComplete() = update { it.copy(completionAcknowledged = true) }

    fun levelUp() = update { state ->
        val profile = state.plan?.profile
        val next = profile?.level?.next
        if (profile == null || next == null) state
        else state.copy(plan = PlanEngine.generate(profile.copy(level = next)), completionAcknowledged = false)
    }

    fun setLang(lang: Lang) = update { it.copy(lang = lang) }
    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun logWeight(weightKg: Double, onSaved: () -> Unit = {}, timestampMillis: Long = System.currentTimeMillis()) {
        if (weightKg !in UserProfile.WEIGHT_RANGE || timestampMillis > System.currentTimeMillis()) return
        update(onSaved) { state ->
            if (state.plan == null) state else {
                val history = (state.weightHistory.filterNot { it.timestampMillis == timestampMillis } + WeightEntry(timestampMillis, weightKg))
                    .sortedBy { it.timestampMillis }
                val weight = history.lastOrNull()?.takeIf { it.timestampMillis == timestampMillis }?.weightKg
                recalibrate(state.copy(weightHistory = history), weight)
            }
        }
    }

    private fun recalibrate(state: StoredState, weight: Double?): StoredState {
        val profile = state.plan?.profile ?: return state
        if (weight == null || profile.weightKg == weight) return state
        return state.copy(plan = PlanEngine.generate(profile.copy(weightKg = weight)), completionAcknowledged = false)
    }
}
