package dev.ividi.militarycalisthenics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.ividi.militarycalisthenics.data.PlanRepository

class MainViewModelFactory(private val repository: PlanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(repository) as T
    }
}
