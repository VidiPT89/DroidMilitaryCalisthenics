package dev.ividi.militarycalisthenics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ividi.militarycalisthenics.data.PlanRepository
import dev.ividi.militarycalisthenics.model.TrainingPlan
import dev.ividi.militarycalisthenics.model.UserProfile
import dev.ividi.militarycalisthenics.model.WeeklyPlan
import dev.ividi.militarycalisthenics.planengine.PlanEngine
import dev.ividi.militarycalisthenics.ui.Lang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PlanRepository) : ViewModel() {

    private val _plan = MutableStateFlow<TrainingPlan?>(null)
    val plan: StateFlow<TrainingPlan?> = _plan.asStateFlow()

    private val _lang = MutableStateFlow(Lang.PT)
    val lang: StateFlow<Lang> = _lang.asStateFlow()

    init {
        viewModelScope.launch {
            _plan.value = repository.currentPlan()
        }
        viewModelScope.launch {
            repository.langFlow.collect { _lang.value = it }
        }
    }

    fun generatePlan(profile: UserProfile) {
        val generated = PlanEngine.generate(profile)
        _plan.value = generated
        viewModelScope.launch { repository.savePlan(generated) }
    }

    fun resetProfile() {
        _plan.value = null
        viewModelScope.launch { repository.clearPlan() }
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

    fun setLang(lang: Lang) {
        _lang.value = lang
        viewModelScope.launch { repository.setLang(lang) }
    }
}
