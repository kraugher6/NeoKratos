package com.example.neokratos.ui.screen.bodymetrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.neokratos.data.repository.BodyMetricsRepository

/**
 * Factory for creating BodyMetricsViewModel with dependencies.
 */
class BodyMetricsViewModelFactory(
    private val bodyMetricsRepository: BodyMetricsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BodyMetricsViewModel::class.java)) {
            return BodyMetricsViewModel(bodyMetricsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}