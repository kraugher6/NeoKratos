package com.example.neokratos.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.repository.AnalyticsRepository

/**
 * Factory for creating AnalyticsViewModel with dependencies.
 */
class AnalyticsViewModelFactory(
    private val analyticsRepository: AnalyticsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(analyticsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}